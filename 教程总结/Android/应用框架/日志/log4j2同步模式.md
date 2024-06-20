
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
implementation 'org.apache.logging.log4j:log4j-api:2.20.0'
implementation 'org.apache.logging.log4j:log4j-core:2.20.0'

用法：
android初始化xml    部分取自https://blog.csdn.net/weixin_37525569/article/details/85123989
 模板参考https://juejin.cn/post/6904201366643441678
xml
```
<?xml version="1.0" encoding="UTF-8"?>
<configuration status="debug" >
    <properties> //配置properties，下面可以引用
      <!--配置日志打印在tcs-consumer-log文件夹里，LOG_HOME获取文件夹的当前路径-->
        <property name="LOG_HOME">${sys:user.dir}</property>
        <property name="FILE_FOLDER">tcs-consumer-log</property>
    </properties>
    <appenders>
        <!--这个输出控制台的配置-->
        <console name="Stdout" target="SYSTEM_OUT">
            <!--输出日志的格式-->
            <ThresholdFilter level="trace" onMatch="ACCEPT" onMismatch="NEUTRAL"/>
            <PatternLayout pattern="%d  [%t] %-5level: %msg%n%throwable"/>
        </console>
        //<rollingPolicy> 标签用来指定滚动策略， 所谓的滚动策略其实就是对日志文件进行归档
        <RollingFile name="rolling" fileName="mnt/sdcard/rolling.log"
            filePattern="mnt/sdcard/rolling-%d{yyyy-MM-dd}-%i.txt" >
            <MarkerFilter marker="NAME" onMatch="ACCEPT" onMismatch="DENY"/>
            <PatternLayout pattern="[%d{yyyy-MM-dd HH:mm:ss}] [%c{1}-%p %l] %m%n"/>
            <Policies>
                <!-- 基于指定文件大小的滚动策略,设置日志文件满1MB后打包,,size属性用来定义每个日志文件的大小 -->
                <SizeBasedTriggeringPolicy size="1M"/>
            </Policies>
            <!-- DefaultRolloverStrategy属性如不设置，默认最多7个文件，这里设置20 -->
            <DefaultRolloverStrategy max="20"/>
        </RollingFile>

    </appenders>


    <loggers>
        <!--记录执行的HQL语句 -->
        <!-- additivity="false"是为了避免日志在root里再次输出 -->
        <Logger name="TestLogger" level="debugr" additivity="false">
            <AppenderRef ref="rolling" />
        </Logger>
        <root level="debug"> 
            <AppenderRef ref="rolling"/>
        </root>
    </loggers>
</configuration>
```
```
ExtendedLogger logger=null;
InputStream open = null;
try {
    open = getAssets().open("log4j2.xml"); //根据配置xml进行初始化
    ConfigurationSource source = new ConfigurationSource(open);
    LoggerContext initialize = Configurator.initialize(null, source);
    logger =  initialize.getLogger(Main.class);
} catch (IOException e) {
    e.printStackTrace();
    try {
        open.close();
    } catch (IOException ioException) {
        ioException.printStackTrace();
    }
}

logger.info("info");
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


org/apache/logging/log4j/LogManager.java
```
   public static Logger getLogger() {
        return getLogger(StackLocatorUtil.getCallerClass(2));
    }
 
     public static Logger getLogger(final Class<?> clazz) {
        final Class<?> cls = callerClass(clazz);
        //通过LoggerContext拿到logger
        return getContext(cls.getClassLoader(), false).getLogger(cls);
    }
    
   
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext) {
        try {
            //默认为SimpleLoggerContextFactory，可以指定
            return factory.getContext(FQCN, loader, null, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, loader, null, currentContext);
        }
    }    
```

org/apache/logging/log4j/simple/SimpleLoggerContextFactory.java
```
public class SimpleLoggerContextFactory implements LoggerContextFactory {
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext) {
        return SimpleLoggerContext.INSTANCE;
    }
 }
