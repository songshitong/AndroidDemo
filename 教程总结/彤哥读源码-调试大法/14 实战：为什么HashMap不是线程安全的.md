
///结果：
///1.当table==null没有元素的时候，插入两个元素，会同时创建两个table，分别在这两个table中插入元素，但最后只有
///一个table赋值给了hashmap本身，这就出现了线程不安全的行为
//2.当table[i]这个下标有元素的时候，同时在i位置再插入两个元素，会导致其中一个可能被挤掉
//
//3.当table[i]这个下标没有元素的时候，同时在i位置再插入两个元素，可能导致其中一个被挤掉
//
//4.当元素的数量已经达到了扩容门槛的时候，再同时插入两个元素，有可能导致扩容两次
//
//5.当元素的数量已经达到了扩容门槛的时候，再同时插入两个元素，有可能导致整个map被情况


1.HashMap的基本数据结构
  内部是一个数组 table[]
  当同样的元素放入table时叫做hash 冲突
  解决hash冲突
    寻址法  查看下一个是否有元素，没有添加
    拉链法/链表法  如果已经存在，新建一个节点，并将以前的指过去
      java8以后 优化为红黑树
      
   hash算法 hashCode的高低位异或  数字类型是他本身，其他类型hashcode
    static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }   
   
   实战：
    在两个put的地方加入断点，suspend改为thread
    运行线程一，查看put方法
     if ((tab = table) == null || (n = tab.length) == 0)
                 n = (tab = resize()).length;
     其中resize方法没有线程同步，记录tab的对象ID，map大小和内容
    运行线程二，同样查看put方法
     发现tab对象ID不一样，map的size为2，但是内容只有一个
    放开断点，运行结果 map只有一个 
     不打断点，直接运行，结果有两个？？   
    
   分析：一开始map为空，同时往第一个位置放入元素，在reszie()中
     Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
     table = newTab;        
     。。。
     retrun newTab
     //同时生成两个newTab，只有一个赋值给了hashMap的table
     
     
   实战2：
    线程开始前map存在元素    map.put(17,"17");
    控制两个线程同时执行到 put 方法  p.next = newNode(hash, key, value, null);
    两个线程同时写肯定有问题
    增加变量watch p.next
     第二次p.next仍然为null，这就不对了，然后继续执行newNode
   
   实战3：
    线程开始前存在6个元素  
    执行分支： table[i]没有元素
    if ((p = tab[i = (n - 1) & hash]) == null)
       tab[i] = newNode(hash, key, value, null);   table[i]出现同时写的问题
       
   同时停在++this.size > this.threshold，不让resize执行 
      ++this.modCount;
              if (++this.size > this.threshold) {
                  this.resize();
              }
   线程1执行扩容，完成后线程2执行扩容
     8个元素，但是扩容了两次，debug的evaluate map.table.length=32  
   
   再一次测试，线程1停在resize扩容的 threshold = newThr; 然后第二个线程也执行到这
   然后在一步步继续          
    出现collecting data 数据计算不出来了，一般是出问题
    打印结果map为空
        
   map
    resize()  扩容，初始化     