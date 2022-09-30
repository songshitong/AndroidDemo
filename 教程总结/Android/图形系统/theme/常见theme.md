

Sdk\platforms\android-30\data\res\values\themes.xml

黑色主题
```
 <style name="Theme.Black">
        <item name="windowBackground">@color/black</item>
        <item name="colorBackground">@color/black</item>
    </style>
```
白色主题
```
 <style name="Theme.Light">
        <item name="isLightTheme">true</item>
        <item name="windowBackground">@drawable/screen_background_selector_light</item>
 ....
 
Sdk\platforms\android-30\data\res\drawable\screen_background_selector_light.xml  
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_accelerated="false"
            android:drawable="@android:drawable/screen_background_light" /> //白色
    <item android:drawable="@android:drawable/background_holo_light" /> //渐变的白色
</selector>
Sdk\platforms\android-30\data\res\values\colors.xml
 <drawable name="screen_background_light">#ffffffff</drawable>
 
Sdk\platforms\android-30\data\res\drawable\background_holo_light.xml 
 <shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
            android:startColor="#ffe8e8e8"
            android:endColor="#ffffffff"
            android:angle="270" />
</shape>     
```

透明的
```
<style name="Theme.Translucent">
        <item name="windowBackground">@color/transparent</item>
        <item name="colorBackgroundCacheHint">@null</item>
        <item name="windowIsTranslucent">true</item>
        <item name="windowAnimationStyle">@style/Animation</item>
    </style>
```
透明主题在android8.0请求屏幕方向会崩溃
Only fullscreen opaque activities can request orientation
解决：只保留其中一个

没有titleBar
Theme.Light.NoTitleBar  白色无titleBar
```
<style name="Theme.Light.NoTitleBar">
        <item name="windowNoTitle">true</item>
    </style>
```
Theme.Black.NoTitleBar  黑色无titleBar
```
 <style name="Theme.Black.NoTitleBar">
        <item name="windowNoTitle">true</item>
    </style>
```
Theme.Translucent.NoTitleBar
```
 <style name="Theme.Translucent.NoTitleBar">
        <item name="windowNoTitle">true</item>
        <item name="windowContentOverlay">@null</item>
    </style>
```
`


全屏相关：
Theme_Black_NoTitleBar_Fullscreen
```
<style name="Theme.Black.NoTitleBar.Fullscreen">
        <item name="windowFullscreen">true</item>
        <item name="windowContentOverlay">@null</item>
    </style>
```
Theme.NoTitleBar.Fullscreen
```
  <style name="Theme.NoTitleBar.Fullscreen">
        <item name="windowFullscreen">true</item>
        <item name="windowContentOverlay">@null</item>
    </style>
```
透明全屏
```
 <style name="Theme.Translucent.NoTitleBar.Fullscreen">
        <item name="windowFullscreen">true</item>
    </style>
```


弹窗
Theme.Dialog 默认dialog样式
```
 <style name="Theme.Dialog">
      //window相关样式
      <item name="windowFrame">@null</item>
        <item name="windowTitleStyle">@style/DialogWindowTitle</item>
        <item name="windowBackground">@drawable/panel_background</item>
        <item name="windowIsFloating">true</item>
        <item name="windowContentOverlay">@null</item>
        <item name="windowAnimationStyle">@style/Animation.Dialog</item>
        <item name="windowSoftInputMode">stateUnspecified|adjustPan</item>
        <item name="windowCloseOnTouchOutside">@bool/config_closeDialogWhenTouchOutside</item>
        <item name="windowActionModeOverlay">true</item>
        <item name="colorBackgroundCacheHint">@null</item>

        ...//文字相关样式

        //偏好相关。。。
        <item name="listPreferredItemPaddingLeft">10dip</item>
        <item name="listPreferredItemPaddingRight">10dip</item>
        <item name="listPreferredItemPaddingStart">10dip</item>
        <item name="listPreferredItemPaddingEnd">10dip</item>

        <item name="preferencePanelStyle">@style/PreferencePanel.Dialog</item>
  </style>       
```