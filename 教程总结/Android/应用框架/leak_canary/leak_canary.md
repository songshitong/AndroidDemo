初始化1.x
```
public class ExampleApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);
    // Normal app init code...
  }
}
```
2.x 不需要初始化代码，在ContentProvider中初始化



参考资料
https://zhuanlan.zhihu.com/p/346338268#:~:text=http%3A//hg.openjdk.java.net/jdk6/jdk6/jdk/raw-file/tip/src/share/demo/jvmti/hprof/manual.html%23mozTocId848088


leak_canary缺点
用于线下，dump堆栈信息耗时并且APP会冻结，生成的hprof文件大
查看Koom的优化  解决dump耗时，fork主进程，Linux采用copy-on-write来fork子线程
https://github.com/KwaiAppTeam/KOOM
https://mp.weixin.qq.com/s/WqYS_RQeE-PeDPPyOhvgsw
https://juejin.cn/post/6867335105322188813
