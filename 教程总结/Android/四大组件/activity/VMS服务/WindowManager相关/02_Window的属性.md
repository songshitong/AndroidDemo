http://liuwangshu.cn/framework/wm/2-window-property.html

1.概述
上一篇文章中我们讲过了Window、WindowManager和WMS之间的关系，WMS是Window的最终管理者，Window好比是员工，WMS是老板，
为了方便老板管理员工则需要定义一些“协议”，这些“协议”就是Window的属性，被定义在WindowManager的内部类LayoutParams中，
了解Window的属性能够更好的理解WMS的内部原理。
Window的属性有很多种，与应用开发最密切的有三种，它们分别是Type(Window的类型)、Flag(Window的标志)和SoftInputMode（软键盘相关模式），
下面分别介绍这三种Window的属性

2.Window的类型和显示次序
Window的类型有很多种，比如应用程序窗口、系统错误窗口、输入法窗口、PopupWindow、Toast、Dialog等等。总来来说分为三大类分别是：
Application Window（应用程序窗口）、Sub Windwow（子窗口）、System Window（系统窗口），每个大类又包含了很多种类型，
它们都定义在WindowManager的静态内部类LayoutParams中，接下来我们分别对这三大类进行讲解。

应用程序窗口
Activity就是一个典型的应用程序窗口，应用程序窗口包含的类型如下所示。
frameworks/base/core/java/android/view/WindowManager.java
```
public static final int FIRST_APPLICATION_WINDOW = 1;//1
public static final int TYPE_BASE_APPLICATION   = 1;//窗口的基础值，其他的窗口值要大于这个值
public static final int TYPE_APPLICATION        = 2;//普通的应用程序窗口类型
public static final int TYPE_APPLICATION_STARTING = 3;//应用程序启动窗口类型，用于系统在应用程序窗口启动前显示的窗口。
public static final int TYPE_DRAWN_APPLICATION = 4;
public static final int LAST_APPLICATION_WINDOW = 99;//2
```

应用程序窗口共包含了以上几种Type值，其中注释1处的Type表示应用程序窗口类型初始值，注释2处的Type表示应用程序窗口类型结束值，
也就是说应用程序窗口的Type值范围为1到99，这个数值的大小涉及到窗口的层级，后面会讲到
子窗口
子窗口，顾名思义，它不能独立的存在，需要附着在其他窗口才可以，PopupWindow就属于子窗口。子窗口的类型定义如下所示：
```
public static final int FIRST_SUB_WINDOW = 1000;//子窗口类型初始值
public static final int TYPE_APPLICATION_PANEL = FIRST_SUB_WINDOW;
public static final int TYPE_APPLICATION_MEDIA = FIRST_SUB_WINDOW + 1;
public static final int TYPE_APPLICATION_SUB_PANEL = FIRST_SUB_WINDOW + 2;
public static final int TYPE_APPLICATION_ATTACHED_DIALOG = FIRST_SUB_WINDOW + 3;
public static final int TYPE_APPLICATION_MEDIA_OVERLAY  = FIRST_SUB_WINDOW + 4; 
public static final int TYPE_APPLICATION_ABOVE_SUB_PANEL = FIRST_SUB_WINDOW + 5;
public static final int LAST_SUB_WINDOW = 1999;//子窗口类型结束值

```

子窗口的Type值范围为1000到1999。

系统窗口
Toast、输入法窗口、系统音量条窗口、系统错误窗口都属于系统窗口。系统窗口的类型定义如下所示：
```
public static final int FIRST_SYSTEM_WINDOW     = 2000;//系统窗口类型初始值
public static final int TYPE_STATUS_BAR         = FIRST_SYSTEM_WINDOW;//系统状态栏窗口
public static final int TYPE_SEARCH_BAR         = FIRST_SYSTEM_WINDOW+1;//搜索条窗口
public static final int TYPE_PHONE              = FIRST_SYSTEM_WINDOW+2;//通话窗口
public static final int TYPE_SYSTEM_ALERT       = FIRST_SYSTEM_WINDOW+3;//系统ALERT窗口
public static final int TYPE_KEYGUARD           = FIRST_SYSTEM_WINDOW+4;//锁屏窗口
public static final int TYPE_TOAST              = FIRST_SYSTEM_WINDOW+5;//TOAST窗口
...

public static final int LAST_SYSTEM_WINDOW      = 2999;//系统窗口类型结束值
```

系统窗口的类型值有接近40个，这里只列出了一小部分， 系统窗口的Type值范围为2000到2999。

