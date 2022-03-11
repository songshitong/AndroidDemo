https://www.jianshu.com/p/e42b638944ae  文章源码基于8.1及以前

///Window，PhoneWindow，DecorView，setContentView


一个顶级窗口查看和行为的一个抽象基类。这个类的实例作为一个顶级View添加到Window Manager
。它提供了一套标准的UI方法，比如添加背景，标题，menu等等。
当你需要用到Window的时候，你应该使用它的唯一实现类PhoneWindow。
可以看到，Window是一个抽象基类，它提供了一系列窗口的方法，比如设置背景，标题等等，
而它的唯一实现类则是PhoneWindow

class Window {
 
    //根据ID获取DecorView中的view
    public View findViewById(@IdRes int id) {
        return getDecorView().findViewById(id);
    }
    public abstract void setContentView(@LayoutRes int layoutResID);
    
    //window中有各种FEATURE，用来表示window的特性
    public static final int FEATURE_NO_TITLE = 1;
    
    //根布局的ID
    public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.content;
    
    ///设置window manager
    public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
                boolean hardwareAccelerated) {
            mAppToken = appToken;
            mAppName = appName;
            mHardwareAccelerated = hardwareAccelerated;
            if (wm == null) {
                wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            }
            ///创建WindowManger的实现类
            mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
        }
}


Window的唯一实现类 PhoneWindow
PhoneWindow 持有DecorView
public class PhoneWindow extends Window implements MenuBuilder.Callback {

    // This is the top-level view of the window, containing the window decor.
    //window中的顶级view
    private DecorView mDecor;

    // This is the view in which the window contents are placed. It is either
    // mDecor itself, or a child of mDecor where the contents go.
    //window中的内容，可能是mDecor也可能是mDecor的子view mContentParent是查找DecorView中的ID_ANDROID_CONTENT
    private ViewGroup mContentParent;

    
    public void setContentView(int layoutResID) {
            // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
            // decor, when theme attributes and the like are crystalized. Do not check the feature
            // before this happens.
            if (mContentParent == null) {
               //第一次调用的时候，调用installDecor方法
              //创建DecorView，并添加到mContentParent上
                installDecor();
            } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
               //标记当前内容加载有没有使用过度动画，也就是转场动画
               //如果内容已经加载过，并且不需要动画，则会调用removeAllViews
                mContentParent.removeAllViews();
            }
    
