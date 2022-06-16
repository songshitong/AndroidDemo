package sst.example.lib;

//基本类型
public class Type {
    public static void main(String[] args) {

        //int的拆箱，装箱
        Integer integer = Integer.valueOf(2);
        int intValue = integer.intValue();
        //List.remove()方法，可以是index也可以是object  如果元素是int时最好进行装箱操作
        //int 输出为二进制
        Integer.toBinaryString(2); //结果10
        outInfo();

        //基础类型转换
//        类型转换都是小范围向大范围转换的，大范围往小范围转化需要用到强制转换，转换后的值会有所损失。
//        不同数据类型精度由小到大排列如下：char-->byte --> short --> int --> long --> float --> double

        //int 转 char
        //1.
        //将整型强制类型转换为字符型，JVM 会把数字当成字符的 ASCII 编码来处理。例如字符 '(' 的 ASCII 编码为 40，所以将整型 40 强制类型转换为字符型，
        //会得到字符 '('。
        int a1 = 40;
        char c = (char) a1;
        //输出：(

        //2.
        //先转成字符串，再转成 char;  此时转为数字的char
        int a = 4;
        String str = String.valueOf(a);
        char[] array = str.toCharArray();
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }
        //输出为4

        //继承的类型转换
        //在继承中，子类可以自动转型为父类，但是父类强制转换为子类时只有当引用类型真正的身份为子类时才会强制转换成功，否则失败
        //声明父类，不能强转为子类  抛出异常java.lang.ClassCastException
//        Food f = new Food();
//        Fruit fruit = (Fruit)f;
//        System.out.println(fruit.name);

        //父类实例为子类，强转为子类
        Food food1 = new Fruit();
        Fruit fruit1 = (Fruit) food1;
        System.out.println(fruit1.name);

        //声明子类，强转为父类  不用强转，直接引用也可以
        Fruit fruit = new Fruit();
        Food food = (Food) fruit;
        System.out.println(food.name);

