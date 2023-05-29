//
// Created by songshitong on 2023/5/25.
//


class NativeLog{
public:
     void init(char* path);
     void log(char* logStr);
     void closeLog();
private:
    int8_t* fileStart;
    int logFileFD;
    char *logFilePath;
    ~NativeLog(){
        closeLog();
    }
};
