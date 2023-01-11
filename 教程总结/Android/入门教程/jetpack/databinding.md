


2022-04-12
https://juejin.cn/post/6844903609079971854

DataBinding 能够省去我们一直以来的 findViewById() 步骤，大量减少 Activity 内的代码，数据能够单向或双向绑定到 layout 文件中，
有助于防止内存泄漏，而且能自动进行空检测以避免空指针异常
可以使用声明性格式（而非程序化地）将布局中的界面组件绑定到应用中的数据源。

使用build.gradle
```
android {
    dataBinding {
        enabled = true   //启用后即便不用默认给module生成一个databindingimpl
    }
}
```
假设存在model
```
public class User {
    private String name;
    private String password;
```
将根本局转为layout  右键根部句Convert to data binding layout
```
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">
    //data 标签用于声明要用到的变量以及变量类型
    <data>
       //model使用1 
          <variable
             alias="TempUser" //定义别名
            name="userInfo"
            type="com.leavesc.databinding_demo.model.User" />
      //model使用2  import导入免去每次指明全部路径
       <import type="com.leavesc.databinding_demo.model.User"/>
        <variable
            name="userInfo"
            type="User"/> 
    </data>

    <android.support.constraint.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".MainActivity">
        //绑定数据
        <TextView
            android:id="@+id/tv_userName"
            ···
            android:text="@{userInfo.name}" />
        
    </android.support.constraint.ConstraintLayout>
</layout>
```
activity中使用   省略setContentView
```
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMain2Binding activityMain2Binding = DataBindingUtil.setContentView(this, R.layout.activity_main2);
        user = new User("leavesC", "123456");
        activityMain2Binding.setUserInfo(user);
    }
```
与viewModel建立关系,将binding的事件传递给viewModel，不然viewmodel接受不到
```
 ActivityLoginBinding binding = ((ActivityLoginBinding) viewDataBinding);
 binding.setLoginVM(loginVm);
```


每个数据绑定布局文件都会生成一个绑定类，ViewDataBinding 的实例名是根据布局文件名来生成，将之改为首字母大写的驼峰命名法来命名，
  并省略布局文件名包含的下划线。控件的获取方式类似，但首字母小写
ba_activity_register.xml 生成的binding为BaActivityRegisterBinding
也可以通过如下方式自定义 ViewDataBinding 的实例名
```
    <data class="CustomBinding">

    </data>
```
Fragment使用dataBinding
```
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBlankBinding fragmentBlankBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_blank, container, false);
        fragmentBlankBinding.setHint("Hello");
        return fragmentBlankBinding.getRoot();
    }
```

默认值方便查看布局
默认值将只在预览视图中显示，且默认值不能包含引号
```
	android:text="@{userInfo.name,default=defaultValue}"
```



单向数据绑定
实现数据变化自动驱动 UI 刷新的方式有三种：BaseObservable、ObservableField、ObservableCollection
BaseObservable
数据变更后 UI 会即时刷新
BaseObservable 提供了 notifyChange() 和 notifyPropertyChanged() 两个方法，前者会刷新所有的值域，后者则只更新对应 BR 的 flag，
  该 BR 的生成通过注释 @Bindable 生成，可以通过 BR notify 特定属性关联的视图
```
public class Goods extends BaseObservable {

    //如果是 public 修饰符，则可以直接在成员变量上方加上 @Bindable 注解
    @Bindable
    public String name;

    //如果是 private 修饰符，则在成员变量的 get 方法上添加 @Bindable 注解
    private String details;

    private float price;

    public Goods(String name, String details, float price) {
        this.name = name;
        this.details = details;
        this.price = price;
    }

    public void setName(String name) {
        this.name = name;
        //只更新本字段
        notifyPropertyChanged(com.leavesc.databinding_demo.BR.name);
    }

    @Bindable
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
        //更新所有字段
        notifyChange();
    }
}
```
实现了 Observable 接口的类允许注册一个监听器，当可观察对象的属性更改时就会通知这个监听器，此时就需要用到 OnPropertyChangedCallback
当中 propertyId 就用于标识特定的字段
```
        goods.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == com.leavesc.databinding_demo.BR.name) {
                    Log.e(TAG, "BR.name");
                } else if (propertyId == com.leavesc.databinding_demo.BR.details) {
                    Log.e(TAG, "BR.details");
                } else if (propertyId == com.leavesc.databinding_demo.BR._all) {
                    Log.e(TAG, "BR._all");
                } else {
                    Log.e(TAG, "未知");
                }
            }
        });

```
ObservableField
ObservableField 可以理解为官方对 BaseObservable 中字段的注解和刷新等操作的封装，官方原生提供了对基本数据类型的封装，
例如 ObservableBoolean、ObservableByte、ObservableChar、ObservableShort、ObservableInt、ObservableLong、ObservableFloat、
ObservableDouble 以及 ObservableParcelable ，也可通过 ObservableField 泛型来申明其他类型
```
public class ObservableGoods {

    private ObservableField<String> name;

    private ObservableFloat price;

    private ObservableField<String> details;

    public ObservableGoods(String name, float price, String details) {
        this.name = new ObservableField<>(name);
        this.price = new ObservableFloat(price);
        this.details = new ObservableField<>(details);
    }
}
```
ObservableCollection
DataBinding 也提供了包装类用于替代原生的 List 和 Map，分别是 ObservableList 和 ObservableMap,当其包含的数据发生变化时，
  绑定的视图也会随之进行刷新

