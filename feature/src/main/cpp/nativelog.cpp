//
// Created by songshitong on 2023/5/25.
//
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <android/log.h>
#include <cstring>
#include <cstdio>
#include <unistd.h>
#include <pthread.h>
#include <condition_variable>
#include <mutex>
#include <atomic>
#include "nativelog.h"
#include "log_buffer.h"

static const unsigned int kBufferBlockLength = 150 * 1024; //todo 配置层
static LogBuffer *log_buff = nullptr; //存储buffer
static std::string logFilePath;
static volatile bool log_close = true; //日志是否关闭

static std::condition_variable cond_buffer_async;
static std::mutex mutex_buffer_async;
static std::atomic_flag bufferChangeLock = ATOMIC_FLAG_INIT; //标记buffer正在变更
static const char* TAG = "AFOLOG";

void log2file(const void *_data, size_t _len, bool _move_file) {
    if (nullptr == _data || 0 == _len || logFilePath.empty()) {
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, TAG, "start write buffer to file logFilePath:%s",
                        logFilePath.c_str());
    //文件分割 todo
    FILE *file = fopen(logFilePath.c_str(), "a+");
    if (nullptr == file) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "log2file file open error");
        return;
    }
    fwrite(_data, _len, 1, file);
    fclose(file);
}

void async_log_thread() {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "async_log_thread param %s",
                        logFilePath.c_str());
    while (true) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "file write thread run....");
        if (nullptr == log_buff) break;
        if (log_close) break;
        AutoBuffer tmp;
        while (!bufferChangeLock.test_and_set()){
            //确保只有一个线程在操作log_buff
            __android_log_print(ANDROID_LOG_ERROR, TAG, "flush buffer start====");
            log_buff->Flush(tmp);
            bufferChangeLock.clear();
            __android_log_print(ANDROID_LOG_ERROR, TAG, "flush buffer end====");
            break;
        }
        if (nullptr != tmp.Ptr()) log2file(tmp.Ptr(), tmp.Length(), true);
        std::unique_lock<std::mutex> lock(mutex_buffer_async);
        cond_buffer_async.wait(lock);
    }
}


void checkWriteFile() {
    if(!log_buff)return;
    size_t bufferLength = log_buff->GetData().Length();
    __android_log_print(ANDROID_LOG_ERROR, TAG, "buffer now length %d", bufferLength);
    if (bufferLength >= kBufferBlockLength * 1 / 3) { //todo 单条log进行分割
        cond_buffer_async.notify_all();
    }
}


NativeLog::NativeLog(char *path) {
    init(path);
}

void NativeLog::init(char *path) {
    if (!log_close) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,
                            "appender has already been opened. path:%s ", path);
        return;
    }
    log_close = false;
    logFilePath = path;
    char prefix[] = ".tmp";
    char *tmpPath = (char *) malloc(strlen(path) + strlen(prefix));
    strcpy(tmpPath, path);
    strcat(tmpPath, prefix);
    int tmpFd = open(tmpPath, O_CREAT | O_RDWR, S_IRWXU); //todo 建立临时文件
//  __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init file fd %d",logFileFD);
//  int fileSize = lseek(logFileFD, 0, SEEK_END); //获取文件大小
//  __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init filesize %d", fileSize);
//  int targetSize = fileSize+length;
    if (!tmpFd) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "tmpFd open error");
        return;
    }
    ftruncate(tmpFd, kBufferBlockLength);//填充文件大小   mmap不能扩展文件长度，这里相当于预先给文件长度，准备一个空架子
    //每次log都进行内存映射和释放，存在性能问题  建立缓存文件，只映射一次即可
    tmpFileStart = (int8_t *) mmap(nullptr, kBufferBlockLength, PROT_READ | PROT_WRITE, MAP_SHARED,
                                   tmpFd, 0);
    close(tmpFd);
    if (tmpFileStart == MAP_FAILED) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init mmap error");
        //创建buffer
        char *buffer = new char[kBufferBlockLength];
        log_buff = new LogBuffer(buffer, kBufferBlockLength, false, "");
    } else {
        log_buff = new LogBuffer(tmpFileStart, kBufferBlockLength, false, "");
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init mmap success ptr:%p",
                            tmpFileStart);
    }
    int threadId;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "init thread param:%s ", path);
    int ret = pthread_create(reinterpret_cast<pthread_t *>(&threadId), nullptr,
                             reinterpret_cast<void *(*)(void *)>(async_log_thread), nullptr);
    if (0 != ret) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "thread create error");
    }
    //映射完成，校验是否写入文件
    checkWriteFile();
}


void NativeLog::log(char *logStr) {
    if (nullptr == log_buff) return;
    if (log_close)return;
    size_t length = strlen(logStr);
    while (!bufferChangeLock.test_and_set()){
        //确保只有一个线程在操作log_buff
        __android_log_print(ANDROID_LOG_ERROR, TAG, "write buffer start====");
        log_buff->Write(logStr, length);
        bufferChangeLock.clear();
        __android_log_print(ANDROID_LOG_ERROR, TAG, "write buffer end====");
        break;
    }
    checkWriteFile();
}


void NativeLog::closeLog() {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "closeLog");
    if (log_close) return;
    log_close = true;
    delete log_buff;
    log_buff = nullptr;
//    delete cond_buffer_async;
//    delete mutex_buffer_async;
    if (tmpFileStart) {
        munmap(tmpFileStart, kBufferBlockLength);
    }
    pthread_exit(nullptr);
}







