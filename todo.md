
https://developer.aliyun.com/article/1366701
零GC（Garbage-free）
从2.6版本开始（2016年），log4j2 默认就以零GC模式运行了。什么叫零GC呢？就是不会由于log4j2而导致GC。
log4j2 中各种Message对象，字符串数组，字节数组等全部复用，不重复创建，大大减少了无用对象的创建，从而做到“零GC”。
更高性能 I/O 写入的支持
log4j 还提供了一个MemoryMappedFileAppender，I/O 部分使用MemoryMappedFile来实现，可以得到极高的I/O性能。不过在使用MemoryMappedFileAppender之前，得确定你足够了解MemoryMappedFile的相关知识，否则不要轻易使用呦。
更强大的参数格式化
API模块和slf4j相比，提供了更丰富的参数格式化功能。
使用{}占位符格式化参数

https://imglarger.com/ 记录


https://mp.weixin.qq.com/s/iALdbq25hDYXlPunnomQQg

https://blog.csdn.net/learnframework/article/details/107720757?spm=1001.2014.3001.5502
framework视频


protobuff中协议编码的思想 其中介绍了ASN.1
https://mp.weixin.qq.com/s/6uiRqOTWVcWg23ZAxGvYeg
ASN.1
ASN.1(Abstract Syntax Notation dotone),抽象语法标记1。是定义抽象数据类型形式的标准，描绘了与任何表示数据的编码技术无关的通用数据结构。
抽象语法使得人们能够定义数据类型，并指明这些类型的值。抽象语法只描述数据的结构形式，与具体的编码格式无关，同时也不涉及这些数据结构在计算机内如何存放。

重试机制
https://mp.weixin.qq.com/s/3csib6DUSf9LYsbmB8aKNA

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


valhalla 将java的对象类型改为值类型来优化性能和内存
https://openjdk.org/projects/valhalla/
https://mp.weixin.qq.com/s/jPAVBurLIxmoN82eLUdhmw


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



https://chat.openai.com/c/3020ae1a-ec5a-4308-89a1-29c235242a96
you
how to get a girlfriend
ChatGPT
Getting a girlfriend involves building connections, being yourself, and fostering mutual attraction. 
Here are some tips to help you:

Self-Improvement: Work on yourself first. Cultivate your interests, hobbies, and passions. Develop confidence and self-esteem. 
Being comfortable with who you are is attractive to others.

Socialize: Put yourself in social situations where you can meet new people. This could be through clubs, 
classes, volunteering, or social events. The more people you meet, the higher the chances of finding someone compatible.

Be Genuine: Authenticity is key. Be yourself when interacting with others. Pretending to be someone 
you're not will eventually backfire.

Communicate: Engage in conversations and listen attentively. Show interest in the other person's life and opinions.
Communication is vital for building connections.

Make Eye Contact and Smile: These are simple gestures, but they can convey warmth and openness, making you more approachable.

Show Respect and Kindness: Treat others with respect and kindness. Being courteous and considerate 
goes a long way in forming meaningful relationships.

Take Initiative: If you're interested in someone, don't be afraid to make the first move. Ask them out for coffee, 
lunch, or to participate in an activity together.

Build Emotional Connection: Share your thoughts, feelings, and experiences. Vulnerability can foster intimacy
and deepen your connection with someone.

Be Patient: Finding the right person takes time. Don't rush or force relationships. Allow things to progress naturally.

Work on Your Appearance: While inner qualities are important, taking care of your appearance can also make you more attractive. 
Dress in a way that makes you feel confident and presentable.

Remember, relationships are about mutual compatibility and respect. It's essential to find someone 
who appreciates you for who you are and vice versa. Don't be discouraged by setbacks or rejections; 
keep putting yourself out there, and eventually, you'll find someone special.



https://chatgpt.com/c/b664ede3-fb85-4ab2-95cc-ddd4acd799ac
追求更高的精神是什么
ChatGPT
追求更高的精神是指个人在超越物质欲望和日常琐事的基础上，寻求内在的成长、道德完善、智慧提升和精神觉悟。这种追求注重内在的丰富和心灵的满足，而不是外在的物质享受和世俗成就。以下是一些具体方面来阐述这个概念：

