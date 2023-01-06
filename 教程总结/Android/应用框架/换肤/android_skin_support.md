
https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E4%B8%89%E6%96%B9%E5%BA%93%E5%8E%9F%E7%90%86/Android-skin-support%E6%8D%A2%E8%82%A4%E5%8E%9F%E7%90%86%E8%AF%A6%E8%A7%A3.md


简单总结一下原理
1 监听APP所有Activity的生命周期(registerActivityLifecycleCallbacks())
2 在每个Activity的onCreate()方法调用时setFactory(),设置创建View的工厂.将创建View的琐事交给SkinCompatViewInflater去处理.
3 库中自己重写了系统的控件(比如View对应于库中的SkinCompatView),实现换肤接口(接口里面只有一个applySkin()方法),表示该控件是支持换肤的.并且将这些控件在创建之后收集起来,方便随时换肤.
4 在库中自己写的控件里面去解析出一些特殊的属性(比如:background, textColor),并将其保存起来
5 在切换皮肤的时候,遍历一次之前缓存的View,调用其实现的接口方法applySkin(),在applySkin()中从皮肤资源(可以是从网络或者本地获取皮肤包)中获取资源.获取资源后设置其控件的background或textColor等,就可实现换肤

androidx appcompat  1.4.2
android-skin-support release-v4.0.1
1 先看一下AppCompatActivity
AppCompatActivity是对新特性在老设备的适配
action bar，dark themes， DrawerLayout
实现方式，将大部分方法和生命周期委托给AppCompatDelegate，AppCompatDelegate负责实现功能和扩展
```
delegate创建
  public AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, this);
        }
        return mDelegate;
    }

//方法代理    
public class AppCompatActivity extends FragmentActivity implements AppCompatCallback,
        TaskStackBuilder.SupportParentable, ActionBarDrawerToggle.DelegateProvider {
   public AppCompatActivity(@LayoutRes int contentLayoutId) {
        super(contentLayoutId);
        initDelegate();
    }

    private void initDelegate() {
        getSavedStateRegistry().registerSavedStateProvider(DELEGATE_TAG,
                new SavedStateRegistry.SavedStateProvider() {
                    @NonNull
                    @Override
                    public Bundle saveState() {
                        Bundle outState = new Bundle();
                        getDelegate().onSaveInstanceState(outState);
                        return outState;
                    }
                });
        addOnContextAvailableListener(new OnContextAvailableListener() {
            @Override
            public void onContextAvailable(@NonNull Context context) {
                final AppCompatDelegate delegate = getDelegate();
                delegate.installViewFactory();
                delegate.onCreate(getSavedStateRegistry()
                        .consumeRestoredStateForKey(DELEGATE_TAG));
            }
        });
    }
      @Override
    protected void onStart() {
        super.onStart();
        getDelegate().onStart();
    }
    ...
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }      
}    
```

AppCompatDelegate是一个抽象类
androidx appcompat-1.5.1  实现类
```
//创建AppCompatDelegate的实现类
AppCompatDelegate{
   public static AppCompatDelegate create(@NonNull Activity activity,
            @Nullable AppCompatCallback callback) {
        return new AppCompatDelegateImpl(activity, callback);
    }
}

class AppCompatDelegateImpl extends AppCompatDelegate
        implements MenuBuilder.Callback, LayoutInflater.Factory2 {
        
 }
```
配置LayoutInflater
```
    public void installViewFactory() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        if (layoutInflater.getFactory() == null) {
            LayoutInflaterCompat.setFactory2(layoutInflater, this);
        } else {
            if (!(layoutInflater.getFactory2() instanceof AppCompatDelegateImpl)) {
                Log.i(TAG, "The Activity's LayoutInflater already has a Factory installed"
                        + " so we can not install AppCompat's");
            }
        }
    }
    
  //Factory2监听view的创建  
  public interface Factory2 extends Factory {
        View onCreateView(@Nullable View parent, @NonNull String name,
                @NonNull Context context, @NonNull AttributeSet attrs);
    }    
```


