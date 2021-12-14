参考https://github.com/xfhy/Android-Notes/blob/master/Blogs/Java/%E5%9F%BA%E7%A1%80/String.md
1.string的实现
  java8及之前是char数组
  Java9以后是byte数组
  在Java中char是2个字节，16位   byte是1个字节，8位
  ISO-8859-1 是latin1或者西欧语言
  对于char数组 如果我们要存储字符A，则为0x00 0x41，此时前面的一个字节空间浪费了。但如果保存中文字符则不存在浪费的情况，
  也就是说如果保存 ISO-8859-1 编码内的字符则浪费，之外的字符则不会浪费

//todo string的实现
 
2.String类为什么是final的   https://www.jianshu.com/p/9c7f5daac283
  1.为了实现字符串池
  2.为了线程安全
  3.为了实现String可以创建HashCode不可变性 
  首先你要理解final的用途，在分析String为什么要用final修饰，final可以修饰类，方法和变量，并且被修饰的类或方法，
  被final修饰的类不能被继承，即它不能拥有自己的子类，被final修饰的方法不能被重写， 
  final修饰的变量，无论是类属性、对象属性、形参还是局部变量，都需要进行初始化操作
  
  final修饰的String，代表了String的不可继承性，final修饰的char[]代表了被存储的数据不可更改性。
  但是：虽然final代表了不可变，但仅仅是引用地址不可变，并不代表了数组本身不会变.起作用的还有private，正是因为两者保证了String的不可变性
  
  为什么保证String不可变呢,因为只有当字符串是不可变的，字符串池才有可能实现。字符串池的实现可以在运行时节约很多heap空间，
  因为不同的字符串变量都指向池中的同一个字符串。但如果字符串是可变的，那么String interning将不能实现，
  因为这样的话，如果变量改变了它的值，那么其它指向这个值的变量的值也会一起改变
  
  如果字符串是可变的，那么会引起很严重的安全问题。譬如，数据库的用户名、密码都是以字符串的形式传入来获得数据库的连接，
  或者在socket编程中，主机名和端口都是以字符串的形式传入。因为字符串是不可变的，所以它的值是不可改变的，否则黑客们可以钻到空子，
  改变字符串指向的对象的值，造成安全漏洞。
  
  因为字符串是不可变的，所以是多线程安全的，同一个字符串实例可以被多个线程共享。这样便不用因为线程安全问题而使用同步。
  字符串自己便是线程安全的
  
  因为字符串是不可变的，所以在它创建的时候HashCode就被缓存了，不需要重新计算。这就使得字符串很适合作为Map中的键，
  字符串的处理速度要快过其它的键对象。这就是HashMap中的键往往都使用字符串。
  
  
3.字符串常量池
  Java堆内存中一个特殊的存储区域,当创建一个String对象时,假如此字符串值已经存在于常量池中,则不会创建一个新的对象,
  而是引用已经存在的对象.JDK 1.6及之前字符串常量池是存放在方法区的,JDK 1.7才开始放在堆中. 
  
4.
  String中的intern方法是什么含义?
  intern方法可以用来声明字符串,它会从字符串常量池中查询当前字符串是否存在,存在则直接返回当前字符串;不存在就会将当前字符串放入常量池中,再返回.
  
5.
  编译器对String做了哪些优化?
  使用"+"连接常量字符串与常量字符串的时候,会将字符串全部加在一起然后存放. 如果用"+"号连接字符串与变量的时候,则是创建StringBuilder或StringBuffer来拼接.
  
  
   String s = "dasdas" + TAG;
   String b = "I like " + "java";
   String c = s + b;    
  
   javap -v -c -s -l Test.class 查看字节码
    stack=2, locals=4, args_size=1
            0: ldc           #3                  // String dasdastag
            2: astore_1
            3: ldc           #4                  // String I like java
            5: astore_2
            6: new           #5                  // class java/lang/StringBuilder
            9: dup
           10: invokespecial #6                  // Method java/lang/StringBuilder."<init>":()V
           13: aload_1
           14: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
           17: aload_2
           18: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
           21: invokevirtual #8                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
           24: astore_3
           25: return
   字节码中String dasdastag   String I like java   都已经拼接好
   编译器在连接字符串时,需要连接的字符串都是常量,就会在编译期直接将其相加;
   如果需要连接的是变量,则会使用StringBuilder(或StringBuffer)进行拼接.
   
6.  String str = new String("abc")创建了多少个对象
  ///todo jvm  类的执行过程，类的加载过程
  代码的执行过程和类的加载过程不同.在类的加载过程中,确实在运行时常量池中创建了一个"abc"对象,而在代码执行过程中只创建了一个String对象.
  这里String str = new String("abc")涉及的是2个对象.  
  
  