dataBinding 也支持在布局文件中使用 数组、Lsit、Set 和 Map，且在布局文件中都可以通过 list[index] 的形式来获取元素
而为了和 variable 标签的尖括号区分开，在声明 Lsit< String > 之类的数据类型时，需要使用尖括号的转义字符
```
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <import type="android.databinding.ObservableList"/>
        <import type="android.databinding.ObservableMap"/>
        <variable
            name="list"
            type="ObservableList&lt;String&gt;"/>
        <variable
            name="map"
            type="ObservableMap&lt;String,String&gt;"/>
        <variable
            name="index"
            type="int"/>
        <variable
            name="key"
            type="String"/>
    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        tools:context="com.leavesc.databinding_demo.Main12Activity">
        <TextView
            ···
            android:padding="20dp"
            android:text="@{list[index],default=xx}"/>
        <TextView
            ···
            android:layout_marginTop="20dp"
            android:padding="20dp"
            android:text="@{map[key],default=yy}"/>
    </LinearLayout>
</layout>
```


双向数据绑定
双向绑定的意思即为当数据改变时同时使视图刷新，而视图改变时也可以同时改变数据
绑定变量的方式比单向绑定多了一个等号： android:text="@={goods.name}"
```
<EditText
            ···
            android:text="@={goods.name}" />
```



事件绑定
严格意义上来说，事件绑定也是一种变量绑定，只不过设置的变量是回调接口而已
事件绑定可用于以下多种回调事件
android:onClick
android:onLongClick
android:afterTextChanged
android:onTextChanged
...

点击的另一种
```
android:onClick="@{loginVM.onSmsBtnClick}"
  public void onSmsBtnClick(View v) {
        Toast.makeText(getApplication(),"111",Toast.LENGTH_SHORT);
        Log.d("AHLoginViewModel","l");
    }
    
```
在 Activity 内部新建一个 UserPresenter 类来声明 onClick() 和 afterTextChanged() 事件相应的回调方法
```
public class UserPresenter {

        public void onUserNameClick(User user) {
            Toast.makeText(Main5Activity.this, "用户名：" + user.getName(), Toast.LENGTH_SHORT).show();
        }
}

//xml使用
 <data>
        <import type="com.leavesc.databinding_demo.model.User" />
        <import type="com.leavesc.databinding_demo.MainActivity.UserPresenter" />
        <variable
            name="userInfo"
            type="User" />
        <variable
            name="userPresenter"
            type="UserPresenter" />
    </data>


 <TextView
            ···
            android:onClick="@{()->userPresenter.onUserNameClick(userInfo)}"
            android:text="@{userInfo.name}" />
```
可以使用方法签名@{userPresenter.afterTextChanged}
可以使用方法引用::  @{UserPresenter::onUserNameClick}
可以使用lambda形式 @{()->userPresenter.onUserNameClick(userInfo)}

其他事件
```
android:afterTextChanged="@{loginVM.onSmsCodeChange}"
  public void onSmsCodeChange(Editable s) {
        Toast.makeText(getApplication(),s.toString(),Toast.LENGTH_SHORT);
        Log.d("AHLoginViewModel",s.toString());
    }

android:onTextChanged="@{loginVM.onTextChanged}"    
public void onTextChanged(CharSequence s, int start, int before, int count) {
          Log.d("AHLoginViewModel",s.toString());
       }        
```





使用类方法
```
导入工具类
<import type="com.leavesc.databinding_demo.StringUtils" />
<TextView
     android:layout_width="match_parent"
     android:layout_height="wrap_content"
     android:text="@{StringUtils.toUpperCase(userInfo.name)}" />
```


