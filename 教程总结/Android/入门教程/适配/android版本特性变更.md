https://storage.googleapis.com/play_public/supported_devices.html
所有手机型号列表
Retail Branding 	Marketing Name	   Device	  Model
Redmi	            K30 PRO	            lmi	      Redmi K30 Pro


https://blog.csdn.net/qq_40881680/article/details/109281912
https://developer.android.com/guide/topics/manifest/uses-sdk-element
https://developer.android.com/about/versions


Android版本	 API级别	  VERSION_CODE	重大改进或变更	                                           开发注意事项
Android     4.4	19	       KITKAT	
外部存储空间读取权限、 使用WebView、使用 AlarmManager、使用ContentResolver同步数据	
参考《Android 4.4 重要行为变更》   https://developer.android.com/about/versions/android-4.4#Behaviors


Android 5.0	21	LOLLIPOP	Android 5.0 
新增了material design样式的支持，新增虚拟机ART，引入64位系统	
参考《Android 5.0 变更》


Android 6.0	23	M	运行时权限	
需要动态申请权限，对于以 Android 6.0（API 级别 23）或更高版本为目标平台的应用，请务必在运行时检查和请求权限。要确定您的应用是否已被授予权限，
请调用新增的 checkSelfPermission( ) 方法。要请求权限，请调用新增的 requestPermissions( ) 方法
引入低电耗模式，当用户设备未插接电源、处于静止状态且屏幕关闭时，该模式会推迟 CPU 和网络活动，从而延长电池寿命


Android 7.0	24	N	系统权限更改
改进低电耗模式，在设备未插接电源且屏幕关闭状态下、但不一定要处于静止状态（例如用户外出时把手持式设备装在口袋里）时应用部分 CPU 和网络限制，
  进一步增强了低电耗模式
传递软件包网域外的 file:// URI 可能给接收器留下无法访问的路径。因此，尝试传递 file:// URI 会触发 FileUriExposedException。
  分享私有文件内容的推荐方法是使用 FileProvider

参考《Android 7.0 行为变更》  https://developer.android.com/about/versions/nougat/android-7.0-changes?hl=zh-cn#permfilesys


Android 8.0	26	O	8.0 系统的通知栏适配	
后台限制，后台应用无法使用其清单注册大部分隐式广播，在后台运行的应用对后台服务的访问受到限制
  降低了后台应用接收位置更新的频率
引入了通知渠道，其允许您为要显示的每种通知类型创建用户可自定义的渠道。用户界面将通知渠道称之为通知类别
什么是通知渠道呢？顾名思义，就是每条通知都要属于一个对应的渠道。每个App都可以自由地创建当前App拥有哪些通知渠道，
但是这些通知渠道的控制权都是掌握在用户手上的。用户可以自由地选择这些通知渠道的重要程度，是否响铃、是否振动、或者是否要关闭这个渠道的通知。
例如：支付宝就可以创建两种通知渠道，一个收支，一个推荐，而我作为用户对推荐类的通知不感兴趣，那么我就可以直接将推荐通知渠道关闭，
   这样既不影响我关心的通知，又不会让那些我不关心的通知来打扰我了

参考 郭霖的 《8.0系统的通知栏适配》 https://blog.csdn.net/guolin_blog/article/details/79854070


Android 9.0	28	P	网络连接变更	
自9.0起，默认不再支持http请求，如果需要支持，需要按 《Android9.0 http无法访问的解决方案 》进行修改和编辑
   https://codechina.csdn.net/qq_40881680/android/-/blob/master/Android9_Solution.md
限制后台应用访问用户输入和传感器数据的能力。 如果您的应用在运行 Android 9 设备的后台运行，系统将对您的应用采取以下限制：
  您的应用不能访问麦克风或摄像头。
  使用连续报告模式的传感器（例如加速度计和陀螺仪）不会接收事件。
  使用变化或一次性报告模式的传感器不会接收事件。
如果您的应用需要在运行 Android 9 的设备上检测传感器事件，请使用前台服务

对使用非 SDK 接口的限制

Android 10.0	29	Q	引入分区存储
分区存储：应用在默认情况下被赋予了对外部存储空间的分区访问权限（即分区存储）。此类应用只能访问外部存储空间上的应用专属目录，
  以及本应用所创建的特定类型的媒体文件。一般有图片，视频，音频，Download/等