```
org/apache/logging/log4j/simple/SimpleLoggerContext.java
```

 public SimpleLoggerContext() {
        //解析props   
        //SYSTEM_PREFIX = "org.apache.logging.log4j.simplelog.";
        props = new PropertiesUtil("log4j2.simplelog.properties");
        showContextMap = props.getBooleanProperty(SYSTEM_PREFIX + "showContextMap", false);
        showLogName = props.getBooleanProperty(SYSTEM_PREFIX + "showlogname", false);
        showShortName = props.getBooleanProperty(SYSTEM_PREFIX + "showShortLogname", true);
        showDateTime = props.getBooleanProperty(SYSTEM_PREFIX + "showdatetime", false);
        final String lvl = props.getStringProperty(SYSTEM_PREFIX + "level");
        defaultLevel = Level.toLevel(lvl, Level.ERROR);
        dateTimeFormat = showDateTime ? props.getStringProperty(SimpleLoggerContext.SYSTEM_PREFIX + "dateTimeFormat",
                DEFAULT_DATE_TIME_FORMAT) : null;
        final String fileName = props.getStringProperty(SYSTEM_PREFIX + "logFile", SYSTEM_ERR);
        ...
    }


 public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        //获取缓存的ExtendedLogger
        final ExtendedLogger extendedLogger = loggerRegistry.getLogger(name, messageFactory);
        if (extendedLogger != null) {
            AbstractLogger.checkMessageFactory(extendedLogger, messageFactory);
            return extendedLogger;
        }
        //获取不到，创建SimpleLogger 然后添加到loggerRegistry
        final SimpleLogger simpleLogger = new SimpleLogger(name, defaultLevel, showLogName, showShortName, showDateTime,
                showContextMap, dateTimeFormat, messageFactory, props, stream);
        loggerRegistry.putIfAbsent(name, messageFactory, simpleLogger);
        return loggerRegistry.getLogger(name, messageFactory);
    }
```



触发日志事件
logger.info("xxx");
查看SimpleLogger的父类AbstractLogger
org/apache/logging/log4j/spi/AbstractLogger.java
```
    public void info(final String message) {
        logIfEnabled(FQCN, Level.INFO, null, message, (Throwable) null);
    }
    
 
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {//判断日志是否可以打印，一般根据level判断
            logMessage(fqcn, level, marker, message, throwable);
        }
    } 
    
 
     protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable throwable) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), throwable);
    }
    
   //todo 编译为30字节 没有触发  35 byte MaxInlineSize的阈值
      @PerformanceSensitive 
    // NOTE: This is a hot method. Current implementation compiles to 30 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void logMessageSafely(final String fqcn, final Level level, final Marker marker, final Message message,
            final Throwable throwable) {
        try {
            logMessageTrackRecursion(fqcn, level, marker, message, throwable);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            ReusableMessageFactory.release(message);
        }
    }  
    
   
    private void logMessageTrackRecursion(final String fqcn,
                                          final Level level,
                                          final Marker marker,
                                          final Message message,
                                          final Throwable throwable) {
        try { //记录层级
            incrementRecursionDepth(); // LOG4J2-1518, LOG4J2-2031
            tryLogMessage(fqcn, getLocation(fqcn), level, marker, message, throwable);
        } finally {
            decrementRecursionDepth();
        }
    } 
    
  
      @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 26 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void tryLogMessage(final String fqcn,
                               final StackTraceElement location,
                               final Level level,
                               final Marker marker,
                               final Message message,
                               final Throwable throwable) {
        try {
            log(level, marker, fqcn, location, message, throwable);
        } catch (final Throwable t) {
            // LOG4J2-1990 Log4j2 suppresses all exceptions that occur once application called the logger
            handleLogMessageException(t, fqcn, message);
        }
    }
    
     protected void log(final Level level, final Marker marker, final String fqcn, final StackTraceElement location,
        final Message message, final Throwable throwable) {
        logMessage(fqcn, level, marker, message, throwable);
    }          
```


org/apache/logging/log4j/simple/SimpleLogger.java
```
  @Override
    public void logMessage(final String fqcn, final Level mgsLevel, final Marker marker, final Message msg,
            final Throwable throwable) {
        final StringBuilder sb = new StringBuilder();
        //拼接额外参数
        if (showDateTime) {
            final Date now = new Date();
            String dateText;
            synchronized (dateFormatter) {
                dateText = dateFormatter.format(now);
            }
            sb.append(dateText);
            sb.append(SPACE);
        }

        sb.append(mgsLevel.toString());
        sb.append(SPACE);
         。。。
        final Object[] params = msg.getParameters();
        Throwable t;
        if (throwable == null && params != null && params.length > 0
                && params[params.length - 1] instanceof Throwable) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        //打印
        stream.println(sb.toString());
        if (t != null) {
            stream.print(SPACE);
            t.printStackTrace(stream);
        }
    }
```