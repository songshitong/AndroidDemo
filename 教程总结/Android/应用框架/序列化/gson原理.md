
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
      //遍历各种factory，如果能创建TypeAdapter就退出该方法
      for (TypeAdapterFactory factory : factories) {
        TypeAdapter<T> candidate = factory.create(this, type);
        if (candidate != null) {
          call.setDelegate(candidate);
          //缓存
          typeTokenCache.put(type, candidate);
          return candidate;
        }
      }
      //所有factory都不能创建TypeAdapter，抛出异常
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



fromJson
```
  public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    //转为string流
    StringReader reader = new StringReader(json);
    T target = (T) fromJson(reader, typeOfT);
    return target;
  }

 public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    JsonReader jsonReader = newJsonReader(json);
    T object = (T) fromJson(jsonReader, typeOfT);
    assertFullConsumption(object, jsonReader);
    return object;
  }
 
  public <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    boolean isEmpty = true;
    boolean oldLenient = reader.isLenient();
    reader.setLenient(true);
    try {
      //读取json的第一个类型
      reader.peek();
      isEmpty = false;
      TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT);
      //获取Adapter，使用Adapter转为对象
      TypeAdapter<T> typeAdapter = getAdapter(typeToken);
      T object = typeAdapter.read(reader);
      return object;
    } catch (EOFException e) {
     ...
  }   
```

可以看到fromJson和toJson最终调用TypeAdapter来进行序列化和反序列化
TypeAdapterFactory符合TypeAdapter的构建
TypeAdapterFactory的类型，gson构造器中
```
//内置的，不能重写
 factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
    factories.add(ObjectTypeAdapter.FACTORY);
//平台基础类型
 factories.add(TypeAdapters.STRING_FACTORY);
    factories.add(TypeAdapters.INTEGER_FACTORY);
    factories.add(TypeAdapters.BOOLEAN_FACTORY);
    factories.add(TypeAdapters.BYTE_FACTORY);
    factories.add(TypeAdapters.SHORT_FACTORY);
...
//合成和用户定义的
this.jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
。。。
factories.add(new ReflectiveTypeAdapterFactory(
        constructorConstructor, fieldNamingStrategy, excluder, jsonAdapterFactory));       
```

用户自定义的Adapter(@JsonAdapter)使用com/google/gson/internal/bind/JsonAdapterAnnotationTypeAdapterFactory.java
```
public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> targetType) {
    Class<? super T> rawType = targetType.getRawType();
    JsonAdapter annotation = rawType.getAnnotation(JsonAdapter.class);
    //注解不为空，创建JsonAdapter
    if (annotation == null) {
      return null;
    }
    return (TypeAdapter<T>) getTypeAdapter(constructorConstructor, gson, targetType, annotation);
  }
```
没有注解(@JsonAdapter)
com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java
```
  @Override public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();
   
    if (!Object.class.isAssignableFrom(raw)) {
      //基础类型，该类不负责解析
      return null; // it's a primitive!
    }
    //类型是对象也就是Object的子类
    ObjectConstructor<T> constructor = constructorConstructor.get(type);
    return new Adapter<T>(constructor, getBoundFields(gson, type, raw));
  }
```
注意区分 ObjectTypeAdapter
com\google\gson\internal\bind\ObjectTypeAdapter.class
```
  public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      //用来解析Object类
      return type.getRawType() == Object.class ? new ObjectTypeAdapter(gson) : null;
    }
  };
```

