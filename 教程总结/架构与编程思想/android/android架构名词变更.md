
AAC  Android Architecture Components   2017 Google I/O发布一套帮助开发者解决Android架构设计的方案
1）生命周期相关的Lifecycle-aware Components
  LifeCycle,LiveData,ViewModel
2）数据库解决方案Room
android_架构_aac.webp
优势：  https://www.jianshu.com/p/7164ebaa00e9
1 通过它可以非常优雅的让数据与界面交互
2 并做一些持久化的东西
3 高度解耦
4 自动管理生命周期
5 而且不用担心内存泄漏的问题.

https://juejin.cn/post/7056983987859750919
MAD  Modern Android Development    2021 google I/O大会提出
其涵盖的内容： 
Android Studio ：持续改进的官方 IDE
Android App Bundle ：先进的应用打包和分发方式
Kotlin ：首推的编程语言
Jetpack ：独立于 AOSP 以外，汇集了大量开发框架的开发套件
Jetpack Compose：Android 平台重大变革的 UI 工具包




https://mp.weixin.qq.com/s/HOnDTPuY1l-gcPXaiqGtjw
Mavericks 又是 Airbnb 开源的一个库，没错，大名鼎鼎 Lottie 动画框架也是他们出品的。它是一个 Android MVI 结构的开源框架，
适用于需要构建复杂、高效、可维护的 Android 应用程序的场景，特别是在需要处理大量异步数据、网络请求和数据持久化的情况下。
Mavericks 的核心在于它的三大件，MavericksState, MavericksViewModel, 和 MavericksView，再配合它独有的 onEach 还有 withState
几乎就可以 cover 我们平时开发的大部分场景，并且能够简化代码，同时官方还提供了依赖用以方便地集成使用 ViewBinding 甚至 Hilt。

写到这里我突然想起这一个月来面试的不少同学，每次问题 App 架构，他们的回答总是，提到“数据驱动 UI 更新”就是 MVVM，提到“逻辑界面分离”就是 MVI。
那么问题来了，如果我的应用是 MVI架构，但是我的数据需要关心大量的状态，并且这些状态又要驱动 UI 更新呢？很少有同学能够答好这个问题。
恭喜你，Mavericks 就是用来解决这一问题的最好框架！