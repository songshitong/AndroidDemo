https://www.jianshu.com/p/accfec53cfe6      android 8.0

总结：
LayoutInflater每个activity一个
xml解析 XmlResourceParser
反射  constructor.newInstance
优先遍历的目录 android/widget,android/webkit,android/app


对于 LayoutInflater，相信每个 Android 开发人员都不会感到陌生。业界一般称它为布局解析器（或填充器），翻开 LayoutInflater 源码发现它是一个抽象类，
 我们先来看下它的自我介绍。

LayoutInflater 就是将 XML 布局文件实例化为对应的 View 对象，LayoutInflater 不能直接通过 new 的方式获取，需要通过 
  Activity.getLayoutInflater() 或 Context.getSystemService() 获取与当前 Context 已经关联且正确配置的标准 LayoutInflater。
//从Context.getSystemService得知LayoutInflater 作为系统服务提供能力

也就是说 LayoutInflater 不能被外部实例化，只能通过系统提供的固有方式获取，但也正因如此，相信很多开发人员对它的认识仍然停留在如下代码：
```
final View content = LayoutInflater.from(this).inflate(R.layout.content, root, false);
```

今天我们就从源码的角度，进一步分析 LayoutInflater 的工作原理，主要涉及到如下几块儿内容：
LayoutInflater 创建过程与实际类型
LayoutInflater 的布局解析原理
不容忽视的 View 创建耗时
LayoutInflater 的高阶使用技巧


LayoutInflater 创建过程与实际类型
系统在 Context 中默认提供的两种获取 LayoutInflater 的方式如下：
```
final LayoutInflater getLayoutInflater = getLayoutInflater();
final LayoutInflater getSystemServiceInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
```

不过它们与直接使用 LayoutInflater.from() 除了 API 不同并没有本质上的差异，所以我们直接从 LayoutInflater 的 from 方法开始入手：
```
public static LayoutInflater from(Context context) {
    // 这里的context可以是Activity、Application、Service。
    LayoutInflater LayoutInflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    if (LayoutInflater == null) {
        throw new AssertionError("LayoutInflater not found.");
    }
    return LayoutInflater;
}
```

注意参数 Context，这里可以是 Activity、Application 或 Service。不知大家是否有跟踪过它们之间有什么区别吗？接下类我们就重点分析下这部分内容。

Context 的 getSystemService 方法默认是抽象的，看下它在 Context 中的声明：   
```
public abstract @Nullable String getSystemServiceName(@NonNull Class<?> serviceClass);
```

这里我们主要以 Activity 为例（其他类型也会在此引申出），在 Activity 的直接父类 ContextThemeWrapper 中重写了 getSystemService 方法。
```
@Override
public Object getSystemService(String name) {
    if (LAYOUT_INFLATER_SERVICE.equals(name)) {
        if (mInflater == null) {
            // 每个Activity都有自己独一无二的Layoutflater
            // 这里首先拿到在SystemServiceRegistry中注册的Application的Layoutflater
            // 然后根据该创建属于每个Activity的PhoneLayoutInflater     cloneInContext根据context创建LayoutInflater
            mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
        }
        return mInflater;
    }
    return getBaseContext().getSystemService(name);
}
```

可以看到，Activity 对于 LayoutInflater 服务的 LAYOUT_INFLATER_SERVICE 做了单独处理，使每个 Activity 都有其独立的 LayoutInflater。
  否则直接通过 getBaseContext().getSystemService() 获取相关服务。

这里，我们有必要先跟踪下 getBaseContext()，它的声明在 ContextThemeWrapper 的直接父类 ContextWrapper 中，如下：
```
public Context getBaseContext() {
    // mBase的实际类型是ContextImpl
    return mBase;
}
```
注意：ContextWrapper 是 Application 和 Service 的直接父类
mBase 的实际类型是 ContextImpl。在 Android 中，Application、Service 和 Activity 在创建后会首先回调其 attach 方法，
   并在该方法为其关联一个 ContextImpl 对象（该部分源码可以参考 Activity / Application 的创建过程在 ActivityThread 中）。

故，这里的 getSystemService() 实际调用到 ContextImpl 的 getSystemService 方法：
```
@Override
public Object getSystemService(String name) {
    return SystemServiceRegistry.getSystemService(this, name);
}
```

可以看到在 ContextImpl 内部，又委托给了 SystemServiceRegistry。SystemServiceRegistry 是应用进程的系统服务注册机，
  在其内部的静态代码块中默认注册了大量系统服务，包括 WINDOW_SERVICE、LOCATION_SERVICE 、AUDIO_SERVICE 等等，
  这里我们重点看下 LayoutInflater 的注册过程：
