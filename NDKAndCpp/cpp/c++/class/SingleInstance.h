//
// Created by ISS Mac on 2019-06-28.
//

#ifndef ANDROIDDEMO_SINGLEINSTANCE_H
#define ANDROIDDEMO_SINGLEINSTANCE_H


//c++实现单例
class SingleInstance {
  private:
    static SingleInstance* instance;
    SingleInstance();
  public:
    static  SingleInstance* getInstance();

};



#endif //ANDROIDDEMO_SINGLEINSTANCE_H
