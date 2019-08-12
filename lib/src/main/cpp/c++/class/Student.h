//
// Created by ISS Mac on 2019-06-28.
//

//ifndef 定义宏，防止被多次引用
// ifndef如果没有被引用  define进行声明

#ifndef ANDROIDDEMO_STUDENT_H
#define ANDROIDDEMO_STUDENT_H



class Student {
  int i;
  //友元函数  可以修改类的私有成员
  friend void test(Student*);
  //友元类 可以访问类所有的私有成员，不用再使用时定义多个友元函数
  friend class Teacher;
  public:
     Student(int i,int j);//无参构造器
     ~Student();//析构函数
     void setJ(int j);
     void getJ(){
       return j;
     }
     void up() const;
  private:
    int j;
  protected:
    int k;
    char m;


};

class Teacher{
  public:
   void call(Student* stu){
     stu->j =200;
   }
}

#endif //ANDROIDDEMO_STUDENT_H
