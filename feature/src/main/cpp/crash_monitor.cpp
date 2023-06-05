//
// Created by songshitong on 2023/6/2.
//

#include "crash_monitor.h"
#include <csignal>
#include <unistd.h>
#include <map>
#include <android/log.h>
#include <jni.h>

static void CallOldHandler(int signo, siginfo_t* info, void* context);
static void SignalHandler(int signo, siginfo_t* info, void* context);
std::map<int, struct sigaction> sOldHandlers;


static void SetUpStack() {
    stack_t stack{};
    stack.ss_sp = new(std::nothrow) char[SIGSTKSZ];

    if (!stack.ss_sp) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "fail to alloc stack for crash catching");
        return;
    }
    stack.ss_size = SIGSTKSZ;
    stack.ss_flags = 0;
    if (stack.ss_sp) {
        if (sigaltstack(&stack, nullptr) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "fail to setup signal stack");
        }
    }
}


static  void SetUpSigHandler() {
    struct sigaction action{};
    action.sa_sigaction = SignalHandler;
    action.sa_flags = SA_SIGINFO | SA_ONSTACK;
    int signals[] = {
            SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGSEGV, SIGPIPE
    };
    struct sigaction old_action;
    for (auto signo : signals) {
        if (sigaction(signo, &action, &old_action) == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd",  "fail to set signal handler for signo %d", signo);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd",  " signo register %d", signo);
            if (old_action.sa_handler != SIG_DFL && old_action.sa_handler != SIG_IGN) {
                sOldHandlers[signo] = old_action;
            }
        }
    }
}

static CrashCallG onCrashCallG;

static  void SignalHandler(int signo, siginfo_t* info, void* context) {
    if(onCrashCallG){
        onCrashCallG();
    }
    CallOldHandler(signo, info, context);
//    exit(0);
}

static  void CallOldHandler(int signo, siginfo_t* info, void* context) {
    auto it = sOldHandlers.find(signo);
    if (it != sOldHandlers.end()) {
        if (it->second.sa_flags & SA_SIGINFO) {
            it->second.sa_sigaction(signo, info, context);
        } else {
            it->second.sa_handler(signo);
        }
    }
}


void CrashMonitor::init(CrashCallG call){
    onCrashCallG = call;
    SetUpStack();
    SetUpSigHandler();
}

CrashMonitor::~CrashMonitor() {
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "CrashMonitor destroy");
  if(onCrashCallG){
      onCrashCallG = nullptr;
  }
}


void NativeCrashTest() {
    volatile int* p = nullptr;
    *p = 1;
}


extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_AFOLog_nNativeCrashTest(JNIEnv *env, jobject thiz) {
    NativeCrashTest();
}