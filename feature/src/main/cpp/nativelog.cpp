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
void NativeLog::init(char *path) {
  int fd = open(path,O_RDWR); //todo 待关闭
  int offset =0;
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init file fd %d",fd);

  struct stat st{};
  stat(path, &st);
  long long int size = st.st_size;
  if(0 == size ){
    write(fd, "", 1);
    size=1;
  }
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init filesize %lld",size);
  fileStart = static_cast<char *>(mmap(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, offset));
  if(fileStart == MAP_FAILED){
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap error");
  } else{
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "NativeLog init mmap success ptr:%p",fileStart);
  }
}

void NativeLog::log(char *logStr) {
  size_t length = strlen(logStr);
  __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "native log filePtr:%p  length:%d",fileStart,length);
  memcpy(fileStart, logStr, length);
}