frameworks/base/core/java/android/app/SystemServiceRegistry.java
```
static {
    // ... 省略
    registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class,
                new CachedServiceFetcher<LayoutInflater>() {
            @Override
            public LayoutInflater createService(ContextImpl ctx) {
                // LayoutInflater 的实际类型是PhoneLayoutInflater
                return new PhoneLayoutInflater(ctx.getOuterContext());
            }}
    );
    // ... 省略
}
```

然后通过 SystemServiceRegistry 的 getSystemService 方法获取相关服务过程如下：
```
public static Object getSystemService(ContextImpl ctx, String name) {
    // 根据name在SYSTEM_SERVICE_FETCHERS获取该服务类型的ServiceFetcher
    ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
    // 通过该Fetcher获取对应的服务类型，如果是第一次调用fetcher.getServie()
    // 会调用其内部的createSercice()
    return fetcher != null ? fetcher.getService(ctx) : null;
}
 static abstract class CachedServiceFetcher<T> implements ServiceFetcher<T> {
        public final T getService(ContextImpl ctx) {
            final Object[] cache = ctx.mServiceCache;
            synchronized (cache) {
                Object service = cache[mCacheIndex];
                if (service == null) {
                    try {
                        service = createService(ctx);
                        cache[mCacheIndex] = service;
                    } catch (ServiceNotFoundException e) {
                        onServiceNotFound(e);
                    }
                }
                return (T)service;
            }
        }

        public abstract T createService(ContextImpl ctx) throws ServiceNotFoundException;
    }
```
SYSTEM_SERVICE_FETCHERS 是一个静态的 Map 容器，上面在 static {} 代码块中注册的服务都将保存在该容器。这里首先根据服务的 name 获取对应的 Fetcher，
  然后通过该 Fetcher 的 getService 方法创建相应 LayoutInflater 对象。

当我们首次获取某个服务类型时，fetcher.getService() 会执行其内部的 createService() 创建对应服务，然后每个服务都会
   被保存在 SystemServiceRegistry 中，这里实际间接保存在 Fetcher 中。

在 createService 方法，我们发现 LayoutInflater 的实际类型是 PhoneLayoutInflater，类定义如下：
```
frameworks/base/core/java/com/android/internal/policy/PhoneLayoutInflater.java
public class PhoneLayoutInflater extends LayoutInflater {

    /**
      * 系统默认 View 目录
      */
    private static final String[] sClassPrefixList = {
            "android.widget.",
            "android.webkit.",
            "android.app."
    };

    /**
     * Application 级的 LayoutInflater 使用该构造方法，就是在 
     * SystemServiceRegistry 的静态代码款中注册的。
     */
    public PhoneLayoutInflater(Context context) {
        super(context);
    }

    /**
     *  在Activity中使用LayoutInflater.form()时候调用该构造方法
     */
    protected PhoneLayoutInflater(LayoutInflater original, Context newContext) {
        super(original, newContext);
    }

   /**
     * 默认View 的创建流程这里（非自定义控件）
     */
    @Override
    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        for (String prefix : sClassPrefixList) {
            try {
                // 首先查找：android/widget目录下，如 Seekbar
                // 然后查找：android/webkit目录下，如 WebView
                // 最后查找：android/app目录下，如 ActionBar
                View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {}
        }

        // 如果以上都不能满足，就是：android/view 目录下
        return super.onCreateView(name, attrs);
    }

    /**
     * newContext是具体的Activity
     */
    public LayoutInflater cloneInContext(Context newContext) {
        // 每个Activity都由其独一无二的 Layoutflater
        return new PhoneLayoutInflater(this, newContext);
    }
}
```
注意 PhoneLayoutInflater 重写了 LayoutInflater 的 onCreateView 方法，这在后面 View 的创建阶段将会分析到。

注意观察 PhoneLayoutInflater 的最后 cloneInContext()，重新回到上面 ContextThemeWrapper 的 getSystemService 方法，
 大家是否注意到通过 LayoutInflater.from() 获取到 LayoutInflater 对象后，又调用其 cloneInContext 方法，
  该方法实际调用到 PhoneLayoutInflater 的 cloneInContext 方法。

此时会为每个 Activity 单独创建一个 LayoutInflater。之所以叫做 “clone”，是因为：系统默认会将进程级的 LayoutInflater 配置给
  每个 Activity 的 LayoutInflater，这也符合了 LayoutInflater 的自我介绍 “且正确配置的标准 LayoutInflater”。
  看下这一过程（实际是配置内部的 Factory）：
