


https://blog.csdn.net/Jason_996/article/details/78659019
Retrofit解决多个BaseURL切换的问题
1. retrofit添加拦截器,根据注解信息,处理多个baseurl
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
        List<String> urlnameList = originalRequest.headers("urlname");
        if (urlnameList != null && urlnameList.size() > 0) {
            //删除原有配置中的值,就是namesAndValues集合里的值
            builder.removeHeader("urlname");
            //获取头信息中配置的value,如：manage或者mdffx
            String urlname = urlnameList.get(0);
            HttpUrl baseURL=null;
            //根据头信息中配置的value,来匹配新的base_url地址
            if ("manage".equals(urlname)) {
                baseURL = HttpUrl.parse(Api.base_url);
            } else if ("mdffx".equals(urlname)) {
                baseURL = HttpUrl.parse(Api.base_url_mdffx);
            }
            //重建新的HttpUrl，需要重新设置的url部分
            HttpUrl newHttpUrl = oldUrl.newBuilder()
                    .scheme(baseURL.scheme())//http协议如：http或者https
                    .host(baseURL.host())//主机地址
                    .port(baseURL.port())//端口
                    .build();
            //获取处理后的新newRequest
            Request newRequest = builder.url(newHttpUrl).build();
            return  chain.proceed(newRequest);
        }else{
            return chain.proceed(originalRequest);
        }
 
    }
}   
```