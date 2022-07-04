
https://mp.weixin.qq.com/s/zhWQtsGQkewPrBYWJJ96pw
开始录制不使用电脑
1.打开设置页面
```
adb shell am start com.android.traceur/com.android.traceur.MainActivity
```
2.打开Settings->System->Developer options->System Tracing

开始录制
点击Record trace按钮
结束
下拉状态栏，点击关闭trace，等待trace文件保存
导出文件
adb pull data/local/traces

也可以在网页https://ui.perfetto.dev/#!/viewer 进行录制

相关设置
```
Per-CPU buffer size
每一个CPU的buffer大小，一般情况下不用改大 ，如果发现Trace有丢失
或者在userdebug手机上抓取trace，最好增大一下，避免trace的丢失。

Clear saved traces
清除手机里已经存储的trace文件

Long traces
一般我们抓的trace，都只会记录开启Record trace之后30s的trace。
对于需要长时间跟踪的，可以开启这个选项
并且配置下方的Maximum long trace size和Maximum long trace duration
```


https://mp.weixin.qq.com/s/sood846ARS2CUI3kqMqMDg
命令录制
Recording settings
首先是选择录制的设置，主要有三种模式，每种模式的作用不一样。

2.1 Stop when full
2.1.1 In-memory buffer size
代表每一个核上可以存储的最大的trace的buffer，在内存中，不会影响IO。

2.1.2 Max duration
设置此次trace抓取的最大时长，图中就是10s

小结
Stop when full模式下，perfetto停止工作受Max duration和buffer size影响，一旦满足其中一个条件，perfetto将会停止。
优点：trace不会因为overwrite而导致丢失。
缺点：如果trace太多，会导致提前结束，无法录制到出现问题时候的trace。
一般10s，64mb也就够用了


2.2 Ring buffer
Ring buffer模式只会收到Max duration的影响，时间到了就停止抓取trace，但是trace会有被overwrite的风险。

2.3 Long trace
用于长时间地抓取trace，但是由于需要定时将buffer中的trace写到文件里面去，会有IO的影响。

2.3.1 Max file size
代表生成long trace的最大文件大小，在使用perfetto，你需要评估一下可能生成的文件的大小，如果在你设置的Max duration期间，Max file size超了，会有异常bug产生，perfetto会停不下来来了。

2.3.2 Flush on disk every
间隔多少时间将buffer中的trace写入到文件中。这个数值不能太大也不能太小。太大了，容易丢trace，太小了容易影响IO。

小结
Long trace主要是受Max duration，或者ctrl + c暂停命令。
我个人最推荐的配置：
64MB，30m，10GB，2500ms。

当然对于long trace，一般是用于用户去复现问题，我更推荐用[061]perfetto使用简介的离线抓取方式。



Recording command
这个界面就是生成指令的地方，点击右上角的复制按钮，在PC连接手机的情况下，运行这个指令就可以抓取trace了。
下面各个选项的打开关闭，都会更新这个Recording command。

四、CPU
CPU选项是经常使用的，基本上除了syscalls不打开，前面三个都会常规打开。

4.1 Coarse CPU usage countor
我一般都会开，但是目前没有发现在perfetto的文件打开之后主要对应那块的数据。

4.2 Scheduling details
可以看到每个cpu上运行的task

4.3 CPU frequency and idle states
可以看到每个cpu的运行频率

4.4 Syscalls
可以记录每一个系统调用，但是我一般不开，因为感觉影响性能比较大。


五、GPU
可以记录GPU的主频和GPU的内存

5.1 GPU frequency
可以看到GPU的频率，繁忙程度

5.2 GPU memory
可以看到目前只能在Android12+以上的使用

六、Power
我基本就不开了，对处理功耗的问题的朋友有用。可以尝试开一下，看看什么效果。

七、Memory
有关内存的那么多选项，大家可以按需选择开启关闭，英文也比较简单的。
我一般不开，当怀疑是内存导致的性能问题，我会选择开启。

八、Android APP & svcs
这是最重要的选项，性能优化肯定要用到的

8.1 Atrace userspace annotations
开启这个选项之后，选择合适的atrace tag就可以开启对应的trace了，这个tag就对应了System Tracing的界面的catergray。