frameworks/base/core/java/android/view/LayoutInflater.java
```
// original是应用进程级的LayoutInflater，即在SystemServiceRegistry中保存
// 的LayoutInflater实例。
protected LayoutInflater(LayoutInflater original, Context newContext) {
    mContext = newContext;
    mFactory = original.mFactory;
    mFactory2 = original.mFactory2;
    mPrivateFactory = original.mPrivateFactory;
    setFilter(original.mFilter);
}
```
跟踪到这，LayoutInflater 的创建及实际类型就已经非常清晰了，并且根据不同的 Context 参数，我们可以总结出如下几条规律：
1 由于 Application 和 Service 都是 ContextWrapper 的直接子类，它们并没有对 getSystemService 方法做单独处理。
  故都是通过 ContextImpl 获取的同一个，也就是保存在 SystemServiceRegistry 中的 LayoutInflater。
2 每个 Activity 都有其独一无二的 LayoutInflater，它的实际类型是 PhoneLayoutInflater。当首次获取某个 Activity 的 LayoutInflater 时，
  系统首先会根据 Application 级的 LayoutInflater 创建并配置对应 Activity 的 LayoutInflater。



LayoutInflater 布局解析
分析完了 LayoutInflater 的创建过程，接下来我们看下大家最熟悉的 xml 布局解析阶段 inflate。
```
final View content = LayoutInflater.from(this).inflate(R.layout.content, root, false);
```
inflate将我们传入的xml布局文件解析成对应的view对象  //todo xml解析 XmlResourceParser  使用，初始化，原理

重点关注问题
<include /> 标签为什么不能作为布局的根节点？
<merge /> 标签为什么要作为布局资源的根节点？
inflate ( int resource, ViewGroup root, boolean attachToRoot) 参数 root 和 attachToRoot 的作用和规则？

```
public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        //通过Resources获取一个XML资源解析器
        final XmlResourceParser parser = res.getLayout(resource);
        try {
            //布局填充
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }
    
public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
    synchronized (mConstructorArgs) {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "inflate");

        final Context inflaterContext = mContext;
        // 获取在XML设置的属性
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        Context lastContext = (Context) mConstructorArgs[0];
        mConstructorArgs[0] = inflaterContext;
        // 注意root容器在这里，在我们当前分析中该root就是mContentParent
        View result = root;

        try {
            // 查找xml布局的根节点
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                // Empty
            }

            // 找到起始根节点
            if (type != XmlPullParser.START_TAG) {
                throw new InflateException(parser.getPositionDescription()
                        + ": No start tag found!");
            }
            // 获取到节点名称
            final String name = parser.getName();

            // 判断是否是merge标签
            if (TAG_MERGE.equals(name)) {
                if (root == null || !attachToRoot) {
                    // 此时如果ViewGroup==null,与attachToRoot==false将会抛出异常
                    // merge必须添加到ViewGroup中，这也是merge为什么要作为布局的根节点，它要添加到上层容器中
                    throw new InflateException("<merge /> can be used only with a valid "
                            + "ViewGroup root and attachToRoot=true");
                }

                rInflate(parser, root, inflaterContext, attrs, false);
            } else {
                // 否则创建该节点View对象
                final View temp = createViewFromTag(root, name, inflaterContext, attrs);

                ViewGroup.LayoutParams params = null;

                // 如果contentParent不为null，在分析setContentView中，这里不为null
                if (root != null) {
                    // 通过root（参数中的 ViewGroup）创建对应LayoutParams
                    params = root.generateLayoutParams(attrs);
                    if (!attachToRoot) {
                        // 如果不需要添加到 root，直接设置该View的LayoutParams
                        temp.setLayoutParams(params);
                    }
                }

                // 解析Child
                rInflateChildren(parser, temp, attrs, true);

                if (root != null && attachToRoot) {
                    // 添加到ViewGroup
                    root.addView(temp, params);
                }

                if (root == null || !attachToRoot) {
                    // 此时布局根节点为temp
                    result = temp;
                }
            }

        } catch (XmlPullParserException e) {} catch (Exception e) {} finally {
            ...
        }
        return result;
    }
}    
```
```
class ViewGroup
public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }
```
while 循环部分，首先找到 XML 布局文件的根节点，如果未找到：if (type != XmlPullParser.START_TAG) 直接抛出异常。
  否则获取到该节点名称，判断如果是 merge 标签，此时需要注意参数 root 和 attachToRoot，root 必须不为null，并且 attachToRoot 必须为 true，
  即 merge 内容必须要添加到 root 容器中。

如果不是 merge 标签，此时根据标签名 name 调用 createViewFromTag() 创建该 View 对象，rInflate 和 rInflateChildren 都是去解析子 View，
  rInflateChildren 方法实际也是调用到了 rInflate 方法：
