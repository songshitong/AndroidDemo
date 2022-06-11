https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E7%B3%BB%E7%BB%9F%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90/LiveData_%E4%BD%BF%E7%94%A8%E5%8F%8A%E5%8E%9F%E7%90%86%E8%A7%A3%E6%9E%90.md

androidx.lifecycle:lifecycle-runtime:2.3.1

原理总结
LiveData利用Lifecycle感知activity/fragment的生命周期，在销毁时移除观察者
observe 保存观察者到SafeIterableMap  ObserverWrapper对Observer进行功能增强，一种是LifecycleBoundObserver跟随生命周期的，
     一种是AlwaysActiveObserver永久激活的观察者
setValue  分发value给所有的观察者  连续两次调用，只有第一次生效，第一个正在分发中   setValue只对活跃的观察者通知
postValue 通过handler将消息发送到主线程  只有在赋值的时候上锁了，可能在线程切换时，其他线程把值变更了，只保留最新的值
    还有一种情况在一个线程连续调用两次postValue，只有第一次生效   
   //连续两次调用，第二次先赋值mPendingData，发现上一次没有分发成功然后就退出了，第一次分发时值mPendingData就变了
```
   val ld = MutableLiveData<String>()
        ld.observe(this) {
            Log.d(TAG,"ld observe $it")
        }
        ld.postValue("1")
        ld.postValue("2")
```
结果： ld observe 2

LiveData是一个类,将数据放在它里面我们可以观察数据的变化.它是lifecycle-aware(生命周期感知的).这个特性非常重要,
  我们可以用它来更新UI的数据,当且仅当activity、fragment或者Service是处于活动状态时。
LiveData一般用在ViewModel中,用于存放一些数据啥的,然后我们可以在Activity或者Fragment中观察其数据的变化(可能是访问数据库或者请求网络)展示数据到相应的UI上.
   这就是数据驱动视图,是MVVM模式的重要思想.

阅读本文需要读者了解Lifecycle原理,下面的很多东西都和Lifecycle的很多类相关
其实谷歌出的Lifecycle和ViewModel,LiveData这些,都特别好用,设计得特别好,特别值得我们深入学习.

下面我将带大家走进LiveData的世界.
一、使用
```
public class MainActivity extends AppCompatActivity {
    
    //1. 首先定义一个LiveData的实例  
    private MutableLiveData<String> mStringLiveData = new MutableLiveData<>();
    private TextView mContentTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContentTv = findViewById(R.id.tv_content);
        
        //2. 观察LiveData数据的变化,变化时将数据展示到TextView上
        mStringLiveData.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String content) {
                //数据变化时会回调这个方法
                mContentTv.setText(content);
            }
        });
        
        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //3. 改变LiveData里面的数据 数据变化时,会回调上面的onChanged()方法
                mStringLiveData.postValue("新数据....");
            }
        });
    }
}
```
kotlin
```
 val ld = MutableLiveData<String>()
 ld.observe(this) { 
    
 }
```
1 首先是定义一个LiveData的实例,LiveData是一个抽象类,MutableLiveData是它的一个子类.
2 然后调用LiveData的observe()方法,进行注册,开始观察数据,当我们的数据变化时,会回调onChanged()方法.
3 LiveData有2个更新数据的方法
  一个是setValue(): 在主线程中调用,直接更新数据
  一个是postValue(): 在主线程或者子线程中调用都行,它最后其实是利用handler在主线程中调用setValue()方法实现的数据更新.

举上面这个例子,主要是让大家感受一下,其实LiveData真的用处很大,不仅可以拿来更新ListView,展示隐藏对话框,按钮展示隐藏等等.这些东西都是数据驱动的,
  当数据变化时,根本不需要另外多写代码,会回调observe()方法,数据就及时地更新到UI上了.简直天衣无缝啊.

二、源码分析
既然我们说了,LiveData这么牛逼,作为一个合格的开发人员.我们不能仅仅是API player,我们要知道其背后用的啥原理,日后深入使用时肯定会很有帮助.
从下面的代码开始入手:
```
mStringLiveData.observe(this, new Observer<String>() {
    @Override
    public void onChanged(String content) {
        mContentTv.setText(content);
    }
});
//调用上面的方法来到了LiveData的observe()方法
//1. 传入的是LifecycleOwner和Observer(观察者)
 public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        //2. 当前必须是在主线程
        assertMainThread("observe");
        //3. 当前的生命周期如果是DESTROYED状态,那么不好意思,不能观察了
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        //4. 这里用装饰者模式将owner, observer封装起来  实现不同功能的观察者，这里的是随生命周期销毁的观察者，还有一种是一直监听的观察者
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        //5. 将观察者缓存起来
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        //6. 添加生命周期观察
        owner.getLifecycle().addObserver(wrapper);
    }
```
首先第1点,我们传入的是Activity,到里面却看到是LifecycleOwner.翻看AppCompatActivity源码..
```
public class AppCompatActivity extends FragmentActivity{}

public class FragmentActivity extends ComponentActivit{}

public class ComponentActivity extends Activity
        implements LifecycleOwner{}
```

