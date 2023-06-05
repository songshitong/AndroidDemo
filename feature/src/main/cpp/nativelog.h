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
    void init(char* path);

    ~NativeLog(){
        //todo close前 缓存需要写入
        closeLog();
    }
private:
    int8_t* tmpFileStart{};


};
