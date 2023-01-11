

启动抓取
edit configuration->profiler

抓取的类型
https://developer.android.com/studio/profile/record-traces
Sample Java Methods:   采样app的Java方法
代码Debug.startMethodTracingSampling   Debug.stopMethodTracing
Trace Java Methods:  每个APP的Java方法，生成的数据量大，持续时间短
Sample C/C++ Functions  采样c++方法  可以看到更多的framework调用
  内部使用simpleperf
   https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/README.md
Trace System Calls   系统调用 一般是Trace.begin生成的，一般看不到APP的方法
  内部使用systrace/perfetto