            if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
                //添加完Content后如有设置了FEATURE_CONTENT_TRANSITIONS则添加Scene来过度启动
                final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                        getContext());
                transitionTo(newScene);
            } else {
                //将我们的资源文件通过LayoutInflater对象转换为View树，并且添加至mContentParent视图中
               //将要加载的资源添加到mContentParent上
                mLayoutInflater.inflate(layoutResID, mContentParent);
            }
            mContentParent.requestApplyInsets();
            final Callback cb = getCallback();
            if (cb != null && !isDestroyed()) {
               //回调通知表示完成界面加载  setContentView完成后通知activity内容改变
                cb.onContentChanged();
            }
            mContentParentExplicitlySet = true;
        }
        
        
        private void installDecor() {
                if (mDecor == null) {
                //调用该方法创建new一个DecorView   generateDecor主要是new DecorView()
                    mDecor = generateDecor();
                    mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                    mDecor.setIsRootNamespace(true);
                    if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                        mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
                    }
                }
                //一开始DecorView未加载到mContentParent，所以此时mContentParent=null
                if (mContentParent == null) {
                //该方法将mDecorView添加到Window上绑定布局
                    mContentParent = generateLayout(mDecor);
        
                    // Set up decor part of UI to ignore fitsSystemWindows if appropriate.
                    mDecor.makeOptionalFitsSystemWindows();
        
                    final DecorContentParent decorContentParent = (DecorContentParent) mDecor.findViewById(
                            R.id.decor_content_parent);
                        
                        ...//添加其他资源
                        ...//设置转场动画
                }
            }
        
        ///生成Decor 
        protected DecorView generateDecor(int featureId) {
                return new DecorView(context, featureId, this, getAttributes());
            }   
            
        
         protected ViewGroup generateLayout(DecorView decor) {
                // Apply data from current theme.
                //根据当前设置的主题来加载默认布局
                TypedArray a = getWindowStyle();
                //如果你在theme中设置了window_windowNoTitle，则这里会调用到，其他方法同理，
                //这里是根据你在theme中的设置去设置的
                if (a.getBoolean(R.styleable.Window_windowNoTitle, false)) {
                    requestFeature(FEATURE_NO_TITLE);
                } else if (a.getBoolean(R.styleable.Window_windowActionBar, false)) {
                    // Don't allow an action bar if there is no title.
                    requestFeature(FEATURE_ACTION_BAR);
                }
                //是否有设置全屏
                if (a.getBoolean(R.styleable.Window_windowFullscreen, false)) {
                    setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN & (~getForcedWindowFlags()));
                }
                
                ...//省略其他加载资源
                
                // 添加布局到DecorView，前面说到，DecorView是继承与FrameLayout，它本身也是一个ViewGroup，
                而我们前面创建它的时候，只是调用了new DecorView，此时里面并无什么东西。而下面的步奏则是根据用户设置的Feature来创建相应的默认布局主题。
                举个例子，如果我在setContentView之前调用了requestWindowFeature(Window.FEATURE_NO_TITLE)，
                这里则会通过getLocalFeatures来获取你设置的feature，进而选择加载对应的布局，此时则是加载没有标题栏的主题，对应的就是R.layout.screen_simple
        
                int layoutResource;
                //getLocalFeatures来获取你设置的feature，进而选择加载对应的布局，这也就是为什么我们要在setContentView之前调用requesetFeature的原因
                int features = getLocalFeatures();
                // System.out.println("Features: 0x" + Integer.toHexString(features));
                if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
                    layoutResource = R.layout.screen_swipe_dismiss;
                } ... //省略其他判断方法
                } else {
                    // Embedded, so no decoration is needed.
                    layoutResource = R.layout.screen_simple;
                }
        
                mDecor.startChanging();
                //选择对应布局创建添加到DecorView中
                 mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
                //查找DecorView中的ID_ANDROID_CONTENT
                ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
                return contentParent;
            }   
            
            
       R.layout.screen_simple
       <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
           android:layout_width="match_parent"
           android:layout_height="match_parent"
           android:fitsSystemWindows="true"
           android:orientation="vertical">
           <ViewStub android:id="@+id/action_mode_bar_stub"
                     android:inflatedId="@+id/action_mode_bar"
                     android:layout="@layout/action_mode_bar"
                     android:layout_width="match_parent"
                     android:layout_height="wrap_content"
                     android:theme="?attr/actionBarTheme" />
           <FrameLayout
                android:id="@android:id/content"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:foregroundInsidePadding="false"
                android:foregroundGravity="fill_horizontal|top"
                android:foreground="?android:attr/windowContentOverlay" />
       </LinearLayout>  
       
       可以看到是LinearLayout里面包含了两个，因为设置可NoTitle，所以上面只有一个ViewStub，否则还有一个FrameLayout。
       也证明前面第一篇中说的，“DecorView只有一个子元素为LinearLayout。代表整个Window界面，包含通知栏，标题栏，内容显示栏三块区域。
       ”注意FrameLayout里面的id，@android:id/content ，我们setContentView的内容就是添加到这个FrameLayout中。
       
   
}

public class Activity extends ContextThemeWrapper implements Window.Callback{
  public void setContentView(@LayoutRes int layoutResID) {
          //调用phoneWindow.setContentView
          getWindow().setContentView(layoutResID);
          initWindowDecorActionBar();
      }
  
   //获取的就是Activity上的Window 如果mWindow为null，代表activity不在window上，不可见
   public Window getWindow() {
          return mWindow;
      }  
   
   // findViewById 即是通过window获取view
   public <T extends View> T findViewById(@IdRes int id) {
           return getWindow().findViewById(id);
       } 
       
   //Window.Callback 监听内容的改变    
   public void onContentChanged() {
       }        
}