原来AppCompatActivity的爷爷(ComponentActivity)实现了LifecycleOwner接口,而LifecycleOwner接口是为了标识标记类有Android的生命周期的,
  比如Activity和Fragment.

第2点,必须是主线程中.
第3点,如果生命周期是DESTROYED,那么不好意思,不能继续往下走了.选择忽略.
第4点,将owner, observer封装了起来,形成一个LifecycleBoundObserver对象.
```
public interface LifecycleEventObserver extends LifecycleObserver {
    void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event);
}
//观察者的增强
private abstract class ObserverWrapper {
        final Observer<? super T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }
        abstract boolean shouldBeActive();
        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }
        void detachObserver() {
        }
        //根据状态执行不同的生命周期回调，并通过dispatchingValue将值通知给观察者
        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            mActive = newActive;
            boolean wasInactive = LiveData.this.mActiveCount == 0;
            //LiveData中记录的激活观察者增加1或-1
            LiveData.this.mActiveCount += mActive ? 1 : -1;
            if (wasInactive && mActive) {
                onActive();
            }
            if (LiveData.this.mActiveCount == 0 && !mActive) {
                onInactive();
            }
            if (mActive) {
                dispatchingValue(this);
            }
        }
}
 class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }
        //当前的状态是STARTED,RESUMED会激活   DESTROYED,INITIALIZED,CREATED未激活
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            activeStateChanged(shouldBeActive());
        }

        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }
```
LifecycleBoundObserver实现了LifecycleEventObserver,而LifecycleEventObserver是实现了LifecycleObserver(标记一个类是生命周期观察者).

第5点我们看到有一个mObservers,它其实是LiveData里面的一个属性,是用来缓存所有的LiveData的观察者的.
```
public abstract class LiveData<T> {
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers =
            new SafeIterableMap<>();
 }
```
再来看第6点,这个在Lifecycle里面讲过,它是用来添加观察者,最终用来观察LifecycleOwner(生命周期拥有者)的生命周期的,
  比如Activity或者Fragment等.
当Activity的生命周期发生变化时,会回调上面LifecycleEventObserver(也就是上面的LifecycleBoundObserver)对象的onStateChanged()方法
```
class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
  public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
      if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
          removeObserver(mObserver);
          return;
      }
      activeStateChanged(shouldBeActive());
  }
}

public abstract class LiveData<T> {
  public void removeObserver(@NonNull final Observer<? super T> observer) {
      assertMainThread("removeObserver");
      ObserverWrapper removed = mObservers.remove(observer);
      if (removed == null) {
          return;
      }
      removed.detachObserver();
      removed.activeStateChanged(false);
  }
}
```
当生命周期处于DESTROYED时,调用removeObserver()方法,移除观察者,那么在Activity中就不会收到回调了.


如何得知数据已经更新
LiveData提供了2种方式,setValue()和postValue()来更新数据.
1. setValue()方式更新数据
   来看LiveData的setValue()方法
```
private volatile Object mData;
 public LiveData(T value) {
        mData = value;
        mVersion = START_VERSION + 1;
    }
public LiveData() {
    mData = NOT_SET;
    mVersion = START_VERSION;
}
 protected void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }    
```   
首先是当前必须是主线程,然后将值保存到了mData属性中.
```
    void dispatchingValue(@Nullable ObserverWrapper initiator) {
        //mDispatchingValue正在分发value
        if (mDispatchingValue) {
            //如果上一个分发value还没完成，标记为mDispatchInvalidated
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                //遍历所有的观察者执行considerNotify()方法
                for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                        mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }
    
private void considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) { 
            return;
        }
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        observer.mObserver.onChanged((T) mData);
    }    
```
然后调用dispatchingValue()方法,遍历所有的观察者,并回调onChanged()方法,数据即得到了更新.


