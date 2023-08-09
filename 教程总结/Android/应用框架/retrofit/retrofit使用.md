
retrofit placeholder
get中path是参数 id为占位符，可以动态改变url
```
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId);
```

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

post改变url
```
    @Multipart
    @POST()
    fun uploadFile(
        @Url url:String,
        @Part file: MultipartBody.Part?
    ): Call<String>
```

参数
@Field  field只能用于@FormUrlEncoded
```
@FormUrlEncoded
@POST("user/edit")
Call<User> updateUser(@Field("first_name") String first, @Field("last_name") String last);
```
多个参数
@FormUrlEncoded
@POST("/things")
Call<ResponseBody> things(@FieldMap Map<String, String> fields);

多类型请求
```
@Multipart
@PUT("user/photo")
Call<User> updateUser(@Part("photo") RequestBody photo, @Part("description") RequestBody description);

 @Multipart    
 @POST("user/updateAvatar.do")
 Call<Response> upload(@Part("upload1\"; filename=\"image1.jpg\"") RequestBody imgs );

https://juejin.cn/post/6844903460555456525
 // 为file建立RequestBody实例
        RequestBody requestFile =
            RequestBody.create( new File(filePath),MediaType.parse("multipart/form-data"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file",filePath,requestFile);
        
@Multipart
@POST("upload")
Call<ResponseBody> uploadFile(
    @Part("description") RequestBody description,
    @Part MultipartBody.Part file);
    
//只有文件    
@Multipart
@POST("upload")
Call<ResponseBody> uploadFile(@Part MultipartBody.Part file);
    
@Multipart
@POST("upload")
Call<ResponseBody> uploadFileWithPartMap(
        @PartMap() Map<String, RequestBody> partMap,
        @Part MultipartBody.Part file);
        }

```
上传成功的报文  没有boundary是不对的
```
I/okhttp.OkHttpClient: --> POST http://...../api/audio/upload
/okhttp.OkHttpClient: Content-Type: multipart/form-data; boundary=e167495e-b6a2-4d30-84ed-a9765dcc298d
/okhttp.OkHttpClient: Content-Length: 1183
/okhttp.OkHttpClient: --e167495e-b6a2-4d30-84ed-a9765dcc298d
/okhttp.OkHttpClient: Content-Disposition: form-data; name="file"; filename="REC04L0301002250_20220607172338.ogg"
/okhttp.OkHttpClient: Content-Type: audio/*
/okhttp.OkHttpClient: Content-Length: 960
/okhttp.OkHttpClient: 
/okhttp.OkHttpClient: OggS��������������
/okhttp.OkHttpClient: --e167495e-b6a2-4d30-84ed-a9765dcc298d--
/okhttp.OkHttpClient: --> END POST (1183-byte body)
```