https://developer.android.com/training/data-storage/app-specific?hl=zh-cn
可以访问应用自身的沙盒和特定的外部存储(高版本不需要权限，低版本需要)
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
      ActivityCompat.requestPermissions(
        this, arrayOf(
          permission.WRITE_EXTERNAL_STORAGE,
          permission.READ_EXTERNAL_STORAGE
        ), 1
      )
 }
android10以后自动获取沙盒权限 
```
可能会遇到无法保存图片到本地，或者造成Permission denied等问题，具体解决方案参考：《关于安卓open failed: EACCES (Permission denied)》
  https://myhub.blog.csdn.net/article/details/108701706
  使用，新增媒体文件使用ContentProvider https://developer.android.com/training/data-storage/shared/media#java
https://developer.android.com/training/data-storage/shared/documents-files?hl=zh-cn
申请所有文件访问权限
https://developer.android.com/training/data-storage/manage-all-files?hl=zh-cn
```
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
    val intent = Intent()
    intent.action= Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
    startActivity(intent)
    //判断是否获取MANAGE_EXTERNAL_STORAGE权限：
    val isHasStoragePermission= Environment.isExternalStorageManager()
```
访问
```
Cursor cursor = getApplicationContext().getContentResolver().query(
    MediaStore.media-type.Media.EXTERNAL_CONTENT_URI,
    projection,
    selection,
    selectionArgs,
    sortOrder
);

while (cursor.moveToNext()) {
   
}
```
打开文件,然后保存
```
ContentResolver resolver = getApplicationContext()
        .getContentResolver();