postValue()方式更新数据
这种方式一般适用于在子线程中更新数据,更新UI的数据
```
volatile Object mPendingData = NOT_SET;
protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        //mPendingData不是NOT_SET，退出，说明上一个还没执行   此时postTask=mPendingData=Value
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }
 //mPostValueRunnable的实现   
 private final Runnable mPostValueRunnable = new Runnable() {
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                mPendingData = NOT_SET;
            }
            //最后还是调用的setValue()嘛
            setValue((T) newValue);
        }
    };    
```
将value值赋值给mPendingData,然后通过ArchTaskExecutor的实例将mPostValueRunnable传入postToMainThread()方法.
ArchTaskExecutor是何方神圣
```
public class ArchTaskExecutor extends TaskExecutor {
    private static volatile ArchTaskExecutor sInstance;
    private TaskExecutor mDelegate;
    private TaskExecutor mDefaultTaskExecutor;

    private static final Executor sMainThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            getInstance().postToMainThread(command);
        }
    };

    private static final Executor sIOThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            getInstance().executeOnDiskIO(command);
        }
    };

    private ArchTaskExecutor() {
        mDefaultTaskExecutor = new DefaultTaskExecutor();
        mDelegate = mDefaultTaskExecutor;
    }
   //单例的
    public static ArchTaskExecutor getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ArchTaskExecutor.class) {
            if (sInstance == null) {
                sInstance = new ArchTaskExecutor();
            }
        }
        return sInstance;
    }

    public void setDelegate(@Nullable TaskExecutor taskExecutor) {
        mDelegate = taskExecutor == null ? mDefaultTaskExecutor : taskExecutor;
    }
    public void executeOnDiskIO(Runnable runnable) {
        mDelegate.executeOnDiskIO(runnable);
    }
    public void postToMainThread(Runnable runnable) {
        mDelegate.postToMainThread(runnable);
    }
}
```
ArchTaskExecutor主要通过代理DefaultTaskExecutor实现它的内部逻辑
```
public class DefaultTaskExecutor extends TaskExecutor {
    private final Object mLock = new Object();

    private final ExecutorService mDiskIO = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private static final String THREAD_NAME_STEM = "arch_disk_io_%d";
        private final AtomicInteger mThreadId = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format(THREAD_NAME_STEM, mThreadId.getAndIncrement()));
            return t;
        }
    });

    private volatile Handler mMainHandler;

    public void executeOnDiskIO(Runnable runnable) {
        mDiskIO.execute(runnable);
    }

    public void postToMainThread(Runnable runnable) {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = createAsync(Looper.getMainLooper());
                }
            }
        }
        //通过handler切换到主线程
        mMainHandler.post(runnable);
    }

    private static Handler createAsync(@NonNull Looper looper) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                        boolean.class)
                        .newInstance(looper, null, true);
            } catch (IllegalAccessException ignored) {
            ...
        }
        return new Handler(looper);
    }
}
```
DefaultTaskExecutor通过handler将任务抛到主线程，在子线程执行通过线程池

postValue总结 
通过ArchTaskExecutor里面的DefaultTaskExecutor里面的postToMainThread()方法,其实将mPostValueRunnable交给了一个mMainHandler,
 这个mMainHandler有主线程的looper.可以方便的将Runnable搞到主线程. 所以最后mPostValueRunnable会到主线程中执行setValue(),毫无问题.

由于 LiveData 值回调的行为是会固定放在主线程完成的，所以 postValue 方法将值回调的逻辑放到 Runnable 中再 Post 给 Handler，
最终交由主线程来执行，因此从调用postValue 方法到 Runnable 被执行之间是会有段时间差的，
此时其它线程可能又调用了setValue/postValue 方法传递了新值

在 mPostValueRunnable 被执行前，所有通过 postValue 方法传递的 value 都会被保存到变量 mPendingData 上，且只会保留最后一个，
直到 mPostValueRunnable 被执行后 mPendingData 才会被重置，
所以使用 postValue 方法在多线程同时调用或者单线程连续调用的情况下是存在丢值（外部的 Observer 只能接收到最新值）的可能性的


小结
LiveData主要是依赖Lifecycle可以感知生命周期,从而避免了内存泄露.然后可以观察里面数据的变化来驱动UI数据展示.



其他
一直监听数据
```
 //需要调用removeObserver手动移除observer 
        mStringLiveData.observeForever();
```
observeForever的实现   与observe的实现的区别主要是AlwaysActiveObserver
```
  public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing instanceof LiveData.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }
```
看一下AlwaysActiveObserver会一直处于active状态
```
 private class AlwaysActiveObserver extends ObserverWrapper {
        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }
        boolean shouldBeActive() {
            return true;
        }
    }
```


