//
// Created by songshitong on 2023/5/25.
//


class NativeLog{
public:
     void log(char* logStr);
     void closeLog();
     static void flushCache();
     int cacheBuffer;
     char* logSpliterator;
     char* strSplitter;
     int singleLogUnit;
     int fileMaxLength;
     char* fileNamePrefix;
    void init(char* path);

    ~NativeLog(){
        flushCache();
        closeLog();
        if(logSpliterator){
            delete logSpliterator;
        }
        if(strSplitter){
            delete strSplitter;
        }
        if(fileNamePrefix){
            delete fileNamePrefix;
        }
    }
private:
    int8_t* tmpFileStart{};


};
