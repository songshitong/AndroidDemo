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
#include <sys/types.h>
#include <dirent.h>
#include <cstdlib>
#include <condition_variable>
#include <mutex>
#include <atomic>
#include "nativelog.h"
#include "log_buffer.h"
#include "native-lib.h"
#include <fstream>
#include <vector>

static LogBuffer *log_buff = nullptr; //存储buffer
static std::string logFileDir;
static std::string logFilePath;
static volatile bool log_close = true; //日志是否关闭

static std::condition_variable cond_buffer_async;
static std::mutex mutex_buffer_async;
static std::atomic_flag bufferChangeLock = ATOMIC_FLAG_INIT; //标记buffer正在变更
pthread_t pthread;
int fileMaxL = 10 * 1024 * 1024;
int mMaxStorage = 100 * 1024 * 1024;
char *fileNameP;
static const char *FILE_EXTENSION = ".log";
static const char *FILE_TMP_EXTENSION = ".tmp";
char *logExtraInfo;

std::string createFilePath(std::string dir);


void checkMaxStorage() {
    struct dirent *entry;
    DIR *dir = opendir(logFileDir.c_str());
    if (dir == nullptr) {
        return;
    }
    int allSize = 0;
    std::vector<FileInfo> fileList;
    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 ||
            strcmp(entry->d_name, "..") == 0 ||
            strlen(entry->d_name) < FILE_NAME_PREFIX_LENGTH)
            continue;

        std::string fullPath;
        fullPath.append(logFileDir);
        fullPath.append("/");
        fullPath.append(entry->d_name);
        struct stat statbuf;
        int result = stat(fullPath.c_str(), &statbuf);
        int size = statbuf.st_size;
        selfLog("file size %d", size);
        if (0 == result) {
            allSize += size;
            std::string name;
            name.append(entry->d_name);
            FileInfo item(name);
            item.size = size;
            item.path = fullPath;
            fileList.push_back(item);
        } else {
            selfLog("file read size error: %s", fullPath.c_str());
        }
    }
    closedir(dir);
    if (allSize > mMaxStorage) {
        selfLog("approach mMaxStorage size:%d , mMaxStorage:%d", allSize, mMaxStorage);
        //排序
        std::sort(fileList.begin(), fileList.end(), FileInfo::compare);
        for (const FileInfo& item: fileList) {
            //遍历中删除
            allSize-= item.size;
            selfLog("淘汰文件,剩余大小:%d 文件大小： 文件名path:%s",allSize,item.size,item.path.c_str());
            int removeResult = remove(item.path.c_str());
            if(0 != removeResult){
                selfLog("文件淘汰失败:%s 原因:%s",item.path.c_str(),strerror(errno));
            }
            if(allSize <= mMaxStorage){
                break;
            }
        }

    }
}

void log2file(const void *_data, size_t _len) {
    if (nullptr == _data || 0 == _len || logFilePath.empty()) {
        return;
    }
    selfLog("start write buffer to file logFilePath:%s",
            logFilePath.c_str());
    struct stat st;
    stat(logFilePath.c_str(), &st);
    int size = st.st_size;
    if (size >= fileMaxL) {
        //文件分割
        selfLog("file approach max length:%d current:%d",
                fileMaxL, size);
        std::string newFileStr = createFilePath(logFileDir);
        logFilePath = newFileStr;
        FILE *create = fopen(logFilePath.c_str(), "w");
        fclose(create);
        selfLog("file approach max length,new file:%s",
                logFilePath.c_str());
        checkMaxStorage();
    }
    FILE *file = fopen(logFilePath.c_str(), "a+");
    if (nullptr == file) {
        selfLog("log2file file open error");
        return;
    }
    if (0 == size && logExtraInfo) {
        selfLog("start write logExtraInfo");
        //文件的第一条
        fwrite(logExtraInfo, strlen(logExtraInfo), 1, file);
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
        if (nullptr == log_buff) break;
        AutoBuffer tmp;
        while (!bufferChangeLock.test_and_set()) {
            //再次校验 !!!
            if (nullptr == log_buff) break;
            //确保只有一个线程在操作log_buff
//            selfLog(  "flush buffer start====");
            log_buff->Flush(tmp);
            bufferChangeLock.clear();
//            selfLog(  "flush buffer end====");
            break;
        }
        if (nullptr != tmp.Ptr()) log2file(tmp.Ptr(), tmp.Length());
        if (log_close) {
            selfLog("async_log_thread exit ====");
            //写入一次，检测退出，不再循环
            pthread_exit(nullptr);
        }
        std::unique_lock<std::mutex> lock(mutex_buffer_async);
        cond_buffer_async.wait(lock);
    }
}


