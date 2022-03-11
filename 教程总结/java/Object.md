

默认情况下也就是从超类Object继承而来的equals方法与‘==’是完全等价的，比较的都是对象的内存地址，但我们可以重写equals方法，
  使其按照我们需求的方式进行比较，如String类重写了equals方法，使其比较的是字符的序列，而不再是内存地址
java中==和equals区别
在java集合中，判断两个对象是否相等的规则是：
1）、判断两个对象的hashCode是否相等。
2）、判断两个对象用equals运算是否相等。


为什么复写equals方法的同时需要复写hashcode方法，前者相同后者是否相同，反过来呢？为什么？
要考虑到类似HashMap、HashTable、HashSet的这种散列的数据类型的运用，当我们重写equals时，是为了用自身的方式去判断两个自定义对象是否相等，
然而如果此时刚好需要我们用自定义的对象去充当hashmap的键值使用时，就会出现我们认为的同一对象，却因为hash值不同而导致hashmap中存了两个对象，
从而才需要进行hashcode方法的覆盖


hashcode()和 equals()的作用、区别、联系？
因为hashCode()并不是完全可靠，有时候不同的对象他们生成的hashcode也会一样（这是生成hash值的公式可能存在的问题），
    所以hashCode()只能说是大部分时候可靠，并不是绝对可靠，所以我们可以得出：
1、equal()相等的两个对象他们的hashCode()肯定相等，也就是用equal()对比是绝对可靠的。
2、hashCode()相等的两个对象他们的equal()不一定相等，也就是hashCode()不是绝对可靠的。