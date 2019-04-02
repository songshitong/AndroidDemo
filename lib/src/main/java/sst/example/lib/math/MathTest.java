package sst.example.lib.math;

public class MathTest {
    public static void main(String[] args) {

        System.out.println("e "+Math.E);
        System.out.println("pi "+Math.PI);

        //平方根
        System.out.println("Math.sqrt(4) "+Math.sqrt(4));

        //立方根
        System.out.println("Math.cbrt(8) "+Math.cbrt(8));

        //先计算x的平方与y的平方之和，然后开方   例如知道三角形的两条直角边求斜边
        System.out.println("Math.hypot(8) "+Math.hypot(3,4));

        //a 的 b次方
        System.out.println("Math.pow(8) "+Math.pow(3,4));

        //e 的x次方      e=2.718
        System.out.println("Math.exp(1) "+Math.exp(1));

        //两个数的最大值
        System.out.println("Math.max(10,11) "+Math.max(10,11));

        //两个数的最小值
        System.out.println("Math.min(10,11) "+Math.min(10,11));


        //求绝对值
        System.out.println("Math.abs(1) "+Math.abs(1));
        System.out.println("Math.abs(-1) "+Math.abs(-1));

        //返回大于等于参数x的最小整数,即对浮点数向上取整
        //-1.0
        System.out.println("Math.abs(-1.3) "+Math.ceil(-1.3));
        System.out.println("Math.abs(-1.7) "+Math.ceil(-1.7));
        //2.0
        System.out.println("Math.abs(1.3) "+Math.ceil(1.3));
        System.out.println("Math.abs(1.7) "+Math.ceil(1.7));

        //返回小于等于参数x的最大整数,即对浮点数向下取整
        //-2.0
        System.out.println("Math.floor(-1.3) "+Math.floor(-1.3));
        System.out.println("Math.floor(-1.7) "+Math.floor(-1.7));
        //1.0
        System.out.println("Math.floor(1.3) "+Math.floor(1.3));
        System.out.println("Math.floor(1.7) "+Math.floor(1.7));


        //随机数 [0,1)
        System.out.println("Math.random() "+Math.random());
//        [0,100)
        System.out.println("Math.random()*100 "+Math.random()*100);


        //四舍五入rint double->double
        System.out.println("Math.rint(1.3) "+Math.rint(1.3));
        System.out.println("Math.rint(1.7) "+Math.rint(1.7));

        //四舍五入round float->int  double->long
        System.out.println("Math.round(1.3) "+Math.round(1.3));
        System.out.println("Math.round(1.7) "+Math.round(1.7));


        //nextUp()方法返回比第一个参数大的最近浮点数
        //实数是非常密集的。任意两个不同的实数中间都可以出现其他实数。
        // 但浮点数则不是这样。对于浮点数和双精度数，也存在下一个浮点数；连续的浮点数和双精度数之间存在最小的有限距离
        float f = 1.0f;
        double d = 1.0;
        //0.0000001    float最小精度单位0.0000001  unit of least precision
        System.out.println("Math.nextUp(f) "+Math.nextUp(f));
        //0.0000000000000002 double最小精度单位 0.0000000000000002；
        System.out.println("Math.nextUp(d) "+Math.nextUp(d));

        //        //nextUp()方法返回比第一个参数小的最近浮点数
        System.out.println("Math.nextDown(f) "+Math.nextDown(f));
        System.out.println("Math.nextDown(d) "+Math.nextDown(d));


        //向后查找小于指定数字的最近浮点数
//        如果 direction大于 start，则 nextAfter()返回在 start之上的下一个数字。
//        如果 direction小于 start，则 nextAfter()返回在 start之下的下一个数字。如果 direction等于 start，则 nextAfter()返回 start本身
        System.out.println("Math.nextAfter(f,d*2) "+Math.nextAfter(f,f*2));
        System.out.println("Math.nextAfter(f,f/2) "+Math.nextAfter(f,f/2));
        System.out.println("Math.nextAfter(d,d*2) "+Math.nextAfter(d,d*2));
        System.out.println("Math.nextAfter(d,d/2) "+Math.nextAfter(d,d/2));

        //弧度 ->角度
        System.out.println("Math.toDegrees(Math.PI) "+Math.toDegrees(Math.PI));
        //角度->弧度
        System.out.println("Math.toRadians(180) "+Math.toRadians(180));

        //三角函数
        //sin
        System.out.println("Math.sin(Math.PI/2) "+Math.sin(Math.PI/2));
        System.out.println("Math.sin(Math.PI/4) "+Math.sin(Math.PI/4));
        //cos
        System.out.println("Math.cos(Math.PI/4) "+Math.cos(Math.PI/4));
        System.out.println("Math.cos(Math.PI/2) "+Math.cos(Math.PI/2));
        //tan
        System.out.println("Math.tan(Math.PI/2)"+Math.tan(Math.PI/2));
        System.out.println("Math.tan(Math.PI/4)"+Math.tan(Math.PI/4));


        //反三角函数
        //asin
        System.out.println("Math.asin(Math.PI/2) "+Math.asin(Math.PI/2));
        System.out.println("Math.asin(Math.PI/4) "+Math.asin(Math.PI/4));
        //acos
        System.out.println("Math.acos(Math.PI/4) "+Math.acos(Math.PI/4));
        System.out.println("Math.acos(Math.PI/2) "+Math.acos(Math.PI/2));
        //atan
        System.out.println("Math.atan(Math.PI/2)"+Math.atan(Math.PI/2));
        System.out.println("Math.atan(Math.PI/4)"+Math.atan(Math.PI/4));


        //log
        System.out.println("Math.log(0) "+Math.log(0));
        System.out.println("Math.log(1) "+Math.log(1));
        System.out.println("Math.log(10) "+Math.log(10));

        //log1p = ln(x+1)
        System.out.println("Math.log1p(0) "+Math.log1p(0));

        //返回 以10為底的對數的 double 值
        //如果參數為NaN或小於零，那麼結果為NaN。
        //如果參數為正無窮大，那麼結果為正無窮大。
        //如果參數為正零或負零，那麼結果是負無窮大。
        //如果參數等於10n整數n，那麼結果是n
        System.out.println("Math.log10(5)"+Math.log10(5));
        System.out.println("Math.log10(10)"+Math.log10(10));



    }
}
