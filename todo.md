https://mp.weixin.qq.com/s/XGKXUYJ4oTbZWHkI-MJbtQ

晕轮效应、中线定理 

//todo 正则表达式  匹配中文[\u4e00-\u9fa5]

http://weishu.me/2016/12/23/dive-into-android-optimize-vm-heap/

dokit
https://github.com/didi/DoKit/tree/master/Android

如何写业务代码  需要了解代码整洁指导  ddd领域驱动设计
https://github.com/alibaba/COLA
https://mp.weixin.qq.com/s/DpTRZs8-7Mzd18JgbYWkUQ

https://mp.weixin.qq.com/s/SAFpBfLQR9IynrQdyE6oJw


为什么不刷新  https://juejin.cn/post/6844903829113143303#heading-3  改变holder.itemView的属性却可以。。
recyclerview
onBindViewHolder中
setListener中不要直接使用方法的itemData，相当于匿名内部类持有外部变量
当使用diffUtil更新时，item的hashcode不一致(生成新的了)，但是属性都是一样的，此时页面不会刷新，而listener中的itemData还是旧的，
此时如果更新状态然后notifyItem，就会发现异常

https://blog.csdn.net/zxt0601/article/details/52562770
DiffUtils 放在子线程使用
notifyItemRangeChanged(int, int, Object)）利用这个方法只改变需要变更的，这样就不需要做所有的绑定逻辑的



如何从底层杀出
https://www.zhihu.com/question/487990120/answer/2295005139

android camera
https://deepinout.com/

马万俊
没有啊。。如果需要其他人推荐，说明自己不了解。对于自己不了解的东西，建议不要碰。
我们只投资自己能力圈内的标的。
//不了解自己认知外的怎么提升？？，

老实人改变

沈冰冰
抓住技术的热爱

学生思维

看不懂Kotlin源码？从Contracts 函数说起~
https://blog.csdn.net/huangliniqng/article/details/125210444
https://www.kotlincn.net/docs/reference/whatsnew13.html

mediacodec
https://www.jianshu.com/p/f5a1c9318524

view.OnAttachStateChangeListener