        //type转bit https://blog.csdn.net/dengzhi_00/article/details/52885820
        //转为string，然后使用charAt
        byte byte1 = (byte) 0x11;
        System.out.println("byte1 is:"+getBit(byte1));
        //转为16进制字符串 结果是"11"，第一个char:1  第二个char:2
        String hexBytes = bytes2Hex(new byte[]{byte1});
        System.out.printf("byte1 is:"+hexBytes);
        for(char temp:hexBytes.toCharArray()){
            System.out.printf("byte1 char:"+temp);
        }
    }
    public static String getBit(byte by){
        StringBuffer sb = new StringBuffer();
        sb.append((by>>7)&0x1)
                .append((by>>6)&0x1)
                .append((by>>5)&0x1)
                .append((by>>4)&0x1)
                .append((by>>3)&0x1)
                .append((by>>2)&0x1)
                .append((by>>1)&0x1)
                .append((by>>0)&0x1);
        return sb.toString();
    }
    public static String bytes2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String tmp;
        for (byte b : bytes) {
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString().toUpperCase();
    }



    //输出基本类型信息
    private static void outInfo() {
        //https://www.runoob.com/java/java-basic-datatypes.html
//        byte：
//        byte 数据类型是8位、有符号的，以二进制补码表示的整数；
//        最小值是 -128（-2^7）；
//        最大值是 127（2^7-1）；
//        默认值是 0；
//        byte 类型用在大型数组中节约空间，主要代替整数，因为 byte 变量占用的空间只有 int 类型的四分之一；
//        例子：byte a = 100，byte b = -50
        System.out.println("基本类型：byte 二进制位数：" + Byte.SIZE);
        System.out.println("包装类：java.lang.Byte");
        System.out.println("最小值：Byte.MIN_VALUE=" + Byte.MIN_VALUE);
        System.out.println("最大值：Byte.MAX_VALUE=" + Byte.MAX_VALUE);
        System.out.println();

//        short：
//        short 数据类型是 16 位、有符号的以二进制补码表示的整数
//        最小值是 -32768（-2^15）；
//        最大值是 32767（2^15 - 1）；
//        Short 数据类型也可以像 byte 那样节省空间。一个short变量是int型变量所占空间的二分之一；
//        默认值是 0；
//        例子：short s = 1000，short r = -20000
        System.out.println("基本类型：short 二进制位数：" + Short.SIZE);
        System.out.println("包装类：java.lang.Short");
        System.out.println("最小值：Short.MIN_VALUE=" + Short.MIN_VALUE);
        System.out.println("最大值：Short.MAX_VALUE=" + Short.MAX_VALUE);
        System.out.println();

//        int：
//        int 数据类型是32位、有符号的以二进制补码表示的整数；
//        最小值是 -2,147,483,648（-2^31）；
//        最大值是 2,147,483,647（2^31 - 1）；
//        一般地整型变量默认为 int 类型；
//        默认值是 0 ；
//        例子：int a = 100000, int b = -20000
        System.out.println("基本类型：int 二进制位数：" + Integer.SIZE);
        System.out.println("包装类：java.lang.Integer");
        System.out.println("最小值：Integer.MIN_VALUE=" + Integer.MIN_VALUE);
        System.out.println("最大值：Integer.MAX_VALUE=" + Integer.MAX_VALUE);
        System.out.println();

//        long：
//        long 数据类型是 64 位、有符号的以二进制补码表示的整数；
//        最小值是 -9,223,372,036,854,775,808（-2^63）；
//        最大值是 9,223,372,036,854,775,807（2^63 -1）；
//        这种类型主要使用在需要比较大整数的系统上；
//        默认值是 0L；
//        例子： long a = 100000L，Long b = -200000L。
//        "L"理论上不分大小写，但是若写成"l"容易与数字"1"混淆，不容易分辩。所以最好大
        System.out.println("基本类型：long 二进制位数：" + Long.SIZE);
        System.out.println("包装类：java.lang.Long");
        System.out.println("最小值：Long.MIN_VALUE=" + Long.MIN_VALUE);
        System.out.println("最大值：Long.MAX_VALUE=" + Long.MAX_VALUE);
        System.out.println();

//        float：
//        float 数据类型是单精度、32位、符合IEEE 754标准的浮点数；
//        float 在储存大型浮点数组的时候可节省内存空间；
//        默认值是 0.0f；
//        浮点数不能用来表示精确的值，如货币；
//        例子：float f1 = 234.5f
        System.out.println("基本类型：float 二进制位数：" + Float.SIZE);
        System.out.println("包装类：java.lang.Float");
        System.out.println("最小值：Float.MIN_VALUE=" + Float.MIN_VALUE);
        System.out.println("最大值：Float.MAX_VALUE=" + Float.MAX_VALUE);
        System.out.println();

//        double：
//        double 数据类型是双精度、64 位、符合 IEEE 754 标准的浮点数；
//        浮点数的默认类型为 double 类型；
//        double类型同样不能表示精确的值，如货币；
//        默认值是 0.0d；
//        例子：
//        double   d1  = 7D ;
//        double   d2  = 7.;
//        double   d3  =  8.0;
//        double   d4  =  8.D;
//        double   d5  =  12.9867;
//        7 是一个 int 字面量，而 7D，7. 和 8.0 是 double 字面量

        System.out.println("基本类型：double 二进制位数：" + Double.SIZE);
        System.out.println("包装类：java.lang.Double");
        System.out.println("最小值：Double.MIN_VALUE=" + Double.MIN_VALUE);
        System.out.println("最大值：Double.MAX_VALUE=" + Double.MAX_VALUE);
        System.out.println();

//        char：
//        char 类型是一个单一的 16 位 Unicode 字符；    一个字节肯定是不够的
//        最小值是 \u0000（十进制等效值为 0）；
//        最大值是 \uffff（即为 65535）；
//        char 数据类型可以储存任何字符；
//        默认值：'u0000'
//        例子：char letter = 'A';
        System.out.println("基本类型：char 二进制位数：" + Character.SIZE);
        System.out.println("包装类：java.lang.Character");
        // 以数值形式而不是字符形式将Character.MIN_VALUE输出到控制台
        System.out.println("最小值：Character.MIN_VALUE="
                + (int) Character.MIN_VALUE);
        // 以数值形式而不是字符形式将Character.MAX_VALUE输出到控制台
        System.out.println("最大值：Character.MAX_VALUE="
                + (int) Character.MAX_VALUE);

        //boolean：
//        boolean数据类型表示一位的信息；
//        只有两个取值：true 和 false；
//        这种类型只作为一种标志来记录 true/false 情况；
//        默认值是 false；
//        例子：boolean one = true。
    }

   static class Fruit extends Food{
        String name = "Fruit";
    }

   static class Food{
        String name ="food";
    }

}