1. 自我认识与反思（Self-Understanding and Reflection）
   追求更高的精神意味着不断地自我反省和内省，了解自己的内心需求、价值观和人生目标。通过自我认识，个体能够找到真正的自我，并在生活中保持一致性和内心的平静。

具体实践：

定期进行自我反思，记录自己的思想和情感。
通过冥想和独处来深入思考自己的生活和目标。
2. 道德和伦理发展（Moral and Ethical Development）
   更高的精神追求还包括个人在道德和伦理上的成长。个体通过培养美德，如诚实、同情、宽容和正义感，来提升自己的道德水平，并在日常生活中践行这些价值。

具体实践：

认真思考并践行自己的道德准则。
在生活中坚持做对的事情，即使面临困难和挑战。
3. 智慧和知识的追求（Pursuit of Wisdom and Knowledge）
   追求更高的精神还涉及到对智慧和知识的不断追求。这不仅仅是积累知识，而是理解和应用知识，培养批判性思维，做出明智的决定。

具体实践：

阅读哲学、文学、科学等领域的经典著作。
参与讨论和辩论，锻炼自己的思维和观点。
4. 精神与心灵的修炼（Spiritual and Inner Cultivation）
   许多人通过宗教、冥想、瑜伽等途径来提升自己的精神层次。这些实践帮助个体与更高的存在或内在的真实自我连接，获得内心的宁静和超越感。

具体实践：

定期进行冥想或祷告，寻找内心的平静。
参与宗教或精神社群，获取支持和指导。
5. 创造性与艺术表达（Creative and Artistic Expression）
   艺术和创造性活动是提升精神的一种重要方式。通过音乐、绘画、写作等形式，个体能够表达内心的感受和思想，达到自我实现和精神满足。

具体实践：

参与艺术创作，如绘画、写作或音乐演奏。
参观艺术展览，感受和理解艺术作品的精神内涵。
6. 与自然的连接（Connection with Nature）
   自然是精神提升的重要源泉。通过亲近自然，个体可以感受到自然的美丽和宁静，从而获得心灵的滋养和启迪。

具体实践：

定期进行户外活动，如徒步旅行、露营或园艺。
观察和欣赏自然景观，感受自然的和谐美。
7. 服务与利他主义（Service and Altruism）
   追求更高的精神也包括为他人服务和贡献社会。通过帮助他人，个体不仅能带来实际的改变，还能在奉献中找到深层次的满足和意义。

具体实践：

参与志愿服务，帮助有需要的人。
在社区中发挥积极作用，推动社会进步。
总结
追求更高的精神是一个不断自我提升和超越的过程，涉及自我认识、道德发展、智慧追求、心灵修炼、创造性表达、自然连接和利他主义等多个方面。这种追求不仅能带来个人的内在满足和成长，
还能对社会和他人产生积极的影响。通过这些实践，个体可以找到生活的深层次意义，获得持久的幸福和内心的平静。




追求更高的精神中梭罗获得幸福了吗
ChatGPT
亨利·戴维·梭罗在《瓦尔登湖》中描述了他通过追求更高的精神获得了一种深层次的幸福。这种幸福并不依赖于物质财富或社会地位，而是源于内心的平静、自我实现和与自然的和谐共处。以下是一些具体方面，说明梭罗如何通过追求更高的精神获得幸福：

1. 简朴生活带来的满足
   梭罗在《瓦尔登湖》中展示了他如何通过简朴生活找到满足。他选择离开城市，过一种简单而自给自足的生活，这种生活方式使他能够专注于内在的精神成长，而不是被物质欲望所困扰。

具体例子：

建造自己的小屋，种植自己的食物，这种自给自足的生活方式让他感到自主和满足。
他描述了在自然中生活的简单快乐，例如每天观察湖水、鸟鸣和森林的变化。
2. 与自然的深度连接
   梭罗在瓦尔登湖畔的生活使他能够与自然建立深度连接。他通过观察和亲近自然，获得了内心的宁静和精神上的提升。

