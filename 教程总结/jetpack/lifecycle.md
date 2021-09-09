生命周期
https://developer.android.com/topic/libraries/architecture/lifecycle
参考文档
https://github.com/leavesC/AndroidGuide/blob/master/android_jetpack/1-Lifecycle%E6%BA%90%E7%A0%81%E8%AF%A6%E8%A7%A3.md
TODO LifecycleRegistry的状态流转，Lifecycling

androidx 版本 
androidx.activity:activity:1.0.0
androidx.lifecycle:lifecycle-runtime:2.1.0
androidx.lifecycle:lifecycle-runtime:2.3.1

使用
getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {       
            }
        });

优点
context不具备生命周期，使用Lifecycle可以让组件具备生命周期感知的能力，自动销毁   
简化activity和fragment的生命周期事件的复杂度，组件自己处理
减少activity泄漏， LifecycleRegistry与activity是弱依赖，发送泄漏的一般是当前组件了

原理
getLifecycle 返回的是LifecycleRegistry

activity中初始化LifecycleRegistry
ComponentActivity{
 LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this)
 
 onCreate(){
   ReportFragment.injectIfNeededIn(this);
 }
}

///具备Android的生命周期，实现类一般是activity，fragment
public interface LifecycleOwner {
    Lifecycle getLifecycle();
} 


LifecycleRegistry{
 ///注册具备Android生命周期的类，弱引用持有，减少泄漏
 WeakReference<LifecycleOwner> mLifecycleOwner;
 ///初始化时 注册LifecycleOwner
 public LifecycleRegistry(@NonNull LifecycleOwner provider) {
         mLifecycleOwner = new WeakReference<>(provider);
     }
     
 ///生命周期观察者类
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
'''    
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
'''            