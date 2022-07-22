https://www.jianshu.com/p/51aaa65d5d25  
https://blog.csdn.net/mynameishuangshuai/article/details/51491074

android 四大组件  

activity  
六大生命周期  
生命周期：onCreate() -> onStart() - > onResume() -> onPause() -> onStop() -> onDestroy()  

![GitHub](https://raw.githubusercontent.com/songshitong/AndroidDemo/master/%E6%95%99%E7%A8%8B%E6%80%BB%E7%BB%93/img/activity_lifecycle.webp "GitHub,Social Coding")

onRestart()：表示activity正在重新启动 ，一般情况下，当前activity从不可见重新变成可见状态时，onRestart()就会被调用，
   这种情形一般是用户行为所导致的，比如用户按HOME键切换到桌面然后重新打开APP或者按back键。
onStart()：activity可见了，但是还没有出现在前台，还无法和用户交互
onPause()：表示activity正在停止，此时可以做一些存储数据，停止动画等工作，注意不能太耗时，因为这会影响到新activity的显示，
   onPause必须先执行完，新的activity的onResume才会执行


启动activity：系统先调用onCreate()，然后调用onStart()，最后调用onResume()方法，activity进入运行状态。
退出activity  onPause,onStop,onDestroy
进入另一个activity  onPause,onStop
从另一个activity返回  onRestart,onStart,onResume

onDestroy 与下一个activity的关系
启动新  旧activity先onPause，新activity后启动  onCreate,onStart,onResume，最后旧activity onStop
消失新  新activity先onPause, 旧activity onRestart,onStart,onResume   新activity onStop,onDestroy


弹出dialog        没有生命周期变化
被其他activity覆盖其上DialogActivity    onPause      DialogActivity消失 onResume

锁屏：onPause,onStop,停滞状态               解锁屏： onRestart,onStart,onResume



Home键回到主屏/menu键  onPause,onStop      返回  onRestart,onStart,onResume

activity与menu
activity onCreate,onStart,onResume,onCreateOptionsMenu,onPrepareOptionsMenu


异常情况生命周期  比如当系统资源配置发生改变以及系统内存不足时，activity就可能被杀死
1 比如说当前activity处于竖屏状态，如果突然旋转屏幕，由于系统配置发生了改变，在默认情况下，activity就会被销毁并且重新创建，
  当然我们也可以阻止系统重新创建我们的activity
  防止系统配置更改activity重建： android:configChanges="orientation|screenSize"用来设置 activity配置改变
    （不仅仅是屏幕方向，还有语言、地区等等）的集合。
  当一个配置改变在运行中发生时，
     activity默认情况下会先销毁然后重新创建。但是，如果通过这个属性声明了某个配置后，将可以避免上面的情况，而是依然运行，
       并回调 onConfigurationChanged() 方法，必须配置为"orientation|screenSize"，其他仍然会进行重建
     android:configChanges="orientation|keyboardHidden|screenSize"
  横竖屏切换   先销毁onPause，onStop，onDestroy 后重建onCreate，onStart，onResume
  状态保存onSaveInstanceState和onRestoreInstanceState  重建会调用状态保存方法
     onSaveInstanceState Android P以前在onStop以前，Android P以后在onStop之后，可以调用该方法进行状态保存
     onRestoreInstanceState 在onStart和onPostCreate之间调用，可以调用该方法进行状态恢复
2 资源内存不足导致低优先级的activity被杀死
   这里的情况和前面的情况1数据存储和恢复是完全一致的，activity按照优先级从高到低可以分为如下三种：
   （1）前台activity---正在和用户交互的activity，优先级最高
   （2）可见但非前台activity---比如activity中弹出了一个对话框，导致activity可见但是位于后台无法和用户直接交互。
   （3）后台activity---已经被暂停的activity，比如执行了onStop，优先级最低。

横竖屏切换代码
```
   //手机，竖屏   
       MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  
     
  //平板，横屏   
       MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);   
```
  

activity与fragment
activity重建伴随着fragment创建，fragment只是不显示
activity onCreate;fragment onAttach,onCreate,onCreateView,onActivityCreated,onStart;activity onStart,onResume;fragment onResume
销毁
fragment onPause;activity onPause;fragment onStop;activity onStop;fragment onDestroy;activity onDestroy
  锁屏  fragment onPause;activity onPause;fragment onStop;activity onStop
  解锁  activity onRestart;fragment onStart; activity onStart,onResume;fragment onResume
  


activity的启动模式   todo 任务栈调度源码
standard模式：默认启动模式，每次启动一个Activity都会重新创建一个新的实例，不管这个实例存不存在，在这种模式下，
   activity默认会进入启动它的activity所属的任务栈中。 注意：在非activity类型的context（如ApplicationContext）并没有所谓的任务栈，
   所以不能通过ApplicationContext去启动standard模式的activity。

singleTop模式：栈顶复用模式。如果新activity位于任务栈的栈顶的时候，activity不会被重新创建，同时它的onNewIntent方法会被回调，
    通过此方法的参数我们可以去除当前请求的信息。 注意：这个activity的onCreate，onStart，onResume不会被回调，因为他们并没有发生改变。
    如果栈顶不存在该Activity的实例，则情况与standard模式相同,进行重新创建；如果当前栈中不存在该Activity的实例时，其行为同standard启动模式
    standard和singleTop启动模式都是在原任务栈中新建Activity实例，不会启动新的Task，即使你指定了taskAffinity属性
登录页面、WXPayEntryActivity、WXEntryActivity 、推送通知栏
   适合接收通知启动的内容显示页面，当收到多条新闻推送时，用于展示新闻的 Activity 设置成此模式，根据传来的 Intent 数据显示不同的新闻信息，
   不会启动多个 Activity

singleTask模式：栈内复用模式。只要activity在一个栈中存在，那么多次启动此activity不会被重新创建单例，复用时会将它上面的 Activity 全部出栈，
       系统会回调onNewIntent。
       比如activityA，系统首先会寻找是否存在A想要的任务栈，这个任务栈就是通过taskAffinity属性指定，如果没有则创建一个新的任务栈，
         然后把activityA压入栈，
       如果存在任务栈，然后再看看有没有activityA的实例，如果实例存在，那么就会把A调到栈顶并调用它的onNewIntent方法
          (activity处于不可见onRestart,onStart,onResume)，
       如果不存在则把它实例化(onCreate,onStart,onResume)并压入栈。
程序模块逻辑入口:主页面（Fragment的containerActivity）、WebView页面、扫一扫页面、电商中：购物界面，确认订单界面，付款界面
   适合作为程序入口点，例如浏览器的主界面，不管从多少个应用启动浏览器，只会启动主界面一次，其余情况都会走 onNewIntent，
  并且会清空主界面上的其它页面  //往后退没有其他的，直接退出了

singleInstance模式：单实例模式。这种模式的activity只能单独地位于一个任务栈中。由于站内复用特性，后续的请求均不会创建新的activity实例
   该模式具备singleTask模式的所有特性外，与它的区别就是，这种模式下的Activity会单独占用一个Task栈，具有全局唯一性，
   即整个系统中就这么一个实例，由于栈内复用的特性，后续的请求均不会创建新的Activity实例，除非这个特殊的任务栈被销毁了。
   以singleInstance模式启动的Activity在整个系统中是单例的，如果在启动这样的Activity时，已经存在了一个实例，
    那么会把它所在的任务调度到前台，重用这个实例
系统Launcher、锁屏键、来电显示等系统应用



Activity 常用 Flags
FLAG_ACTIVITY_NEW_TASK
为 Activity 指定 singleTask 启动模式，效果和在 XML 中指定该模式相同
FLAG_ACTIVITY_SINGLE_TOP
为 Activity 指定 singleTop 启动模式，效果和在 XML 中指定该模式相同
FLAG_ACTIVITY_CLEAR_TOP
具有此标记的 Activity，当它启动时，在同一个任务栈中所有位于它上面的 Activity 都要出栈
FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
具有这个标记的 Activity 不会出现在历史 Activity 的列表中，在某些情况下我们不希望用户通过历史列表回到我们的 Activity 的
   时候这个标记比较有用。它等同于在 XML 中指定 Activity 的属性 android:excludeFromRecents="true"。


taskAffinity 是否相同会直接影响到后台是否在同一张任务卡片里
默认情况下，所有activity所需的任务栈的名字为应用的包名，可以通过给activity指定TaskAffinity属性来指定任务栈，
   **这个属性值不能和包名相同，否则就没有意义 ** 。

taskAffinity属性呢，可以简单的理解为任务相关性。
这个参数标识了一个Activity所需任务栈的名字，默认情况下，所有Activity所需的任务栈的名字为应用的包名
我们可以单独指定每一个Activity的taskAffinity属性覆盖默认值
一个任务的affinity决定于这个任务的根activity（root activity）的taskAffinity
在概念上，具有相同的affinity的activity（即设置了相同taskAffinity属性的activity）属于同一个任务
为一个activity的taskAffinity设置一个空字符串，表明这个activity不属于任何task

很重要的一点taskAffinity属性不对standard和singleTop模式有任何影响，即时你指定了该属性为其他不同的值，
  这两种启动模式下不会创建新的task（如果不指定即默认值，即包名）
<activity  android:taskAffinity="com.demo.singletop"/>


https://www.jianshu.com/p/388dbdbcc197
一个完全透明的Activity有很多用处，比如在应用启动的时候，可以用它处理根据用户是否登录的状态来跳转相应界面的情况，实际上微信启动的时候就是做了这种效果、或者在Server检测版本更新的时候弹出它来显示对话框
不展示的activity
android:theme="@android:style/Theme.NoDisplay"
透明的activity
android:theme="@android:style/Theme.Translucent"
自己定义theme
```
<style name="TranslucentTheme">
   <item name="android:windowBackground">@android:color/transparent</item>
   <item name="android:colorBackgroundCacheHint">@null</item>
   <item name="android:windowIsTranslucent">true</item>
   <item name="android:windowAnimationStyle">@android:style/Animation</item>
   <item name="android:windowNoTitle">true</item>
   <item name="android:windowContentOverlay">@null</item>
</style>
```
1像素activity https://www.jianshu.com/p/ef4a9531bc15
```
public class OnePiexlActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置1像素
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);  
    }
}
//设置透明主题
    <style name="OnePixelActivity" parent="android:Theme.Holo.Light.NoActionBar">//无标题
        <item name="android:windowIsTranslucent">true</item>//透明
    </style>
```