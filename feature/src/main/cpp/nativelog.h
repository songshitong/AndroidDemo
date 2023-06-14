//
// Created by songshitong on 2023/5/25.
//


const int FILE_NAME_PREFIX_LENGTH = 23; //yyyyMM-dd-HH-mm-ss.log的长度
class NativeLog {
public:
    int cacheBuffer;
    char *logSpliterator;
    char *strSplitter;
    int singleLogUnit;
    int fileMaxLength;
    char *fileNamePrefix;
    char *extraInfo;
    int maxStorage;
    void log(char *logStr);

    void closeLog();

    static void flushCache();

    void flushCache(char *str);

    void init(char *path);

    void writeBuffer(char *logStr);

    ~NativeLog() {
        if (logSpliterator) {
            delete logSpliterator;
        }
        if (strSplitter) {
            delete strSplitter;
        }
        if (fileNamePrefix) {
            delete fileNamePrefix;
        }
        if (extraInfo) {
            delete extraInfo;
        }
    }

private:
    int8_t *tmpFileStart{};
};

class FileInfo{
public:
    int size=0;
    std::string name; //名称
    std::string path; //路径

    FileInfo(std::string mName){
        this->name = mName;
        if(!name.empty()&&name.length()>FILE_NAME_PREFIX_LENGTH){
           time = name.substr(name.length()-FILE_NAME_PREFIX_LENGTH);
        }
    }
    static bool compare(FileInfo a,FileInfo b);
private:
    std::string time;
};
