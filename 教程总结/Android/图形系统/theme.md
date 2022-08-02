
appcompat-1.2.0  sdk 30
https://blog.csdn.net/yanbober/article/details/51015630

theme的属性继承，以DarkActionBar为例
Theme.AppCompat.Light.DarkActionBar

定义在
appcompat-1.2.0\res\values\values.xml
```
<style name="Theme.AppCompat.Light.DarkActionBar" parent="Base.Theme.AppCompat.Light.DarkActionBar"/>
```
Base.Theme.AppCompat.Light.DarkActionBar的实现
```
<style name="Base.Theme.AppCompat.Light.DarkActionBar" parent="Base.Theme.AppCompat.Light">
        <item name="actionBarPopupTheme">@style/ThemeOverlay.AppCompat.Light</item>
        <item name="actionBarWidgetTheme">@null</item>
        <item name="actionBarTheme">@style/ThemeOverlay.AppCompat.Dark.ActionBar</item>

        <!-- Panel attributes -->
        <item name="listChoiceBackgroundIndicator">@drawable/abc_list_selector_holo_dark</item>

        <item name="colorPrimaryDark">@color/primary_dark_material_dark</item>
        <item name="colorPrimary">@color/primary_material_dark</item>
    </style>
```
然后一路追踪
```
<style name="Base.Theme.AppCompat.Light" parent="Base.V7.Theme.AppCompat.Light">
<style name="Base.V7.Theme.AppCompat.Light" parent="Platform.AppCompat.Light">
        <item name="windowNoTitle">false</item>
        <item name="windowActionBar">true</item>
        <item name="windowActionBarOverlay">false</item>
        <item name="windowActionModeOverlay">false</item>
        <item name="actionBarPopupTheme">@null</item>
        ...
</style>

android-30\data\res\values\themes_holo.xml
 <style name="Platform.AppCompat.Light" parent="android:Theme.Holo.Light">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>
        ...
 </style>

platforms\android-30\data\res\values\themes.xml
 <style name="Theme.Holo.Light" parent="Theme.Light">
        <item name="colorForeground">@color/bright_foreground_holo_light</item>
        <item name="colorForegroundInverse">@color/bright_foreground_inverse_holo_light</item>
        <item name="colorBackground">@color/background_holo_light</item>
        <item name="colorBackgroundFloating">@color/background_holo_light</item>
        <item name="colorBackgroundCacheHint">@color/background_cache_hint_selector_holo_light</item>
        <item name="disabledAlpha">0.5</item> 
        ...
 </style>  
 
  <style name="Theme.Light">
        <item name="isLightTheme">true</item>
        <item name="windowBackground">@drawable/screen_background_selector_light</item>
        <item name="windowClipToOutline">false</item>                    
```
最终的theme

Sdk\platforms\android-30\data\res\values\themes.xml
```
 <style name="Theme">
        <!-- Text styles -->
        <!-- Button styles -->
        <!-- List attributes -->
        <!-- @hide -->
        <!-- Gallery attributes -->
        <!-- Window attributes -->
        <!-- Define these here; ContextThemeWrappers around themes that define them should
             always clear these values. -->
        <!-- Dialog attributes -->
        <!-- AlertDialog attributes -->
        <!-- Presentation attributes (introduced after API level 10 so does not
             have a special old-style theme. -->
        <!-- Toast attributes -->
        <!-- Panel attributes -->
        <!-- These three attributes do not seems to be used by the framework. Declared public though -->
        <!-- Scrollbar attributes -->
        <!-- Text selection handle attributes -->
        <!-- Widget styles -->
        <!-- Preference styles -->
        <!-- Search widget styles -->
        <!-- Action bar styles -->
        <!-- Floating toolbar styles -->
        <!-- SearchView attributes -->
        <!-- PreferenceFrameLayout attributes -->
        <!-- NumberPicker style-->
        <!-- CalendarView style-->
        <!-- TimePicker style -->
        <!-- TimePicker dialog theme -->
        <!-- DatePicker style -->
        <!-- DatePicker dialog theme -->
        <!-- Pointer style -->
        <!-- Accessibility focused drawable -->
        <!-- Lighting and shadow properties -->
  </style> 
```
它里面定义了关于我们整个应用中文字样式、按钮样式、列表样式、窗体样式、对话框样式等，这些样式都是默认样式，它还有很多我们常用的扩展样式，
譬如Theme.Light、Theme.NoTitleBar、Theme.NoTitleBar.Fullscreen等等