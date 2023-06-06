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
#include <cstdlib>
#include <condition_variable>
#include <mutex>
#include <atomic>
#include "nativelog.h"
#include "log_buffer.h"

static LogBuffer *log_buff = nullptr; //存储buffer
static std::string logFileDir;
static std::string logFilePath;
static volatile bool log_close = true; //日志是否关闭

static std::condition_variable cond_buffer_async;
static std::mutex mutex_buffer_async;
static std::atomic_flag bufferChangeLock = ATOMIC_FLAG_INIT; //标记buffer正在变更
static const char *TAG = "AFOLOG";
pthread_t pthread;
int fileMaxL = 10 * 1024 * 1024;
char *fileNameP = new char[]{};
static const char *FILE_EXTENSION = ".log";
static const char *FILE_TMP_EXTENSION = ".tmp";

std::string createFilePath(std::string dir);

void log2file(const void *_data, size_t _len) {
    if (nullptr == _data || 0 == _len || logFilePath.empty()) {
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, TAG, "start write buffer to file logFilePath:%s",
                        logFilePath.c_str());
    struct stat st;
    stat(logFilePath.c_str(), &st);
    int size = st.st_size;
    if (size >= fileMaxL) {
        //文件分割
        __android_log_print(ANDROID_LOG_ERROR, TAG, "file approach max length:%d current:%d",
                            fileMaxL, size);
        std::string newFileStr = createFilePath(logFileDir);
        logFilePath = newFileStr;
        FILE *create = fopen(logFilePath.c_str(), "w");
        fclose(create);
        __android_log_print(ANDROID_LOG_ERROR, TAG, "file approach max length,new file:%s",
                            logFilePath.c_str());
    }
    FILE *file = fopen(logFilePath.c_str(), "a+");
    if (nullptr == file) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "log2file file open error");
        return;
    }
    fwrite(_data, _len, 1, file);
    fclose(file);
}

std::string createFilePath(std::string dir) {
    std::string newFileStr;
    newFileStr.append(dir);
    newFileStr.append("/");
    newFileStr.append(fileNameP);
    newFileStr.append("-");
    time_t t = time(nullptr);
    char tmp[21] = {'\0'};
    strftime(tmp, sizeof(tmp), "%Y-%m-%d-%H-%M-%S", localtime(&t));
    newFileStr.append(tmp);
    newFileStr.append(FILE_EXTENSION);
    return newFileStr;
}

void async_log_thread() {
    while (true) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "file write thread run....");
        if (nullptr == log_buff) break;
        AutoBuffer tmp;
        while (!bufferChangeLock.test_and_set()) {
            //确保只有一个线程在操作log_buff
            __android_log_print(ANDROID_LOG_ERROR, TAG, "flush buffer start====");
            log_buff->Flush(tmp);
            bufferChangeLock.clear();
            __android_log_print(ANDROID_LOG_ERROR, TAG, "flush buffer end====");
            break;
        }
        if (nullptr != tmp.Ptr()) log2file(tmp.Ptr(), tmp.Length());
        if (log_close) break; //写入一次，检测退出，不再循环
        std::unique_lock<std::mutex> lock(mutex_buffer_async);
        cond_buffer_async.wait(lock);
    }
}


void checkWriteFile(NativeLog *nativeLog) {
    if (!log_buff)return;
    size_t bufferLength = log_buff->GetData().Length();
    __android_log_print(ANDROID_LOG_ERROR, TAG, "buffer now length %d", bufferLength);
    if (bufferLength >= nativeLog->cacheBuffer * 1 / 3) {
        cond_buffer_async.notify_all();
    }
}


void NativeLog::init(char *path) {
    if (!log_close) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,
                            "appender has already been opened. path:%s ", path);
        return;
    }
    log_close = false;
    logFileDir = path;
    fileMaxL = fileMaxLength;
    fileNameP = fileNamePrefix;
    logFilePath = createFilePath(logFileDir);
    std::string tmp;
    tmp.append(logFileDir);
    tmp.append("/");
    tmp.append(fileNameP);
    tmp.append(FILE_TMP_EXTENSION);
    __android_log_print(ANDROID_LOG_ERROR, TAG,
                        "tmpfile path:%s ", tmp.c_str());
    int tmpFd = open(tmp.c_str(), O_CREAT | O_RDWR, S_IRWXU);

    if (!tmpFd) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "tmpFd open error");
        return;
    }
    ftruncate(tmpFd, cacheBuffer);//填充文件大小   mmap不能扩展文件长度，这里相当于预先给文件长度，准备一个空架子
    //每次log都进行内存映射和释放，存在性能问题  建立缓存文件，只映射一次即可
    tmpFileStart = (int8_t *) mmap(nullptr, cacheBuffer, PROT_READ | PROT_WRITE, MAP_SHARED,
                                   tmpFd, 0);
    close(tmpFd);
    if (tmpFileStart == MAP_FAILED) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init mmap error");
        //创建buffer
        char *buffer = new char[cacheBuffer];
        log_buff = new LogBuffer(buffer, cacheBuffer, false, "");
    } else {
        log_buff = new LogBuffer(tmpFileStart, cacheBuffer, false, "");
        __android_log_print(ANDROID_LOG_ERROR, TAG, "NativeLog init mmap success ptr:%p",
                            tmpFileStart);
    }
    __android_log_print(ANDROID_LOG_ERROR, TAG, "init thread param:%s ", path);
    int ret = pthread_create(&pthread, nullptr,
                             reinterpret_cast<void *(*)(void *)>(async_log_thread), nullptr);
    if (0 != ret) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "thread create error");
    }
    //映射完成，校验是否写入文件
    checkWriteFile(this);
}


void NativeLog::log(char *logStr) {
    if (nullptr == log_buff) return;
    if (log_close)return;
    size_t length = strlen(logStr);
    while (!bufferChangeLock.test_and_set()) {
        //确保只有一个线程在操作log_buff
        __android_log_print(ANDROID_LOG_ERROR, TAG, "write buffer start====");
        log_buff->Write(logStr, length);
        bufferChangeLock.clear();
        __android_log_print(ANDROID_LOG_ERROR, TAG, "write buffer end====");
        break;
    }
    checkWriteFile(this);
}


void NativeLog::closeLog() {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "closeLog");
    if (log_close) return;
    log_close = true;
    while (!bufferChangeLock.test_and_set()) {
        //确保只有一个线程在操作log_buff
        __android_log_print(ANDROID_LOG_ERROR, TAG, "flush delete start====");
        delete log_buff;
        log_buff = nullptr;
        bufferChangeLock.clear();
        __android_log_print(ANDROID_LOG_ERROR, TAG, "flush delete end====");
        break;
    }

//    delete cond_buffer_async;
//    delete mutex_buffer_async;
//    if (tmpFileStart) { //todo 确定使用mmap还是内存 看看需要删除  内存关闭前需要写一次文件
//        munmap(tmpFileStart, kBufferBlockLength);
//    }
    delete fileNamePrefix;
    delete FILE_EXTENSION;
    delete TAG;
    delete[] fileNameP;
    pthread_exit(nullptr);
}


void NativeLog::flushCache() {
    if (log_close)return;
    log_close = true;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "flushCache 写入日志");
    cond_buffer_async.notify_all();//执行一次写入
    pthread_join(pthread, nullptr);  //等待子线程完成
    __android_log_print(ANDROID_LOG_ERROR, TAG, "flushCache 写入日志完成");
}




