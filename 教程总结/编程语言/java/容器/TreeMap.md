https://www.jianshu.com/p/2dcff3634326
https://www.liaoxuefeng.com/wiki/1252599548343744/1265117109276544
todo   enummap


https://www.jianshu.com/p/2dcff3634326
TreeMap具有如下特点：
不允许出现重复的key；
可以插入null键，null值；
可以对元素进行排序；
插入和遍历顺序不一致


https://www.cs.usfca.edu/~galles/visualization/RedBlack.html 红黑树着色及旋转过程


jdk-11
红黑树节点
```
    private static final boolean RED   = false;
    private static final boolean BLACK = true;
    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left; //左
        Entry<K,V> right; //右
        Entry<K,V> parent; //父
        boolean color = BLACK; //节点颜色

        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
    }
```

查看插入操作
```
 public V put(K key, V value) {
        Entry<K,V> t = root;
        //设置根节点 默认为黑色
        if (t == null) {
            compare(key, key); // type (and possibly null) check

            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K,V> parent;
        // split comparator and comparable paths
        Comparator<? super K> cpr = comparator;
        //找到父节点
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0) //小于0 往左找
                    t = t.left;
                else if (cmp > 0) //大于0 往右找
                    t = t.right;
                else
                    return t.setValue(value); //节点相等，value值覆盖就行
            } while (t != null);
        }
        else { //没有定义comparator，使用自然比较
            if (key == null)
                throw new NullPointerException();
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        //创建节点 放在父的左或者右
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;
        fixAfterInsertion(e);
        size++; //记录增加
        modCount++; //modCount用于校验遍历时没有进行添加，移除操作
        return null;
    }

//todo 处理的图片
private void fixAfterInsertion(Entry<K,V> x) {
        x.color = RED; //初始着色
        
        //平衡条件
        //x不为null，不是根节点，父节点是红色（三者均满足才进行维护）：
        while (x != null && x != root && x.parent.color == RED) {
             //节点x的父节点 是 x祖父节点的左孩子：
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                //x的叔叔节点 y
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
                
                if (colorOf(y) == RED) { //叔叔节点是红的
                   //将父节点，叔叔节点设为黑色
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    //祖父节点设为红色
                    setColor(parentOf(parentOf(x)), RED);
                    //循环处理祖父节点颜色
                    x = parentOf(parentOf(x));
                } else {//叔叔节点是黑的
                    if (x == rightOf(parentOf(x))) { //x是父的右节点
                        x = parentOf(x); //开始处理父节点
                        rotateLeft(x);  //左旋父节点
                    }
                    setColor(parentOf(x), BLACK); //父节点为黑
                    setColor(parentOf(parentOf(x)), RED); //祖父为红
                    rotateRight(parentOf(parentOf(x))); //右旋祖父
                }
            } else {  //节点x的父节点 是 x祖父节点的右孩子：
                //叔叔节点y
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) { //叔叔节点为红
                    //设置 父黑，叔叔黑，祖父红
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x)); //重新处理祖父
                } else {//叔叔节点为黑
                    if (x == leftOf(parentOf(x))) { //x为父的左节点
                        x = parentOf(x);
                        rotateRight(x); //右旋父节点
                    }
                    //设置父节点黑色 ， 祖父节点红色，左旋祖父节点
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK; //根节点是黑的
    }


 //todo 右旋的对比图  注意，里面仍然保持了大小的顺序
 private void rotateRight(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> l = p.left;
            p.left = l.right; //p的左子的右子 挂在P的左子
            if (l.right != null) l.right.parent = p; //parent指向P
            //更新left的父
            l.parent = p.parent;
            if (p.parent == null)
                root = l; //没有根节点，l成为根节点
            else if (p.parent.right == p)
                p.parent.right = l; //p为父的右节点时，替换为l
            else p.parent.left = l; //p为父的左节点时，替换为l
            l.right = p; //l的右为p   p>l，所以，l成为父节点后，p为右
            p.parent = l; //p的父为l
        }
    }
    
 
  private void rotateLeft(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> r = p.right;
            p.right = r.left; //p.right为p.right.left
            if (r.left != null)
                r.left.parent = p; //更新right.left的父节点
            r.parent = p.parent;  //更新right的父
            if (p.parent == null)
                root = r; //没有根节点，设为right
            else if (p.parent.left == p)
                p.parent.left = r; //更新p的父的left
            else
                p.parent.right = r; //更新p的父的right
            r.left = p;   //r的左节点   r>p，所以r成为父节点后，p是left
            p.parent = r; //p的父节点
        }
    }   
```

元素获取
主要使用二分查找
```
    public V get(Object key) {
        Entry<K,V> p = getEntry(key);
        return (p==null ? null : p.value);
    }

 final Entry<K,V> getEntry(Object key) {
        // Offload comparator-based version for sake of performance
        if (comparator != null)
            return getEntryUsingComparator(key); //使用comparator与自然比较类似，不看了
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K,V> p = root;
        while (p != null) {
            //从root节点开始查找
            int cmp = k.compareTo(p.key);
            if (cmp < 0) //向左子寻找
                p = p.left;
            else if (cmp > 0) //向右子寻找
                p = p.right;
            else //key相等，返回节点
                return p;
        }
        return null; //找到叶子节点也没有
    }
```