```
final void rInflateChildren(XmlPullParser parser, View parent, AttributeSet attrs,
        boolean finishInflate) throws XmlPullParserException, IOException {
    //还是调用rInflate方法
    rInflate(parser, parent, parent.getContext(), attrs, finishInflate);
}
```

区别在于最后一个参数 finishInflate，它的作用是标志当前 ViewGroup 树创建完成后回调其 onFinishInflate 方法。
如果根标签是 merge，此时 finishInflate 为 false，这也很容易理解，此时的父容器为 inflate() 传入的 ViewGroup，
  它是不需要再次回调 onFinishInflate() ，该过程如下：
```
void rInflate(XmlPullParser parser, View parent, Context context,
        AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {

    final int depth = parser.getDepth();
    int type;
    boolean pendingRequestFocus = false;

    while (((type = parser.next()) != XmlPullParser.END_TAG ||
            parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

        if (type != XmlPullParser.START_TAG) {
            continue;
        }

        // 获取到节点名称
        final String name = parser.getName();

        if (TAG_REQUEST_FOCUS.equals(name)) {
            pendingRequestFocus = true;
            consumeChildElements(parser);
        } else if (TAG_TAG.equals(name)) {
            parseViewTag(parser, parent, attrs);
        } else if (TAG_INCLUDE.equals(name)) {
            // include标签
            if (parser.getDepth() == 0) {
                // include如果为根节点则抛出异常了
                // include不能作为布局文件的根节点
                throw new InflateException("<include /> cannot be the root element");
            }
            parseInclude(parser, context, parent, attrs);
        } else if (TAG_MERGE.equals(name)) {
            // 如果此时包含merge标签，此时也会抛出异常
            // merge只能作为布局文件的根节点
            throw new InflateException("<merge /> must be the root element");
        } else {
            // 创建该节点的View对象
            final View view = createViewFromTag(parent, name, context, attrs);
            final ViewGroup viewGroup = (ViewGroup) parent;

            final ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
            rInflateChildren(parser, view, attrs, true);
            // 添加到父容器
            viewGroup.addView(view, params);
        }
    }

    if (pendingRequestFocus) {
        parent.restoreDefaultFocus();
    }

    if (finishInflate) {
        // 回调ViewGroup的onFinishInflate方法
        parent.onFinishInflate();
    }
}

//解析view 的tag标签
private void parseViewTag(XmlPullParser parser, View view, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final Context context = view.getContext();
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ViewTag);
        final int key = ta.getResourceId(R.styleable.ViewTag_id, 0);
        final CharSequence value = ta.getText(R.styleable.ViewTag_value);
        view.setTag(key, value);
        ta.recycle();

        consumeChildElements(parser);
    }
```
while 循环部分，parser.next() 获取下一个节点，如果获取到节点名为 include，此时 parse.getDepth() == 0 表示根节点，
  直接抛出异常：“<include /> cannot be the root element”，即 <include /> 不能作为布局的根节点。

如果此时获取到节点名称为 merge，也是直接抛出异常了，即 <merge /> 只能作为布局的根节点：“<merge /> must be the root element”。

否则创建该节点对应 View 对象，rInflateChildren 递归完成以上步骤，并将解析到的 View 添加到其直接父容器：viewGroup.addView(view, params)。

注意方法的最后通知调用每个 ViewGroup 的 onFinishInflate()，大家是否有注意到这其实是入栈的操作，即最顶层的 ViewGroup 最后回调 onFinishInflate()。


至此，我们可以回答上面提出的相关问题了
1 如果布局根节点为 merge ，会判断 inflate 方法参数 if ( root != null && attachToRoot == true )，表示布局文件要直接添加到 root 中，
  否则抛出异常：“<merge /> can be used only with a valid ViewGroup root and attachToRoot=true”；

2 继续解析子节点的过程中如果再次解析到 merge 标签，则直接抛出异常：“<merge /> must be the root element”。
  即  <merge /> 标签必须作为布局文件的根节点。

3 如果解析到节点名称为 include，会判断当前节点深度是否为 0，0 表示当前处于根节点，此时直接抛出异常：“<include /> cannot be the root element”。
  即 <include /> 不能作为布局文件的根节点。

4 root与attachToRoot的作用  attachToRoot标志是否调用root.addView() 将解析到的View添加到root中   