void checkWriteFile(NativeLog *nativeLog) {
    if (!log_buff)return;
    size_t bufferLength = log_buff->GetData().Length();
    if (bufferLength >= nativeLog->cacheBuffer * 1 / 3) {
        selfLog("notify thread");
        cond_buffer_async.notify_all();
    }
}


void NativeLog::init(char *path) {
    if (!log_close) {
        selfLog(
                "appender has already been opened. path:%s ", path);
        return;
    }
    log_close = false;
    logFileDir = path;
    fileMaxL = fileMaxLength;
    fileNameP = fileNamePrefix;
    logExtraInfo = extraInfo;
    mMaxStorage = maxStorage;
    logFilePath = createFilePath(logFileDir);
    FILE *create = fopen(logFilePath.c_str(), "w");
    fclose(create); //创建文件

    std::string tmp;
    tmp.append(logFileDir);
    tmp.append("/");
    tmp.append(fileNameP);
    tmp.append(FILE_TMP_EXTENSION);
    selfLog(
            "tmpfile path:%s ", tmp.c_str());
    int tmpFd = open(tmp.c_str(), O_CREAT | O_RDWR, S_IRWXU);

    if (!tmpFd) {
        selfLog("tmpFd open error");
        return;
    }
    ftruncate(tmpFd, cacheBuffer);//填充文件大小   mmap不能扩展文件长度，这里相当于预先给文件长度，准备一个空架子
    //每次log都进行内存映射和释放，存在性能问题  建立缓存文件，只映射一次即可
    tmpFileStart = (int8_t *) mmap(nullptr, cacheBuffer, PROT_READ | PROT_WRITE, MAP_SHARED,
                                   tmpFd, 0);
    close(tmpFd);
    if (tmpFileStart == MAP_FAILED) {
        selfLog("NativeLog init mmap error");
        //创建buffer
        char *buffer = new char[cacheBuffer];
        log_buff = new LogBuffer(buffer, cacheBuffer, false, "");
    } else {
        log_buff = new LogBuffer(tmpFileStart, cacheBuffer, false, "");
        selfLog("NativeLog init mmap success ptr:%p",
                tmpFileStart);
    }
    int ret = pthread_create(&pthread, nullptr,
                             reinterpret_cast<void *(*)(void *)>(async_log_thread), nullptr);
    if (0 != ret) {
        selfLog("thread create error");
    } else {
        pthread_setname_np(pthread, "async_log_thread");
    }
    //映射完成，校验是否写入文件
    checkWriteFile(this);
}


void NativeLog::log(char *logStr) {
    if (log_close)return;
    size_t length = strlen(logStr);
    while (!bufferChangeLock.test_and_set()) {
        if (nullptr == log_buff) return;
        //确保只有一个线程在操作log_buff
//        selfLog(  "write buffer start====");
        log_buff->Write(logStr, length);
        bufferChangeLock.clear();
//        selfLog(  "write buffer end====");
        break;
    }
    checkWriteFile(this);
}

void NativeLog::writeBuffer(char *logStr) {
    if (log_close)return;
    if (log_buff) {
        log_buff->Write(logStr, strlen(logStr));
    }
}


void NativeLog::closeLog() {
    selfLog("closeLog");
    if (log_close) return;
    log_close = true;
    cond_buffer_async.notify_all();
    pthread_join(pthread, nullptr);  //等待子线程完成

    if (tmpFileStart) {
        munmap(tmpFileStart, cacheBuffer);
    } else {
        if(nullptr != log_buff){
            delete[] (char *) ((log_buff->GetData()).Ptr());
        }
    }
    //确保只有一个线程在操作log_buff
    delete log_buff;
    log_buff = nullptr;
    selfLog(" delete end====");
}


void NativeLog::flushCache() {
    if (log_close)return;
    selfLog("flushCache 写入日志");
    cond_buffer_async.notify_all();//执行一次写入
    selfLog("flushCache 写入日志完成");
}

void NativeLog::flushCache(char *str) {
    log(str);
    flushCache();
}


bool FileInfo::compare(FileInfo a, FileInfo b) {
    int  result = strcmp(a.time.c_str(),b.time.c_str());
    if(result<0){
        return true;
    }else{
        return false;
    }
}
