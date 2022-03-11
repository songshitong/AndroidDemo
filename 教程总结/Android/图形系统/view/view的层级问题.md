
view的层级太深，页面复杂的问题
1. 布局解析加载耗时 
    LayoutInflater_全面解析.md  里面生成View对象使用了大量的放射

2. 布局重新测量导致的性能问题    代码测试MeasureTestActivity.kt   //todo LinearLayout或ViewRootImpl的源码怎么触发多次测量
https://rengwuxian.com/compose-intrinsic-measurement/
   做 Android 开发的都知道一个规矩：布局文件的界面层级要尽量地少，越少越好，因为层级的增加会大幅拖慢界面的加载。这种拖慢的主要原因就在于各种 Layout 的重复测量。
   虽然重复测量对于布局过程是必不可少的，但这也确实让界面层级的数量对加载时间的影响变成了指数级。而 Jetpack Compose 是不怕层级嵌套的，
   因为它从根源上解决了这种问题。它解决的方式也非常巧妙而简单——它不许重复测量。
……嗯？

View 层数和界面加载性能的关系
在定制 ViewGroup 的布局过程的时候，我们需要重写两个方法： onMeasure() 用来测量子 View，onLayout() 用来摆放测量好的子 View。
  测量和摆放明明是连续的过程，为什么要拆成两步呢？因为我们在 ViewGroup 里可能会对子 View 进行多次测量。

比如一个纵向的 LinearLayout，当它的宽度被设置成了 wrap_content 的时候：
```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical">
  ...
</LinearLayout>
```
它会依次测量自己所有的子 View，然后把它们最宽的那个的宽度作为自己最终的宽度。

但……如果它内部有一个子 View 的宽度是 match_parent：
```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <View
        android:layout_width="match_parent"
        android:layout_height="48dp" />

    <View
        android:layout_width="120dp"
        android:layout_height="48dp" />

    <View
        android:layout_width="160dp"
        android:layout_height="48dp" />
</LinearLayout>
```
这时候， LinearLayout 就会先以 0 为强制宽度测量一下这个子 View，并正常地测量剩下的其他子 View，然后再用其他子 View 里最宽的那个的宽度，
  二次测量这个 match_parent 的子 View，最终得出它的尺寸，并把这个宽度作为自己最终的宽度。

这是对单个子 View 的二次测量，如果有多个子 View 写了 match_parent ，那就需要对它们每一个都进行二次测量。

而如果所有的子 View 全都是 match_parent——哎呀跑题了。总之，在 Android 里，一个 ViewGroup 是可能对子 View 进行二次测量的。
  不只是二次，有时候还会出现三次甚至更多次的测量。而且这不是特殊场景，重复测量在 Android 里是很常见的。

重复测量是 ViewGroup 实现正确测量所必需的手段，但同时也让我们需要非常注意尽量减少布局的层级。

为什么呢？来看一个最简单的例子，如果我们的布局有两层，其中父 View 会对每个子 View 做二次测量，那它的每个子 View 一共需要被测量 2 次：

如果增加到三层，并且每个父 View 依然都做二次测量，这时候最下面的子 View 被测量的次数就直接翻倍了，变成 4 次：

也就是说，对于会做二次测量的系统，每个 View 的测量算法的时间复杂度是 O(2ⁿ) ，其中这个 n 是 View 的层级深度。

当然了，现实中并不是每个父 View 都会进行二次测量，以及有些父 View 会对子 View 做三次或者更多次的测量，所以这只是一个粗略估计，
   不过——大致就是这个数量级了。

而 O(2ⁿ) 这种指数型的时间复杂度，说白了就是，View 的层级每增加 1，加载时间就会翻一倍。

所以为什么 Android 官方文档会建议我们的布局文件少一些层级？因为它对性能的影响太大了！



Compose 的 Intrinsic Measurement
而 Compose 是禁止二次测量的。
如果每个父组件对每个子组件只测量一次，那就直接意味着界面中的每个组件只会被测量一次：

这样的话，就把组件加载的时间复杂度从 O(2ⁿ) 降到了 O(n)。

不过……如果禁用二次测量这么好用的话，Android 干嘛不在传统的 View 系统直接禁掉？——因为它有用啊！

那 Compose 禁用了二次测量，它就不用了吗？

这就是 Compose 巧妙的地方了：Compose 禁用了二次测量，但加入了一个新东西：Intrinsic Measurement，官方把它翻译做「固有特性测量」。

这个「固有特性测量」，你要说翻译得不对吧，其实字面上已经非常精确了，但这么翻却又完全没抓住这个功能的灵魂。

所谓的 Intrinsic Measurement，指的是 Compose 允许父组件在对子组件进行测量之前，先测量一下子组件的「固有尺寸」，
   直白地说就是「你内部内容的最大或者最小尺寸是多少」。这是一种粗略的测量，虽说没有真正的「二次测量」模式那么自由，但功能并不弱，
   因为各种 Layout 里的重复测量，其实本来就是先进行这种「粗略测量」再进行最终的「正式测量」的——比如刚才说的那种
  「外面 wrap_content 里面 match_parent」的，对吧？想想是不是？这种「粗略」的测量是很轻的，并不是因为它量得快，而是因为
  它在机制上不会像传统的二次测量那样，让组件的测量时间随着层级的加深而不断加倍。

当界面需要这种 Intrinsic Measurement——也就是说那个所谓的「固有特性测量」——的时候，Compose 会先对整个组件树进行一次 Intrinsic 测量，
  然后再对整体进行正式的测量。这样开辟两个平行的测量过程，就可以避免因为层级增加而对同一个子组件反复测量所导致的测量时间的不断加倍了。

总结成一句话就是，在 Compose 里疯狂嵌套地写界面，和把所有组件全都写进同一层里面，性能是一样的！

这……还怕嵌套？

刚才那个「固有特性测量」的翻译，我为什么觉得没有灵魂呢，主要是那个「固有特性」指的其实就是「固有尺寸」，也就是这个组件它自身的宽度和高度。
  而翻译成「固有特性测量」就有点太直了，直到反而让含义有点扭曲了。不过无伤大雅啊，不管是「固有尺寸测量」还是「固有特性测量」，
  这个设计真的很好，它让 Compose 逃过了 Android 原生 View 系统里的一个性能陷阱。