不容忽视的 View 创建耗时
在分析 XML 布局解析阶段，我们忽略了一个非常重要的 View 创建过程 createViewFromTag 方法，接下来我们就详细跟踪下这部分内容。
```
View createViewFromTag(View parent, String name, Context context, AttributeSet attrs,
                           boolean ignoreThemeAttr) {
    if (name.equals("view")) {
        name = attrs.getAttributeValue(null, "class");
    }

    // Apply a theme wrapper, if allowed and one is specified.
    if (!ignoreThemeAttr) {
        final TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS_THEME);
        final int themeResId = ta.getResourceId(0, 0);
        if (themeResId != 0) {
            context = new ContextThemeWrapper(context, themeResId);
        }
        ta.recycle();
    }

    if (name.equals(TAG_1995)) {
        // Let's party like it's 1995!
        return new BlinkLayout(context, attrs);
    }

    try {
        View view;
        // 首先通过mFactory2加载View
        if (mFactory2 != null) {
            // 交给Factory2工程创建
            view = mFactory2.onCreateView(parent, name, context, attrs);
        } else if (mFactory != null) {
            // 其次通过mFactory工程创建
            view = mFactory.onCreateView(name, context, attrs);
        } else {
            view = null;
        }

        // 私有工厂
        if (view == null && mPrivateFactory != null) {
            view = mPrivateFactory.onCreateView(parent, name, context, attrs);
        }
        // 如果上边都没有满足，走默认
        if (view == null) {
            // 保存最后一次上下文
            final Object lastContext = mConstructorArgs[0];
            mConstructorArgs[0] = context;
            try {
                // 判断name是否包含点，表示是否是自定义控件
                // 比如如果类名是 ImageView，实际是反射创建，也就是要通过类的全限定名进行加载
                // Android 系统默认加载View目录有三个在PhoneLayoutInflater中
                // 系统的控件如android.widget.ImageView，在xml中使用<ImageView></ImageView> 系统进行了路径补全
                // 自定义控件为全路径
                if (-1 == name.indexOf('.')) {
                    view = onCreateView(parent, name, attrs);
                } else {
                    // 否则是 custom view
                    view = createView(name, null, attrs);
                }
            } finally {
                mConstructorArgs[0] = lastContext;
            }
        }
        return view;
    } catch (InflateException e) {} catch (ClassNotFoundException e) {} catch (Exception e) {}
}
```

注意，在 createViewFromTag 方法会依次判断 mFactory2、mFactory、mPrivateFactory 是否为 null。也就是会依次根据 
  mFactory2、mFactory、mPrivateFactory 来创建 View 对象。看下他们在 LayoutInflater 中的声明：
```
public abstract class LayoutInflater {

    // ... 省略

    private Factory mFactory;
    private Factory2 mFactory2;
    private Factory2 mPrivateFactory;
    private Filter mFilter;

    // ... 省略
}
```
Factory 和 Factory2 都属于 LayoutInflater 的内部接口，声明如下： 
```
public interface Factory {
     public View onCreateView(String name, Context context, AttributeSet attrs);
}
public interface Factory2 extends Factory {
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs);
}
```
//f1,f2的区别：Factory2支持将创建的view添加到parent中
Factory2 是在 Android 3.0 版本添加，两者功能基本一致（Factory2 优先级高于 Factory）。其实前面我们讲到 Activity 的 LayoutInflater 
  是通过 cloneInContext 方法创建，这一过程就是要复用它的 mFactory2、mFactory、mPrivateFactory 这样不需要再重新设置了
  （关联且正确配置的标准 LayoutInflater）。

Factory 只包括一个核心的 onCreateView 方法，即创建 View 对象的过程。这一特性为我们提供了 View 创建过程的 Hack 机会，
  例如替换某个 View 类型，动态换肤、View 复用等。这部分内容在后面高阶技巧中再详细介绍。

如果以上条件都不满足，则执行 LayoutInflater 的默认 View 创建流程，注意这里首先会根据解析到的标签名 name 是否包含 “.” ，
  用于判断当前标签是否属于自定义控件类型。

类加载器只能通过类的全限定名来加载对应的类。例如 ImageView，此时系统要为其补齐前缀后变为：“android.widget.ImageView”。
   但是我们在 XML 中只是声明了 View 的名称如下（自定义 View 除外，因为其声明已经是全限定名）：
```
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- 系统提供的 View -->
    <ImageView
         android:layout_width="wrap_content"
         android:layout_height="wrap_content"/>

     <!-- 自定义 View，已经是全限定名 -->
     <com.xxx.android.custom.CircleView
         android:layout_width="wrap_content"
         android:layout_height="wrap_content"/>

</LinearLayout>
```

