生命周期
https://developer.android.com/topic/libraries/architecture/lifecycle
参考文档
https://github.com/leavesC/AndroidGuide/blob/master/android_jetpack/1-Lifecycle%E6%BA%90%E7%A0%81%E8%AF%A6%E8%A7%A3.md
TODO LifecycleRegistry的状态流转，Lifecycling

androidx 版本 
androidx.activity:activity:1.0.0
androidx.lifecycle:lifecycle-runtime:2.1.0
androidx.lifecycle:lifecycle-runtime:2.3.1

总结  
LifecycleRegistry使用WeakReference保存LifecycleOwner，
LifecycleOwner是有生命周期的，一般是activity,fragment
ComponentActivity里面存在LifecycleRegistry，ReportFragment通过向activity添加一个空的fragment进行感知生命周期，
  生命周期改变时通过LifecycleRegistry进行事件分发


使用
```
new 组件(lifecycle) 组件可以处理自己的生命周期了   组件->lifecycle->WeakReference lifecycleOwner(activity,fragment)
getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {       
            }
        });
//kotlin        
lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {

        }

        override fun onResume(owner: LifecycleOwner) {

        }

        override fun onDestroy(owner: LifecycleOwner) {

        }
    })        
```


优点
context不具备生命周期，使用Lifecycle可以让组件具备生命周期感知的能力，自动销毁   
简化activity和fragment的生命周期事件的复杂度，组件自己处理
减少activity泄漏， LifecycleRegistry与activity是弱依赖，发生泄漏的一般是当前组件了  

Lifecycle 是 Jetpack 整个家族体系内最为基础的组件之一，正是因为有了 Lifecycle 的存在，使得如今开发者搭建依赖于生命周期变化的业务逻辑变得简单高效了许多，
使得我们可以用一种统一的方式来监听 Activity、Fragment、Service、甚至是 Process 的生命周期变化，且大大减少了业务代码发生内存泄漏和 NPE 的风险


原理
getLifecycle 返回的是Lifecycle，实际是LifecycleRegistry
```
public abstract class Lifecycle {
    AtomicReference<Object> mInternalScopeRef = new AtomicReference<>();
    public abstract void addObserver(@NonNull LifecycleObserver observer);
    public abstract void removeObserver(@NonNull LifecycleObserver observer);
    public abstract State getCurrentState();
    }
```

activity中初始化LifecycleRegistry
ComponentActivity{
 LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this)
 
 onCreate(){
    //生命周期感知
   ReportFragment.injectIfNeededIn(this);
 }
}

///具备Android的生命周期，实现类一般是activity，fragment
public interface LifecycleOwner {
    Lifecycle getLifecycle();
} 


LifecycleRegistry extends Lifecycle{
 ///注册具备Android生命周期的类，弱引用持有，减少泄漏
 WeakReference<LifecycleOwner> mLifecycleOwner;
 ///初始化时 注册LifecycleOwner
 public LifecycleRegistry(@NonNull LifecycleOwner provider) {
         mLifecycleOwner = new WeakReference<>(provider);
     }
     
 ///生命周期观察者类  todo FastSafeIterableMap
 FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap  
 public void addObserver(@NonNull LifecycleObserver observer) {
      ...
         ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver);
      ...   
      //将当前的生命周期进行分发
      statefulObserver.dispatchEvent(lifecycleOwner, upEvent(statefulObserver.mState));
    }  
    
 
  ///收到外部事件并分发
  public void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
         State next = getStateAfter(event);
         moveToState(next);
     }
}


类的关联关系已完成
生命周期事件上报
ReportFragment{
  public static void injectIfNeededIn(Activity activity) {
          ///sdk>=29 activity可以注册生命周期事件了  activity.registerActivityLifecycleCallbacks
          if (VERSION.SDK_INT >= 29) {
              ReportFragment.LifecycleCallbacks.registerIn(activity);
          }
          ///添加空的fragment ReportFragment 进行事件分发
          FragmentManager manager = activity.getFragmentManager();
          if (manager.findFragmentByTag("androidx.lifecycle.LifecycleDispatcher.report_fragment_tag") == null) {
              manager.beginTransaction().add(new ReportFragment(), "androidx.lifecycle.LifecycleDispatcher.report_fragment_tag").commit();
              manager.executePendingTransactions();
          }
      }
  
  ///分发start事件    
  public void onStart() {
          super.onStart();
          this.dispatchStart(this.mProcessListener);
          this.dispatch(Event.ON_START);
      }    
  
  ///
  static void dispatch(@NonNull Activity activity, @NonNull Event event) {
           ///获取activity的lifecyle
           Lifecycle lifecycle = ((LifecycleOwner)activity).getLifecycle();
           if (lifecycle instanceof LifecycleRegistry) {
             ///通过lifecyle的子类LifecycleRegistry分发事件
                ((LifecycleRegistry)lifecycle).handleLifecycleEvent(event);
            }
      }    
}    
        
        
LifecycleRegistry的内部类  将LifecycleObserver转为LifecycleEventObserver  dispatchEvent进行事件触发
```
static class ObserverWithState {
        State mState;
        LifecycleEventObserver mLifecycleObserver;

        ObserverWithState(LifecycleObserver observer, State initialState) {
            mLifecycleObserver = Lifecycling.lifecycleEventObserver(observer);
            mState = initialState;
        }

        void dispatchEvent(LifecycleOwner owner, Event event) {
            State newState = getStateAfter(event);
            mState = min(mState, newState);
            mLifecycleObserver.onStateChanged(owner, event);
            mState = newState;
        }
    }
```         