具体例子：

他花时间观察湖泊、森林和野生动物，记录下这些自然现象，并从中获得灵感和智慧。
梭罗强调自然对人的精神滋养作用，认为亲近自然可以带来内心的平静和满足。
3. 自我反思与内在成长
   梭罗利用在瓦尔登湖的独处时间进行深度的自我反思和内在探索。他通过思考人生的意义、价值和目标，获得了对自我和生活的更深理解。

具体例子：

他写道：“大多数人过着平静的绝望生活”，这表明他通过反思发现了许多人生活中的问题，并努力避免这种生活。
梭罗不断反思自己的生活方式和价值观，追求内在的真实和精神的提升。
4. 精神独立和自主
   梭罗强调自我依靠的重要性，认为个人应该依靠自己的能力和智慧来过独立的生活。这种精神上的自主和独立带给他巨大的幸福感。

具体例子：

他独自建造小屋、种植食物，这些行为不仅展示了他的自我依靠能力，也带给他深层次的满足和成就感。
他在书中多次提到独立思考和行动的重要性，这种精神上的独立是他获得幸福的重要来源。
5. 哲学思考带来的启迪
   通过对生活的哲学思考，梭罗获得了深层次的智慧和启迪，这种智慧使他能够以一种更开阔和宁静的心态面对生活。

具体例子：

他探讨了简朴生活、物质主义和精神追求之间的关系，并从中获得对生活的深刻理解。
他的哲学思考帮助他找到了一种平衡的生活方式，这种方式既符合自然规律，也满足了他的精神需求。
总结
梭罗在《瓦尔登湖》中通过简朴生活、与自然的连接、自我反思、精神独立和哲学思考，找到了深层次的幸福。他的这种幸福并不依赖于外在的物质和社会地位，
而是源于内心的平静、满足和对生命意义的深刻理解。因此，可以说，梭罗通过追求更高的精神获得了真正的幸福。






我应该怎样获得幸福感
ChatGPT
获得幸福感是一个个体化的过程，因为每个人对幸福的定义和实现方式都可能不同。然而，以下是一些普遍适用的方法，可以帮助你获得更多的幸福感：

1. 培养积极的心态
   尝试保持乐观的态度，关注生活中的积极方面。
   感恩并珍惜所拥有的一切，包括人际关系、健康和个人成就。
   学会接受生活中的不完美和挑战，将其视为成长的机会。
2. 培养健康的生活方式
   注重身体健康，包括充足的睡眠、健康饮食和适度的运动。
   寻找适合自己的放松和减压方法，如冥想、瑜伽或阅读。
   培养良好的社交关系，与家人、朋友和社区保持联系。
3. 追求个人成长和目标
   设定具体的、可实现的目标，并逐步努力实现它们。
   不断学习和成长，探索新的兴趣爱好和领域。
   让自己感到有成就感，通过实现自己的目标和价值观来提升自信心和满足感。
4. 培养积极的人际关系
   建立亲密和支持性的人际关系，与他人分享快乐和挑战。
   学会沟通和解决冲突，保持良好的人际关系。
   提供帮助和支持他人，通过服务他人来获得内心的满足和幸福感。
5. 发掘内在的意义和目的
   深入探索自己的价值观、信仰和生活意义，找到内心的平静和满足。
   寻找自己的使命和目标，让生活更具有意义和方向性。
   关注自己内心的声音，从中找到生活的真正意义和幸福感。
6. 享受当下的每一刻
   学会专注于当下，珍惜每一刻的美好和快乐。
   通过参与有意义的活动，如旅行、艺术创作或志愿服务，丰富自己的生活经历。
   学会放慢节奏，享受生活中的小事情和平凡的快乐。

   总结
   获得幸福感是一个综合性的过程，需要综合考虑个人的生活方式、价值观、人际关系和内心需求。通过培养积极的心态、健康的生活方式、个人成长和目标、积极的人际关系、发掘内在的意义和目的，以及享受当下的每一刻，你可以逐渐提升自己的幸福感，并找到生活的真正意义和满足。



