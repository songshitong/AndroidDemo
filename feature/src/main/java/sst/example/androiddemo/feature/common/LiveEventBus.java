package sst.example.androiddemo.feature.common;

import androidx.lifecycle.MutableLiveData;
import java.util.HashMap;
import java.util.Map;

//缺点：
//1  不支持粘性和非粘性的获取
//2  不支持数据的清空  例如退出登录时，将所有数据重置

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
