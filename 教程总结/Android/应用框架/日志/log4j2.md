
https://blog.csdn.net/qq_43540406/article/details/112248912
对于log4j1.x的升级
log4j2参考了logback的一些优秀的设计，并且修复了一些问题，因此带来了一些重大的提升，主要有：
1 异常处理：在logback中，Appender中的异常不会被应用感知到，但是在log4j2中，提供了一些异常处理机制。
2 性能提升：log4j2相较于log4j 1和logback都具有很明显的性能提升。
3 自动重载配置：参考了logback的设计，提供自动刷新参数配置，可以动态的修改日志的级别而不需要重启应用。
4 无垃圾机制，log4j2在大部分情况下，都可以使用其设计的一套无垃圾机制，避免频繁的日志收集导致的jvm gc


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


org/apache/logging/log4j/Logger.java
logger接口，输出日志相关的

org/apache/logging/log4j/spi/LoggerContext.java
与logger对接