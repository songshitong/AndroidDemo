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
#include <mutex>
#include "nativelog.h"
#include "log_buffer.h"
static const unsigned int kBufferBlockLength = 150 * 1024; //todo 配置层
static LogBuffer* log_buff = nullptr; //存储buffer
static volatile bool log_close = true; //日志是否关闭

//static Condition cond_buffer_async;
//static Mutex mutex_buffer_async;
static void async_log_thread();

NativeLog::NativeLog(char *path) {
    init(path);
}

 void NativeLog::init(char *path) {
    if (!log_close) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd","appender has already been opened. path:%s ", path);
        return;
    }
  log_close = false;
  logFilePath = path;
  logFileFD = open(path,O_RDWR,S_IRWXU); //todo 建立临时文件
//  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init file fd %d",logFileFD);
//  int fileSize = lseek(logFileFD, 0, SEEK_END); //获取文件大小
//  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init filesize %d", fileSize);
//  int targetSize = fileSize+length;
  ftruncate(logFileFD, kBufferBlockLength);//填充文件大小   mmap不能扩展文件长度，这里相当于预先给文件长度，准备一个空架子

  //每次log都进行内存映射和释放，存在性能问题  建立缓存文件，只映射一次即可
  fileStart = (int8_t *)mmap(nullptr, kBufferBlockLength, PROT_READ | PROT_WRITE, MAP_PRIVATE,
                             logFileFD, 0);
  if(fileStart == MAP_FAILED){
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap error");
    //创建buffer
    char* buffer = new char[kBufferBlockLength];
    log_buff = new LogBuffer(buffer, kBufferBlockLength, false, "");
  } else{
    PtrBuffer mmapBuffer(fileStart,kBufferBlockLength);
    log_buff = new LogBuffer(&mmapBuffer, kBufferBlockLength, false, "");
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap success ptr:%p",fileStart);
  }
  int threadId;
//  int ret =pthread_create(reinterpret_cast<pthread_t *>(&threadId), nullptr,
//                          reinterpret_cast<void *(*)(void *)>(async_log_thread), nullptr);
//  if(0 != ret){
//      __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "thread create error");
//  }

}

static void log2file(const void* _data, size_t _len, bool _move_file) {
//    if (NULL == _data || 0 == _len || sg_logdir.empty()) { todo 校验dir
//        return;
//    }
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "start write buffer to file");

}

static void async_log_thread() {
//    while (true) {
////        ScopedLock lock_buffer(mutex_buffer_async);
//        if (nullptr == log_buff) break;
//        AutoBuffer tmp;
//        log_buff->Flush(tmp);
////        lock_buffer.unlock();
//
//        if (nullptr != tmp.Ptr())  log2file(tmp.Ptr(), tmp.Length(), true);
//
//        if (log_close) break;
////        cond_buffer_async.wait(15 * 60 * 1000);
//    }
}




 void NativeLog::log(char *logStr) {
  if(nullptr == log_buff) return;
  size_t length = strlen(logStr);
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "log str prt:%p, log_buff prt:%p  length:%d",logStr,log_buff,length);
  log_buff->Write(&"1",strlen("1"));
//  memcpy(fileStart+fileSize, logStr, length);
    if (log_buff->GetData().Length() >= kBufferBlockLength*1/3) { //todo 单条log进行分割
//        cond_buffer_async.notifyAll();
    }

}



void NativeLog::closeLog() {
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "closeLog");
    if (log_close) return;

    log_close = true;
    delete log_buff;
    log_buff = nullptr;
//    delete cond_buffer_async;
//    cond_buffer_async = NULL;
    if(logFileFD){
      close(logFileFD);
    }
    if(fileStart){
      munmap(fileStart, kBufferBlockLength);
    }
    pthread_exit(nullptr);
}