8.2 Event log（logcat）
这是perfetto一个很牛逼的功能，可以实时记录log，然后将log和trace信息一一对应。
非常有利于分析问题。选择合适的log类型，就可以记录log了。

生成的perfetto文件，滑动下方的android log，可以看到有一根竖线，对应到trace的tag，日志和trace tag的一一对应，
是不是很牛逼的功能。

8.3 Frame timeline
这个是Android 12（S）的新功能，具体如何使用可以参考这个网址的官方视频
https://www.youtube.com/playlist?list=PLWz5rJ2EKKc-xjSI-rWn9SViXivBhQUnp
可以看到SF某一帧是合成的APP的哪一帧，已经合成的状态，具体作用，大家可以参考上面的链接视频


九、Chrome
这个选项，主要是分析webview相关的性能问题，我也用的不多，大家如果遇到需要分析webview相关的性能问题，可以尝试开启这些功能。


十、Advanced settings
目前就一个功能，开启ftrace，对于需要分析内核性能问题，可以开启这个，选择对应的tag。


贴一个我常用的perfetto的指令
下面的命令不能在Android 9上工作，因为 --txt 选项是在Android 10中引入的，
```
adb shell perfetto \
  -c - --txt \
  -o /data/misc/perfetto-traces/trace \
<<EOF

buffers: {
    size_kb: 63488
    fill_policy: DISCARD
}
buffers: {
    size_kb: 2048
    fill_policy: DISCARD
}
data_sources: {
    config {
        name: "android.gpu.memory"
    }
}
data_sources: {
    config {
        name: "linux.process_stats"
        target_buffer: 1
        process_stats_config {
            scan_all_processes_on_start: true
        }
    }
}
data_sources: {
    config {
        name: "android.log"
        android_log_config {
            log_ids: LID_EVENTS
            log_ids: LID_CRASH
            log_ids: LID_KERNEL
            log_ids: LID_DEFAULT
            log_ids: LID_RADIO
            log_ids: LID_SECURITY
            log_ids: LID_STATS
            log_ids: LID_SYSTEM
        }
    }
}
data_sources: {
    config {
        name: "android.surfaceflinger.frametimeline"
    }
}
data_sources: {
    config {
        name: "linux.sys_stats"
        sys_stats_config {
            stat_period_ms: 1000
            stat_counters: STAT_CPU_TIMES
            stat_counters: STAT_FORK_COUNT
        }
    }
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "sched/sched_switch"
            ftrace_events: "power/suspend_resume"
            ftrace_events: "sched/sched_wakeup"
            ftrace_events: "sched/sched_wakeup_new"
            ftrace_events: "sched/sched_waking"
            ftrace_events: "power/cpu_frequency"
            ftrace_events: "power/cpu_idle"
            ftrace_events: "power/gpu_frequency"
            ftrace_events: "gpu_mem/gpu_mem_total"
            ftrace_events: "sched/sched_process_exit"
            ftrace_events: "sched/sched_process_free"
            ftrace_events: "task/task_newtask"
            ftrace_events: "task/task_rename"
            ftrace_events: "ftrace/print"
            atrace_categories: "am"
            atrace_categories: "adb"
            atrace_categories: "aidl"
            atrace_categories: "dalvik"
            atrace_categories: "audio"
            atrace_categories: "binder_lock"
            atrace_categories: "binder_driver"
            atrace_categories: "bionic"
            atrace_categories: "camera"
            atrace_categories: "database"
            atrace_categories: "gfx"
            atrace_categories: "hal"
            atrace_categories: "input"
            atrace_categories: "network"
            atrace_categories: "nnapi"
            atrace_categories: "pm"
            atrace_categories: "power"
            atrace_categories: "rs"
            atrace_categories: "res"
            atrace_categories: "rro"
            atrace_categories: "sm"
            atrace_categories: "ss"
            atrace_categories: "vibrator"
            atrace_categories: "video"
            atrace_categories: "view"
            atrace_categories: "webview"
            atrace_categories: "wm"
        }
    }
}
duration_ms: 10000

EOF
```

switch legacy ui 切换到systrace类似的页面


快捷键
w/s 放大缩小   a/d 左右移动
选择区域点击后拖动   选中后可以在底部查看详细信息
m 临时标记区域
ctrl+b 是否展示侧边栏
置顶功能 选择某个线程，旁边有五角星，选中后可以置顶