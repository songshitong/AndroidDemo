

配置异步模式
0
添加jvm参数-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
1
只需要将系统属性Log4jContextSelector设置为AsyncLoggerContextSelector
在配置属性文件log4j2.component.properties中配置Log4jContextSelector为AsyncLoggerContextSelector
2
```
<logers>
    <AsyncLogger name="experiment.test" level="trace" includeLocation="true">
      <AppenderRef ref="warnLog"/>
    </AsyncLogger>
</logers>
```

https://juejin.cn/post/7199620944339337274


异步模式使用了RingBuffer
https://zhuanlan.zhihu.com/p/229338771
https://github.com/LMAX-Exchange/disruptor



org\apache\logging\log4j\core\async\AsyncLoggerContextSelector.class
```
  public static boolean isSelected() {
  //根据Properties判断是否选定
    return AsyncLoggerContextSelector.class.getName().equals(PropertiesUtil.getProperties().getStringProperty("Log4jContextSelector"));
  }
  //创建LoggerContext
  protected LoggerContext createContext(final String name, final URI configLocation) {
    return new AsyncLoggerContext(name, (Object)null, configLocation);
  }
```
org\apache\logging\log4j\core\async\AsyncLoggerContext.class
```
public class AsyncLoggerContext extends LoggerContext {
  private final AsyncLoggerDisruptor loggerDisruptor;

  public AsyncLoggerContext(final String name) {
    super(name);
    //创建AsyncLoggerDisruptor
    this.loggerDisruptor = new AsyncLoggerDisruptor(name, () -> {
      return this.getConfiguration().getAsyncWaitStrategyFactory();
    });
  }
  
    protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
    //创建logger
    return new AsyncLogger(ctx, name, messageFactory, this.loggerDisruptor);
  }
}
```
父类的处理
org\apache\logging\log4j\core\LoggerContext.class
```
  public Logger getLogger(final String name, final MessageFactory messageFactory) {
    Logger logger = (Logger)this.loggerRegistry.getLogger(name, messageFactory);
    if (logger != null) {
      AbstractLogger.checkMessageFactory(logger, messageFactory);
      return logger;
    } else {
      logger = this.newInstance(this, name, messageFactory);
      this.loggerRegistry.putIfAbsent(name, messageFactory, logger);
      return (Logger)this.loggerRegistry.getLogger(name, messageFactory);
    }
  }
  
  protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
    return new Logger(ctx, name, messageFactory);
  }  
```


//分发器   使用Disruptor和RingBuffer
org\apache\logging\log4j\core\async\AsyncLoggerDisruptor.class
```
  public synchronized void start() {
    if (this.disruptor != null) {
    } else if (this.isStarting()) {
    } else {
      this.setStarting();
      this.ringBufferSize = DisruptorUtil.calculateRingBufferSize("AsyncLogger.RingBufferSize");
      AsyncWaitStrategyFactory factory = (AsyncWaitStrategyFactory)this.waitStrategyFactorySupplier.get();
      this.waitStrategy = DisruptorUtil.createWaitStrategy("AsyncLogger.WaitStrategy", factory);
      ThreadFactory threadFactory = new Log4jThreadFactory("AsyncLogger[" + this.contextName + "]", true, 5) {
        public Thread newThread(final Runnable r) {
          Thread result = super.newThread(r);
          AsyncLoggerDisruptor.this.backgroundThreadId = result.getId();
          return result;
        }
      };
      this.asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();
      //创建Disruptor
      this.disruptor = new Disruptor(RingBufferLogEvent.FACTORY, this.ringBufferSize, threadFactory, ProducerType.MULTI, this.waitStrategy);
      ExceptionHandler<RingBufferLogEvent> errorHandler = DisruptorUtil.getAsyncLoggerExceptionHandler();
      this.disruptor.setDefaultExceptionHandler(errorHandler);
      RingBufferLogEventHandler[] handlers = new RingBufferLogEventHandler[]{new RingBufferLogEventHandler()};
      this.disruptor.handleEventsWith(handlers);
      this.disruptor.start();
      super.start();
    }
  }
  
    //发布事件
    boolean tryPublish(final RingBufferLogEventTranslator translator) {
      try {
      return this.disruptor.getRingBuffer().tryPublishEvent(translator);
     } catch (NullPointerException var3) {
      this.logWarningOnNpeFromDisruptorPublish(translator);
      return false;
     }
    }
    
    //发布事件时是否需要加锁
    void enqueueLogMessageWhenQueueFull(final RingBufferLogEventTranslator translator) {
      try {
        if (this.synchronizeEnqueueWhenQueueFull()) {
          synchronized(this.queueFullEnqueueLock) {
          this.disruptor.publishEvent(translator);
        }
      } else {
        this.disruptor.publishEvent(translator);
      }
      } catch (NullPointerException var5) {
        this.logWarningOnNpeFromDisruptorPublish(translator);
      }
   } 
```

