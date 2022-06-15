

https://square.github.io/retrofit/
消息头header
@Headers("Cache-Control: max-age=640000")
参数作为header
```
@GET("user")
Call<User> getUser(@Header("Authorization") String authorization)
//map作为header
@GET("user")
Call<User> getUser(@HeaderMap Map<String, String> headers)
```

get方法
@GET("users/list")
get中path是参数
```
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId);
```
get中的问号?
```
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId, @Query("sort") String sort);
```

post方法
@POST("users/new")
post中参数为body
```
@POST("users/new")
Call<User> createUser(@Body User user);
```
当请求为json,body为String mobile时，格式化为"18111111"，需要将其包装为对象或者放进HashMap

参数
@Field  field只能用于@FormUrlEncoded
```
@FormUrlEncoded
@POST("user/edit")
Call<User> updateUser(@Field("first_name") String first, @Field("last_name") String last);
```

多类型请求
```
@Multipart
@PUT("user/photo")
Call<User> updateUser(@Part("photo") RequestBody photo, @Part("description") RequestBody description);
```