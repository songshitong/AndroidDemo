转换器主要用于将okhttp的ResponseBody转换为业务类，或者将业务类转为RequestBody

converter接口    
```
public interface Converter<F, T> {
  @Nullable
  T convert(F value) throws IOException;  //通过泛型定义输入输出

  abstract class Factory {
    //ResponseBody 是okhttp的    //通过泛型定义了具体的转换
    public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
        Type type, Annotation[] annotations, Retrofit retrofit) {
      return null;
    }
    //RequestBody是okhttp的
    public @Nullable Converter<?, RequestBody> requestBodyConverter(
        Type type,
        Annotation[] parameterAnnotations,
        Annotation[] methodAnnotations,
        Retrofit retrofit) {
      return null;
    }

    public @Nullable Converter<?, String> stringConverter(
        Type type, Annotation[] annotations, Retrofit retrofit) {
      return null;
    }

    protected static Type getParameterUpperBound(int index, ParameterizedType type) {
      return Utils.getParameterUpperBound(index, type);
    }
    
    static Class<?> getRawType(Type type) {
      return Utils.getRawType(type);
    }
  }
}
```
Converter只有一个方法convert，就是将一个类转为另一个类
内部类Factory用于创建一个将任意类转为指定类的converter
这里主要分析
  1 responseBodyConverter  将ResponseBody转换为业务类
  2 requestBodyConverter   将业务类转为RequestBody

自定义Converter需要继承Factory，每个方法的类要实现Converter接口
以GsonConverterFactory为例
```
public final class GsonConverterFactory extends Converter.Factory {
  public static GsonConverterFactory create() {
    return create(new Gson());
  }
  
  public static GsonConverterFactory create(Gson gson) {
    if (gson == null) throw new NullPointerException("gson == null");
    return new GsonConverterFactory(gson);
  }
  private final Gson gson;
  private GsonConverterFactory(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    //TypeAdapter是Gson的自带类  
    TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
    return new GsonResponseBodyConverter<>(gson, adapter);
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(
      Type type,
      Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations,
      Retrofit retrofit) {
    TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
    return new GsonRequestBodyConverter<>(gson, adapter);
  }
}
```
GsonConverterFactory的responseBodyConverter方法构建了一个GsonResponseBodyConverter
  requestBodyConverter方法构建了一个GsonRequestBodyConverter

看一个将ResponseBodyConverter转为业务类的过程
```
final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final Gson gson;
  private final TypeAdapter<T> adapter;

  GsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  public T convert(ResponseBody value) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(value.charStream());
    try {
      T result = adapter.read(jsonReader);
      if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonIOException("JSON document was not fully consumed.");
      }
      return result;
    } finally {
      value.close();
    }
  }
}
```
可以看到GsonResponseBodyConverter实现了Converter的convert方法
  主要过程就是读取ResponseBody的字符流，使用Gson的反序列化，将字符流转为业务类返回


同理，看一下业务类转为RequestBody的过程
```
final class GsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
  private final Gson gson;
  private final TypeAdapter<T> adapter;

  GsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override
  public RequestBody convert(T value) throws IOException {
    Buffer buffer = new Buffer();
    Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
    JsonWriter jsonWriter = gson.newJsonWriter(writer);
    adapter.write(jsonWriter, value);
    jsonWriter.close();
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
```
可以看到GsonRequestBodyConverter实现了Converter的convert方法
  主要过程就是使用Gson的序列化，将业务类转为流，然后使用Okio的ByteString创建RequestBody


Converter的初始化顺序
1.BuiltInConverters
  用于转换 ResponseBody, java void转为null,kotlin Unit转为Unit.INSTANCE
2.添加自定义的Converter
```
public Builder addConverterFactory(Converter.Factory factory) {
      converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
      return this;
    }
```
2.默认的converter
  android24 存在OptionalConverterFactory() 存在optional类时，将ResponseBody转为业务类同时包裹一层Optional
 

requestBodyConverter的使用
```
okhttp3.Request create(Object[] args) throws IOException {
    @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
    ...
    List<Object> argumentList = new ArrayList<>(argumentCount);
    for (int p = 0; p < argumentCount; p++) {
      argumentList.add(args[p]);
      //主要调用converter 将注解参数添加到request
      handlers[p].apply(requestBuilder, args[p]);
    }

    return requestBuilder.get().tag(Invocation.class, new Invocation(method, argumentList)).build();
  }
//ParameterHandler.Part 还有其他的convert调用,Field,HeaderMap,QueryMap等调用convert的其他实现
void apply(RequestBuilder builder, @Nullable T value) {
      if (value == null) return; // Skip null values.
      RequestBody body;
      try {
        //将业务类转为RequestBody
        body = converter.convert(value);
      } catch (IOException e) {
        throw Utils.parameterError(method, p, "Unable to convert " + value + " to RequestBody", e);
      }
      builder.addPart(headers, body);
    }
```
ParameterHandler主要用于存储参数的注解
 在构建Request时，将参数相关的信息添加到Request中，这就是requestBodyConverter的使用


ResponseBodyConverter的使用
```
//OkHttpCall
Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
    ResponseBody rawBody = rawResponse.body();
    rawResponse =
        rawResponse
            .newBuilder()
            .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
            .build();

    int code = rawResponse.code();
    if (code < 200 || code >= 300) {
      try {
        // Buffer the entire body to avoid future I/O.
        ResponseBody bufferedBody = Utils.buffer(rawBody);
        return Response.error(bufferedBody, rawResponse);
      } finally {
        rawBody.close();
      }
    }

    if (code == 204 || code == 205) {
      rawBody.close();
      return Response.success(null, rawResponse);
    }

    ExceptionCatchingResponseBody catchingBody = new ExceptionCatchingResponseBody(rawBody);
    try {
      //将ResponseBody转为业务类
      T body = responseConverter.convert(catchingBody);
      return Response.success(body, rawResponse);
    } catch (RuntimeException e) {

      catchingBody.throwIfCaught();
      throw e;
    }
  }
  
//请求的取消
 public void cancel() {
    canceled = true;

    okhttp3.Call call;
    synchronized (this) {
      call = rawCall;
    }
    if (call != null) {
      call.cancel();
    }
  }  
```
可以看到主要是用于将网络响应ResponseBody转换为业务类


其他converter
ScalarsConverter
request 将基本数据类型转为string
response 将string转为基本数据类型
以responseBodyConverter为例 scalars/src/main/java/retrofit2/converter/scalars/ScalarsConverterFactory.java
```
 public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (type == String.class) {
      return StringResponseBodyConverter.INSTANCE;
    }
    if (type == Boolean.class || type == boolean.class) {
      return BooleanResponseBodyConverter.INSTANCE;
    }
    if (type == Byte.class || type == byte.class) {
      return ByteResponseBodyConverter.INSTANCE;
    }
    if (type == Character.class || type == char.class) {
      return CharacterResponseBodyConverter.INSTANCE;
    }
    if (type == Double.class || type == double.class) {
      return DoubleResponseBodyConverter.INSTANCE;
    }
    if (type == Float.class || type == float.class) {
      return FloatResponseBodyConverter.INSTANCE;
    }
    if (type == Integer.class || type == int.class) {
      return IntegerResponseBodyConverter.INSTANCE;
    }
    if (type == Long.class || type == long.class) {
      return LongResponseBodyConverter.INSTANCE;
    }
    if (type == Short.class || type == short.class) {
      return ShortResponseBodyConverter.INSTANCE;
    }
    return null;
  }
}
```