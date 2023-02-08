exo version com.google.android.exoplayer:exoplayer:2.18.1

https://blog.csdn.net/tst116/article/details/118101947
主视频播放控件StyledPlayerView
控制控件PlayerControlView
主视频覆盖view，exo_overlay


控件PlayerView与PlayerControlView都提供的是布局layout id设置方式来让用户自定义界面。
在控件中解析layout id中特定id的控件来达到用户自定义布局中的控件与视频播放ExoPlayer关联
app:player_layout_id="@layout/live_playback_custom_player_view_layout"

自定义布局中  自定义布局必须有原始布局的viewId
id=exo_overlay为视频之上可显示的控件，主要显示自定义的播放视频的各种状态显示。
id=exo_controller为用户控制播放器的控件id


com/google/android/exoplayer2/ui/StyledPlayerView.java
布局解析方式
将对应LayoutId的布局添加到ViewGroup中
```
LayoutInflater.from(context).inflate(playerLayoutId, this);
contentFrame = findViewById(R.id.exo_content_frame);
StyledPlayerControlView customController = findViewById(R.id.exo_controller); //控制器
```
布局为exo_content_frame,exo_shutter,exo_artwork,exo_subtitles,exo_buffering,exo_error_message,exo_ad_overlay
exo_overlay,exo_controller_placeholder    
可以在布局添加自己的view，从ViewGroup中findViewById然后进行控制,显示或隐藏就跟随播放器或者控制面板了

exo_styled_player_view.xml
```
<merge xmlns:android="http://schemas.android.com/apk/res/android">
  <com.google.android.exoplayer2.ui.AspectRatioFrameLayout android:id="@id/exo_content_frame"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_gravity="center">

    <View android:id="@id/exo_shutter"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:background="@android:color/black"/>

    <ImageView android:id="@id/exo_artwork"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:scaleType="fitXY"/>

    <com.google.android.exoplayer2.ui.SubtitleView android:id="@id/exo_subtitles"
      android:layout_width="match_parent"
      android:layout_height="match_parent"/>

    <ProgressBar android:id="@id/exo_buffering"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:indeterminate="true"
      android:layout_gravity="center"/>

    <TextView android:id="@id/exo_error_message"
      android:layout_width="wrap_content"
      android:layout_height="@dimen/exo_error_message_height"
      android:layout_gravity="center"
      android:layout_marginBottom="@dimen/exo_error_message_margin_bottom"
      android:gravity="center"
      android:textColor="@color/exo_white"
      android:textSize="@dimen/exo_error_message_text_size"
      android:background="@drawable/exo_rounded_rectangle"
      android:paddingLeft="@dimen/exo_error_message_text_padding_horizontal"
      android:paddingRight="@dimen/exo_error_message_text_padding_horizontal"
      android:paddingTop="@dimen/exo_error_message_text_padding_vertical"
      android:paddingBottom="@dimen/exo_error_message_text_padding_vertical"/>

  </com.google.android.exoplayer2.ui.AspectRatioFrameLayout>

  <FrameLayout android:id="@id/exo_ad_overlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>

  <FrameLayout android:id="@id/exo_overlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>

  <View android:id="@id/exo_controller_placeholder"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
</merge>
```


exo_player_control_view.xml
控制器的常用组件exo_play，exo_pause，exo_position，exo_duration  
```
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:paddingTop="4dp"
    android:orientation="horizontal">

    <ImageButton android:id="@id/exo_prev"
      style="@style/ExoMediaButton.Previous"/>

    <ImageButton android:id="@id/exo_rew"
      style="@style/ExoMediaButton.Rewind"/>

    <ImageButton android:id="@id/exo_shuffle"
      style="@style/ExoMediaButton"/>

    <ImageButton android:id="@id/exo_repeat_toggle"
      style="@style/ExoMediaButton"/>

    <ImageButton android:id="@id/exo_play"
      style="@style/ExoMediaButton.Play"/>

    <ImageButton android:id="@id/exo_pause"
      style="@style/ExoMediaButton.Pause"/>

    <ImageButton android:id="@id/exo_ffwd"
      style="@style/ExoMediaButton.FastForward"/>

    <ImageButton android:id="@id/exo_next"
      style="@style/ExoMediaButton.Next"/>

    <ImageButton android:id="@id/exo_vr"
      style="@style/ExoMediaButton.VR"/>

  </LinearLayout>
  
  。。。
  <LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:gravity="center_vertical"
    android:orientation="horizontal">

    <TextView android:id="@id/exo_position"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:textSize="14sp"
      android:textStyle="bold"
      android:paddingLeft="4dp"
      android:paddingRight="4dp"
      android:includeFontPadding="false"
      android:textColor="#FFBEBEBE"/>

    <View android:id="@id/exo_progress_placeholder"
      android:layout_width="0dp"
      android:layout_weight="1"
      android:layout_height="26dp"/>

    <TextView android:id="@id/exo_duration"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:textSize="14sp"
      android:textStyle="bold"
      android:paddingLeft="4dp"
      android:paddingRight="4dp"
      android:includeFontPadding="false"
      android:textColor="#FFBEBEBE"/>

  </LinearLayout>
```