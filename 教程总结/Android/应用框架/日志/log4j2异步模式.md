

配置异步模式
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

