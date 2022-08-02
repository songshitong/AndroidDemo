https://blog.csdn.net/yanbober/article/details/51015630


setTheme放在setContentView之前
android-12.0.0_r3
setContentView->installDecor->generateLayout
http://www.aospxref.com/android-12.0.0_r3/xref/frameworks/base/core/java/com/android/internal/policy/PhoneWindow.java#2378
```
 //获取当前主题
 TypedArray a = getWindowStyle();
 ...
 //解析一堆主题属性，譬如下面的是否浮动window（dialog）等
 mIsFloating = a.getBoolean(R.styleable.Window_windowIsFloating, false);
 ...
 if (a.getBoolean(R.styleable.Window_windowNoTitle, false)) {
 ...
 if (a.getBoolean(R.styleable.Window_windowActionBarOverlay, false)) {
 ...
 
 //依据属性获取不同的布局添加到Decor
  int layoutResource;
  //可以查看/frameworks/base/core/java/android/view/Window.java
  //常见的feature有FEATURE_NO_TITLE，FEATURE_ACTION_BAR ，FEATURE_ACTIVITY_TRANSITIONS 等
  int features = getLocalFeatures();
  // System.out.println("Features: 0x" + Integer.toHexString(features));
  if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
      if (mIsFloating) {
          TypedValue res = new TypedValue();
          getContext().getTheme().resolveAttribute(
                  R.attr.dialogTitleIconsDecorLayout, res, true);
          layoutResource = res.resourceId;
      } else {
          layoutResource = R.layout.screen_title_icons;
      }
   ...
    else if ((features & (1 << FEATURE_ACTION_MODE_OVERLAY)) != 0) {
              layoutResource = R.layout.screen_simple_overlay_action_mode;
          } else {
              layoutResource = R.layout.screen_simple;
          }   
  ...    
  mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
  ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
  ...
   return contentParent;
```
/frameworks/base/core/java/android/view/Window.java
getWindowStyle
```
     public final TypedArray getWindowStyle() {
          synchronized (this) {
              if (mWindowStyle == null) {
                  mWindowStyle = mContext.obtainStyledAttributes(
                          com.android.internal.R.styleable.Window);
              }
              return mWindowStyle;
          }
      }
```
/frameworks/base/core/java/android/content/Context.java
```
   public final TypedArray obtainStyledAttributes(
              @Nullable AttributeSet set, @NonNull @StyleableRes int[] attrs) {
          return getTheme().obtainStyledAttributes(set, attrs, 0, 0);
      }
public abstract Resources.Theme getTheme();      
```
context的实现
ContextThemeWapprer
/frameworks/base/core/java/android/view/ContextThemeWrapper.java
```
//获取主题
public Resources.Theme getTheme() {
          if (mTheme != null) {
              return mTheme;
          }
           //没有设置Theme则获取默认的selectDefaultTheme
          mThemeResource = Resources.selectDefaultTheme(mThemeResource,
                  getApplicationInfo().targetSdkVersion);
          initializeTheme();
  
          return mTheme;
      }
//设置主题
public void setTheme(int resid) {
    //通过外部设置以后mTheme和mThemeResource就不为null了
    if (mThemeResource != resid) {
        mThemeResource = resid;
        //初始化选择的主题，mTheme就不为null了
        initializeTheme();
    }
}    
```
默认主题的获取
/frameworks/base/core/java/android/content/res/Resources.java
```
   public static int selectDefaultTheme(int curTheme, int targetSdkVersion) {
          return selectSystemTheme(curTheme, targetSdkVersion,
                  com.android.internal.R.style.Theme,
                  com.android.internal.R.style.Theme_Holo,
                  com.android.internal.R.style.Theme_DeviceDefault,
                  com.android.internal.R.style.Theme_DeviceDefault_Light_DarkActionBar);
      }
      
   public static int selectSystemTheme(int curTheme, int targetSdkVersion, int orig, int holo,
              int dark, int deviceDefault) {
          if (curTheme != ID_NULL) {
              return curTheme;
          }
          if (targetSdkVersion < Build.VERSION_CODES.HONEYCOMB) {
             //android 3.0以下 R.style.Theme
              return orig;
          }
          if (targetSdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
               //android4.0以下  Theme_Holo
              return holo;
          }
          if (targetSdkVersion < Build.VERSION_CODES.N) {
             //android7.0以下   Theme_DeviceDefault 深色主题
              return dark;
          }
          //其他使用 Theme_DeviceDefault_Light_DarkActionBar
          return deviceDefault;
      }
      
```