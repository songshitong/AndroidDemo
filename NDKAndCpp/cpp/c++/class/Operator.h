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

};

int main(){
//    箭头与点的区别
//    箭头（->）：左边必须为指针；
//    点号（.）：左边必须为实体
    Operator *op; //声明一个指针
    (*op).i;
    op->i;

    Operator o;//声明一个对象
    o.i;
    (&o)->i; //获取对象的地址然后使用
}


#endif //ANDROIDDEMO_OPERATOR_H
