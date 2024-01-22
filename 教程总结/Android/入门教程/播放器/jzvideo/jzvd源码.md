版本7.7
项目结构
demo  常用的播放器功能实现
library 基础的播放器ui+播放器功能接口

功能扩展
jzvd和jzvdStd大部分方法可以重写修改逻辑
JzvdStd继承jzvd实现完整的播放逻辑
布局文件为res/layout/jz_layout_std.xml
1 可以自定义布局，并增加其中已经声明的控件，如surface_container，poster，bottom_progress等
2 重写init方法，代码动态添加布局

内核切换的实现
自定义UI+播放器接口(不同的内核只要实现ui对应的播放器接口的功能即可)
java/cn/jzvd/JZMediaInterface.java
```
//常见的播放功能
public abstract class JZMediaInterface implements TextureView.SurfaceTextureListener {
    //参数为需要一个播放界面的ui也就是jzvd
    public JZMediaInterface(Jzvd jzvd) {
        this.jzvd = jzvd;
    }
    public abstract void start();
    public abstract void prepare();
    public abstract void pause();
    public abstract void seekTo(long time);
    public abstract void release();
    public abstract long getCurrentPosition();
    public abstract long getDuration();
    public abstract void setVolume(float leftVolume, float rightVolume);
    public abstract void setSpeed(float speed);
    public abstract void setSurface(Surface surface);
}

//java/cn/jzvd/Jzvd.java  播放器
public abstract class Jzvd extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
  //布局初始化
  public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        jzvdContext = context;
        startButton = findViewById(R.id.start);
        fullscreenButton = findViewById(R.id.fullscreen);
        ...
    }
    
  //ui与播放器接口的绑定  mediaInterfaceClass为播放器接口
  public void setUp(JZDataSource jzDataSource, int screen, Class mediaInterfaceClass) {
        this.jzDataSource = jzDataSource;
        this.screen = screen;
        onStateNormal();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }  
 //播放器初始化 通过反射构建   
 public void startVideo() {
        Log.d(TAG, "startVideo [" + this.hashCode() + "] ");
        setCurrentJzvd(this);
        try {
            Constructor<JZMediaInterface> constructor = mediaInterfaceClass.getConstructor(Jzvd.class);
            this.mediaInterface = constructor.newInstance(this);
        }...
        addTextureView();
        JZUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onStatePreparing();
    }    
}
```

内核为MediaPlayer的实现
java/cn/jzvd/JZMediaSystem.java
```
public class JZMediaSystem extends JZMediaInterface implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {
    public MediaPlayer mediaPlayer;
    public JZMediaSystem(Jzvd jzvd) {
        super(jzvd);
    }
    
    //构建播放器
     public void prepare() {
       ...
       mediaPlayer = new MediaPlayer();
       ...         
    }
    
    //实现播放器的功能
    public void start() {
        mMediaHandler.post(() -> mediaPlayer.start());
    }
    ...
}    
```

横竖屏切换的实现
1 将自己从parent移除
2 按照填充满的方式重新添加布局
3 请求横屏配置  manifest中配置 android:configChanges="orientation|screenSize|keyboardHidden"
   防止横竖屏切换导致activity重建
java/cn/jzvd/Jzvd.java
```
 public void gotoFullscreen() {
        gotoFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup) getParent();
        jzvdContext = vg.getContext();
        blockLayoutParams = getLayoutParams();
        blockIndex = vg.indexOfChild(this);
        blockWidth = getWidth();
        blockHeight = getHeight();
        vg.removeView(this);
        cloneAJzvd(vg);
        CONTAINER_LIST.add(vg);
        vg = (ViewGroup) (JZUtils.scanForActivity(jzvdContext)).getWindow().getDecorView();

        ViewGroup.LayoutParams fullLayout = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        vg.addView(this, fullLayout);

        setScreenFullscreen();
        JZUtils.hideStatusBar(jzvdContext);
        JZUtils.setRequestedOrientation(jzvdContext, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(jzvdContext);//华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326
    }
```

播放进度缓存  通过sp实现
java/cn/jzvd/JZUtils.java
```
 public static void saveProgress(Context context, Object url, long progress) {
        if (!Jzvd.SAVE_PROGRESS) return;
        Log.i(TAG, "saveProgress: " + progress);
        if (progress < 5000) {
            progress = 0;
        }
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spn.edit();
        editor.putLong("newVersion:" + url.toString(), progress).apply();
    }

    public static long getSavedProgress(Context context, Object url) {
        if (!Jzvd.SAVE_PROGRESS) return 0;
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                Context.MODE_PRIVATE);
        return spn.getLong("newVersion:" + url.toString(), 0);
    }
```