窗口显示次序
当一个进程向WMS申请一个窗口时，WMS会为窗口确定显示次序。为了方便窗口显示次序的管理，手机屏幕可以虚拟的用X、Y、Z轴来表示，
其中Z轴垂直于屏幕，从屏幕内指向屏幕外，这样确定窗口显示次序也就是确定窗口在Z轴上的次序，这个次序称为Z-Oder。Type值是Z-Oder排序的依据，
我们知道应用程序窗口的Type值范围为1到99，子窗口1000到1999 ，系统窗口 2000到2999，，一般情况下，Type值越大则Z-Oder排序越靠前
，就越靠近用户。当然窗口显示次序的逻辑不会这么简单，情况会比较多，举个常见的情况：当多个窗口的Type值都是TYPE_APPLICATION，
这时WMS会结合各种情况给出最终的Z-Oder，这个逻辑不在本文的讨论范围，这里我们只需要知道窗口显示次序的基本规则就好。

3.Window的标志
Window的标志也就是Flag，用于控制Window的显示，同样被定义在WindowManager的内部类LayoutParams中，一共有20多个，
这里我们给出几个比较常用。
Flag	                         描述
FLAG_ALLOW_LOCK_WHILE_SCREEN_ON	只要窗口可见，就允许在开启状态的屏幕上锁屏
FLAG_NOT_FOCUSABLE	      窗口不能获得输入焦点，设置该标志的同时，FLAG_NOT_TOUCH_MODAL也会被设置
FLAG_NOT_TOUCHABLE	      窗口不接收任何触摸事件
FLAG_NOT_TOUCH_MODAL	  在该窗口区域外的触摸事件传递给其他的Window,而自己只会处理窗口区域内的触摸事件
FLAG_KEEP_SCREEN_ON	      只要窗口可见，屏幕就会一直亮着
FLAG_LAYOUT_NO_LIMITS	  允许窗口超过屏幕之外
FLAG_FULLSCREEN	          隐藏所有的屏幕装饰窗口，比如在游戏、播放器中的全屏显示
FLAG_SHOW_WHEN_LOCKED	  窗口可以在锁屏的窗口之上显示
FLAG_IGNORE_CHEEK_PRESSES	当用户的脸贴近屏幕时（比如打电话），不会去响应此事件
FLAG_TURN_SCREEN_ON	       窗口显示时将屏幕点亮

设置Window的Flag有三种方法，第一种是通过Window的addFlags方法：
```
Window mWindow =getWindow(); 
mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
```
第二种通过Window的setFlags方法:
```
Window mWindow =getWindow();            
mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
,WindowManager.LayoutParams.FLAG_FULLSCREEN);
```
其实Window的addFlags方法内部会调用setFlags方法，因此这两种方法区别不大。
第三种则是给LayoutParams设置Flag，并通过WindowManager的addView方法进行添加，如下所示
```
WindowManager.LayoutParams mWindowLayoutParams =
              new WindowManager.LayoutParams();
      mWindowLayoutParams.flags=WindowManager.LayoutParams.FLAG_FULLSCREEN;
      WindowManager mWindowManager =(WindowManager) getSystemService(Context.WINDOW_SERVICE);  
      TextView mTextView=new TextView(this);
      mWindowManager.addView(mTextView,mWindowLayoutParams);
                                                                                                                     
```

4.软键盘相关模式
窗口和窗口的叠加是非常常见的场景，但如果其中的窗口是软键盘窗口，可能就会出现一些问题，比如典型的用户登录界面，
默认的情况弹出的软键盘窗口可能会盖住输入框下方的按钮，这样用户体验会非常糟糕。
为了使得软键盘窗口能够按照期望来显示，WindowManager的静态内部类LayoutParams中定义了软键盘相关模式，这里给出常用的几个：
SoftInputMode	                描述
SOFT_INPUT_STATE_UNSPECIFIED	没有指定状态,系统会选择一个合适的状态或依赖于主题的设置
SOFT_INPUT_STATE_UNCHANGED	    不会改变软键盘状态
SOFT_INPUT_STATE_HIDDEN	        当用户进入该窗口时，软键盘默认隐藏
SOFT_INPUT_STATE_ALWAYS_HIDDEN	当窗口获取焦点时，软键盘总是被隐藏
SOFT_INPUT_ADJUST_RESIZE	    当软键盘弹出时，窗口会调整大小
SOFT_INPUT_ADJUST_PAN	        当软键盘弹出时，窗口不需要调整大小，要确保输入焦点是可见的
从上面给出的SoftInputMode ，可以发现，它们与AndroidManifest中Activity的属性android:windowSoftInputMode是对应的。
因此，除了在AndroidMainfest中为Activity设置android:windowSoftInputMode以外还可以在Java代码中为Window设置SoftInputMode：
```
getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
```

结语
好了，就讲到这里，本篇文章讲了Window的属性，分别是Type(Window的类型)、Flag(Window的标志)和SoftInputMode（软键盘相关模式），
这些知识会为后续的介绍WMS的系列文章打下基础。本系列的下一篇会介绍WindowManager添加Window的过程，敬请期待