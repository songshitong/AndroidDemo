
版本   implementation 'com.google.code.gson:gson:2.8.6'

https://www.jianshu.com/p/d04beef7f52a

toJson
com/google/gson/Gson.java
```
  public String toJson(Object src) {
    if (src == null) {
      return toJson(JsonNull.INSTANCE);
    }
    return toJson(src, src.getClass());
  }

  public String toJson(Object src, Type typeOfSrc) {
    StringWriter writer = new StringWriter();
    toJson(src, typeOfSrc, writer);
    return writer.toString();
  }
  
  public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
    try {
      JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
      toJson(src, typeOfSrc, jsonWriter);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
 
  public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
    TypeAdapter<?> adapter = getAdapter(TypeToken.get(typeOfSrc));
    boolean oldLenient = writer.isLenient();
    writer.setLenient(true);
    boolean oldHtmlSafe = writer.isHtmlSafe();
    writer.setHtmlSafe(htmlSafe);
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setSerializeNulls(serializeNulls);
    try {
      ((TypeAdapter<Object>) adapter).write(writer, src);
    } catch (IOException e) {
      ...
  }    

TypeToken代表一个类型
com/google/gson/reflect/TypeToken.java
public class TypeToken<T> {
  final Class<? super T> rawType;
  final Type type;
  final int hashCode;
 }   
```
TypeAdapter负责不同类型的序列化和反序列化，自定义重写read和write就可以了
com/google/gson/TypeAdapter.java
```
public abstract class TypeAdapter<T> {
  public abstract void write(JsonWriter out, T value) throws IOException;
  public abstract T read(JsonReader in) throws IOException;
}
```

getAdapter
```
 public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
    //从缓存获取
    TypeAdapter<?> cached = typeTokenCache.get(type == null ? NULL_KEY_SURROGATE : type);
    if (cached != null) {
      return (TypeAdapter<T>) cached;
    }
    //ThreadLocal缓存TypeAdapter对象，不同的线程使用缓存来解析的时候互不影响
    Map<TypeToken<?>, FutureTypeAdapter<?>> threadCalls = calls.get();
    boolean requiresThreadLocalCleanup = false;
    if (threadCalls == null) {
      threadCalls = new HashMap<TypeToken<?>, FutureTypeAdapter<?>>();
      calls.set(threadCalls);
      requiresThreadLocalCleanup = true;
    }

    FutureTypeAdapter<T> ongoingCall = (FutureTypeAdapter<T>) threadCalls.get(type);
    if (ongoingCall != null) {
      return ongoingCall;
    }
    //创建新的
    try {
      FutureTypeAdapter<T> call = new FutureTypeAdapter<T>();
      threadCalls.put(type, call);
      //factories存的是各种TypeAdapter
      for (TypeAdapterFactory factory : factories) {
        TypeAdapter<T> candidate = factory.create(this, type);
        if (candidate != null) {
          call.setDelegate(candidate);
          //缓存
          typeTokenCache.put(type, candidate);
          return candidate;
        }
      }
      throw new IllegalArgumentException("GSON (" + GsonBuildConfig.VERSION + ") cannot handle " + type);
    } ...
  }
```
factories在gson构造器初始化
```
 List<TypeAdapterFactory> factories = new ArrayList<TypeAdapterFactory>();
    factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
    ...
    // type adapters for basic platform types
    factories.add(TypeAdapters.STRING_FACTORY);
    factories.add(TypeAdapters.INTEGER_FACTORY);
    factories.add(TypeAdapters.BOOLEAN_FACTORY);
    factories.add(TypeAdapters.BYTE_FACTORY);
    ... 
```