接下来，我们就看下系统是如何加载对应 View 标签以及创建过程：
```
protected View onCreateView(String name, AttributeSet attrs)
            throws ClassNotFoundException {
    return createView(name, "android.view.", attrs);
}
```
注意： 由于 LayoutInflater 的实际类型为 PhoneLayoutInflater，还记得上面贴出的 PhoneLayoutInflater 中重写了 onCreateView 方法：
```
protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
   for (String prefix : sClassPrefixList) {
       try {
           // 首先查找：android/widget目录下，如 Seekbar
           // 然后查找：android/webkit目录下，如 WebView
           // 最后查找：android/app目录下，如 SurfaceView
           View view = createView(name, prefix, attrs);
           if (view != null) {
               return view;
           }
        } catch (ClassNotFoundException e) {}
    }

    // 如果以上都不能满足，就是：android/view 目录下
    return super.onCreateView(name, attrs);
}
```
这里将依次遍历原生 View 所在目录，这一过程就是为解析到的 View 标签补齐前缀组成类的全限定名，然后通过 ClassLoader 进行加载，
   直到加载成功，否则抛出异常。Android 系统提供的 View 视图目录如下（其实这里还包括一个 android.view，它将作为最后）：
```
private static final String[] sClassPrefixList = {
        "android.widget.",
        "android.webkit.",
        "android.app."
};
```
View 的创建过程 createView 方法如下，name 表示在 XML 中的标签名称如 “ImageView”，prefix 表示 ImageView 标签的前缀为“android.widget”，
  组成 “android.widget.ImageView” 交给 ClassLoader 尝试加载：
```
public final View createView(String name, String prefix, AttributeSet attrs)
            throws ClassNotFoundException, InflateException {
    // 查找name类型的的构造方法
    Constructor<? extends View> constructor = sConstructorMap.get(name);
    // verifyClassLoader方法验证是否是同一个ClassLoader
    if (constructor != null && !verifyClassLoader(constructor)) {
        constructor = null;
        // 如果ClassLoader不匹配，则删除该类型的缓存
        sConstructorMap.remove(name);
    }
    Class<? extends View> clazz = null;

    try {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, name);

        if (constructor == null) {
            // 类未在缓存中找到，通过ClassLoader根据类全限定名加载，并缓存到sConstructorMap容器
            clazz = mContext.getClassLoader().loadClass(
                        prefix != null ? (prefix + name) : name).asSubclass(View.class);

            // mFilter 可以拦截是否被允许创建该视图类的对象
            if (mFilter != null && clazz != null) {
                boolean allowed = mFilter.onLoadClass(clazz);
                if (!allowed) {
                    failNotAllowed(name, prefix, attrs);
                }
            }
            constructor = clazz.getConstructor(mConstructorSignature);
            constructor.setAccessible(true);
            // 进行缓存
            sConstructorMap.put(name, constructor);
        } else {
            // 否则判断是否设置了 Filter
            // Filter 的主要作用是拦截当前视图类是否可以创建视图对象
            if (mFilter != null) {
                // Have we seen this name before?
                Boolean allowedState = mFilterMap.get(name);
                if (allowedState == null) {
                    // New class -- remember whether it is allowed
                    clazz = mContext.getClassLoader().loadClass(
                                prefix != null ? (prefix + name) : name).asSubclass(View.class);

                    boolean allowed = clazz != null && mFilter.onLoadClass(clazz);
                    mFilterMap.put(name, allowed);
                    if (!allowed) {
                        // 这里将会直接抛出异常表示不允许创建该视图类对象
                        failNotAllowed(name, prefix, attrs);
                    }
                } else if (allowedState.equals(Boolean.FALSE)) {
                    failNotAllowed(name, prefix, attrs);
                }
             }
        }

        Object lastContext = mConstructorArgs[0];
        if (mConstructorArgs[0] == null) {
            // Fill in the context if not already within inflation.
            mConstructorArgs[0] = mContext;
        }
        Object[] args = mConstructorArgs;
        args[1] = attrs;

        // 创建该View对象
        final View view = constructor.newInstance(args);
        if (view instanceof ViewStub) {
            // Use the same context when inflating ViewStub later.
            final ViewStub viewStub = (ViewStub) view;
            viewStub.setLayoutInflater(cloneInContext((Context) args[0]));
        }
        mConstructorArgs[0] = lastContext;
        return view;

    } catch (NoSuchMethodException e) {
        // ... 省略所有异常报错
    } finally {
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
}
```
//todo ViewStub
sConstructorMap 是一个 static Map 容器，用于缓存某个 View 类的构造方法，这算是一层优化。避免每次 loadClass() 执行类的加载过程。

注意查看 ClassLoader 的 loadClass 方法，将 prefix 和 name 组成类的全限定名进行加载，如果成功加载对应类，
   获取它的构造方法并缓存在 sConstructorMap 容器。