换肤原理
通过LayoutInflater拦截View创建过程,配置不同的属性
调用流程
1 初始化
androidx skin.support
```
SkinCompatManager.withoutActivity(application).addInflater(new SkinAppCompatViewInflater());

    public static SkinCompatManager withoutActivity(Application application) {
        //初始化单例和SharePreference
        init(application);
        SkinActivityLifecycle.init(application);
        return sInstance;
    }
```
java/skin/support/app/SkinActivityLifecycle.java
```
    private SkinActivityLifecycle(Application application) {
        //监听activity创建
        application.registerActivityLifecycleCallbacks(this);
        installLayoutFactory(application);
        SkinCompatManager.getInstance().addObserver(getObserver(application));
    }
    
  @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (isContextSkinEnable(activity)) {
            给每个activity设置setFactory2
            installLayoutFactory(activity);
            updateWindowBackground(activity);
            if (activity instanceof SkinCompatSupportable) {
                ((SkinCompatSupportable) activity).applySkin();
            }
        }
    } 
  
      private void installLayoutFactory(Context context) {
        try {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            LayoutInflaterCompat.setFactory2(layoutInflater, getSkinDelegate(context));
        } catch (Throwable e) {
            Slog.i("SkinActivity", "A factory has already been set on this LayoutInflater");
        }
    }
    
  //SkinCompatDelegate是factory2的实现类  
  private SkinCompatDelegate getSkinDelegate(Context context) {
        if (mSkinDelegateMap == null) {
            mSkinDelegateMap = new WeakHashMap<>();
        }

        SkinCompatDelegate mSkinDelegate = mSkinDelegateMap.get(context);
        if (mSkinDelegate == null) {
            mSkinDelegate = SkinCompatDelegate.create(context);
            mSkinDelegateMap.put(context, mSkinDelegate);
        }
        return mSkinDelegate;
    }       
```
开启换肤的条件
1 配置了mSkinAllActivityEnable
2 带有注解Skinable
3 实现SkinCompatSupportable接口
```
  private boolean isContextSkinEnable(Context context) {
        return SkinCompatManager.getInstance().isSkinAllActivityEnable()
                || context.getClass().getAnnotation(Skinable.class) != null
                || context instanceof SkinCompatSupportable;
    }
``` 
java/skin/support/widget/SkinCompatSupportable.java
可应用换肤的接口
```
public interface SkinCompatSupportable {
    void applySkin();
}
```

java/skin/support/app/SkinCompatDelegate.java
```
public class SkinCompatDelegate implements LayoutInflater.Factory2 {
    private SkinCompatViewInflater mSkinCompatViewInflater; //实现具体的解析方法
    private List<WeakReference<SkinCompatSupportable>> mSkinHelpers = new CopyOnWriteArrayList<>();//收集可以换肤的view

    public View createView(View parent, final String name, @NonNull Context context,
                           @NonNull AttributeSet attrs) {
        if (mSkinCompatViewInflater == null) {
            mSkinCompatViewInflater = new SkinCompatViewInflater();
        }
        ... 
        //创建具体的view 
        return mSkinCompatViewInflater.createView(parent, name, context, attrs);
    }
    
     @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View view = createView(null, name, context, attrs);
        if (view == null) {
            return null;
        }
        //收集可以换肤的view
        if (view instanceof SkinCompatSupportable) {
            mSkinHelpers.add(new WeakReference<>((SkinCompatSupportable) view));
        }
        return view;
    }
}
```
SkinCompatViewInflater 解析过程
java/skin/support/app/SkinCompatViewInflater.java
```
 public final View createView(View parent, final String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        View view = createViewFromHackInflater(context, name, attrs);

        if (view == null) {
            view = createViewFromInflater(context, name, attrs);
        }
        if (view == null) {
            view = createViewFromTag(context, name, attrs);
        }
        if (view != null) {
            checkOnClickListener(view, attrs);
        }
        return view;
    }
 //使用SkinCompatManager配置的解析器，解析对应的view，可以通过 SkinCompatManager.getInstance().addInflater添加
 private View createViewFromInflater(Context context, String name, AttributeSet attrs) {
        View view = null;
        for (SkinLayoutInflater inflater : SkinCompatManager.getInstance().getInflaters()) {
            view = inflater.createView(context, name, attrs);
            if (view == null) {
                continue;
            } else {
                break;
            }
        }
        return view;
    }    
```