日志打印
org\apache\logging\log4j\core\async\AsyncLogger.class
```
  public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
    this.getTranslatorType().log(fqcn, level, marker, message, thrown);
  }
  public void log(final Level level, final Marker marker, final String fqcn, final StackTraceElement location, final Message message, final Throwable throwable) {
    this.getTranslatorType().log(fqcn, location, level, marker, message, throwable);
  }
  
  //是否使用线程本地缓存
    private TranslatorType getTranslatorType() {
    return this.loggerDisruptor.isUseThreadLocals() ? this.threadLocalTranslatorType : this.varargTranslatorType;
  }
  
  //使用线程本地缓存打印日志
   private final TranslatorType threadLocalTranslatorType = new TranslatorType() {
    void log(String fqcn, StackTraceElement location, Level level, Marker marker, Message message, Throwable thrown) {
      AsyncLogger.this.logWithThreadLocalTranslator(fqcn, location, level, marker, message, thrown);
    }
   ...
  };
  
  //不使用线程本地缓存
  private final TranslatorType varargTranslatorType = new TranslatorType() {
    void log(String fqcn, StackTraceElement location, Level level, Marker marker, Message message, Throwable thrown) {
      AsyncLogger.this.logWithVarargTranslator(fqcn, location, level, marker, message, thrown);
    }
    ...
  };
  
  
  //直接发布  生成大量临时缓存
   private void logWithVarargTranslator(final String fqcn, final StackTraceElement location, final Level level, final Marker marker, final Message message, final Throwable thrown) {
    Disruptor<RingBufferLogEvent> disruptor = this.loggerDisruptor.getDisruptor();
    if (disruptor == null) {
      LOGGER.error("Ignoring log event after Log4j has been shut down.");
    } else {
      if (!this.isReused(message)) { //message不复用时处理
        InternalAsyncUtil.makeMessageImmutable(message);
      }

      if (!disruptor.getRingBuffer().tryPublishEvent(this, new Object[]{this, location, fqcn, level, marker, message, thrown})) {
        this.handleRingBufferFull(location, fqcn, level, marker, message, thrown);
      }

    }
  }
  
  //使用缓存对象发布
  //获取threadLocal的RingBufferLogEventTranslator
  private RingBufferLogEventTranslator getCachedTranslator() {
    RingBufferLogEventTranslator result = (RingBufferLogEventTranslator)this.threadLocalTranslator.get();
    if (result == null) {
      result = new RingBufferLogEventTranslator();
      this.threadLocalTranslator.set(result);
    }
    return result;
  }
  
  private void logWithThreadLocalTranslator(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
    RingBufferLogEventTranslator translator = this.getCachedTranslator();
    //重新初始化   translator用于将数据转换为disruptor需要的event
    this.initTranslator(translator, fqcn, level, marker, message, thrown);
    this.initTranslatorThreadValues(translator); //更新线程相关信息
    //发布事件
    this.publish(translator);
  }
```

