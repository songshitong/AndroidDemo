#include "stdio.h"

int main(){
//   C 语言把任何非零和非空的值假定为 true，把零或 null 假定为 false  
  //c中没有bool类型
   int a =0;
   int b =1;
   if(b){
       printf("b为true的条件\n");
   }
   if(a){
        printf("a为true的条件\n");
   }else{
       printf("a为false的条件\n");
   }


   
//    三元运算符
    (a>b)?printf("a>b\n"):printf("a<b\n");

    //switch case
    int switchA=2;
    printf("input integer number: ");
    scanf("%d",&switchA); //运行scanf要使用vscode的run c/c++file，不然没有命令行的输入
    switch(switchA)
    {
        case 1:printf("Monday\n");
        break;
        case 2:printf("Tuesday\n");
        break;
        case 3:printf("Wednesday\n");
        break;
        case 4:printf("Thursday\n");
        break;
        case 5:printf("Friday\n");
        break;
        case 6:printf("Saturday\n");
        break;
        case 7:printf("Sunday\n");
        break;
        default:printf("error\n");
    }

}