为什么要在SkinCompatViewInflater还要细化,还需要交由更细的SkinLayoutInflater来处理呢?
xfhy觉得是因为方便扩展,库中给出了几个SkinLayoutInflater,
有SkinAppCompatViewInflater（基础控件构建器）、SkinMaterialViewInflater（material design控件构造器）、
SkinConstraintViewInflater（ConstraintLayout构建器）、SkinCardViewInflater（CardView v7构建器）

androidx skin-support-appcompat
java/skin/support/app/SkinAppCompatViewInflater.java
```
public interface SkinLayoutInflater {
    View createView(@NonNull Context context, final String name, @NonNull AttributeSet attrs);
}
public class SkinAppCompatViewInflater implements SkinLayoutInflater, SkinWrapper {
 @Override
    public View createView(Context context, String name, AttributeSet attrs) {
        View view = createViewFromFV(context, name, attrs);

        if (view == null) {
            view = createViewFromV7(context, name, attrs);
        }
        return view;
    }
  //解析常用组件的包装，可以支持换肤  
  private View createViewFromFV(Context context, String name, AttributeSet attrs) {
        View view = null;
        if (name.contains(".")) {
            return null;
        }
        switch (name) {
            case "View":
                view = new SkinCompatView(context, attrs);
                break;
            case "LinearLayout":
                view = new SkinCompatLinearLayout(context, attrs);
                break;
           ...
        }
        return view;
    }  
    
  //解析androidx.appcompat.widget.Toolbar
  private View createViewFromV7(Context context, String name, AttributeSet attrs) {
        View view = null;
        switch (name) {
            case "androidx.appcompat.widget.Toolbar":
                view = new SkinCompatToolbar(context, attrs);
                break;
            default:
                break;
        }
        return view;
    }   
}
```
以TextView的皮肤类为例
java/skin/support/widget/SkinCompatTextView.java
```
public class SkinCompatTextView extends AppCompatTextView implements SkinCompatSupportable {
    private SkinCompatTextHelper mTextHelper;
    private SkinCompatBackgroundHelper mBackgroundTintHelper;
  
    //设置背景资源和字体
    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setTextAppearance(int resId) {
        setTextAppearance(getContext(), resId);
    }  
   
    @Override
    public void applySkin() {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySkin();
        }
        if (mTextHelper != null) {
            mTextHelper.applySkin();
        }
    } 
}
```
背景加载
java/skin/support/widget/SkinCompatBackgroundHelper.java
```
public abstract class SkinCompatHelper {
    ...
    abstract public void applySkin();
}

public class SkinCompatBackgroundHelper extends SkinCompatHelper {
    private final View mView;
    //对某个view设置背景
    public SkinCompatBackgroundHelper(View view) {
        mView = view;
    }
    
    public void onSetBackgroundResource(int resId) {
        mBackgroundResId = resId;
        applySkin();
    }

    @Override
    public void applySkin() {
        ...
        //res，皮肤包等加载drawable
        Drawable drawable = SkinCompatVectorResources.getDrawableCompat(mView.getContext(), mBackgroundResId);
        if (drawable != null) {
            int paddingLeft = mView.getPaddingLeft();
            int paddingTop = mView.getPaddingTop();
            int paddingRight = mView.getPaddingRight();
            int paddingBottom = mView.getPaddingBottom();
            //加载drawable，设置padding
            ViewCompat.setBackground(mView, drawable);
            mView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }
}
```
java/skin/support/content/res/SkinCompatVectorResources.java
```
  private Drawable getSkinDrawableCompat(Context context, int resId) {
       if (!SkinCompatResources.getInstance().isDefaultSkin()) {
         //当前是非默认皮肤
                try {
                    return SkinCompatDrawableManager.get().getDrawable(context, resId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }     
}          
```
java/skin/support/content/res/SkinCompatDrawableManager.java
```
Drawable getDrawable(@NonNull Context context, @DrawableRes int resId,
                         boolean failIfNotKnown) {
        checkVectorDrawableSetup(context);

        Drawable drawable = loadDrawableFromDelegates(context, resId);
        if (drawable == null) {
            drawable = createDrawableIfNeeded(context, resId);
        }
        if (drawable == null) {
            drawable = SkinCompatResources.getDrawable(context, resId);
        }

        if (drawable != null) {
            // Tint it if needed
            drawable = tintDrawable(context, resId, failIfNotKnown, drawable);
        }
        if (drawable != null) {
            // See if we need to 'fix' the drawable
            SkinCompatDrawableUtils.fixDrawable(drawable);
        }
        return drawable;
    }
```
java/skin/support/content/res/SkinCompatResources.java
```
 private Drawable getSkinDrawable(Context context, int resId) {
        //是否有皮肤颜色缓存 
        if (!SkinCompatUserThemeManager.get().isColorEmpty()) {
            ColorStateList colorStateList = SkinCompatUserThemeManager.get().getColorStateList(resId);
            if (colorStateList != null) {
                return new ColorDrawable(colorStateList.getDefaultColor());
            }
        }
        //是否有皮肤drawable缓存
        if (!SkinCompatUserThemeManager.get().isDrawableEmpty()) {
            Drawable drawable = SkinCompatUserThemeManager.get().getDrawable(resId);
            if (drawable != null) {
                return drawable;
            }
        }
         //加载策略非空  可以通过加载策略去加载drawable,开发者可自定义
        if (mStrategy != null) {
            Drawable drawable = mStrategy.getDrawable(context, mSkinName, resId);
            if (drawable != null) {
                return drawable;
            }
        }
         //非默认皮肤 去皮肤中加载资源
        if (!isDefaultSkin) {
            int targetResId = getTargetResId(context, resId);
            if (targetResId != 0) {
                return mResources.getDrawable(targetResId);
            }
        }
        //从context的resources加载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(resId, context.getTheme());
        }
        return context.getResources().getDrawable(resId);
    }
```