getBoundFields(gson, type, raw)
将实体类中需要解析的字段添加一个集合里，在反序列化时进行赋值。
BoundField标记类的字段名字，是否需要序列化和反序列化
```
 static abstract class BoundField {
    final String name;
    final boolean serialized;
    final boolean deserialized;
}
```
```
 private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
    Map<String, BoundField> result = new LinkedHashMap<String, BoundField>();
    if (raw.isInterface()) {
      return result;
    }

    Type declaredType = type.getType();
    while (raw != Object.class) {
      //获取类所有的属性
      Field[] fields = raw.getDeclaredFields();
      for (Field field : fields) {
        //是否需要序列化和反序列化
        boolean serialize = excludeField(field, true);
        boolean deserialize = excludeField(field, false);
        if (!serialize && !deserialize) {
          continue;
        }
        accessor.makeAccessible(field);
        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
        //拿到@SerializedName注解标注的名字
        List<String> fieldNames = getFieldNames(field);
        BoundField previous = null;
        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
          String name = fieldNames.get(i);
          if (i != 0) serialize = false; // only serialize the default name
          //创建BoundField并保存
          BoundField boundField = createBoundField(context, field, name,
              TypeToken.get(fieldType), serialize, deserialize);
          //HashMap存入新的，保存以前的    
          BoundField replaced = result.put(name, boundField);
          if (previous == null) previous = replaced;
        }
        if (previous != null) {
          throw new IllegalArgumentException(declaredType
              + " declares multiple JSON fields named " + previous.name);
        }
      }
      //父类进行处理
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return result;
  }
  

 private ReflectiveTypeAdapterFactory.BoundField createBoundField(
      final Gson context, final Field field, final String name,
      final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
    //是否是基础类型  
    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
    // special casing primitives here saves ~5% on Android...
    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
    TypeAdapter<?> mapped = null;
    if (annotation != null) {
      //使用自定义TypeAdapter
      mapped = jsonAdapterFactory.getTypeAdapter(
          constructorConstructor, context, fieldType, annotation);
    }
    final boolean jsonAdapterPresent = mapped != null;
    //查询TypeAdapter
    if (mapped == null) mapped = context.getAdapter(fieldType);

    final TypeAdapter<?> typeAdapter = mapped;
    //创建新的BoundField
    return new ReflectiveTypeAdapterFactory.BoundField(name, serialize, deserialize) {
      @SuppressWarnings({"unchecked", "rawtypes"}) // the type adapter and field type always agree
      @Override void write(JsonWriter writer, Object value)
          throws IOException, IllegalAccessException {
        //获取字段的值写到TypeAdapter  
        Object fieldValue = field.get(value);
        TypeAdapter t = jsonAdapterPresent ? typeAdapter
            : new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
        t.write(writer, fieldValue);
      }
      @Override void read(JsonReader reader, Object value)
          throws IOException, IllegalAccessException {
        //从TypeAdapter读到值，写到字段field
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          field.set(value, fieldValue);
        }
      }
      //值存不存在，需不需序列化
      @Override public boolean writeField(Object value) throws IOException, IllegalAccessException {
        if (!serialized) return false;
        Object fieldValue = field.get(value);
        return fieldValue != value; // avoid recursion for example for Throwable.cause
      }
    };
  }  
```

反序列化的过程
typeAdapter.read
gson\internal\bind\ReflectiveTypeAdapterFactory.java
```
 @Override public T read(JsonReader in) throws IOException {
      ...
      //反射构建实例
      T instance = constructor.construct();

      try {
        in.beginObject();
        //遍历json流
        while (in.hasNext()) {
          //读取json的key
          String name = in.nextName();
          BoundField field = boundFields.get(name);
          if (field == null || !field.deserialized) {
            in.skipValue();
          } else {
            //将读到的值写入对象实例instance  最终使用jsonReader读取
            field.read(in, instance);
          }
        }
      } ...
      //停止
      in.endObject();
      return instance;
    }

```
构造实例过程
gson\internal\ObjectConstructor.java
```
public interface ObjectConstructor<T> {
  public T construct();
}
```
com/google/gson/internal/ConstructorConstructor.java
```
 private <T> ObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {
    try {
      //获取构造器
      final Constructor<? super T> constructor = rawType.getDeclaredConstructor();
      if (!constructor.isAccessible()) {
        accessor.makeAccessible(constructor);
      }
      return new ObjectConstructor<T>() {
        @Override public T construct() {
          try {
            Object[] args = null;
            //反射创建
            return (T) constructor.newInstance(args);
          } ...
        }
      };
    }...
  }
```

JsonReader读取json
json格式标准  https://www.ietf.org/rfc/rfc7159.txt
beginObject()  开始
com/google/gson/stream/JsonReader.java
```
  public void beginObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_OBJECT) {
      push(JsonScope.EMPTY_OBJECT);
      peeked = PEEKED_NONE;
    } else {
      throw new IllegalStateException("Expected BEGIN_OBJECT but was " + peek() + locationString());
    }
  }
  
  public boolean hasNext() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    //对象结束}，数组结束]标记着json没了
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY;
  }  
  
   public String nextName() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED_NAME) {
      //非引号
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
      //单引号
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
     //双引号
      result = nextQuotedValue('"');
    } else {
      throw new IllegalStateException("Expected a name but was " + peek() + locationString());
    }
    peeked = PEEKED_NONE;
    pathNames[stackSize - 1] = result;
    return result;
  }
```