接收到事件的处理
org/apache/logging/log4j/core/async/AsyncLoggerDisruptor.java
```
 final RingBufferLogEventHandler[] handlers = {new RingBufferLogEventHandler()};
        disruptor.handleEventsWith(handlers);
```
org/apache/logging/log4j/core/async/RingBufferLogEventHandler.java
```
  public void onEvent(final RingBufferLogEvent event, final long sequence,
            final boolean endOfBatch) throws Exception {
        try {
           ...
            if (event.isPopulated()) {
                event.execute(endOfBatch);
            }
        }
        ...
    }
```
org/apache/logging/log4j/core/async/RingBufferLogEvent.java
```
    public void execute(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
        asyncLogger.actualAsyncLog(this);
    }
```
org/apache/logging/log4j/core/async/AsyncLogger.java
```
    public void actualAsyncLog(final RingBufferLogEvent event) {
        final LoggerConfig privateConfigLoggerConfig = privateConfig.loggerConfig;
        final List<Property> properties = privateConfigLoggerConfig.getPropertyList();
        if (properties != null) {
            onPropertiesPresent(event, properties);
        }
        privateConfigLoggerConfig.getReliabilityStrategy().log(this, event);
    }
```
ReliabilityStrategy的默认实现
org/apache/logging/log4j/core/config/DefaultReliabilityStrategy.java
```
    public DefaultReliabilityStrategy(final LoggerConfig loggerConfig) {
        this.loggerConfig = Objects.requireNonNull(loggerConfig, "loggerConfig is null");
    }
    
      @Override
    public void log(final Supplier<LoggerConfig> reconfigured, final LogEvent event) {
        loggerConfig.log(event); //由loggerConfig实现
    }  
```
org/apache/logging/log4j/core/config/LoggerConfig.java
```
 public LoggerConfig() {
        this.logEventFactory = LOG_EVENT_FACTORY;
        this.level = Level.ERROR;
        this.name = Strings.EMPTY;
        this.properties = null;
        this.propertiesRequireLookup = false;
        this.config = null;
        //默认传入的logerConfig
        this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
    }
    

 public void log(final LogEvent event) {
        log(event, LoggerConfigPredicate.ALL);
    }

    protected void log(final LogEvent event, final LoggerConfigPredicate predicate) {
        if (!isFiltered(event)) {
            processLogEvent(event, predicate);
        }
    }    
```
子类的实现
org/apache/logging/log4j/core/async/AsyncLoggerConfig.java
```
    protected void log(final LogEvent event, final LoggerConfigPredicate predicate) {
        if (predicate == LoggerConfigPredicate.ALL &&
                ASYNC_LOGGER_ENTERED.get() == Boolean.FALSE &&
                hasAppenders()) {
            // This is the first AsnycLoggerConfig encountered by this LogEvent
            ASYNC_LOGGER_ENTERED.set(Boolean.TRUE);
            try {
                super.log(event, LoggerConfigPredicate.SYNCHRONOUS_ONLY);          
                logToAsyncDelegate(event);
            } finally {
                ASYNC_LOGGER_ENTERED.set(Boolean.FALSE);
            }
        } else {
            super.log(event, predicate);
        }
    }
    
 
   private void logToAsyncDelegate(final LogEvent event) {
       //是否过滤
        if (!isFiltered(event)) {
            // Passes on the event to a separate thread that will call
            // asyncCallAppenders(LogEvent).
            populateLazilyInitializedFields(event);
            if (!delegate.tryEnqueue(event, this)) {
                handleQueueFull(event);
            }
        }
    }
   
```
org/apache/logging/log4j/core/async/AsyncLoggerConfigDisruptor.java
```
    public boolean tryEnqueue(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
        final LogEvent logEvent = prepareEvent(event);
        return disruptor.getRingBuffer().tryPublishEvent(translator, logEvent, asyncLoggerConfig);
    }


disruptor的创建
  @Override
    public synchronized void start() {
        ...
        ringBufferSize = DisruptorUtil.calculateRingBufferSize("AsyncLoggerConfig.RingBufferSize");
        waitStrategy = DisruptorUtil.createWaitStrategy(
                "AsyncLoggerConfig.WaitStrategy", asyncWaitStrategyFactory);

        final ThreadFactory threadFactory = new Log4jThreadFactory("AsyncLoggerConfig", true, Thread.NORM_PRIORITY) {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread result = super.newThread(r);
                backgroundThreadId = result.getId();
                return result;
            }
        };
        asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();

        translator = mutable ? MUTABLE_TRANSLATOR : TRANSLATOR;
        factory = mutable ? MUTABLE_FACTORY : FACTORY;
        disruptor = new Disruptor<>(factory, ringBufferSize, threadFactory, ProducerType.MULTI, waitStrategy);

        final ExceptionHandler<Log4jEventWrapper> errorHandler = DisruptorUtil.getAsyncLoggerConfigExceptionHandler();
        disruptor.setDefaultExceptionHandler(errorHandler);

        final Log4jEventWrapperHandler[] handlers = {new Log4jEventWrapperHandler()};
        disruptor.handleEventsWith(handlers);

        ....
        disruptor.start();
        super.start();
    }
    
 private static class Log4jEventWrapperHandler implements SequenceReportingEventHandler<Log4jEventWrapper> {
        @Override
        public void onEvent(final Log4jEventWrapper event, final long sequence, final boolean endOfBatch)
                throws Exception {
            event.event.setEndOfBatch(endOfBatch);
            event.loggerConfig.logToAsyncLoggerConfigsOnCurrentThread(event.event);
            event.clear();
            notifyIntermediateProgress(sequence);
        }
}    
```
org/apache/logging/log4j/core/async/AsyncLoggerConfig.java
```
    void logToAsyncLoggerConfigsOnCurrentThread(final LogEvent event) {
        log(event, LoggerConfigPredicate.ASYNCHRONOUS_ONLY);
    }
    
 protected void log(final LogEvent event, final LoggerConfigPredicate predicate) {
        // See LOG4J2-2301
        if (predicate == LoggerConfigPredicate.ALL &&
              ...
            try {
              ...
            } finally {
                ASYNC_LOGGER_ENTERED.set(Boolean.FALSE);
            }
        } else {
            super.log(event, predicate);
        }
    }    
```
org/apache/logging/log4j/core/config/LoggerConfig.java
```
  protected void log(final LogEvent event, final LoggerConfigPredicate predicate) {
        if (!isFiltered(event)) {
            processLogEvent(event, predicate);
        }
    }
    
     private void processLogEvent(final LogEvent event, final LoggerConfigPredicate predicate) {
        event.setIncludeLocation(isIncludeLocation());
        if (predicate.allow(this)) {
            callAppenders(event);
        }
        logParent(event, predicate);
    }
 
 protected void callAppenders(final LogEvent event) {
        final AppenderControl[] controls = appenders.get();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < controls.length; i++) {
            controls[i].callAppender(event);
        }
    }      
```
org/apache/logging/log4j/core/config/AppenderControl.java
```
    public void callAppender(final LogEvent event) {
        if (shouldSkip(event)) {
            return;
        }
        callAppenderPreventRecursion(event);
    }
    private void callAppenderPreventRecursion(final LogEvent event) {
        try {
            recursive.set(this);
            callAppender0(event);
        } finally {
            recursive.set(null);
        }
    }
    private void callAppender0(final LogEvent event) {
        ensureAppenderStarted();
        if (!isFilteredByAppender(event)) {
            tryCallAppender(event);
        }
    }
 
 private void tryCallAppender(final LogEvent event) {
        try {
            appender.append(event);
        } catch (final RuntimeException error) {
            handleAppenderError(event, error);
        } catch (final Throwable throwable) {
            handleAppenderError(event, new AppenderLoggingException(throwable));
        }
    }       
```
以ConsoleAppender为例，看看appender的输出
org/apache/logging/log4j/core/appender/ConsoleAppender.java

