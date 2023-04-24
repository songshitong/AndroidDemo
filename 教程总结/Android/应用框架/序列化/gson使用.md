
```
//直接使用
toJson(）

//方式1
Gson gson = new Gson(); // Or use new GsonBuilder().create();
   MyType target = new MyType();
   String json = gson.toJson(target);
   
//方式2   
Type listType = new TypeToken<List<String>>() {}.getType();
   List<String> target = new LinkedList<String>();
   target.add("blah");
  
   Gson gson = new Gson();
   //序列化
   String json = gson.toJson(target, listType);
   //反序列化
   List<String> target2 = gson.fromJson(json, listType);


写入流
Buffer buffer = new Buffer();
Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
JsonWriter jsonWriter = gson.newJsonWriter(writer);
TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
adapter.write(jsonWriter, value);
jsonWriter.close();   
从流读入
JsonReader jsonReader = gson.newJsonReader(value.charStream());
    try {
       TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
      T result = adapter.read(jsonReader);
      if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonIOException("JSON document was not fully consumed.");
      }
      return result;
    } finally {
      value.close();
    }
```



https://blog.csdn.net/pngfi/article/details/62057628
定义序列化的名字
@SerializedName("userId",alternate = {"stuName","studentName"})
alternate  alternative可供替代的
只工作在反序列化中，类似备用名

部分序列化，部分反序列化
@Expose
```
@Expose//序列化也反序列化
    public int id;
    @Expose(deserialize = false)//只序列化
    public String name;
    @Expose(serialize = false)//只反序列化
    public int age;
    @Expose(serialize = false,deserialize = false)//既不序列化也不反序列化
    public boolean isLOLPlayer;
```
自定义序列化和反序列化JsonAdapter
```
@JsonAdapter(UserJsonAdapter.class)
   public class User {
     public final String firstName, lastName;
     private User(String firstName, String lastName) {
       this.firstName = firstName;
       this.lastName = lastName;
     }
   }
   public class UserJsonAdapter extends TypeAdapter<User> {
     @Override public void write(JsonWriter out, User user) throws IOException {
       // implement write: combine firstName and lastName into name
       out.beginObject();
       out.name("name");
       out.value(user.firstName + " " + user.lastName);
       out.endObject();
       // implement the write method
     }
     @Override public User read(JsonReader in) throws IOException {
       // implement read: split name into firstName and lastName
       in.beginObject();
       in.nextName();
       String[] nameParts = in.nextString().split(" ");
       in.endObject();
       return new User(nameParts[0], nameParts[1]);
     }
   }
   
使用
 private static final class Gadget {
     @JsonAdapter(UserJsonAdapter2.class)
     final User user;
     Gadget(User user) {
       this.user = user;
     }
   }   
```


标记json版本
```
//新增
 @Since(1.0) private String emailAddress;
 @Since(1.0) private String password;
 @Since(1.1) private Address address;
 
 //废弃
 @Until(1.1) private String emailAddress;
 @Until(1.1) private String password;
```

直接将json的数字解析为Object时，默认为double类型
"progress":1   中1会解析为1.0
1 使用java自带的jsonObject解析
2 自定义解析器
https://blog.csdn.net/xiao_jun_0820/article/details/50232017
```
Gson gson = new GsonBuilder().
                registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
 
                    @Override
                    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                        if (src == src.longValue())
                            return new JsonPrimitive(src.longValue());
                        return new JsonPrimitive(src);
                    }
                }).create();
```
即如果是Double类型的，判断一下它和它的longValue是否相等，如果相等则说明小数位是补了一个".0",那么我们就返回src.longValue,否则直接返回Double src


gson统一设置时间格式
```
Gson gson = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd")  //"yyyy-MM-dd hh:mm:ss"
        .create();
```