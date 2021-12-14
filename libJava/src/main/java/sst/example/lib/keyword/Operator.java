package sst.example.lib.keyword;

public class Operator {

    public static void main(String[] args) {

        //移位运算符
//        java中有三种移位运算符
//        <<      :     左移运算符，num << 1,相当于num乘以2
//        >>      :     右移运算符，num >> 1,相当于num除以2
//        >>>    :     无符号右移，忽略符号位，空位都以0补齐
        // 运算符的优先级
        //优先级	运算符	   结合性
        //1	   ()、[]、{}	    从左向右
        //2	    !、+、-、~、++、--	从右向左
        //3	    *、/、%	     从左向右
        //4	     +、-	    从左向右
        //5	     «、»、>>>	从左向右
        //6	   <、<=、>、>=、instanceof	从左向右
        //7	     ==、!=	          从左向右
        //8	       &	     从左向右
        //9	       ^	     从左向右
        //10	     |	        从左向右
        //11	   &&	       从左向右
        //12	   ||	       从左向右
        //13	   ?:	       从右向左
        //14	   =、+=、-=、*=、/=、&=、|=、^=、~=、«=、»=、>>>=	从右向左

        //位运算的优先级较低，最好增加()
        int left =10;
        int right =20;
        int mid = left+((right-left)>>2);  //15
        int num = left+(right-left)>>2; //10
    }

    int getNum1(){
        int i=0;
        //返回0  i++是先用后加，返回i
        return  i++;
    }
    int getNum2(){
        int i=0;
        //返回1  ++i是先加后用，返回i+1
        return ++i;
    }
}
