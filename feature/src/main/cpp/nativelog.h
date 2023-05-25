//
// Created by songshitong on 2023/5/25.
//


class NativeLog{
public:
     void init(char* path);
     void log(char* logStr);
private:
    char* fileStart;
};