文件上传进度
可以使用https://github.com/JessYanCoding/ProgressManager
可以自定义RequestBody
https://stackoverflow.com/questions/33338181/is-it-possible-to-show-progress-bar-when-upload-image-via-retrofit-2
自定义ProgressRequestBody
```
public class ProgressRequestBody extends RequestBody {
  private File mFile;
  private UploadCallbacks mListener;
  private String content_type;
  private static final int DEFAULT_BUFFER_SIZE = 2048;
  Handler handler = new Handler(Looper.getMainLooper());

  public interface UploadCallbacks {
    void onProgressUpdate(int percentage);

    void onError();

    void onFinish();
  }

  public ProgressRequestBody(final File file, String content_type, final UploadCallbacks listener) {
    this.content_type = content_type;
    mFile = file;
    mListener = listener;
  }

  @Override
  public MediaType contentType() {
    return MediaType.parse(content_type + "/*");
  }

  @Override
  public long contentLength() throws IOException {
    return mFile.length();
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    long fileLength = mFile.length();
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    FileInputStream in = new FileInputStream(mFile);
    long uploaded = 0;

    try {
      int read;
      while ((read = in.read(buffer)) != -1) {
        //读完了
        uploaded += read;
        // update progress on UI thread
        handler.post(new ProgressUpdater(uploaded, fileLength));
        sink.write(buffer, 0, read);
      }

    } catch (Exception e) {
       Log.e(e)
       callListenerError();
    } finally {
      try{
            in.close();
      }catch(Exception e){
        Log.e(e)
      }
    }
  }
  
  //切换到主线程
  private void callListenerError() {
    if (null != mListener) {
      handler.post(()-> mListener.onError());
    }
  }
  
  private class ProgressUpdater implements Runnable {
    private long mUploaded;
    private long mTotal;

    public ProgressUpdater(long uploaded, long total) {
      mUploaded = uploaded;
      mTotal = total;
    }

    @Override
    public void run() {
      if(null != mListener){
        mListener.onProgressUpdate((int) (100 * mUploaded / mTotal));
        if (mUploaded == mTotal) {
          mListener.onFinish();
        }
      }
    }
  }
}


//使用
 ProgressRequestBody fileBody = new ProgressRequestBody(file, "multipart/form-data",callBacks);
            MultipartBody.Part filePart = 

            MultipartBody.Part.createFormData("image", file.getName(), fileBody);

            Call<JsonObject> request = RetrofitClient.uploadImage(filepart);

             request.enqueue(new Callback<JsonObject>() {
             @Override
             public void onResponse(Call<JsonObject> call,   Response<JsonObject> response) {
                if(response.isSuccessful()){
                /* Here we can equally assume the file has been downloaded successfully because for some reasons the onFinish method might not be called, I have tested it myself and it really not consistent, but the onProgressUpdate is efficient and we can use that to update our progress on the UIThread, and we can then set our progress to 100% right here because the file already downloaded finish. */
                  }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                      /* we can also stop our progress update here, although I have not check if the onError is being called when the file could not be downloaded, so I will just use this as a backup plan just in case the onError did not get called. So I can stop the progress right here. */
            }
        });
```

RequestBody.writeTo调用两次
https://stackoverflow.com/questions/40691610/retrofit-2-requestbody-writeto-method-called-twice
writeTo可能被其他日志拦截器调用，从而多次调用
1.移除HttpLoggingInterceptor或更改基本为 HEADERS, BASIC or NONE 或者重写HttpLoggingInterceptor
    重写时默认的request.header的Content-Type为null 
    可以使用requestBody.contentType()?.type  != "multipart"
```
else if(requestBody is MultipartBody) { //multipart只输出header
          //文件上传 写入header
          requestBody.parts.forEach {
            it.headers?.forEach { header->
              logger.log(header.toString())
            }
          }
        }
```
2. 判断调用次数，一个拦截器，第二次是自己的逻辑if(firstTimeCounter==2){}


retrofit多域名  
1.支持域名切换
https://github.com/JessYanCoding/RetrofitUrlManager
2.不同接口不同域名
(@Url String url) 动态执行url
https://blog.csdn.net/Jason_996/article/details/78659019
Retrofit解决多个BaseURL切换的问题
1 retrofit添加拦截器,根据注解信息,处理多个baseurl
   缺点：注解中的url为常量，无法进行拼接
