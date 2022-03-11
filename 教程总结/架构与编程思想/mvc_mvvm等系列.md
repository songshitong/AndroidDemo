
https://juejin.cn/post/7022624191723601928
https://www.ruanyifeng.com/blog/2015/02/mvcmvp_mvvm.html
MVC
MVC架构主要分为以下几部分
视图层（View）：对应于xml布局文件和java代码动态view部分
控制层（Controller）：主要负责业务逻辑，在android中由Activity承担，同时因为XML视图功能太弱，所以Activity既要负责视图的显示又要加入控制逻辑，
     承担的功能过多。
模型层（Model）：主要负责网络请求，数据库处理，I/O的操作，即页面的数据来源
架构设计_mvc.webp   todo mvc有两种方式
1 View 传送指令到 Controller
2 Controller 完成业务逻辑后，要求 Model 改变状态
3 Model 将新的数据发送到 View，用户得到反馈

由于android中xml布局的功能性太弱,Activity实际上负责了View层与Controller层两者的工作，所以在android中mvc更像是这种形式：
Model->双向依赖<- controller/view
因此MVC架构在android平台上的主要存在以下问题：
1 Activity同时负责View与Controller层的工作，违背了单一职责原则
2 Model层与View层存在耦合，存在互相依赖，违背了最小知识原则


MVP架构介绍      //todo mvp的代码   这几个框架的代码示例
由于MVC架构在Android平台上的一些缺陷，MVP也就应运而生了,其架构图如下所示
MVP架构主要分为以下几个部分
View层：对应于Activity与XML,只负责显示UI,只与Presenter层交互，与Model层没有耦合
Presenter层： 主要负责处理业务逻辑，通过接口回调View层
Model层：主要负责网络请求，数据库处理等操作，这个没有什么变化  数据来源
架构设计_mvp.webp
1. view与Presenter是双向的，Presenter与Model都是双向的
2. view与model不发生联系，通过Presenter传递
3. View 非常薄，不部署任何业务逻辑，称为"被动视图"（Passive View），即没有任何主动性，而 Presenter非常厚，所有逻辑都部署在那里

我们可以看到，MVP解决了MVC的两个问题，即Activity承担了两层职责与View层与Model层耦合的问题  
但MVP架构同样有自己的问题
1 Presenter层通过接口与View通信，实际上持有了View的引用
2 但是随着业务逻辑的增加，一个页面可能会非常复杂，这样就会造成View的接口会很庞大


MVVM架构介绍
Model:数据来源  View:activity与xml   ViewModel
MVVM 模式将 Presenter 改名为 ViewModel，基本上与 MVP 模式完全一致，同样view与model不直接交互
唯一的区别是，它采用双向数据绑定（data-binding）：View的变动，自动反映在 ViewModel，反之亦然  
MVVM架构图如下所示：
架构设计_mvvm.webp
可以看出MVVM与MVP的主要区别在于,你不用去主动去刷新UI了，只要Model数据变了，会自动反映到UI上。换句话说，MVVM更像是自动化的MVP
  解决了MVP中Presenter与View耦合的问题，降低了接口数量

MVVM架构有什么不足
相信使用MVVM架构的同学都有如下经验，为了保证数据流的单向流动，LiveData向外暴露时需要转化成immutable的，这需要添加不少模板代码并且容易遗忘，
 如下所示
```
class TestViewModel : ViewModel() {
    //为保证对外暴露的LiveData不可变，增加一个状态就要添加两个LiveData变量
    private val _pageState: MutableLiveData<PageState> = MutableLiveData()
    val pageState: LiveData<PageState> = _pageState
    private val _state1: MutableLiveData<String> = MutableLiveData()
    val state1: LiveData<String> = _state1
    private val _state2: MutableLiveData<String> = MutableLiveData()
    val state2: LiveData<String> = _state2
    //...
}
```
如上所示，如果页面逻辑比较复杂，ViewModel中将会有许多全局变量的LiveData,并且每个LiveData都必须定义两遍，一个可变的，
  一个不可变的。这其实就是我通过MVVM架构写比较复杂页面时最难受的点。
其次就是View层通过调用ViewModel层的方法来交互的，View层与ViewModel的交互比较分散，不成体系
小结一下，在我的使用中，MVVM架构主要有以下不足
1 为保证对外暴露的LiveData是不可变的，需要添加不少模板代码并且容易遗忘
2 View层与ViewModel层的交互比较分散零乱，不成体系