DecorView 继承FrameLayout   Decor指代风格
既然是FrameLayout，也就可以加载布局文件，也就是说，我们那些标题栏，内容栏，顶级上看是加载在DecorView上的。
而DecorView则是由PhoneWindow负责添加
```
public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks {
   ViewGroup mContentRoot;
   
   //加载资源
   void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
           mDecorCaptionView = createDecorCaptionView(inflater);
           final View root = inflater.inflate(layoutResource, null);
           if (mDecorCaptionView != null) {
               if (mDecorCaptionView.getParent() == null) {
                   addView(mDecorCaptionView,
                           new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
               }
               mDecorCaptionView.addView(root,
                       new ViewGroup.MarginLayoutParams(MATCH_PARENT, MATCH_PARENT));
           } else {
   
               // Put it below the color views.
               addView(root, 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
           }
           mContentRoot = (ViewGroup) root;
           initializeElevation();
       }
}
```

setContentView流程
```
 public void setContentView(@LayoutRes int layoutResID) {
        //window为phoneWindow
        getWindow().setContentView(layoutResID);
        initWindowDecorActionBar();
    }
```
phoneWindow.setContentView
```
 @Override
    public void setContentView(int layoutResID) {
        // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
        // decor, when theme attributes and the like are crystalized. Do not check the feature
        // before this happens.
        if (mContentParent == null) {
            //创建DecorView，并添加到mContentParent上
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
            //将要加载的资源添加到mContentParent上
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            //回调通知表示完成界面加载
            cb.onContentChanged();
        }
    }
```

installDecor
```
private void installDecor() {
        if (mDecor == null) {
        //调用该方法创建new一个DecorView
            mDecor = generateDecor();
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        }
        //一开始DecorView未加载到mContentParent，所以此时mContentParent=null
        if (mContentParent == null) {
        //该方法将mDecorView添加到Window上绑定布局
            mContentParent = generateLayout(mDecor);

            // Set up decor part of UI to ignore fitsSystemWindows if appropriate.
            mDecor.makeOptionalFitsSystemWindows();

            final DecorContentParent decorContentParent = (DecorContentParent) mDecor.findViewById(
                    R.id.decor_content_parent);
                
                ...//添加其他资源
                ...//设置转场动画
        }
    }
```

generateLayout
```
 protected ViewGroup generateLayout(DecorView decor) {
        // Apply data from current theme.
        //根据当前设置的主题来加载默认布局
        TypedArray a = getWindowStyle();
        //如果你在theme中设置了window_windowNoTitle，则这里会调用到，其他方法同理，
        //这里是根据你在theme中的设置去设置的
        if (a.getBoolean(R.styleable.Window_windowNoTitle, false)) {
            requestFeature(FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.Window_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestFeature(FEATURE_ACTION_BAR);
        }
        //是否有设置全屏
        if (a.getBoolean(R.styleable.Window_windowFullscreen, false)) {
            setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN & (~getForcedWindowFlags()));
        }
        
        ...//省略其他加载资源
        
        // 添加布局到DecorView，前面说到，DecorView是继承与FrameLayout，它本身也是一个ViewGroup，而我们前面创建它的时候，
        //只是调用了new DecorView，此时里面并无什么东西。而下面的步奏则是根据用户设置的Feature来创建相应的默认布局主题。
        //举个例子，如果我在setContentView之前调用了requestWindowFeature(Window.FEATURE_NO_TITLE)，
        //这里则会通过getLocalFeatures来获取你设置的feature，进而选择加载对应的布局，此时则是加载没有标题栏的主题，
        //对应的就是R.layout.screen_simple

        int layoutResource;
        int features = getLocalFeatures();
        // System.out.println("Features: 0x" + Integer.toHexString(features));
        if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
            layoutResource = R.layout.screen_swipe_dismiss;
        } ... //省略其他判断方法
        } else {
            // Embedded, so no decoration is needed.
            layoutResource = R.layout.screen_simple;
            // System.out.println("Simple!");
        }

        mDecor.startChanging();
        //选择对应布局创建添加到DecorView中
        View in = mLayoutInflater.inflate(layoutResource, null);
        decor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mContentRoot = (ViewGroup) in;
        ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
        ...
        return contentParent;
    }
```

1 DecorView，顶级视图，继承与FrameLayout，setContentView则是添加在它里面的@id/content里
2 setContentView里面创建了DecorView，根据Theme，Feature添加了对应的布局文件
3 当setContentView设置显示后会回调Activity的onContentChanged方法
activity
  phoneWindow
  DecorView  继承FrameLayout
    ContentView