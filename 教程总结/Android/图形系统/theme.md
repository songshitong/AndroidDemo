
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


manifest中theme的调用
/frameworks/base/core/java/android/app/ActivityThread.java
```
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
  //创建activity
  ...
   Activity activity = null;
          try {
              java.lang.ClassLoader cl = appContext.getClassLoader();
              activity = mInstrumentation.newActivity(
                      cl, component.getClassName(), r.intent);
   ...                   
  ..
  int theme = r.activityInfo.getThemeResource();
                  if (theme != 0) {
                      activity.setTheme(theme);
                  }
 ....                 
}
```

initializeTheme
/frameworks/base/core/java/android/view/ContextThemeWrapper.java
```
private void initializeTheme() {
          final boolean first = mTheme == null;
          if (first) {
              mTheme = getResources().newTheme();
              final Resources.Theme theme = getBaseContext().getTheme();
              if (theme != null) {
                  mTheme.setTo(theme);
              }
          }
          onApplyThemeResource(mTheme, mThemeResource, first);
      }

 protected void onApplyThemeResource(Resources.Theme theme, int resId, boolean first) {
          theme.applyStyle(resId, true);
      }      
```
/frameworks/base/core/java/android/content/res/Resources.java
```
   public final Theme newTheme() {
          Theme theme = new Theme();
          //根据Resources创建theme的实现
          theme.setImpl(mResourcesImpl.newThemeImpl());
          synchronized (mThemeRefs) {
              mThemeRefs.add(new WeakReference<>(theme));
              if (mThemeRefs.size() > mThemeRefsNextFlushSize) {
                  mThemeRefs.removeIf(ref -> ref.get() == null);
                  mThemeRefsNextFlushSize = Math.max(MIN_THEME_REFS_FLUSH_SIZE,
                          2 * mThemeRefs.size());
              }
          }
          return theme;
      }
```
Theme是Resources的内部类，里面的方法大部分是ResourcesImpl.ThemeImpl实现的

theme.applyStyle
/frameworks/base/core/java/android/content/res/ResourcesImpl.java
```
  void applyStyle(int resId, boolean force) {
     //AssetManager mAssets;
              mAssets.applyStyleToTheme(mTheme, resId, force);
              mThemeResId = resId;
              mKey.append(resId, force);
          }
```
/frameworks/base/core/java/android/content/res/AssetManager.java
```
   void applyStyleToTheme(long themePtr, @StyleRes int resId, boolean force) {
          synchronized (this) {
              ensureValidLocked();
              nativeThemeApplyStyle(mObject, themePtr, resId, force);
          }
      }
```
/frameworks/base/core/jni/android_util_AssetManager.cpp
```
 static void NativeThemeApplyStyle(JNIEnv* env, jclass /*clazz*/, jlong ptr, jlong theme_ptr,
                                    jint resid, jboolean force) {
    ScopedLock<AssetManager2> assetmanager(AssetManagerFromLong(ptr));
    Theme* theme = reinterpret_cast<Theme*>(theme_ptr);
    CHECK(theme->GetAssetManager() == &(*assetmanager));
    (void) assetmanager;
    theme->ApplyStyle(static_cast<uint32_t>(resid), force);
  }
```
/frameworks/base/libs/androidfw/AssetManager2.cpp
```
base::expected<std::monostate, NullOrIOError> Theme::ApplyStyle(uint32_t resid, bool force) {
    ...
    //资源id  
    auto bag = asset_manager_->GetBag(resid);
    ...
    type_spec_flags_ |= (*bag)->type_spec_flags;
  
    for (auto it = begin(*bag); it != end(*bag); ++it) {
      const uint32_t attr_res_id = it->key;
 
      if (!is_valid_resid(attr_res_id)) {
        return base::unexpected(std::nullopt);
      }
      bool is_undefined = it->value.dataType == Res_value::TYPE_NULL &&
          it->value.data != Res_value::DATA_NULL_EMPTY;
      if (!force && is_undefined) {
        continue;
      }
  
      Theme::Entry new_entry{attr_res_id, it->cookie, (*bag)->type_spec_flags, it->value};
      //对集合进行操作std::vector<Entry> entries_
      auto entry_it = std::lower_bound(entries_.begin(), entries_.end(), attr_res_id,
                                       ThemeEntryKeyComparer{});
      if (entry_it != entries_.end() && entry_it->attr_res_id == attr_res_id) {
        //移除未定义的属性
        if (is_undefined) {       
          entries_.erase(entry_it);
        } else if (force) {
          *entry_it = new_entry;
        }
      } else {
        //插进集合
        entries_.insert(entry_it, new_entry);
      }
    }
    return {};
  }
  
}
```
todo AssetManager相关  entries_

getTheme().obtainStyledAttributes
/frameworks/base/core/java/android/content/res/Resources.java
```
public TypedArray obtainStyledAttributes(@NonNull @StyleableRes int[] attrs) {
              synchronized (mLock) {
                  return mThemeImpl.obtainStyledAttributes(this, null, attrs, 0, 0);
              }
          }
```

/frameworks/base/core/java/android/content/res/ResourcesImpl.java
```
 TypedArray obtainStyledAttributes(@NonNull Resources.Theme wrapper,
                  AttributeSet set,
                  @StyleableRes int[] attrs,
                  @AttrRes int defStyleAttr,
                  @StyleRes int defStyleRes) {
              final int len = attrs.length;
              //生成TypedArray
              final TypedArray array = TypedArray.obtain(wrapper.getResources(), len);
              final XmlBlock.Parser parser = (XmlBlock.Parser) set;
              mAssets.applyStyle(mTheme, defStyleAttr, defStyleRes, parser, attrs,
                      array.mDataAddress, array.mIndicesAddress);
              array.mTheme = wrapper;
              //xml 解析器
              array.mXml = parser;
              return array;
          }
```

