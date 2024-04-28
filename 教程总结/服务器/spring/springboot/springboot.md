
热部署插件jRebel


https://www.liaoxuefeng.com/wiki/1252599548343744/1266265175882464
Spring Boot是一个基于Spring的套件，它帮我们预组装了Spring的一系列组件，以便以尽可能少的代码和配置来开发基于Spring的Java应用程
spring boot配置文件使用yaml而不是xml


https://www.liaoxuefeng.com/wiki/1252599548343744/1282386318852129
Conditional编程
使用Profile能根据不同的Profile进行条件装配，但是Profile控制比较糙，如果想要精细控制，例如，配置本地存储，AWS存储和阿里云存储
，将来很可能会增加Azure存储等，用Profile就很难实现。

Spring本身提供了条件装配@Conditional，但是要自己编写比较复杂的Condition来做判断，比较麻烦。Spring Boot则为我们准备好了几个非常有用的条件：
@ConditionalOnProperty：如果有指定的配置，条件生效；
@ConditionalOnBean：如果有指定的Bean，条件生效；
@ConditionalOnMissingBean：如果没有指定的Bean，条件生效；
@ConditionalOnMissingClass：如果没有指定的Class，条件生效；
@ConditionalOnWebApplication：在Web环境中条件生效；
@ConditionalOnExpression：根据表达式判断条件是否生效。

实现示例：
```
@Component
@ConditionalOnProperty(value = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    ...
}


@Component
@ConditionalOnProperty(value = "storage.type", havingValue = "aws")
public class AwsStorageService implements StorageService {
    ...
}
```

加载配置文件
加载配置文件可以直接使用注解@Value，例如，我们定义了一个最大允许上传的文件大小配置：
```
storage:
  local:
    max-size: 102400
```
在某个FileUploader里，需要获取该配置，可使用@Value注入：
```
@Component
public class FileUploader {
    @Value("${storage.local.max-size:102400}")
    int maxSize;
}
```
Spring Boot允许创建一个Bean，持有一组配置，并由Spring Boot自动注入
保证Java Bean的属性名称与配置一致
```
//yaml配置
storage:
  local:
    # 文件存储根目录:
    root-dir: ${STORAGE_LOCAL_ROOT:/var/storage}
    # 最大文件大小，默认100K:
    max-size: ${STORAGE_LOCAL_MAX_SIZE:102400}
    # 是否允许空文件:
    allow-empty: false
    # 允许的文件类型:
    allow-types: jpg, png, gif

@Configuration
@ConfigurationProperties("storage.local")
public class StorageConfiguration {
    private String rootDir;
    private int maxSize;
    private boolean allowEmpty;
    private List<String> allowTypes;
}
```
使用
```
@Component
public class StorageService {
    @Autowired
    StorageConfiguration storageConfig;
}
```


添加Filter
Spring Boot会自动扫描所有的FilterRegistrationBean类型的Bean，然后，将它们返回的Filter自动注册到Servlet容器中，无需任何配置
```
@Component
public class ApiFilterRegistrationBean extends FilterRegistrationBean<Filter> {
    @PostConstruct
    public void init() {
        setOrder(20);
        setFilter(new ApiFilter());
        setUrlPatterns(List.of("/api/*"));
    }

    class ApiFilter implements Filter {
        ...
    }
}
```
FilterRegistrationBean本身不是Filter，它实际上是Filter的工厂。Spring Boot会调用getFilter()，把返回的Filter注册到Servlet容器中。
因为我们可以在FilterRegistrationBean中注入需要的资源，然后，在返回的AuthFilter中，这个内部类可以引用外部类的所有字段，自然也包括注入的UserService，
所以，整个过程完全基于Spring的IoC容器完成。

再注意到AuthFilterRegistrationBean使用了setOrder(10)，因为Spring Boot支持给多个Filter排序，数字小的在前面，所以，多个Filter的顺序是可以固定的。
调用setUrlPatterns()传入要过滤的URL列表

禁用自动配置
Spring Boot大量使用自动配置和默认配置，极大地减少了代码，通常只需要加上几个注解，并按照默认规则设定一下必要的配置即可
对于需要禁用自动配置的场景
1 可以通过@EnableAutoConfiguration(exclude = {...})指定禁用的自动配置；
```
@SpringBootApplication
// 启动自动配置，但排除指定的自动配置:
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class Application {
    ...
}
```
2 可以通过@Import({...})导入自定义配置
```
@SpringBootApplication
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@Import({ MasterDataSourceConfiguration.class, SlaveDataSourceConfiguration.class})
public class Application {
    ...
}
```