从皮肤包加载皮肤
其实皮肤包就是一个apk,只不过里面没有任何代码,只有一些需要换肤的资源或者颜色什么的.而且这些资源的名称必须和当前app中的资源名称是一致的,才能替换.
需要什么皮肤资源,直接去皮肤包里面去拿就好了
使用
```
SkinCompatManager.getInstance().loadSkin("night.skin", null, CustomSDCardLoader.SKIN_LOADER_STRATEGY_SDCARD);
```
流程：
java/skin/support/SkinCompatManager.java
```
    public AsyncTask loadSkin(String skinName, SkinLoaderListener listener, int strategy) {
        SkinLoaderStrategy loaderStrategy = mStrategyMap.get(strategy);
        if (loaderStrategy == null) {
            return null;
        }
        return new SkinLoadTask(listener, loaderStrategy).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, skinName);
    }
    
 private class SkinLoadTask extends AsyncTask<String, Void, String> {
        private final SkinLoaderListener mListener;
        private final SkinLoaderStrategy mStrategy;
        
    @Override
        protected String doInBackground(String... params) {
            synchronized (mLock) {
                while (mLoading) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mLoading = true;
            }
            try {
                if (params.length == 1) {
                  //根据加载策略去后台加载皮肤
                    String skinName = mStrategy.loadSkinInBackground(mAppContext, params[0]);
                    if (TextUtils.isEmpty(skinName)) {
                        SkinCompatResources.getInstance().reset(mStrategy);
                        return "";
                    }
                    return params[0];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            SkinCompatResources.getInstance().reset();
            return null;
        }
}
```

