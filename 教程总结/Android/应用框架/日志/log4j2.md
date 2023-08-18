
https://blog.csdn.net/qq_43540406/article/details/112248912
对于log4j1.x的升级
log4j2参考了logback的一些优秀的设计，并且修复了一些问题，因此带来了一些重大的提升，主要有：
1 异常处理：在logback中，Appender中的异常不会被应用感知到，但是在log4j2中，提供了一些异常处理机制。
2 性能提升：log4j2相较于log4j 1和logback都具有很明显的性能提升。
3 自动重载配置：参考了logback的设计，提供自动刷新参数配置，可以动态的修改日志的级别而不需要重启应用。
4 无垃圾机制，log4j2在大部分情况下，都可以使用其设计的一套无垃圾机制，避免频繁的日志收集导致的jvm gc
https://dorgenjones.github.io/2017/03/13/%E5%9F%BA%E7%A1%80%E5%B7%A5%E5%85%B7/log/3.log4j2%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90/

项目地址 https://github.com/apache/logging-log4j2
集成页面 https://logging.apache.org/log4j/2.x/maven-artifacts.html

用法：
```
private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String... args) {
        String thing = args.length > 0 ? args[0] : "world";
        LOGGER.info("Hello, {}!", thing);
        LOGGER.debug("Got calculated value only if debug enabled: {}", () -> doSomeCalculation());
    }

    private static Object doSomeCalculation() {
        // do some complicated calculation
    }
```

https://logging.apache.org/log4j/2.x/manual/architecture.html
常用组件

org/apache/logging/log4j/spi/LoggerContext.java
与logger对接
```
public interface LoggerContext {
   default ExtendedLogger getLogger(Class<?> cls) {
        final String canonicalName = cls.getCanonicalName();
        return getLogger(canonicalName != null ? canonicalName : cls.getName());
    }
}
```

org/apache/logging/log4j/Logger.java
logger接口，输出日志相关的
```
public interface Logger {
  void debug(Marker marker, Message message);
  void info(String message);
}
```

Configuration   org/apache/logging/log4j/core/config/Configuration.java
组件配置项  
```
public interface Configuration extends Filterable {
  LoggerConfig getLoggerConfig(String name)
  <T extends Appender> T getAppender(String name)
  Map<String, LoggerConfig> getLoggers();
}
```

LoggerConfig 日志的配置  org/apache/logging/log4j/core/config/LoggerConfig.java
```
public class LoggerConfig extends AbstractFilterable implements LocationAware {
   //创建logger
   public static LoggerConfig createLogger(final String additivity,
            // @formatter:off
            final Level level,
            @PluginAttribute("name") final String loggerName,
            final String includeLocation,
            final AppenderRef[] refs,
            final Property[] properties,
            @PluginConfiguration final Configuration config,
            final Filter filter) {
    }
}
```
Filter: 过滤器
org/apache/logging/log4j/core/Filter.java
```
    Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t);
```


Appender: 追加器，定义日志输出位置以及日志输出格式
org/apache/logging/log4j/core/Appender.java
```
public interface Appender extends LifeCycle {
 void append(LogEvent event);
  Layout<? extends Serializable> getLayout();
}
```
Layout: 定义日志的输出格式
org/apache/logging/log4j/core/Layout.java
```
public interface Layout<T extends Serializable> extends Encoder<LogEvent> {
    byte[] getFooter();
    byte[] getHeader();
   String getContentType();
}
```