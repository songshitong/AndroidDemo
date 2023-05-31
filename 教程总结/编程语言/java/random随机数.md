
伪随机
1 什么是伪随机？
(1) 伪随机数是看似随机实质是固定的周期性序列，也就是有规则的随机。
(2) 只要这个随机数是由确定算法生成的，那就是伪随机，只能通过不断算法优化，使你的随机数更接近随机。(随机这个属性和算法本身就是矛盾的)
(3) 通过真实随机事件取得的随机数才是真随机数。

2 Java随机数产生原理
Java的随机数产生是通过线性同余公式产生的，也就是说通过一个复杂的算法生成的。

3 伪随机数的不安全性
Java自带的随机数函数是很容易被黑客破解的，因为黑客可以通过获取一定长度的随机数序列来推出你的seed，然后就可以预测下一个随机数。
比如eos的dapp竞猜游戏，就因为被黑客破解了随机规律，而盗走了大量的代币。

4 如何优化随机
主要要考虑生成的随机数不能重复，如果重复则重新生成一个。可以用数组或者Set存储来判断是否包含重复的随机数，配合递归方式来重新生成一个新的随机数


同一个种子，生成N个随机数，当你设定种子的时候，这N个随机数是什么已经确定。相同次数生成的随机数字是完全相同的
```
Random random1 = new Random(1000);
System.out.println("random1 "+random1.nextInt());

Random random2 = new Random(1000);
System.out.println("random2 "+random2.nextInt());

结果：
random1 -1244746321
random2 -1244746321
```


随机数初始化
java\util\Random.class
```
private final AtomicLong seed; //线程安全的long
  public Random() {
        this(seedUniquifier() ^ System.nanoTime());
    }
    
 public Random(long seed) {
        if (getClass() == Random.class)
            this.seed = new AtomicLong(initialScramble(seed));
        else {
            // subclass might have overriden setSeed
            this.seed = new AtomicLong();
            setSeed(seed);
        }
    }  
    
//论文地址  https://www.ams.org/journals/mcom/1999-68-225/S0025-5718-99-00996-5/S0025-5718-99-00996-5.pdf
private static long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        for (;;) {
            long current = seedUniquifier.get(); //默认为 8682522807148012L
            long next = current * 1181783497276652981L; //生成新的种子
            if (seedUniquifier.compareAndSet(current, next)) //更新seedUniquifier
                return next;
        }
    } 
    

   private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;  //multiplier:0x5DEECE66DL  mask:(1L << 48) - 1
    }
         
```
种子的作用是什么？
种子就是产生随机数的第一次使用值，机制是通过一个函数，将这个种子的值转化为随机数空间中的某一个点上，并且产生的随机数均匀的散布在空间中，
以后产生的随机数都与前一个随机数有关

无参的通过seedUniquifier() ^ System.nanoTime()生成一个种子，里面使用了CAS自旋锁实现。使用System.nanoTime()方法来得到一个纳秒级的时间量，
参与48位种子的构成，然后还进行了一个很变态的运算：不断乘以181783497276652981L，直到某一次相乘前后结果相同来进一步增大随机性
，这里的nanotime可以算是一个真随机数，不过有必要提的是，nanoTime和我们常用的currenttime方法不同，
返回的不是从1970年1月1日到现在的时间，而是一个随机的数：只用来前后比较计算一个时间段，比如一行代码的运行时间，数据库导入的时间等，
而不能用来计算今天是哪一天。
//个人： 直到某一次相乘前后结果相同来进一步增大随机性?? 这不对吧

不要随便设置随机种子，可能运行次数多了会获取到相同的随机数，Random类自己生成的种子已经能满足平时的需求了


生成随机数nextInt
```
  public int nextInt() {
        return next(32);
    }
    
    protected int next(int bits) {
        long oldseed, nextseed;
        AtomicLong seed = this.seed;
        do {
            oldseed = seed.get();
            nextseed = (oldseed * multiplier + addend) & mask;  //multiplier:0x5DEECE66DL  addend:0xBL mask:(1L << 48) - 1
        } while (!seed.compareAndSet(oldseed, nextseed));
        return (int)(nextseed >>> (48 - bits)); //右移16
    }
```