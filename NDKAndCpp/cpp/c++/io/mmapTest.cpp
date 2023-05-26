//
// Created by songshitong on 2023/5/26.
//

#include "mmapTest.h"
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include<unistd.h>
using namespace std;
int main() {
    char data[] = "test_data";
    int len = strlen(data);
    int fd = open("test.txt", O_RDWR | O_CREAT, 00777);	// 不存在则创建
    lseek(fd, len - 1, SEEK_END);		// 定义空文件长度
    write(fd, "", 1);					// 文件最后写入一个空字符
    char *buffer = (char *) mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);
    memcpy(buffer, data, len);
    munmap(buffer, len);
}
