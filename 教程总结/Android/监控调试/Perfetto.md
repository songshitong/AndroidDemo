

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

$ adb shell perfetto --config :test --out /data/misc/perfetto-traces/trace 
//使用内置的test配置，然后输出到/data/misc/perfetto-traces/trace

取出
adb pull /data/misc/perfetto-traces/trace  C:\Users\songshitong\Desktop
使用https://ui.perfetto.dev/#!/record 查看



自定义
ui.perfetto.devPerfetto----》Record new trace
选择好你想要的tracepoints之后，点击Start recording，就会生成如下命令内容，将这个命令内容拷贝出来直接在终端就可以执行，
完成之后就把/data/misc/perfetto-traces/trace 这个文件拷贝出来，用上面的ui.perfetto.dev分析就可以了

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