

1.进程开启
```
adb shell setprop persist.traced.enable 1
查看
adb logcat -s perfetto
```
adb shell perfetto
```
--config         -c      : /path/to/trace/config/file or - for stdin 必须的
```
-c和--time, --size, --buffer, --app, ATRACE_CAT, FTRACE_EVENT不能同时使用


$ adb shell perfetto --config :test --out /data/misc/perfetto-traces/trace 
//使用内置的test配置，然后输出到/data/misc/perfetto-traces/trace

adb shell perfetto --app com.appName -o /data/misc/perfetto-traces/trace -t 20s view sched freq idle am wm gfx view binder_driver hal dalvik camera input res memory

取出
adb pull /data/misc/perfetto-traces/trace  C:\Users\songshitong\Desktop
使用https://ui.perfetto.dev/#!/record 查看





自定义
ui.perfetto.devPerfetto----》Record new trace
选择好你想要的tracepoints之后，点击Start recording，就会生成如下命令内容，将这个命令内容拷贝出来直接在终端就可以执行，
完成之后就把/data/misc/perfetto-traces/trace 这个文件拷贝出来，用上面的ui.perfetto.dev分析就可以了

window要使用bash-based terminal   例如git bash的MinGW64
推荐命令
```
adb shell perfetto \
  -c - --txt \
  --config huawei \
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
duration_ms: 10000

EOF
```

config文件
创建config.pbtx文件
```
duration_ms: 10000

buffers: {
    size_kb: 8960
    fill_policy: DISCARD
}
buffers: {
    size_kb: 1280
    fill_policy: DISCARD
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "sched/sched_switch"
            ftrace_events: "power/suspend_resume"
            ftrace_events: "sched/sched_process_exit"
            ftrace_events: "sched/sched_process_free"
            ftrace_events: "task/task_newtask"
            ftrace_events: "task/task_rename"
            ftrace_events: "ftrace/print"
            atrace_categories: "gfx"
            atrace_categories: "view"
            atrace_categories: "webview"
            atrace_categories: "camera"
            atrace_categories: "dalvik"
            atrace_categories: "power"
        }
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
EOF
```
执行
cat config.pbtx | adb shell perfetto -c - --txt -o /data/misc/perfetto-traces/trace.perfetto-trace
或者
adb push config.pbtx /data/local/tmp/config.pbtx
adb shell 'cat /data/local/tmp/config.pbtx | perfetto --txt -c - -o /data/misc/perfetto-traces/trace.perfetto-trace'


python脚本
curl -O https://raw.githubusercontent.com/google/perfetto/master/tools/record_android_trace
python record_android_trace.py -c config.pbtx -o trace_file.perfetto-trace
