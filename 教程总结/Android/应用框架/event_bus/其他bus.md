

FlowBus
https://mp.weixin.qq.com/s/Q179VEa9ziLkjz3_-zr93g

livedatabus
https://github.com/JeremyLiao/LiveEventBus

livedata实现bus
https://www.jianshu.com/p/79d909b6f8bd
//所有的事件的值都会保留   
1 好处，再次使用，可以拿到以前的值
2 坏处，没有自动重置，再次使用可能收到以前的干扰  所有状态都保存在内存中。。。。
  没有默认值，需要自己改造
```
public class LiveEventBus {

 //Class<T> clz用于确定liveData传入的泛型，并没有使用
  public static <T> MutableLiveData<T> getDefault(String key, Class<T> clz) {
    return ready().with(key, clz);
  }

  private final Map<String, MutableLiveData<Object>> bus;

  private LiveEventBus() {
    bus = new HashMap<>();
  }

  private static class InstanceHolder {
    static final LiveEventBus INSTANCE = new LiveEventBus();
  }

  private static LiveEventBus ready() {
    return LiveEventBus.InstanceHolder.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  private <T> MutableLiveData<T> with(String key, Class<T> clz) {
    if (!bus.containsKey(key)) {
      MutableLiveData<Object> liveData = new MutableLiveData<>();
      bus.put(key, liveData);
    }
    return (MutableLiveData<T>) bus.get(key);
  }
}
```