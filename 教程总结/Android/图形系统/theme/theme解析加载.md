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



https://www.jianshu.com/p/a021e628c1bf
view的主题加载
以button为例
Sdk\sources\android-32\android\widget\Button.java
```
public Button(Context context) {
        this(context, null);
    }
public Button(Context context, AttributeSet attrs) {
    //默认的button样式buttonStyle
    this(context, attrs, com.android.internal.R.attr.buttonStyle);
}
public Button(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
}  
public Button(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
}      
```


AttributeSet：在XML中明确写出了的属性集合
attrs 自定义的属性写在declare-styleable中
defStyleAttr：这是一个定义在attrs.xml文件中的attribute。这个值起作用需要两个条件：1. 值不为0；2. 在Theme中使用了（出现即可）
defStyleRes：这是在styles.xml文件中定义的一个style。只有当defStyleAttr没有起作用，才会使用到这个值。

属性查找
framework中 frameworks\base\core\res\res\values目录下的attrs.xml、styles.xml、themes.xml三个
或者sdk   Sdk\platforms\android-32\data\res\values\attrs.xml
```
 <attr name="buttonStyle" format="reference" />
```
theme中使用
themes.xml文件下，有这样一个style：
```
<item name="buttonStyle">@style/Widget.Button</item>
```
style查看
Sdk\platforms\android-32\data\res\values\styles.xml
```
    <style name="Widget.Button">
        <item name="background">@drawable/btn_default</item>
        <item name="focusable">true</item>
        <item name="clickable">true</item>
        <item name="textAppearance">?attr/textAppearanceSmallInverse</item>
        <item name="textColor">@color/primary_text_light</item>
        <item name="gravity">center_vertical|center_horizontal</item>
    </style>
//themes.xml
<item name="textAppearanceSmallInverse">@style/TextAppearance.Small.Inverse</item>
//styles.xml
<style name="TextAppearance.Small.Inverse">
    <item name="textColor">?textColorSecondaryInverse</item>
    <item name="textColorHint">?textColorHintInverse</item>
    <item name="textColorHighlight">?textColorHighlightInverse</item>
    <item name="textColorLink">?textColorLinkInverse</item>
</style>   
```

Sdk\sources\android-32\android\widget\TextView.java
```
  public TextView(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
 TypedArray a = theme.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.TextViewAppearance, defStyleAttr, defStyleRes);

}        
```
TextViewAppearance的属性
```
Sdk\platforms\android-32\data\res\values\attrs.xml
  <declare-styleable name="TextViewAppearance">
        <attr name="textAppearance" />
    </declare-styleable>
    
Sdk\platforms\android-32\data\res\values\themes.xml 
        <item name="textAppearance">@style/TextAppearance</item>

Sdk\platforms\android-32\data\res\values\styles.xml 
 <style name="TextAppearance">
        <item name="textColor">?textColorPrimary</item>
        <item name="textColorHighlight">?textColorHighlight</item>
        <item name="textColorHint">?textColorHint</item>
        <item name="textColorLink">?textColorLink</item>
        <item name="textSize">16sp</item>
        <item name="textStyle">normal</item>
    </style>  
```
通过样式对比button重写了TextView的textAppearance和textColor
Sdk\sources\android-32\android\widget\TextView.java
TextView的构造器读取主题中属性然后进行配置
```
 a = theme.obtainStyledAttributes(
                    attrs, com.android.internal.R.styleable.TextView, defStyleAttr, defStyleRes);
        saveAttributeDataForStyleable(context, com.android.internal.R.styleable.TextView, attrs, a,
                defStyleAttr, defStyleRes);
        ...
        int n = a.getIndexCount();

        boolean textIsSetFromXml = false;
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.TextView_editable:
                    editable = a.getBoolean(attr, editable);
                    break;
                case com.android.internal.R.styleable.TextView_inputMethod:
                    inputMethod = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.TextView_numeric:
                    numeric = a.getInt(attr, numeric);
                    break;                  
             ....
```