// Window和WindowManager的创建与Activity
https://www.jianshu.com/p/e42b638944ae

ViewManager接口定义了一组规则，也就是add、update、remove的操作View接口。也就是说ViewManager是用来添加和移除activity中View的接口，
可以通过Context.getSystemService 获取
public interface ViewManager
{
    public void addView(View view, ViewGroup.LayoutParams params);
    public void updateViewLayout(View view, ViewGroup.LayoutParams params);
    public void removeView(View view);
}

ViewManager 的实现类 ViewGroup
public abstract class ViewGroup extends View implements ViewParent, ViewManager {
 
    public void addView(View child, LayoutParams params) {
        addView(child, -1, params);
    }
    
  
    可以看到ViewGroup里面实现了ViewManager接口，View通过ViewGroup的addView方法添加到ViewGroup中，
    而ViewGroup层层嵌套到最顶级都会显示在在一个窗口Window中
    public void addView(View child, int index, LayoutParams params) {
        // addViewInner() will call child.requestLayout() when setting the new LayoutParams
        // therefore, we call requestLayout() on ourselves before, so that the child's request
        // will be blocked at our level
        requestLayout();
        invalidate(true);
        addViewInner(child, index, params, false);
    }

ViewManager 的继承 WindowManager
WindowManager字面理解就是窗口管理器，每一个窗口管理器都与一个的窗口显示绑定。获取实例可以通过
Context.getSystemService(Context.WINDOW_SERVICE)获取。既然继承了ViewManager，那么它也就可以进行添加删除View的操作了，
不过它的操作放在它的实现类WindowManagerImpl里面
public interface WindowManager extends ViewManager {
    //addView时它的LayoutParams无效则会被抛出，或是添加第二个View的时候没有移除第一个View则会被抛出
    public static class BadTokenException extends RuntimeException{...}
    
    //如果一个窗口是在一个二级的显示上而指定的显示找不到则会被抛出
    public static class InvalidDisplayException extends RuntimeException{...}
    
    //返回当前WindowManager管理的显示Display
    public Display getDefaultDisplay()；
    
    //表示从窗口上移除View，一般是当View调用了onDetachedFromWindow也就是从Window上分开后，把它移除
    public void removeViewImmediate(View view);
    
    //静态内部类。显然是Window的布局参数，里面定义了一系列的窗口属性
    public static class LayoutParams extends ViewGroup.LayoutParams
        implements Parcelable
        
 }
 
 
WindowManager的实现类  实际实现WindowManagerGlobal
public final class WindowManagerImpl implements WindowManager {
    private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
    private final Display mDisplay;
    private final Window mParentWindow;
     @Override
    public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        applyDefaultToken(params);
        mGlobal.addView(view, params, mDisplay, mParentWindow);
    }
    ...
     @Override
    public void removeView(View view) {
        mGlobal.removeView(view, false);
    }
} 

WindowManagerGlobal
public final class WindowManagerGlobal {
    private static final String TAG = "WindowManager";
     public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
        ViewRootImpl root;
        View panelParentView = null;
                
        ...
                    
        root = new ViewRootImpl(view.getContext(), display);
        view.setLayoutParams(wparams);
        mViews.add(view);
        mRoots.add(root);
        mParams.add(wparams);
        //ViewRootImpl开始绘制view
        root.setView(view, wparams, panelParentView)
     }
}


Window如何创建如何与WindowManager绑定与Activity绑定呢，这个时候就需要一个场景来逐一理解。
我们都知道每一个Activity都是与一个Window绑定一起的，那么Window的创建以及WindowManager的绑定会不会在创建启动Activity的过程中就绑定的呢。
activity 创建过程略过
activity.attach

final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor) {
        //ContextImpl的绑定
        attachBaseContext(context);
        //在当前Activity创建Window
        mWindow = new PhoneWindow(this);
        mWindow.setCallback(this);
        mWindow.setOnWindowDismissedCallback(this);
        mWindow.getLayoutInflater().setPrivateFactory(this);
        ...
        //为Window设置WindowManager
        mWindow.setWindowManager(
                (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
                mToken, mComponent.flattenToString(),
                (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
        if (mParent != null) {
            mWindow.setContainer(mParent.getWindow());
        }
        //创建完后通过getWindowManager就可以得到WindowManager实例
        mWindowManager = mWindow.getWindowManager();
        mCurrentConfig = config;
    }

可以看到在Activity创建到attach的时候，对应的Window窗口也被创建起来，而且Window也与WindowManager绑定。
而mWindow，和mWindowManager则是Activity的成员变量。
可以看到这里WindiwManager的创建是context.getSystemService(Context.WINDOW_SERVICE)

mWindow.setWindowManager 会创建WindowManager的实现类
我们平时在Activity中使用getWindow（）和getWindowManager，就是返回对应这两个成员变量