mFilter 是一个 Filter 类型对象，用于决定是否允许创建某个 View 类的对象，它的声明如下：
```
public interface Filter {

   // 参数clazz，表示即将要创建视图对象的类对象
   // 返回值 true 表示允许创建该视图类对象，否则返回 false，不允许。
   boolean onLoadClass(Class clazz);

}
```
最后通过反射 newInstance() 创建对应的 View 对象并返回。
LayoutInflater 在 View 对象的创建过程使用了大量反射，如果某个布局界面内容又较复杂，该过程耗时是不容忽视的。更极端的情况可能是
  某个 View 的创建过程需要执行 4 次，例如 SurfaceView，因为系统默认遍历规则依次为 android/weight、android/webkit 和 android/app，
  但是由于 SurfaceView 属于 android/view 目录下，故此时需要第 4 次 loadClass 才可以正确加载，这个效率会有多差
  （在 AppCompatActivity 中该过程略有改善，后面的高阶阶段介绍）！

至此 LayoutInflater 的工作原理就已经分析完了，个人认为 Android 系统对布局 View 的创建过程处理的过于简单粗暴了。但是换个角度，
  这也给我们留下更多优化和学习的空间。

//todo android Trace.traceBegin(Trace.TRACE_TAG_VIEW, name)   怎么拿到这些日志



LayoutInflater 的高阶使用技巧
通过上面的分析，其实大家也能猜到这部分主要围绕 LayoutInflater.Factory 展开，接下来我们就来看下利用 LayoutInflater.Factory 可以
  帮助我们完成哪些工作？

1. Activity 默认实现了 LayoutInflater.Factory2 接口
   这可能也是很多开发人员所不了解的，其实我们完全可以在自己的 Xxx-Activity 中重写对应方法，实现例如 View 替换、复用等机制。
```
public class Activity extends ContextThemeWrapper
        implements LayoutInflater.Factory2 ... {

    /**
      * LayoutInflater.Factory，LayoutInflater.Factory2 继承自 LayoutInflater.Factory
      */
    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    /**
      * LayoutInflater.Factory2
      */
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return onCreateView(name, context, attrs);
        }
        return mFragments.onCreateView(parent, name, context, attrs);
    }
}
```

2. AppCompatActivity 兼容设计
   Android 在 5.0 之后引入了 Material Design 的设计，为了更好的支撑 Material 主题、调色版、Toolbar 等各种新特性，
   兼容版本的 AppCompatActivity 就应运而生了。大家是否有注意过，使用 AppCompatActivity 之后，所有（需要兼容特性的 View） View 控件
   都被替换成了 AppCompat-Xxx 类型：   Layout分析的截图

AppCompatActivity 则是利用了 AppCompatDelegate 在不同的 Android 版本之间实现兼容配置。其中将 View 的创建阶段又单独委托给
  AppCompatViewInflater，它本质还是利用了 LayoutInflater.Factory2 接口：
androidx.appcompat.app.AppCompatDelegate
androidx.appcompat.app.AppCompatViewInflater
```
    public final View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs, boolean inheritContext,
            boolean readAndroidTheme, boolean readAppTheme, boolean wrapContext) {

        // ... 省略

        View view = null;
        // 根据View类型替换成对应的AppCompat类型
        switch (name) {
            case "TextView":
                view = new AppCompatTextView(context, attrs);
                break;
            case "ImageView":
                view = new AppCompatImageView(context, attrs);
                break;
            case "Button":
                view = new AppCompatButton(context, attrs);
                break;
            case "EditText":
                view = new AppCompatEditText(context, attrs);
                break;
            case "Spinner":
                view = new AppCompatSpinner(context, attrs);
                break;
            case "ImageButton":
                view = new AppCompatImageButton(context, attrs);
                break;
            case "CheckBox":
                view = new AppCompatCheckBox(context, attrs);
                break;
            case "RadioButton":
                view = new AppCompatRadioButton(context, attrs);
                break;
            case "CheckedTextView":
                view = new AppCompatCheckedTextView(context, attrs);
                break;
            case "AutoCompleteTextView":
                view = new AppCompatAutoCompleteTextView(context, attrs);
                break;
            case "MultiAutoCompleteTextView":
                view = new AppCompatMultiAutoCompleteTextView(context, attrs);
                break;
            case "RatingBar":
                view = new AppCompatRatingBar(context, attrs);
                break;
            case "SeekBar":
                view = new AppCompatSeekBar(context, attrs);
                break;
        }

        if (view == null && originalContext != context) {
            // If the original context does not equal our themed context, then we need to manually
            // inflate it using the name so that android:theme takes effect.
            view = createViewFromTag(context, name, attrs);
        }

        if (view != null) {
            // If we have created a view, check its android:onClick
            checkOnClickListener(view, attrs);
        }

        return view;
    }
```
可以非常清晰的看到，整个兼容版 View 的替换过程。不过还是有一些 View 需要通过反射加载创建。其实这一部分也是我们最应该优化的，
  有关布局 View 的创建过程，我们完全可以将其接手以避免反射或极端的遍历加载过程。


