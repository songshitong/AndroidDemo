
https://juejin.cn/post/7022624191723601928
https://www.ruanyifeng.com/blog/2015/02/mvcmvp_mvvm.html
MVC
MVC架构主要分为以下几部分
视图层（View）：对应于xml布局文件和java代码动态view部分
控制层（Controller）：主要负责业务逻辑，在android中由Activity承担，同时因为XML视图功能太弱，
   所以Activity既要负责视图的显示又要加入控制逻辑，承担的功能过多。
模型层（Model）：主要负责网络请求，数据库处理，I/O的操作，即页面的数据来源
架构设计_mvc.webp   todo mvc有两种方式
view->controller->model->view
1 View 传送指令到 Controller
2 Controller 完成业务逻辑后，要求 Model 改变状态
3 Model 将新的数据发送到 View，用户得到反馈
代码实例   https://zh.wikipedia.org/wiki/MVC
```
/** 模拟 Model, View, Controller */
var M = {}, V = {}, C = {};

/** Model 负责存放资料 */
M.data = "hello world";

/** View 负责将资料输出给用户 */
V.render = (M) => { alert(M.data); }

/** Controller 作为连接 M 和 V 的桥梁 */
C.handleOnload = () => { V.render(M); }

/** 在网页读取的时候呼叫 Controller */
window.onload = C.handleOnload;
```


由于android中xml布局的功能性太弱,Activity实际上负责了View层与Controller层两者的工作，所以在android中mvc更像是这种形式：
Model->双向依赖<- controller/view
因此MVC架构在android平台上的主要存在以下问题：
1 Activity同时负责View与Controller层的工作，违背了单一职责原则
2 Model层与View层存在耦合，存在互相依赖，违背了最小知识原则

缺点：
1 Activity并不是一个标准的MVC模式中的Controller，它的首要职责是加载应用的布局和初始化用户界面，接受并处理来自用户的操作请求，
  进而做出响应。随着界面及其逻辑的复杂度不断提升，Activity类的职责不断增加，以致变得庞大臃肿。
2 View层和Model层直接进行交互，就必然会导致Model和View之间的耦合，不易开发和维护。

使用场景
适用场景：适用于较小，功能较少，业务逻辑较少的项目
内存消耗：activity,model
优点：
用一种业务逻辑、数据、界面显示分离的方法组织代码，将业务逻辑聚集到一个部件里面，在改进和个性化定制界面及用户交互的同时，
不需要重新编写业务逻辑



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

MVP优点：
Model与View完全分离，可以修改View而不影响Model
可以更高效的使用Model，因为Model与外界的交互都在Presenter内部
可以将一个Presenter用于多个View，而不需要改变Presenter的逻辑。这个特性很有用，因为View的变化总是比Model的变化频繁
把逻辑放在Presenter中，就可以脱离UI来测试这些逻辑（单元测试）

MVP缺点：
1 Presenter作为桥梁协调View和Model，会导致Presenter变得很臃肿，维护比较困难
2 Presenter（以下简称P）层与View（以下简称V）层是通过接口进行交互的，接口粒度不好控制。粒度太小，就会存在大量接口的情况，
使代码太过碎版化；粒度太大，解耦效果不好。同时对于UI的输入和数据的变化，需要手动调用V层或者P层相关的接口，相对来说缺乏自动性、
  监听性。如果数据的变化能自动响应到UI、UI的输入能自动更新到数据，那该多好！
3 MVP是以UI为驱动的模型，更新UI都需要保证能获取到控件的引用，同时更新UI的时候要考虑当前是否是UI线程，也要考虑Activity的生命周期（是否已经销毁等）
4 MVP是以UI和事件为驱动的传统模型，数据都是被动地通过UI控件做展示，但是由于数据的时变性，我们更希望数据能转被动为主动，
  希望数据能更有活性，由数据来驱动UI。

适用场景：视图界面不是很多的项目中
内存消耗：activity,presenter,model


MVVM架构介绍
Model:数据来源  View:activity与xml   ViewModel
MVVM 模式将 Presenter 改名为 ViewModel，基本上与 MVP 模式完全一致，同样view与model不直接交互
唯一的区别是，它采用双向数据绑定（data-binding）：View的变动，自动反映在 ViewModel，反之亦然  
MVVM架构图如下所示：
架构设计_mvvm.webp
view->viewModel->model->viewModel->view
可以看出MVVM与MVP的主要区别在于,你不用去主动去刷新UI了，只要Model数据变了，会自动反映到UI上。换句话说，MVVM更像是自动化的MVP
  解决了MVP中Presenter与View耦合的问题，降低了接口数量
使用场景：
1. 数据驱动视图，状态多复杂的页面可以使用

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

优点：
数据驱动
在常规的开发模式中，数据变化需要更新UI的时候，需要先获取UI控件的引用，然后再更新UI。获取用户的输入和操作也需要通过UI控件的引用。
 在MVVM中，这些都是通过数据驱动来自动完成的，数据变化后会自动更新UI，UI的改变也能自动反馈到数据层，数据成为主导因素。
 这样MVVM层在业务逻辑处理中只要关心数据，不需要直接和UI打交道，在业务处理过程中简单方便很多
