//
// Created by songshitong on 2023/5/25.
//


class NativeLog{
public:
     NativeLog(char* path);
     void init(char* path);
     static void log(char* logStr);
     void closeLog();
    ~NativeLog(){
        //todo close前 缓存需要写入
        closeLog();
    }
private:
    int8_t* fileStart{};
    int logFileFD{};
    char *logFilePath{};

};