try (InputStream stream = resolver.openInputStream(content-uri)) {
    // Perform operations on "stream".
}
```
插入文件
```
  val value = ContentValues()
      value.put(Downloads.DISPLAY_NAME,fileName) //指定插入的名字
 Uri insertUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value);
    try {
      if (insertUri != null){
        OutputStream outputStream = getContentResolver().openOutputStream(insertUri, "rw");
        toBitmap.compress(JPEG, 90, outputStream);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
```
启用手势导航。用户启用后，手势导航会影响设备上的所有应用
新增了一个系统级的深色主题


Android 11.0	30	R	
强制执行分区存储机制、单次授权、自动重置权限、后台位置信息访问权限、软件包可见性、前台服务
 单次授权   对位置信息、麦克风和摄像头的临时访问权限
 自动重置权限 如果应用数月未使用，系统会通过自动重置用户已授予应用的运行时敏感权限来保护用户数据
 后台位置信息访问权限  应用在后台运行，它每小时只能接收几次位置信息更新
    通过对权限请求方法的多次单独调用，逐步请求在前台（粗略或精确）和后台访问位置信息的权限。必要时，说明用户授予该权限所能得到的益处
 软件包可见性 与设备上的其他已安装应用交互的应用，将 <queries> 元素添加到应用的清单
 前台服务  在 Android 11 或更高版本上运行且在前台服务中访问位置信息、摄像头或麦克风的应用
《Android 11 隐私设置更新》  https://developer.android.com/about/versions/11/privacy


Android 12.0	31	S	
Material You设计
小组件改进   
应用启动画面  低版本中实现了自定义启动画面，则需要将您的应用迁移到 SplashScreen API
更安全的组件导出   必须为应用组件(activity,service,provider,广播)显式声明 android:exported 属性
剪切板通知  会弹出一个消息框消息，通知用户对剪贴板的访问
当应用使用麦克风或相机时，图标会出现在状态栏中。
activity 生命周期 按下“返回”按钮时，不再完成根启动器 activity
  Android 12 更改了在按下“返回”按钮时系统对为其任务根的启动器 activity 的默认处理方式。在以前的版本中，系统会在按下“返回”按钮时完成这些 activity。
  在 Android 12 中，现在系统会将 activity 及其任务移到后台，而不是完成 activity。当使用主屏幕按钮或手势从应用中导航出应用时，
  新行为与当前行为一致。用户可以更快地从温状态恢复应用，而不必从冷状态完全重启应用。
沉浸模式下的手势导航改进、前台服务通知延迟、不受信任的触摸事件被屏蔽、应用无法关闭系统对话框	
 前台服务 应用无法在后台运行时启动前台服务，少数特殊情况除外
 不受信任的触摸 系统会屏蔽穿透某些窗口的触摸操作
 应用无法关闭系统对话框  弃用了 ACTION_CLOSE_SYSTEM_DIALOGS intent 操作
《行为变更：以 Android 12 为目标平台的应用》
https://developer.android.com/about/versions/12/behavior-changes-12
https://juejin.cn/post/6985806802831015950
1. exported  需要Intent-filter的组件显性的声明exported属性
2. pendingIntent必须声明可变性  https://developer.android.com/about/versions/12/behavior-changes-12
```
PendingIntent pendingActivityIntent = PendingIntent.getActivity(Application.getContext(), R.drawable.ic_launcher, intent,
PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
```
滚动列表增加拉伸效果
在 Android 11 及更低版本中，滚动事件会使视觉元素发光。在 Android 12 及更高版本中，发生拖动事件时，视觉元素会拉伸和反弹；
发生快速滑动事件时，它们会快速滑动和反弹   停用：android:overScrollMode="never"


android 13
https://juejin.cn/post/7099762078977622053
1 更安全地导出上下文注册的接收器
以 Android 13(33) 或更高版本为目标平台的应用，必须为每个广播接收器指定 RECEIVER_EXPORTED 或 RECEIVER_NOT_EXPORTED，
否则当 App 尝试注册广播接收器时，系统会抛出 SecurityException
```
context.registerReceiver(sharedBroadcastReceiver, intentFilter,
    RECEIVER_EXPORTED);

context.registerReceiver(privateBroadcastReceiver, intentFilter,
    RECEIVER_NOT_EXPORTED);
```


android 14
缓存应用冻结
https://developer.android.com/about/versions/14/behavior-changes-all
Android 14 开始缓存的 App 会在短时间内被冻结
在 Android 14 里，除了前台服务 和 JobScheduler/WorkManager 之外，App 应该不再运行其他形式的后台工作
缓存应用定义：
an app's process is in a cached state when it's moved to the background and no other app process components are running.
cached process 更多介绍
https://developer.android.com/guide/components/activities/process-lifecycle

广播优化
On Android 14, the system may place context-registered broadcasts in a queue while the app is in the cached state.
When the app leaves the cached state, such as returning to the foreground, the system delivers any queued broadcasts

manifest声明的广播不受影响
Manifest-declared broadcasts aren't queued, and apps are removed from the cached state for broadcast delivery

https://mp.weixin.qq.com/s/GWtY8qV-BhTkPEnSbIjTHQ
所有动态加载的文件都必须标记为只读(例如DEX、JAR 或 APK 文件)。否则，系统将抛出异常。官方建议应用尽可能避免动态加载代码，
因为这样做会大大增加应用被代码注入或代码篡改破坏的风险。
```
val jar = File("xxx.jar")
val os = FileOutputStream(jar)
os.use {
    jar.setReadOnly()
}
val cl = PathClassLoader(jar.absolutePath, parentClassLoader)
```

Foreground service types are required
类型有：需要在manifest或代码中指定
camera，connectedDevice，dataSync，health，location，mediaPlayback，mediaProjection，microphone，phoneCall，
remoteMessaging，shortService，specialUse，systemExempted
如果前台服务的逻辑与这些类型无关，建议使用WorkManager/user-initiated data transfer jobs

user-initiated data transfer jobs 就是由用户发起的数据传输任务。此 API 是 Android14 新增的，
适用于需要由用户发起的持续时间较长的数据传输，例如从远程服务器下载文件。这些任务需要在通知栏中显示一个通知，会立即启动，
并且可能在系统条件允许的情况下长时间运行。我们可以同时运行多个由用户发起的数据传输作业
```
<uses-permission android:name="android.permission.RUN_USER_INITIATED_JOBS" />
<service android:name="com.example.app.CustomTransferService"
        android:permission="android.permission.BIND_JOB_SERVICE"
        android:exported="false">
        ...
</service>
class CustomTransferService : JobService() {
  ...
}
https://developer.android.com/about/versions/14/changes/user-initiated-data-transfers 
```

Runtime-registered broadcasts receivers must specify export behavior
```
val filter = IntentFilter("alarmReceiver_custom_action")
val listenToBroadcastsFromOtherApps = true
val receiverFlags = if (listenToBroadcastsFromOtherApps) {
    ContextCompat.RECEIVER_EXPORTED    // 该接收器对其他应用开放
} else {
    ContextCompat.RECEIVER_NOT_EXPORTED    // 该接收器不对其他应用开放
}
// 这里的 registerReceiver 方法必须设置 receiverFlags 参数
registerReceiver(requireContext(), AlarmReceiver(), filter, receiverFlags)
```