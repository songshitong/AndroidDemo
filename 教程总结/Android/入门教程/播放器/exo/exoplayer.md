
exoplayer后续整合到media3
https://developer.android.com/guide/topics/media/media3
https://developer.android.com/guide/topics/media/exoplayer

https://exoplayer.dev/hello-world.html 参考官网最好
https://www.jianshu.com/p/6e466e112877
ExoPlayer实现的通用组件是：
MediaSource：媒体资源，用于定义要播放的媒体，加载媒体，以及从哪里加载媒体。简单的说，MediaSource就是代表我们要播放的媒体文件，
   可以是本地资源，可以是网络资源。MediaSource在播放开始的时候，通过ExoPlayer.prepare方法注入。
Renderer：渲染器，用于渲染媒体文件。当创建播放器的时候，Renderers被注入。
TrackSelector：轨道选择器，用于选择MediaSource提供的轨道（tracks），供每个可用的渲染器使用。
LoadControl：用于控制MediaSource何时缓冲更多的媒体资源以及缓冲多少媒体资源。LoadControl在创建播放器的时候被注入。


ExoPlayer库提供了在普通使用场景下上述组件的默认实现。ExoPlayer可以使用这些默认的组件，也可以使用自定义组件。
   例如可以注入一个自定义的LoadControl用来改变播放器的缓存策略，或者可以注入一个自定义渲染器以使用Android本身不支持的视频解码器。


https://stackoverflow.com/questions/28590782/android-exoplayer-onprogresschanged
进度更新
```
handler = new Handler();
runnable = new Runnable() {
      @Override
      public void run() {
           progressbar.setProgress((int) ((exoPlayer.getCurrentPosition()*100)/exoPlayer.getDuration()));
           handler.postDelayed(runnable, 1000);
      }
};
handler.postDelayed(runnable, 0);
```

stop方法 版本com.google.android.exoplayer:exoplayer:2.18.0
会释放播放的资源，最好使用pause方法
seekto方法
拖动进度条seekto自动播放，pasue不好使
解决 拖动进度条播放状态seekto，停止状态记录进度，播放按钮时再seekto


获取当前播放url
```
player.currentMediaItem?.playbackProperties?.uri
```


exoplayer常用监听
```
 interface EventListener {

    //播放总时间线改变，这里可用于设置播放总时长
    default void onTimelineChanged(Timeline timeline, @TimelineChangeReason int reason) { 
    }

    //播放资源有改变
    default void onStaticMetadataChanged(List<Metadata> metadataList) {}

    
    //是否在加载
    default void onIsLoadingChanged(boolean isLoading) {
      onLoadingChanged(isLoading);
    }
 
    //播放器播放状态改变，查看 State 有IDLE，BUFFERING加载中， READY 资源准备好， ENDED 已结束 
    default void onPlaybackStateChanged(@State int state) {}

   //视频资源准备好就播放的设置改变    
    default void onPlayWhenReadyChanged(
        boolean playWhenReady, @PlayWhenReadyChangeReason int reason) {}
 
    //播放状态改变，开始播放或暂停
    default void onIsPlayingChanged(boolean isPlaying) {}

    //重复播放的模式改变 
    default void onRepeatModeChanged(@RepeatMode int repeatMode) {}
 
    default void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

    //播放器报错
    default void onPlayerError(ExoPlaybackException error) {}
 

 	//参数改变    
    default void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}
 
     
    default void onEvents(Player player, Events events) {}
  }

```