7.
  euqals 方法
  大体实现思路:
  先判断引用是否相同
  再判断该Object对象是否是String的实例
  再判断两个字符串的长度是否一致
  最后挨个字符进行比较   
  public boolean equals(Object anObject) {
      //1. 引用相同  
      if (this == anObject) {
          return true;
      }
      //2. 是String的实例
      if (anObject instanceof String) {
          String anotherString = (String)anObject;
          int n = value.length;
          //3. 长度
          if (n == anotherString.value.length) {
              char v1[] = value;
              char v2[] = anotherString.value;
              int i = 0;
              //4. 挨个字符进行比较
              while (n-- != 0) {
                  if (v1[i] != v2[i])
                      return false;
                  i++;
              }
              return true;
          }
      }
      return false;
  }
  
  
  hash code方法
  hash默认为0，只有在使用的时候才会初始化，并且只计算一次    hashcode的不变依赖于string常量池的实现
  public int hashCode() {
          int h = hash;
          final int len = length();
          if (h == 0 && len > 0) {
              for (int i = 0; i < len; i++) {
                  h = 31 * h + charAt(i);
              }
              hash = h;
          }
          return h;
      }
 
 
  replace 方法
  getfield opcode  getfield是Java的字节码  opcode操作码
  在一个方法中需要大量引用实例域变量的时候，使用方法中的局部变量代替引用可以减少getfield操作的次数，提高性能。
  public String replace(char oldChar, char newChar) {
      //1. 如果两者相同,那么就没必要进行比较了
      if (oldChar != newChar) {
          int len = value.length;
          int i = -1;
          char[] val = value; /* avoid getfield opcode */
  
          //2. 从最前面开始,循环遍历,找到与oldChar相同的字符
          while (++i < len) {
              if (val[i] == oldChar) {
                  break;
              }
          }
          //3. 如果找到了与oldChar相同的字符才进入if
          if (i < len) {
              //4. 新建一个数组,用于存放新数据
              char buf[] = new char[len];
              //5. 将i前面的全部复制进新数组里面去
              for (int j = 0; j < i; j++) {
                  buf[j] = val[j];
              }
              //6. 在i后面的字符,我们将其一个一个地放入新数组中,当然在放入时需要比对是否和oldChar相同,相同则存放newChar
              while (i < len) {
                  char c = val[i];
                  buf[i] = (c == oldChar) ? newChar : c;
                  i++;
              }
              //7. 最终重新new一个String
              return new String(buf, true);
          }
      }
      return this;
  }
  
  
  //如果参数字符串等于此字符串，则返回值 0；
  //  如果此字符串小于字符串参数，则返回一个小于 0 的值；
  //  如果此字符串大于字符串参数，则返回一个大于 0 的值。
  //
  // 挨个字符比较，如果字符不同，转为大写判断，如果大写不同，转为小写判断
  //  如果转换后前n个字符相同，返回长度差值
  public int compareToIgnoreCase(String str) {
          return CASE_INSENSITIVE_ORDER.compare(this, str);
      }
      
  public static final Comparator<String> CASE_INSENSITIVE_ORDER
                                           = new CaseInsensitiveComparator();  
                                           
  CaseInsensitiveComparator{
     public int compare(String s1, String s2) {
                 int n1 = s1.length();
                 int n2 = s2.length();
                 int min = Math.min(n1, n2);
                 for (int i = 0; i < min; i++) {
                     char c1 = s1.charAt(i);
                     char c2 = s2.charAt(i);
                     if (c1 != c2) {
                         c1 = Character.toUpperCase(c1);
                         c2 = Character.toUpperCase(c2);
                         if (c1 != c2) {
                             c1 = Character.toLowerCase(c1);
                             c2 = Character.toLowerCase(c2);
                             if (c1 != c2) {
                                 // No overflow because of numeric promotion
                                 return c1 - c2;
                             }
                         }
                     }
                 }
                 return n1 - n2;
             }
  }                                         


截取字符串  前闭后开
substring
```
public String substring(int beginIndex, int endIndex) {
        return ((beginIndex == 0) && (endIndex == value.length)) ? this
                : new String(value, beginIndex, subLen);
    }
   //只有开始 
   public String substring(int beginIndex) {

        int subLen = value.length - beginIndex;
        return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
    }    
```


返回指定index的字符
```
  public char charAt(int index) {
        return value[index];
    }
```

长度函数
```
public int length() {
        return value.length;
    }
```

toLowerCase
```
 public String toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }
```

split 方法
```
public String[] split(String regex, int limit) {
```
