
子线程刷新ui导致的假死
https://mp.weixin.qq.com/s/zOiYoUNb1UmtxCe1OWgpwg

https://mp.weixin.qq.com/s/Wpc3gaqvoBn7qCeD4oLkzg

//ppt等办公历史
https://mp.weixin.qq.com/s/aX7kVqj5kYaV9J0XlTt4EA
主流的三个办公文件excel、word、ppt本质上都是一个包含多个文件目录且每个目录中含有一些.xml文件的压缩包而已
word格式 没有位置信息，不同软件存在兼容
```
<paragraph font="14" color="#000">大家好，我是一串文字</paragraph>
```
pdf格式
PDF有一套自己的「语法规则」，这套规则描述了一张固定大小的纸张上哪个文字应该放在哪个位置，这个信息是绝对的。如果对比上面的word文档，
他们描述信息的方式采用的是「xml」，xml只是存储了信息，但是这些信息的具体「排布方式」是由各自的软件决定的
```
【文字开始】
    缩放比例1倍 坐标(1036,572) 【文字定位】
    /TT1 12磅 【选择字体】
    [ (He) 间距24 (l) 间距-48 (l) 间距-48 (o) ] 【绘制文字】
【文字结束】
【文字开始】
    缩放比例1倍 坐标(1147,572) 【文字定位】
    /TT1 12磅 【选择字体】
    (空格) 【绘制文字】
【文字结束】
【文字开始】
    缩放比例1倍 坐标(1060,572) 【文字定位】
    /TT1 12磅 【选择字体】
    [ (w) 间距24 (or) 间距-84 (l) 间距-24 (d) ] 【绘制文字】
【文字结束】
```



阿德勒有说过？？？
https://mp.weixin.qq.com/s/Gs-NM7GUrOv-VbIQr7QhpQ
自由是不再寻求认可，就是被别人讨厌。有人讨厌你，反而说明你活得自由，活出了自我。阿德勒说过：“人生的意义在于创造意义，而不是在于寻找意义。”


是这样的？
《活在当下》这本书讲诉过这种状态，作者奥凯利是美国的商界精英，知名律所毕马威的董事长，正当他处在事业巅峰时，2005年5月，他被诊断为脑癌晚期，
最多还能活3个月到6个月。他用这剩下的时光写了一本书，并告诉世人：“人生不可以重来，不可以跳过，我们只能选择最有意义的方式度过，那就是活在当下，追逐日光。”

https://github.com/byhook/ffmpeg4android

聚焦优化音频卡顿、杂音问题
https://mp.weixin.qq.com/s/yQQ5q8vvi7ltVxVQrfcl0Q


《下沉年代》https://mp.weixin.qq.com/s/olIbhn54zoR_FkuFQfY5ww
美国作家乔治·帕克的《下沉年代》，讲的就是伴随着美国“去工业化”的进程，不同阶层、职业的人所经历的人生意义的幻灭故事。
帕克对于“去工业化”的价值判断很明确：
这件事非常糟糕，不仅无数底层劳动者丧失谋生手段，更让无数人丢掉了人生方向，迷茫而不知所往，包括新时代暴富的新贵。



webrtc
https://mp.weixin.qq.com/s/bsK5mCLmioIshKaBMKmoCQ

mmap
https://mp.weixin.qq.com/s?__biz=MjM5Njg5NDgwNA==&mid=2247491413&idx=1&sn=b4a7a3b6b5407e6d012441415c4dc5de&chksm=a6e31e6e91949778fb959258a7b460977f300a94a739cf96440fb4bb8123ba750244aadd770a#rd

不同语言的错误处理
https://mp.weixin.qq.com/s/bvkF1X2cTvo2D2DV7vadpA

https://mp.weixin.qq.com/s/hAfrPlPD2KBCWxpIuGkQTQ

https://mp.weixin.qq.com/s/XGKXUYJ4oTbZWHkI-MJbtQ

https://mp.weixin.qq.com/s/kJF_ezR2TdPnYlfPCjwdKQ 缓存一致性

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