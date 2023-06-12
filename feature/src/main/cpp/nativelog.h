//
// Created by songshitong on 2023/5/25.
//


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

    //日志控件内部打印
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
    std::string name;
    FileInfo(std::string aname){
        this->name = aname;
        if(!name.empty()&&name.length()>23){
           time = name.substr(name.length()-23);
        }
    }
    static bool compare(FileInfo a,FileInfo b);
private:
    std::string time;
};