1.1 增强版 注解只用key，配置baseUrl的集合，根据key进行替换，
```
public class Api {
    public static final String base_url = "http://172.0.0.92:8080/";
    public static final String base_url_mdffx = "http://11.254.16.19/";
}
public interface IRxjavaService {
   //额外的注解
    @Headers({"urlname:manage"})
    @POST("members/auth")
    Observable<LoginBean> doLogin(@Body RequestBody requestBody);
 
    @Headers({"urlname:mdffx"})
    @FormUrlEncoded
    @POST("login")
    Observable<LoginMdffxBean> doLoginMdffx(@Field("username") String username,@Field("password") String password);
 
 
    @Headers({"urlname:manage"})
    @GET("members/datas")
    Observable<TongjiDataBean> doData(@Query("type") int type,@Query("params") int params);
}

//添加拦截器
public OkHttpClient getokhttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient = new OkHttpClient.Builder()
                    //处理多BaseUrl,添加应用拦截器
                    .addInterceptor(new MoreBaseUrlInterceptor())
                    //添加头部信息
                    .addInterceptor(new AddHeadersInterceptor())
                    .addNetworkInterceptor(httpLoggingInterceptor)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }
    
//拦截器处理
public class MoreBaseUrlInterceptor implements Interceptor {
  @Override
  public Response intercept(Chain chain) throws IOException {
    //获取原始的originalRequest
    Request originalRequest = chain.request();
    //获取老的url
    HttpUrl oldUrl = originalRequest.url();
    //获取originalRequest的创建者builder
    Request.Builder builder = originalRequest.newBuilder();
    //获取头信息的集合如：manage,mdffx
    String key = AHApiService.DOMAIN_KEY.replace(": ","");
    String urlname = originalRequest.headers().get(key);
    if (urlname != null ) {
      //删除原有配置中的值,就是namesAndValues集合里的值
      builder.removeHeader(key);
      //获取头信息中配置的value,如：manage或者mdffx
      //HttpUrl baseURL=null;
      //根据头信息中配置的value,来匹配新的base_url地址
      //baseURL = HttpUrl.parse(urlname);
      //重建新的HttpUrl，需要重新设置的url部分
      //替换baseurl
      HttpUrl newHttpUrl = HttpUrl.parse(oldUrl.toString().replace(AHApiService.BASE_URL,urlname));
      //HttpUrl newHttpUrl = oldUrl.newBuilder()
      //    .scheme(baseURL.scheme())//http协议如：http或者https
      //    .host(baseURL.host())//主机地址
      //    .port(baseURL.port())//端口
      //    .addPathSegment(baseURL.pathSegments().get(0))//第一个
      //    .build();
      //获取处理后的新newRequest
      Request newRequest = builder.url(newHttpUrl).build();
      return  chain.proceed(newRequest);
    }else{
      return chain.proceed(originalRequest);
    }

  }
}
```
使用
```
 public static final String DOMAIN_BASE_URL = "Domain-Name: baseUrl";
 @Headers(DOMAIN_BASE_URL)
  Observable<LoginEntity> userLogin(@Body Map map);
```
2 使用不同的retrofit实例和不同的域名


全局header
```
OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("X-Requested-With", "XMLHttpRequest")
                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36")
                                .build();
                        return chain.proceed(request);
                    }
                }).build();
```

todo 请求取消
https://juejin.cn/post/6844903729418764302
https://cloud.tencent.com/developer/article/1783096
https://www.jianshu.com/p/206f0060e1e4
https://stackoverflow.com/questions/39592084/how-to-cancel-retrofit-request


自定义注解与处理  Retrofit2.6.0以后
https://stackoverflow.com/questions/47760861/retrofit-2-custom-annotations-for-custom-interceptor
自定义注解YourAnnotation
获取注解
request.tag(Invocation.class).getClass().getAnnotation(YourAnnotation.class)


下载文件  https://juejin.cn/post/6844903601341464589
```
@Streaming //大文件时要加不然会OOM
@GET
Call<ResponseBody> downloadFile(@Url String fileUrl)

 mCall = mApi.downloadFile(url);
            mCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {
                    //下载文件放在子线程
                    mThread = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            //保存到本地
                            writeFile2Disk(response, mFile, downloadListener);
                        }
                    };
                    mThread.start();
                }
//后续的读写流
 InputStream is = response.body().byteStream(); //获取下载输入流
  long totalLength = response.body().contentLength(); 
```

给每个请求设置不同的超时时长   
1 可以使用header或者自定义注解  缺点不能设置callTimeout也就是整个请求的超时
2 配置不同的okhttpClient
https://stackoverflow.com/questions/46845206/how-to-change-timeout-for-a-request-in-okhttp
```
class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return request.header("Custom-Timeout")?.let {
            val newTimeout = it.toInt()
            chain.withReadTimeout(newTimeout, TimeUnit.MILLISECONDS)
                .withConnectTimeout(newTimeout, TimeUnit.MILLISECONDS)
                .withWriteTimeout(newTimeout, TimeUnit.MILLISECONDS)
                .proceed(request)
        } ?: chain.proceed(request)
    }
}
```