MutableLiveData的实现  暴露LiveData的postValue和setValue  LiveData的访问是protected
```
public class MutableLiveData<T> extends LiveData<T> {

    public MutableLiveData(T value) {
        super(value);
    }
    public MutableLiveData() {
        super();
    }

   //重写为public
    public void postValue(T value) {
        super.postValue(value);
    }
   //重写为public
    public void setValue(T value) {
        super.setValue(value);
    }
}
```

MediatorLiveData  合并多个 LiveData 源
MediatorLiveData 是 LiveData 的子类，允许您合并多个 LiveData 源。只要任何原始的 LiveData 源对象发生更改，
  就会触发 MediatorLiveData 对象的观察者。
例如，如果界面中有可以从本地数据库或网络更新的 LiveData 对象，则可以向 MediatorLiveData 对象添加以下源：
 1 与存储在数据库中的数据关联的 LiveData 对象。
 2 与从网络访问的数据关联的 LiveData 对象。
您的 Activity 只需观察 MediatorLiveData 对象即可从这两个源接收更新
使用
```
LiveData  liveData1 = ...;
LiveData  liveData2 = ...;
  
   MediatorLiveData  liveDataMerger = new MediatorLiveData<>();
   //情景1 添加两个源LiveData，liveData1和liveData2的值改变后都会更新liveDataMerger
   liveDataMerger.addSource(liveData1, value -> liveDataMerger.setValue(value));
   liveDataMerger.addSource(liveData2, value -> liveDataMerger.setValue(value));
   //监听liveDataMerger的数据改变
   liveDataMerger.observe(this, new Observer() {
            @Override
            public void onChanged(Object o) {
                
            }
        });
   
   //情景2 监听liveData1，当liveData1改变10次后，将值添加到liveDataMerger，并移除源liveData1
   liveDataMerger.addSource(liveData1, new Observer () {
        private int count = 1;
         public void onChanged(@Nullable Integer s) {
            count++;
            liveDataMerger.setValue(s);
            if (count > 10) {
                liveDataMerger.removeSource(liveData1);
            }
        }
   });
```
原理
Source类的理解
```
private static class Source<V> implements Observer<V> {
        final LiveData<V> mLiveData;
        final Observer<? super V> mObserver;
        int mVersion = START_VERSION;
        Source(LiveData<V> liveData, final Observer<? super V> observer) {
            mLiveData = liveData;
            mObserver = observer;
        }

        void plug() {
            mLiveData.observeForever(this);
        }

        void unplug() {
            mLiveData.removeObserver(this);
        }

        public void onChanged(@Nullable V v) {
            if (mVersion != mLiveData.getVersion()) {
                mVersion = mLiveData.getVersion();
                mObserver.onChanged(v);
            }
        }
    }
```
source接收一个liveData监听，并在liveData的值改变后，通过observer保留给外部
```
public class MediatorLiveData<T> extends MutableLiveData<T> {
    //保存多个源liveData的map
    private SafeIterableMap<LiveData<?>, Source<?>> mSources = new SafeIterableMap<>();

    public <S> void addSource(@NonNull LiveData<S> source, @NonNull Observer<? super S> onChanged) {
        Source<S> e = new Source<>(source, onChanged);
        //将当前源缓存，不能重复添加一个源和它的监听
        Source<?> existing = mSources.putIfAbsent(source, e);
        if (existing != null && existing.mObserver != onChanged) {
            throw new IllegalArgumentException(
                    "This source was already added with the different observer");
        }
        if (existing != null) {
            return;
        }
        if (hasActiveObservers()) {
            //source开始监听一个liveData的数据变化，并通过onChanged暴露给外部
            e.plug();
        }
    }

    public <S> void removeSource(@NonNull LiveData<S> toRemote) {
        Source<?> source = mSources.remove(toRemote);
        if (source != null) {
            source.unplug();
        }
    }

    //每个source开始监听它的liveData
    protected void onActive() {
        for (Map.Entry<LiveData<?>, Source<?>> source : mSources) {
            source.getValue().plug();
        }
    }
    //每个source取消监听它的liveData
    protected void onInactive() {
        for (Map.Entry<LiveData<?>, Source<?>> source : mSources) {
            source.getValue().unplug();
        }
    }
}
```
MediatorLiveData做的主要事情就是感知另一个liveData的数据变化

在看上面的使用代码
```
liveDataMerger.addSource(liveData1, value -> liveDataMerger.setValue(value));
liveDataMerger.addSource(liveData2, value -> liveDataMerger.setValue(value));
liveDataMerger.observe(this, new Observer() {
            @Override
            public void onChanged(Object o) {
                
            }
        });
```
MediatorLiveData分别监听liveData1和liveData2的数据改变，当数据改变时，设置到自己父类LiveData的mData，
  然后通知MediatorLiveData的数据观察者