data binding支持运算符
基础运算符
算术  +  -  /  *  %
字符串合并  +
逻辑  &&  ||
二元  &  |  ^
一元  +  -  !  ~
移位 >>  >>>  <<
比较 ==  >  <  >=  <=
Instanceof
Grouping ()
character, String, numeric, null
Cast
方法调用
Field 访问
Array 访问 []
三元 ?:

目前不支持以下操作
this
super
new
显示泛型调用

空合并运算符 ??   Null Coalescing
会取第一个不为 null 的值作为返回值
```
<TextView
     android:layout_width="match_parent"
     android:layout_height="wrap_content"
     android:text="可见性变化"
     android:visibility="@{user.male  ? View.VISIBLE : View.GONE}" />

<TextView
     android:layout_width="match_parent"
     android:layout_height="wrap_content"
     android:text="@{user.name ?? user.password}" />
```
避免空指针异常
DataBinding 也会自动帮助我们避免空指针异常
例如，如果 "@{userInfo.password}" 中 userInfo 为 null 的话，userInfo.password 会被赋值为默认值 null，而不会抛出空指针异常




绑定适配器 BindingAdapter
dataBinding 提供了 BindingAdapter 这个注解用于支持自定义属性，或者是修改原有属性。注解值可以是已有的 xml 属性，
   例如 android:src、android:text等，也可以自定义属性然后在 xml 中使用
例如，对于一个 ImageView ，我们希望在某个变量值发生变化时，可以动态改变显示的图片，此时就可以通过 BindingAdapter 来实现
代码声明
```
    @BindingAdapter({"url"})
    public static void loadImage(ImageView view, String url) {
        Log.e(TAG, "loadImage url : " + url);
    }
  
kotlin
 @BindingAdapter(value = ["colorCircleViewColor"])
 @JvmStatic
 fun colorCircleViewColor(view: MyColorCircleView, color: Int) {
    view.setView(color)
 } 
```
xml使用，bind:url=""
```
<ImageView
            android:id="@+id/image"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:src="@drawable/ic_launcher_background"
            bind:url="@{image.url}" />
```
BindingAdapter 可以覆盖 Android 原先的控件属性。例如，可以设定每一个 Button 的文本都要加上后缀：“-Button”
```
    @BindingAdapter("android:text")
    public static void setText(Button view, String text) {
        view.setText(text + "-Button");
    }
    xml:
    <Button
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:onClick="@{()->handler.onClick(image)}"
       android:text='@{"改变图片Url"}'/>
```
这样，整个工程中使用到了 "android:text" 这个属性的控件，其显示的文本就会多出一个后缀"-Button"



BindingConversion
DataBinding 还支持对数据进行转换，或者进行类型转换    BindingConversion还实现BindingAdapter类似的功能
代码声明将String转为Drawable
```
@BindingConversion
    public static Drawable convertStringToDrawable(String str) {
        if (str.equals("红色")) {
            return new ColorDrawable(Color.parseColor("#FF4081"));
        }
        if (str.equals("蓝色")) {
            return new ColorDrawable(Color.parseColor("#3F51B5"));
        }
        return new ColorDrawable(Color.parseColor("#344567"));
    }
 
 @BindingConversion
    public static int convertStringToColor(String str) {
        if (str.equals("红色")) {
            return Color.parseColor("#FF4081");
        }
        if (str.equals("蓝色")) {
            return Color.parseColor("#3F51B5");
        }
        return Color.parseColor("#344567");
    }
```
xml使用
```
 <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background='@{"红色"}'
            android:padding="20dp"
            android:text="红色背景蓝色字"
            android:textColor='@{"蓝色"}'/>
```
颜色绑定
https://stackoverflow.com/questions/39910832/set-text-color-using-data-binding-on-android
<data>
<import type="androidx.core.content.ContextCompat" />
</data>
android:textColor="@{ContextCompat.getColor(context, data.colorRes)}"/>


资源引用
DataBinding 支持对尺寸和字符串这类资源的访问
Dimens.xml  定义尺寸
```
    <dimen name="paddingBig">190dp</dimen>
    <dimen name="paddingSmall">150dp</dimen>
```
Strings.xml 定义string资源
```
    <string name="format">%s is %s</string>
```
xml使用
```
    <data>
        <variable
            name="flag"
            type="boolean" />
    </data>       
	<Button
         android:layout_width="match_parent"
         android:layout_height="wrap_content"
         android:paddingLeft="@{flag ? @dimen/paddingBig:@dimen/paddingSmall}"
         android:text='@{@string/format("leavesC", "Ye")}'
         android:textAllCaps="false" />
```

