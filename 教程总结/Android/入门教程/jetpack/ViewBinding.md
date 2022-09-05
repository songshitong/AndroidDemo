https://juejin.cn/post/7056983987859750919
https://juejin.cn/post/7012645650030395400#heading-1

View Binding
通常的话绑定布局里的 View 实例有哪些办法？又有哪些缺点？

通常做法           缺点
findViewById()   NPE 风险、大量的绑定代码、类型转换危险
@ButterKnifeNPE   风险、额外的注解代码、不适用于多模块项目（APT 工具解析 Library 受限）
KAE 插件         NPE 风险、操作其他布局的风险、Kotlin 语言独占、已经废弃
ViewBinding


使用
```
android {
         viewBinding {
             enabled = true
         }
     }
```
activity中使用
```
//首先声明变量  类型是类名+Binding
 private lateinit var binding: ActivityMainBinding
 
 override fun onCreate(savedInstanceState: Bundle?) {
   super.onCreate(savedInstanceState)
   
   //再通过生成的binding去加载该布局
   binding = ActivityMainBinding.inflate(layoutInflater)
   
   //调用Binding类的getRoot()函数可以得到activity_main.xml中根元素的实例
   val view = binding.root
   
   //将根视图传递到 setContentView()，使其成为屏幕上的活动视图
   setContentView(view)
   
   //使用binding实例获取到控件
   binding.testViewBinding.text = "button"
 }
```
fragment中使用
```
 private var _binding: ResultProfileBinding? = null
 // This property is only valid between onCreateView and
 // onDestroyView.
 private val binding get() = _binding!!
 override fun onCreateView(
  inflater: LayoutInflater,
  container: ViewGroup?,
  savedInstanceState: Bundle?
  ): View? {
    _binding = ResultProfileBinding.inflate(inflater, container, false)
    val view = binding.root
    return view
  }
 override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
```
这个binding变量只有在onCreateView与onDestroyView才是可用的。因为我们fragment的生命周期和activity的不同，
  fragment 可以超出其视图的生命周期，如果不将这里置为空，有可能引起内存泄漏


viewbinding含有include标签中布局的使用
```
 <!--在include标签中添加id属性-->
     <include
         android:id="@+id/title_bar"
         layout="@layout/title_bar"/>
     ...
```
代码使用
```
//直接使用include标签的id，然后再根据include的id引用include布局里面的id
         binding.titleBar.testInclude.text = "hello"
```


viewbinding含有merge的标签的布局中使用
首先是要吧include标签里面的id去掉
```
private lateinit var binding: ActivityMainBinding
//声明变量
private lateinit var titleBarBinding: TitleBarBinding
override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  binding = ActivityMainBinding.inflate(layoutInflater)
  //调用TitleBarBind的bind函数让title_bar.xml和我们的activity_main.xml关联起来
  titleBarBinding = TitleBarBinding.bind(binding.root)
  val view = binding.root
  setContentView(view)
  //直接使用titlrBarBinding变量引用控件
  titleBarBinding.testInclude.text = "button"
}
```


viewbinding含有Adapter中的使用
```
class BindingAdapter(val mData:List<String>): RecyclerView.Adapter<BindingAdapter.MyHolder>() {
  //Myholder接受RvItemBinding参数，RecyclerView.ViewHolder接受的是一个View，通过这个binding.root返回一个root
  inner class MyHolder(binding: RvItemBinding):RecyclerView.ViewHolder(binding.root){
      val textView = binding.textView
   }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
     //首先调用RvItemBinding的inflate函数去加载rv_item.xml的布局
     val binding = RvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
     return MyHolder(binding)
   }

  override fun getItemCount() = mData.size
  
  override fun onBindViewHolder(holder: MyHolder, position: Int) {
     //通过holder直接使用它自己的成员变量
     holder.textView.text = mData[position]
   }
}

```

使用viewBinding比synthetic的好处
synthetic算法是android提供的一个插件实现的。通过在build.gradle添加 apply plugin: 'kotlin-android-extensions'就可以引用。
```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_synthetic)
    //直接使用synthetic_button就可以使用控件
    synthetic_button.text = "hello"
   }
```

将上面的代码反编译成java代码看看
```
public final class SyntheticActivity extends AppCompatActivity {
 //新增一个成员变量
  private HashMap _$_findViewCache;

  protected void onCreate(@Nullable Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     this.setContentView(1300109);
     Button var10000 = (Button)this._$_findCachedViewById(id.synthetic_button);
     Intrinsics.checkExpressionValueIsNotNull(var10000, "synthetic_button");
     var10000.setText((CharSequence)"hello");
  }

  //通过命名一个奇怪的函数名来避免与用户声明的函数重复，并通过这个函数找到我们的view
  public View _$_findCachedViewById(int var1) {
    if (this._$_findViewCache == null) {
     this._$_findViewCache = new HashMap();
    }

    View var2 = (View)this._$_findViewCache.get(var1);
    if (var2 == null) {
     var2 = this.findViewById(var1);
     this._$_findViewCache.put(var1, var2);
   }
    return var2;
  }

  public void _$_clearFindViewByIdCache() {
    if (this._$_findViewCache != null) {
     this._$_findViewCache.clear();
   }
  }
}
```
编译器通过findViewById，自动实现view的绑定
新增一个成员变量来帮助我们实现findViewById的功能。
这无形中增加了我们的内存开销。这是其中一点，还有一点就是它提高了我们程序的不稳定性。使用过这个控件的同事都知道
他通过引入import kotlinx.android.synthetic.main.activity_synthetic.* 来直接使用控件id使用控件。那么就会存在一个问题，
  如果不小心引入其他布局，使用了其他布局的控件，那么这个错误不会在编译时期被发现。是一个运行时的错误。这种运行时的错误就使得我们的程序变得不稳定。
  特别是一旦项目复杂起来，存在很多命名一样的控件，这更加会增大我们程序的不稳定性



DataBinding与ViewBinding的关系
ViewBinding可以做的，DataBinding可以做，更强大
DataBinding的生成的类继承于ViewDataBinding，实现了ViewBinding接口
public abstract class ViewDataBinding extends BaseObservable implements ViewBinding

dataBinding生成文件目录
/build/generated/data_binding_base_class_source_out/debug/out/.../databinding/ActivityMainBinding.java
//todo dataBinding原理 与synthetic的实现对比