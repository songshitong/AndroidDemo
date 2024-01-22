
ijkplayer基本停止维护了，慎用

配置常用参数
https://blog.csdn.net/u011520181/article/details/129862964
```
// OPT_CATEGORY_PLAYER 类的参数设置

// 是否开启 Mediacodec 硬解，1 为开启，0 为关闭
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
// 关闭 Mediacodec 硬解，使用 FFmpeg 软解
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);

// 音频播放是否使用 openSL，1：使用 openSL，0：使用 AudioTrack
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);

// 当 CPU 处理不过来的时候的丢帧帧数，默认为 0，参数范围是 [-1, 120]，详情见：ff_ffplay_options.h
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);

// 在资源准备好后是否自动播放，1：自动播放，0：准备好后不自动播放
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);

// 使用 mediacodec 时是否根据 meta 信息自动旋转视频
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);

// 使用 mediacodec 时是否处理分辨率改变
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);

// 设置视频显示格式
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);

// 是否开启精准 seek，默认关闭
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)

// 是否启用 soundtouch，配合 setSpeed 实现变速播放
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1)
ijkMediaPlayer.setSpeed(0.5f);


// OPT_CATEGORY_CODEC 类的参数设置

// 是否开启跳过 loop filter，0 为开启，画面质量高，但解码开销大，48 为关闭，画面质量稍差，但解码开销小
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);


// OPT_CATEGORY_FORMAT 类的参数设置

// 设置探测输入的分析时长，单位：微秒，详情见：libavformat/options_table.h，通常设置 1 达到首屏秒开效果
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1);

// 在每个数据包之后启用 I/O 上下文的刷新
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);

```