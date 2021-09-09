//
// Created by ISS Mac on 2019-06-28.
//

#ifndef ANDROIDDEMO_OPERATOR_H
#define ANDROIDDEMO_OPERATOR_H
class Operator{
  public:
    int i;
    //可以重载大部分内置运算符 https://www.runoob.com/cplusplus/cpp-overloading.html
    // + - * / %  || && ! 等
    Operator operator+(const Operator& p){
       Operator temp;
       temp.i = this->i+p.i;

       return temp;

    }

}



#endif //ANDROIDDEMO_OPERATOR_H