父类
org/apache/logging/log4j/core/appender/AbstractOutputStreamAppender.java
```
 public void append(final LogEvent event) {
        try {
            tryAppend(event);
       ...
    }

    private void tryAppend(final LogEvent event) {
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            directEncodeEvent(event);
        } else {
            writeByteArrayToManager(event);
        }
    }

    protected void directEncodeEvent(final LogEvent event) {
        //进行格式化布局
        getLayout().encode(event, manager);
        if (this.immediateFlush || event.isEndOfBatch()) {
            manager.flush();
        }
    }
```
org/apache/logging/log4j/core/layout/AbstractLayout.java
```
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final byte[] data = toByteArray(event); //将event写入ByteBufferDestination 也就是manager
        destination.writeBytes(data, 0, data.length);
    }
```

org/apache/logging/log4j/core/appender/ConsoleAppender.java
使用OutputStreamManager
```
 public static ConsoleAppender createAppender(Layout<? extends Serializable> layout,
            final Filter filter,
            final String targetStr,
            final String name,
            final String follow,
            final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        final boolean isFollow = Boolean.parseBoolean(follow);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final Target target = targetStr == null ? DEFAULT_TARGET : Target.valueOf(targetStr);
        return new ConsoleAppender(name, layout, filter, getManager(target, isFollow, false, layout), ignoreExceptions, target, null);
    }
 
     private static OutputStreamManager getManager(final Target target, final boolean follow, final boolean direct,
            final Layout<? extends Serializable> layout) {
        final OutputStream os = getOutputStream(follow, direct, target);
        final String managerName = target.name() + '.' + follow + '.' + direct;
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }  

 private static OutputStream getOutputStream(final boolean follow, final boolean direct, final Target target) {
        final String enc = Charset.defaultCharset().name();
        OutputStream outputStream;
        try {
            // @formatter:off
            outputStream = target == Target.SYSTEM_OUT ?
                direct ? new FileOutputStream(FileDescriptor.out) :
                    (follow ? new PrintStream(new SystemOutStream(), true, enc) : System.out) :
                direct ? new FileOutputStream(FileDescriptor.err) :
                    (follow ? new PrintStream(new SystemErrStream(), true, enc) : System.err);
            // @formatter:on
            outputStream = new CloseShieldOutputStream(outputStream);
        } catch (final UnsupportedEncodingException ex) { // should never happen
            throw new IllegalStateException("Unsupported default encoding " + enc, ex);
        }
       .....
        return outputStream;
    }     
```
以SystemOutStream为例
org/apache/logging/log4j/core/appender/ConsoleAppender.java
```
 private static class SystemOutStream extends OutputStream {
......
        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }
...
```