3. 动态换肤
   关于动态换肤，业界做的比较好的要属网易云音乐了。通过动态换肤满足用户的新鲜感，提升增值业务产品吸引力。

动态换肤主要涉及两个核心过程：① 采集需要换肤的控件，② 加载相应皮肤包，并替换所有需要换肤的控件。

如何确定哪些控件需要动态换肤呢？这里简单提供一种思路，首先要明确换肤到底是换的什么？只要理解了换的是什么，我们就知道要查询哪些属性了：
```
    static {
        mAttributes.add("background");
        mAttributes.add("src");

        mAttributes.add("textColor");
        mAttributes.add("drawableLeft");
        mAttributes.add("drawableTop");
        mAttributes.add("drawableRight");
        mAttributes.add("drawableBottom");
    }
```
那如何采集呢？其实这一过程就可以利用到今天介绍的 LayoutInflater.Factory。而且还可以结合 ActivityLifecycleCallback 进一步减少代码的侵入性。
```
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
         
        // ... View 的创建过程省略

        // 筛选符合换肤条件的 View
        skinAttribute.load(view, attrs);

        return view;
    }
```
筛选过程主要是遍历 View 的属性集合 AttributeSet，查找 View 是否包含匹配的换肤属性。该过程如下：   //todo AttributeSet
```
    public void load(View view, AttributeSet attrs) {
        final List<SkinPair> skinPairs = new ArrayList<>();
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            // 获得属性名
            String attributeName = attrs.getAttributeName(i);
            // 是否符合需要筛选的属性名
            if (mAttributes.contains(attributeName)) {
                String attributeValue = attrs.getAttributeValue(i);
                if (attributeValue.startsWith("#")) {
                    // 属性资源写死了，无法替换
                    continue;
                }
                //资源id
                int resId;
                if (attributeValue.startsWith("?")) {
                    // 主题资源，attr Id
                    int attrId = Integer.parseInt(attributeValue.substring(1));
                    // 获得主题style中对应 attr 的资源id值
                    resId = SkinThemeUtils.getResId(view.getContext(), new int[]{attrId})[0];
                } else {
                    // @12343455332
                    resId = Integer.parseInt(attributeValue.substring(1));
                }
                if (resId != 0) {
                    // 可以被替换的属性
                    SkinPair skinPair = new SkinPair(attributeName, resId);
                    skinPairs.add(skinPair);
                }
            }
        }

        // 将View与之对应的可以动态替换的属性集合放入集合中
        if (!skinPairs.isEmpty()) {
            // 每个SkinView表示可以被换肤的控件
            // skinPairs表示该控件哪些属性需要被换肤
            SkinView skinView = new SkinView(view, skinPairs);
            skinView.applySkin();
            mSkinViews.add(skinView);
        }
    }
```
有关 Android 换肤原理网上资料也比较多，感兴趣的朋友可以进一步学习理解。


4. setFactory / setFactory2
   LayoutInflater 内部为开发者提供了直接设置 Factory 的方法，不过需要注意该方法只能被设置一次，否则将会抛出异常。
   聪明的你很快就会想到可以利用反射将其修改（LayoutInflater 并没有被 @hide 声明）
```
    public void setFactory(Factory factory) {
        if (mFactorySet) {
            // 注意该变量，被设置过一次后会被置为true
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        }
        if (factory == null) {
            // factory 不能为null
            throw new NullPointerException("Given factory can not be null");
        }
        mFactorySet = true;
        if (mFactory == null) {
            // 如果之前不存在，直接赋值
            mFactory = factory;
        } else {
            // 否则合并
            mFactory = new FactoryMerger(factory, null, mFactory, mFactory2);
        }
    }
```
看似一个简单的 LayoutInflater.Factory 可以说在 View 的加载和创建过程提供了“无限种”可能。这也体现了优秀的策略模式，
   这种策略对于应用的扩展和兼容都提供了很大的帮助，AppCompat 就是比较经典的例子。

通过今天的分析，在你的项目中 View 的创建过程是否存在优化的空间呢？可以将今天的内容优化到具体的应用中，以帮助我们更好的优化 UI 渲染性能。



思考： Fragment 中也会使用到 LayoutInflater，它是否和 Activity 使用的同一个呢？欢迎大家的分享留言或指正。  //todo