低耦合
View可以独立于Model变化和修改，一个ViewModel可以绑定到不同的View上，当View变化的时候Model可以不变，当Model变化的时候View也可以不变。
可重用性
可以把一些View逻辑放在一个ViewModel里面，让很多view重用这段View逻辑。
独立开发
开发人员可以专注于业务逻辑和数据的开发（ViewModel），另一个开发人员可以专注于UI开发。
可测试
界面素来是比较难于测试的，而现在测试可以针对ViewModel来写。
提高可维护性 提供双向绑定机制，解决了MVP大量的手动View和Model同步的问题。从而提高了代码的可维护性。

缺点：
过于简单的图形界面不适用。
对于大型的图形应用程序，视图状态较多，ViewModel的构建和维护的成本都会比较高。
数据绑定的声明是指令式地写在View的模版当中的，这些内容是没办法去打断点debug的。
目前这种架构方式的实现方式比较不完善规范，常见的就是DataBinding框架。

适用场景：适用于界面展示的数据较多的项目。
内存消耗：activity,viewModel,model,dataBinding


MVI
MVI 与 MVVM 很相似，其借鉴了前端框架的思想，更加强调数据的单向流动和唯一数据源
Model: 与MVVM中的Model不同的是，MVI的Model主要指UI状态（State）。例如页面加载状态、控件位置等都是一种UI状态
View: 与其他MVX中的View一致，可能是一个Activity或者任意UI承载单元。MVI中的View通过订阅Model的变化实现界面刷新
Intent: 此Intent不是Activity的Intent，用户的任何操作都被包装成Intent后发送给Model层进行数据请求
数据流动
1 用户操作以Intent的形式通知Model
2 Model基于Intent更新State
3 View接收到State变化刷新UI。
数据永远在一个环形结构中单向流动，不能反向流动

总体设计
Model层承载UI状态，并暴露出ViewState供View订阅，ViewState是个data class,包含所有页面状态
View层通过Action更新ViewState，替代MVVM通过调用ViewModel方法交互的方式

优点：
1 强调数据单向流动，很容易对状态变化进行跟踪和回溯
2 使用ViewState对State集中管理，只需要订阅一个 ViewState 便可获取页面的所有状态，相对 MVVM 减少了不少模板代码
3 ViewModel通过ViewState与Action通信，通过浏览ViewState 和 Aciton 定义就可以理清 ViewModel 的职责，可以直接拿来作为接口文档使用。
缺点：
1 所有的操作最终都会转换成State，所以当复杂页面的State容易膨胀
2 state是不变的，因此每当state需要更新时都要创建新对象替代老对象，这会带来一定内存开销  //每一帧页面有对应状态类，对比flutter，这很合理

代码：
```
data class MainViewState(val fetchStatus: FetchStatus, val newsList: List<NewsItem>)  

sealed class MainViewEvent {
    data class ShowSnackbar(val message: String) : MainViewEvent()
    data class ShowToast(val message: String) : MainViewEvent()
}
```
1 ViewState只定义了两个，一个是请求状态，一个是页面数据
2 ViewEvent也很简单，一个简单的密封类，显示Toast与SnackBar
//个人：一个代表动作，一个代表数据？

ViewState更新
```
class MainViewModel : ViewModel() {
    private val _viewStates: MutableLiveData<MainViewState> = MutableLiveData()
    val viewStates = _viewStates.asLiveData()
    private val _viewEvents: SingleLiveEvent<MainViewEvent> = SingleLiveEvent()
    val viewEvents = _viewEvents.asLiveData()

    init {
        emit(MainViewState(fetchStatus = FetchStatus.NotFetched, newsList = emptyList()))
    }

    private fun fabClicked() {
        count++
        emit(MainViewEvent.ShowToast(message = "Fab clicked count $count"))
    }

    private fun emit(state: MainViewState?) {
        _viewStates.value = state
    }

    private fun emit(event: MainViewEvent?) {
        _viewEvents.value = event
    }
}
```
1 ViewEvents是一次性的，通过SingleLiveEvent实现，当然你也可以用Channel当来实现
2 当状态更新时，通过emit来更新状态


View监听ViewState
```
  private fun initViewModel() {
        viewModel.viewStates.observe(this) {
            renderViewState(it)
        }
        viewModel.viewEvents.observe(this) {
            renderViewEvent(it)
        }
    }
```
MVI 使用 ViewState 对 State 集中管理，只需要订阅一个 ViewState 便可获取页面的所有状态，相对 MVVM 减少了不少模板代码
//个人：页面状态多了，viewSate是不是很大

View通过Action更新State
```
class MainActivity : AppCompatActivity() {
	private fun initView() {
        fabStar.setOnClickListener {
            viewModel.dispatch(MainViewAction.FabClicked)
        }
    }
}
class MainViewModel : ViewModel() {
    fun dispatch(action: MainViewAction) =
        reduce(viewStates.value, action)

    private fun reduce(state: MainViewState?, viewAction: MainViewAction) {
        when (viewAction) {
            is MainViewAction.NewsItemClicked -> newsItemClicked(viewAction.newsItem)
            MainViewAction.FabClicked -> fabClicked()
            MainViewAction.OnSwipeRefresh -> fetchNews(state)
            MainViewAction.FetchNews -> fetchNews(state)
        }
    }
}
```
View通过Action与ViewModel交互，通过 Action 通信，有利于 View 与 ViewModel 之间的进一步解耦，同时所有调用以 Action 的形式汇总到一处，
也有利于对行为的集中分析和监控


mvc的发展
https://time.geekbang.org/column/article/140196