皮肤包的加载策略 查看ZipSDCardLoader
java/com/ximsfei/skindemo/loader/SkinSDCardLoader.java
```
public interface SkinLoaderStrategy {
        String loadSkinInBackground(Context context, String skinName);
        String getTargetResourceEntryName(Context context, String skinName, int resId);
        ColorStateList getColor(Context context, String skinName, int resId);
        ColorStateList getColorStateList(Context context, String skinName, int resId);
        Drawable getDrawable(Context context, String skinName, int resId);
        int getType();
    }
    
public abstract class SkinSDCardLoader implements SkinLoaderStrategy {
      @Override
    public String loadSkinInBackground(Context context, String skinName) {
        if (TextUtils.isEmpty(skinName)) {
            return skinName;
        }
        //皮肤包路径
        String skinPkgPath = getSkinPath(context, skinName);
        if (SkinFileUtils.isFileExists(skinPkgPath)) {
            String pkgName = SkinCompatManager.getInstance().getSkinPackageName(skinPkgPath);
            //根据路径获得resources
            Resources resources = SkinCompatManager.getInstance().getSkinResources(skinPkgPath);
            if (resources != null && !TextUtils.isEmpty(pkgName)) {
                //保存resources
                SkinCompatResources.getInstance().setupSkin(
                        resources,
                        pkgName,
                        skinName,
                        this);
                return skinName;
            }
        }
        return null;
    }
}

java/skin/support/content/res/SkinCompatResources.java
public void setupSkin(Resources resources, String pkgName, String skinName, SkinCompatManager.SkinLoaderStrategy strategy) {
       ...
        mResources = resources;
        mSkinPkgName = pkgName;
        mSkinName = skinName;
        mStrategy = strategy;
        //标记当前为非默认皮肤
        isDefaultSkin = false;
        SkinCompatUserThemeManager.get().clearCaches();
        for (SkinResources skinResources : mSkinResources) {
            skinResources.clear();
        }
    }


java/skin/support/SkinCompatManager.java
 public Resources getSkinResources(String skinPkgPath) {
        try {
            PackageInfo packageInfo = mAppContext.getPackageManager().getPackageArchiveInfo(skinPkgPath, 0);
            packageInfo.applicationInfo.sourceDir = skinPkgPath;
            packageInfo.applicationInfo.publicSourceDir = skinPkgPath;
            //根据PackageInfo加载resources
            Resources res = mAppContext.getPackageManager().getResourcesForApplication(packageInfo.applicationInfo);
            Resources superRes = mAppContext.getResources();
            return new Resources(res.getAssets(), superRes.getDisplayMetrics(), superRes.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
```
等到applySkin时，就从SkinCompatResources加载对应的资源



观察者相关 通知activity更新，是否更新等
观察者建立
java/skin/support/app/SkinActivityLifecycle.java
```
    private SkinActivityLifecycle(Application application) {
        application.registerActivityLifecycleCallbacks(this);
        installLayoutFactory(application);
        //被观察者SkinCompatManager  观察者LazySkinObserver
        SkinCompatManager.getInstance().addObserver(getObserver(application));
    }
    
    private LazySkinObserver getObserver(final Context context) {
        if (mSkinObserverMap == null) {
            mSkinObserverMap = new WeakHashMap<>();
        }
        LazySkinObserver observer = mSkinObserverMap.get(context);
        if (observer == null) {
            observer = new LazySkinObserver(context);
            mSkinObserverMap.put(context, observer);
        }
        return observer;
    }
    
      @Override
    public void onActivityResumed(Activity activity) {
        mCurActivityRef = new WeakReference<>(activity);
        if (isContextSkinEnable(activity)) {
            //添加观察者，并判断是否需要更新
            LazySkinObserver observer = getObserver(activity);
            SkinCompatManager.getInstance().addObserver(observer);
            observer.updateSkinIfNeeded();
        }
    } 
    
   
   private class LazySkinObserver implements SkinObserver {
        private final Context mContext;
        private boolean mMarkNeedUpdate = false; //默认不可以更新

        @Override
        public void updateSkin(SkinObservable observable, Object o) {
            // 当前Activity，或者非Activity，立即刷新，否则延迟到下次onResume方法中刷新。
            if (mCurActivityRef == null
                    || mContext == mCurActivityRef.get()
                    || !(mContext instanceof Activity)) {
                updateSkinForce();
            } else {
                //标记下一次更新
                mMarkNeedUpdate = true;
            }
        }

        void updateSkinIfNeeded() {
            if (mMarkNeedUpdate) {
                updateSkinForce();
            }
        }

        void updateSkinForce() {
            ...
            //更新皮肤
            if (mContext instanceof Activity && isContextSkinEnable(mContext)) {
                updateWindowBackground((Activity) mContext);
            }
            getSkinDelegate(mContext).applySkin();
            if (mContext instanceof SkinCompatSupportable) {
                ((SkinCompatSupportable) mContext).applySkin();
            }
            mMarkNeedUpdate = false;
        }
    } 
```


java/skin/support/SkinCompatManager.java
```
private class SkinLoadTask extends AsyncTask<String, Void, String> {
  protected void onPostExecute(String skinName) {
            synchronized (mLock) {
                //资源包加载成功，通知所有观察者更新
                if (skinName != null) {
                    SkinPreference.getInstance().setSkinName(skinName).setSkinStrategy(mStrategy.getType()).commitEditor();
                    notifyUpdateSkin();
                    if (mListener != null) {
                        mListener.onSuccess();
                    }
                }
                ...
            }
        }
}
```