/frameworks/base/core/java/android/content/res/AssetManager.java
```
   void applyStyle(long themePtr, @AttrRes int defStyleAttr, @StyleRes int defStyleRes,
              @Nullable XmlBlock.Parser parser, @NonNull int[] inAttrs, long outValuesAddress,
              long outIndicesAddress) {
          Objects.requireNonNull(inAttrs, "inAttrs");
          synchronized (this) {
              ensureValidLocked();
              nativeApplyStyle(mObject, themePtr, defStyleAttr, defStyleRes,
                      parser != null ? parser.mParseState : 0, inAttrs, outValuesAddress,
                      outIndicesAddress);
          }
      }
```
/frameworks/base/core/jni/android_util_AssetManager.cpp
```
 static void NativeApplyStyle(JNIEnv* env, jclass /*clazz*/, jlong ptr, jlong theme_ptr,
                              jint def_style_attr, jint def_style_resid, jlong xml_parser_ptr,
                              jintArray java_attrs, jlong out_values_ptr, jlong out_indices_ptr) {
   ScopedLock<AssetManager2> assetmanager(AssetManagerFromLong(ptr));
   Theme* theme = reinterpret_cast<Theme*>(theme_ptr);
   CHECK(theme->GetAssetManager() == &(*assetmanager));
   (void) assetmanager;
 
   ResXMLParser* xml_parser = reinterpret_cast<ResXMLParser*>(xml_parser_ptr);
   uint32_t* out_values = reinterpret_cast<uint32_t*>(out_values_ptr);
   uint32_t* out_indices = reinterpret_cast<uint32_t*>(out_indices_ptr);
 
   jsize attrs_len = env->GetArrayLength(java_attrs);
   jint* attrs = reinterpret_cast<jint*>(env->GetPrimitiveArrayCritical(java_attrs, nullptr));
   if (attrs == nullptr) {
     return;
   }
   //关键方法 根据attr，使用xmlParser解析
   ApplyStyle(theme, xml_parser, static_cast<uint32_t>(def_style_attr),
              static_cast<uint32_t>(def_style_resid), reinterpret_cast<uint32_t*>(attrs), attrs_len,
              out_values, out_indices);
   env->ReleasePrimitiveArrayCritical(java_attrs, attrs, JNI_ABORT);
 }
```

/frameworks/base/libs/androidfw/AttributeResolution.cpp
```
base::expected<std::monostate, IOError> ApplyStyle(Theme* theme, ResXMLParser* xml_parser,
                                                     uint32_t def_style_attr,
                                                     uint32_t def_style_resid,
                                                     const uint32_t* attrs, size_t attrs_length,
                                                     uint32_t* out_values, uint32_t* out_indices) {
   。。。
    int indices_idx = 0;
    const AssetManager2* assetmanager = theme->GetAssetManager();
  
    // Load default style from attribute, if specified...
    uint32_t def_style_theme_flags = 0U;
    const auto default_style_bag = GetStyleBag(theme, def_style_attr, def_style_resid,
                                               &def_style_theme_flags);
   。。。
    uint32_t xml_style_theme_flags = 0U;
    const auto xml_style_bag = GetXmlStyleBag(theme, xml_parser, &def_style_theme_flags);
    。。。
    BagAttributeFinder def_style_attr_finder(default_style_bag.value_or(nullptr));
    BagAttributeFinder xml_style_attr_finder(xml_style_bag.value_or(nullptr));
    XmlAttributeFinder xml_attr_finder(xml_parser);
    //遍历
    for (size_t ii = 0; ii < attrs_length; ii++) {
      const uint32_t cur_ident = attrs[ii];  
      AssetManager2::SelectedValue value{};
      uint32_t value_source_resid = 0;
      
      const size_t xml_attr_idx = xml_attr_finder.Find(cur_ident);
      if (xml_attr_idx != xml_attr_finder.end()) {
        Res_value attribute_value{};
        xml_parser->getAttributeValue(xml_attr_idx, &attribute_value);
        value.type = attribute_value.dataType;
        value.data = attribute_value.data;
        value_source_resid = xml_parser->getSourceResourceId();
      }
  
      if (value.type == Res_value::TYPE_NULL && value.data != Res_value::DATA_NULL_EMPTY) {
        const ResolvedBag::Entry* entry = xml_style_attr_finder.Find(cur_ident);
        if (entry != xml_style_attr_finder.end()) {
          value = AssetManager2::SelectedValue(*xml_style_bag, *entry);
          value.flags |= xml_style_theme_flags;
          value_source_resid = entry->style;
          。。。
        }
      }
      。。。
      //将最终的值写回Java 
      out_values[STYLE_TYPE] = value.type;
      out_values[STYLE_DATA] = value.data;
      out_values[STYLE_ASSET_COOKIE] = ApkAssetsCookieToJavaCookie(value.cookie);
      out_values[STYLE_RESOURCE_ID] = value.resid;
      out_values[STYLE_CHANGING_CONFIGURATIONS] = value.flags;
      out_values[STYLE_DENSITY] = value.config.density;
      out_values[STYLE_SOURCE_RESOURCE_ID] = value_source_resid;
      。。。。
    }
  
    out_indices[0] = indices_idx;
    return {};
  }
```