//
// Created by songshitong on 2023/6/2.
//

#include "crash_monitor.h"
#include "native-lib.h"
#include <csignal>
#include <unistd.h>
#include <string>
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
        selfLog("fail to alloc stack for crash catching");
        return;
    }
    stack.ss_size = SIGSTKSZ;
    stack.ss_flags = 0;
    if (stack.ss_sp) {
        if (sigaltstack(&stack, nullptr) != 0) {
            selfLog( "fail to setup signal stack");
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
            selfLog( "fail to set signal handler for signo %d", signo);
        } else {
            selfLog(  " signo register %d", signo);
            if (old_action.sa_handler != SIG_DFL && old_action.sa_handler != SIG_IGN) {
                sOldHandlers[signo] = old_action;
            }
        }
    }
}

static CrashCallG onCrashCallG;

static  char*  DumpSignalInfo(int signo,siginfo_t* info) {
    std::string result;
    result.append("catch native crash:\n");
    switch (signo) {
        case SIGILL:
            result.append("signal SIGILL caught\n");
            switch (info->si_code) {
                case ILL_ILLOPC:
                    result.append("illegal opcode\n");
                    break;
                case ILL_ILLOPN:
                    result.append("illegal operand\n");
                    break;
                case ILL_ILLADR:
                    result.append("illegal addressing mode\n");
                    break;
                case ILL_ILLTRP:
                    result.append("illegal trap\n");
                    break;
                case ILL_PRVOPC:
                    result.append("privileged opcode\n");
                    break;
                case ILL_PRVREG:
                    result.append("privileged register\n");
                    break;
                case ILL_COPROC:
                    result.append("coprocessor error\n");
                    break;
                case ILL_BADSTK:
                    result.append("internal stack error\n");
                    break;
                default:
                    result.append("code = ");
                    result.append(std::to_string(info->si_code));
                    result.append("\n");
                    break;
            }
            break;
        case SIGFPE:
            result.append("signal SIGFPE caught\n");
            switch (info->si_code) {
                case FPE_INTDIV:
                    result.append("integer divide by zero\n");
                    break;
                case FPE_INTOVF:
                    result.append("integer overflow\n");
                    break;
                case FPE_FLTDIV:
                    result.append("floating-point divide by zero\n");
                    break;
                case FPE_FLTOVF:
                    result.append("floating-point overflow\n");
                    break;
                case FPE_FLTUND:
                    result.append("floating-point underflow\n");
                    break;
                case FPE_FLTRES:
                    result.append("floating-point inexact result\n");
                    break;
                case FPE_FLTINV:
                    result.append("invalid floating-point operation\n");
                    break;
                case FPE_FLTSUB:
                    result.append("subscript out of range\n");
                    break;
                default:
                    result.append("code =");
                    result.append(std::to_string(info->si_code));
                    result.append("\n");
                    break;
            }
            break;
        case SIGSEGV:
            result.append("signal SIGSEGV caught\n");
            switch (info->si_code) {
                case SEGV_MAPERR:
                    result.append("address not mapped to object\n");
                    break;
                case SEGV_ACCERR:
                    result.append("invalid permissions for mapped object\n");
                    break;
                default:
                    result.append("code =");
                    result.append(std::to_string(info->si_code));
                    result.append("\n");
                    break;
            }
            break;
        case SIGBUS:
            result.append("signal SIGBUS caught\n");
            switch (info->si_code) {
                case BUS_ADRALN:
                    result.append("invalid address alignment\n");
                    break;
                case BUS_ADRERR:
                    result.append("nonexistent physical address\n");
                    break;
                case BUS_OBJERR:
                    result.append("object-specific hardware error\n");
                    break;
                default:
                    result.append("code = ");
                    result.append(std::to_string(info->si_code));
                    result.append("\n");
                    break;
            }
            break;
        case SIGABRT:
            result.append("signal SIGABRT caught\n");
            break;
        case SIGPIPE:
            result.append("signal SIGPIPE caught\n");
            break;
        default:
            result.append("signo  caught:");
            result.append(std::to_string(info->si_signo));
            result.append("\n");
            result.append("code = ");
            result.append(std::to_string(info->si_code));
            result.append("\n");
    }
    result.append("errno = ");
    result.append(std::to_string(info->si_errno));
    result.append("\n");
   return const_cast<char *>(result.c_str());
}

static  void SignalHandler(int signo, siginfo_t* info, void* context) {
    if(onCrashCallG){
        onCrashCallG(DumpSignalInfo(signo,info));//todo 获取native崩溃栈
    }
    CallOldHandler(signo, info, context);
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