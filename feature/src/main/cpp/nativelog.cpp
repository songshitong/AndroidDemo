//
// Created by songshitong on 2023/5/25.
//
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <android/log.h>
#include <string.h>
#include <stdio.h>
#include<unistd.h>

#include "nativelog.h"
static const unsigned int kBufferBlockLength = 150 * 1024; //todo 配置层

void NativeLog::init(char *path) {
  logFilePath = path;
  logFileFD = open(path,O_RDWR,S_IRWXU);
}

void NativeLog::log(char *logStr) {
  size_t length = strlen(logStr);
//  // lseek将文件指针往后移动length-1位
//  lseek(logFileFD,length-1,SEEK_END);
//  // 从指针处写入一个空字符；
//  write(logFileFD, "", 1);

  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init file fd %d",logFileFD);
  int fileSize = lseek(logFileFD, 0, SEEK_END); //获取文件大小
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init filesize %d", fileSize);
  int targetSize = fileSize+length;
  ftruncate(logFileFD, targetSize);//填充文件大小   mmap不能扩展文件长度，这里相当于预先给文件长度，准备一个空架子

  fileStart = (int8_t *)mmap(nullptr, targetSize, PROT_READ | PROT_WRITE, MAP_SHARED,
                                            logFileFD, 0);
  if(fileStart == MAP_FAILED){
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap error");
  } else{
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap success ptr:%p",fileStart);
  }
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "native log filePtr:%p  length:%d",fileStart,length);

  if(nullptr == fileStart){
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "write data err ptr: %p",fileStart);
    return;
  }
  memcpy(fileStart+fileSize, logStr, length);

  //每次log都进行内存映射和释放，存在性能问题
}

void NativeLog::closeLog() {
    if(logFileFD){
      close(logFileFD);
    }
    if(fileStart){
      munmap(fileStart, kBufferBlockLength);
    }
}

