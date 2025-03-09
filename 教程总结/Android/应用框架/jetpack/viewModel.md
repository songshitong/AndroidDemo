https://github.com/xfhy/Android-Notes/blob/master/Blogs/Android/%E7%B3%BB%E7%BB%9F%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90/ViewModel_%E4%BD%BF%E7%94%A8%E5%8F%8A%E5%8E%9F%E7%90%86%E8%A7%A3%E6%9E%90.md

androidx.lifecycle:lifecycle-runtime:2.3.1

原理总结
ViewModelStore使用HashMap保存ViewModel
ViewModelStoreOwner持有ViewModelStore，进行ViewModelStore的新建与保存  一般是ComponentActivity.getViewModelStore
  存储HashMap中，key为DEFAULT_KEY + ":" + canonicalName
ViewModelProvider.get() 从ViewModelStore获取ViewModel，如果没有使用Factory新建后保存在ViewModelStore
NewInstanceFactory使用反射modelClass.newInstance构建无参ViewModel  限制了ViewModel的构造参数
AndroidViewModelFactory使用反射构建需要Application的ViewModel
   modelClass.getConstructor(Application.class).newInstance(mApplication)

保存数据的原理
使用NonConfigurationInstances保存mViewModelStore，重写了onRetainNonConfigurationInstance方法
   这个方法可以保存自定义的Object
当ViewModelStore为null时从getLastNonConfigurationInstance恢复
onRetainNonConfigurationInstance 与onSaveInstanceState类似，返回一个Object进行状态保存而不是bundle
类似的getLastNonConfigurationInstance从一个Object恢复状态，而onRestoreInstanceState从bundle恢复状态


数据清理
ComponentActivity通过lifecycle感知生命周期，当销毁时调用ViewModelStore的clear方法进而调用所有ViewModel的clear

//todo 画一个图

1.概述
ViewModel旨在以生命周期意识的方式存储和管理用户界面相关的数据,它可以用来管理Activity和Fragment中的数据.
  还可以拿来处理Fragment与Fragment之间的通信等等.
当Activity或者Fragment创建了关联的ViewModel,那么该Activity或Fragment只要处于活动状态,那么该ViewModel就不会被销毁,
  即使是该Activity屏幕旋转时重建了.所以也可以拿来做数据的暂存.
ViewModel主要是拿来获取或者保留Activity/Fragment所需要的数据的,开发者可以在Activity/Fragment中观察ViewModel中的数据更改
  (这里需要配合LiveData食用).

ps: ViewModel只是用来管理UI的数据的,千万不要让它持有View、Activity或者Fragment的引用(小心内存泄露)。

2.使用
2.1定义一个User数据类
```
class User implements Serializable {

    public int age;
    public String name;

    public User(int age, String name) {
        this.age = age;
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "age=" + age +
                ", name='" + name + '\'' +
                '}';
    }
}
```
2.2定义ViewModel
```
public class UserModel extends ViewModel {

    public final MutableLiveData<User> mUserLiveData = new MutableLiveData<>();

    public UserModel() {
        //模拟从网络加载用户信息
        mUserLiveData.postValue(new User(1, "name1"));
    }
    
    //模拟 进行一些数据骚操作
    public void doSomething() {
        User user = mUserLiveData.getValue();
        if (user != null) {
            user.age = 15;
            user.name = "name15";
            mUserLiveData.setValue(user);
        }
    }
}
```
2.3 这时候在Activity中就可以使用ViewModel了. 其实就是一句代码简单实例化,然后就可以使用ViewModel了.
```
//这些东西我是引入的androidx下面的
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends FragmentActivity {

    private TextView mContentTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContentTv = findViewById(R.id.tv_content);

        //构建ViewModel实例
        final UserModel userModel =new ViewModelProvider(this,new ViewModelProvider.NewInstanceFactory()).get(UserModel.class);

        //让TextView观察ViewModel中数据的变化,并实时展示
        userModel.mUserLiveData.observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                mContentTv.setText(user.toString());
            }
        });

        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击按钮  更新User数据  观察TextView变化
                userModel.doSomething();
            }
        });
    }
}
```
这个时候,我们点击一下按钮(user中的age变为15),我们可以旋转手机屏幕(这个时候其实Activity是重新创建了,也就是onCreate()方法被再次调用,
 但是ViewModel其实是没有重新创建的,还是之前那个ViewModel),但是当我们旋转之后,发现TextView上显示的age居然还是15,,,,
 这就是ViewModel的魔性所在.这个就不得不提ViewModel的生命周期了,它只有在Activity销毁之后,它才会自动销毁(所以别让ViewModel持有Activity引用啊,会内存泄露的).
下面引用一下谷歌官方的图片,将ViewModel的生命周期展示的淋漓尽致.
android_jetpack_viewModel生命周期.png

3. ViewModel妙用1: Activity与Fragment"通信"
   有了ViewModel,Activity与Fragment可以共享一个ViewModel,因为Fragment是依附在Activity上的,在实例化ViewModel时将该Activity传入ViewModelProviders,
   它会给你一个该Activity已创建好了的ViewModel,这个Fragment可以方便的访问该ViewModel中的数据.在Activity中修改userModel数据后,
   该Fragment就能拿到更新后的数据.
```
public class MyFragment extends Fragment {
     public void onStart() {
        //这里拿到的ViewModel实例,其实是和Activity中创建的是一个实例
         UserModel userModel = new ViewModelProvider(getActivity(),new ViewModelProvider.NewInstanceFactory()).get(UserModel.class);
     }
 }
```   

4. ViewModel妙用2: Fragment与Fragment"通信"
   下面我们来看一个例子(Google官方例子)
```
public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Item> selected = new MutableLiveData<Item>();

    public void select(Item item) {
        selected.setValue(item);
    }

    public LiveData<Item> getSelected() {
        return selected;
    }
}


public class MasterFragment extends Fragment {
    private SharedViewModel model;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(getActivity,new ViewModelProvider.NewInstanceFactory()).get(SharedViewModel.class);
        itemSelector.setOnClickListener(item -> {
            model.select(item);
        });
    }
}

public class DetailFragment extends Fragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedViewModel model = new ViewModelProvider(getActivity(),new ViewModelProvider.NewInstanceFactory()).get(SharedViewModel.class);
        model.getSelected().observe(this, { item ->
           // Update the UI.
        });
    }
}
``` 
1 首先定义一个ViewModel,在里面放点数据
2 然后在MasterFragment和DetailFragment都可以拿到该ViewModel,拿到了该ViewModel就可以拿到里面的数据了,
  相当于间接通过ViewModel通信了. so easy....


二、ViewModel源码解析
又到了我们熟悉的源码解析环节
我们从下面这句代码start.
```
final UserModel userModel = new ViewModelProvider(this,new ViewModelProvider.NewInstanceFactory()).get(UserModel.class);
```
先看构造器
```
 //构建一个ViewModelProvider,当Activity是alive时它会保留所有的该Activity对应的ViewModels
 public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }
   public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        mFactory = factory;
        mViewModelStore = store;
    }   
```

后面先分析Factory后分析ViewModelStore

Factory是ViewModelProvider的一个内部接口,它的实现类是拿来构建ViewModel实例的.它里面只有一个方法,就是创建一个ViewModel.
```
public interface Factory {
        //可以看到返回的是ViewModel的子类
        <T extends ViewModel> T create(@NonNull Class<T> modelClass);
    }
```
Factory的实现类
1.NewInstanceFactory 构造无参ViewModel
```
public static class NewInstanceFactory implements Factory {
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            try {
                return modelClass.newInstance();
            } catch (InstantiationException e) {
             ....
        }
    }
```
NewInstanceFactory专门用来实例化那种构造方法里面没有参数的class,并且ViewModel里面是不带Context的,然后它是通过newInstance()去实例化的.

2.AndroidViewModelFactory    
```
public static class AndroidViewModelFactory extends ViewModelProvider.NewInstanceFactory {
        private static AndroidViewModelFactory sInstance;
        public static AndroidViewModelFactory getInstance(@NonNull Application application) {
            if (sInstance == null) {
                sInstance = new AndroidViewModelFactory(application);
            }
            return sInstance;
        }

        private Application mApplication;

        public AndroidViewModelFactory(@NonNull Application application) {
            mApplication = application;
        }

        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (AndroidViewModel.class.isAssignableFrom(modelClass)) {
                try {
                    return modelClass.getConstructor(Application.class).newInstance(mApplication);
                } catch (NoSuchMethodException e) {
               ...
            }
            return super.create(modelClass);
        }
    }
```
AndroidViewModel是ViewModel的子类，构造器参数是Application,可以通过AndroidView获取application

AndroidViewModelFactory专门用来实例化那种构造方法里面有参数的class,并且ViewModel里面可能是带Application的.
 1 它是通过newInstance(application)去实例化的.如果有带application参数则是这样实例化
 2 如果没有带application参数的话,则还是会走newInstance()方法去构建实例.
AndroidViewModelFactory通过构造方法给ViewModel带入Application,就可以在ViewModel里面拿到Context,因为Application是APP全局的,
  那么不存在内存泄露的问题.完美解决了有些ViewModel里面需要Context引用,但是又担心内存泄露的问题.

3. KeyedFactory  抽象类，根据key新建ViewModel
```
abstract static class KeyedFactory implements Factory {
        @NonNull
        public abstract <T extends ViewModel> T create(@NonNull String key,
                @NonNull Class<T> modelClass);

        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            throw new UnsupportedOperationException("create(String, Class<?>) must be called on "
                    + "implementaions of KeyedFactory");
        }
    }
```

ViewModelStore 相关    
  ViewModelStore其实就是一个普普通通的使用HashMap保存ViewModel的类
```
public class ViewModelStore {
    private final HashMap<String, ViewModel> mMap = new HashMap<>();
    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.put(key, viewModel);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
    }

    final ViewModel get(String key) {
        return mMap.get(key);
    }

    Set<String> keys() {
        return new HashSet<>(mMap.keySet());
    }

    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.clear();
        }
        mMap.clear();
    }
}
```

ViewModelStore的获取
```
ViewModelStoreOwner.getViewModelStore()
```
ViewModelStoreOwner是ViewModelStore的拥有者，负责在configuration改变时持有ViewModelStore，并在其destroyed时清空ViewModelStore
  一般实现是androidx.activity.ComponentActivity和androidx.fragment.app.Fragment
//todo 分析fragment的实现
以ComponentActivity为例
```
 //获取这个Activity相关联的ViewModelStore
 public ViewModelStore getViewModelStore() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mViewModelStore == null) {
           //获取最近一次横竖屏切换时保存下来的数据
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                // Restore the ViewModelStore from NonConfigurationInstances
                mViewModelStore = nc.viewModelStore;
            }
            if (mViewModelStore == null) {
                mViewModelStore = new ViewModelStore();
            }
        }
        return mViewModelStore;
    }
    
  static final class NonConfigurationInstances {
        Object custom;
        ViewModelStore viewModelStore;
    }   
```

Android横竖屏切换时会触发onSaveInstanceState()，而还原时会调用onRestoreInstanceState()，但是Android的Activity类
还有2个方法名为onRetainNonConfigurationInstance()和getLastNonConfigurationInstance()这两个方法。
来具体看看这2个素未谋面的方法
```
//保留所有acitivity的状态。你不能自己覆写它！
public final Object onRetainNonConfigurationInstance() {
        //这个自己定义方法即将被废弃，如果要保留自己的状态，请使用ViewModel
        Object custom = onRetainCustomNonConfigurationInstance();

        ViewModelStore viewModelStore = mViewModelStore;
        if (viewModelStore == null) {
            //如果viewModelStore为空，获取最近的nc，更新viewModelStore
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                viewModelStore = nc.viewModelStore;
            }
        }

        if (viewModelStore == null && custom == null) {
            return null;
        }

        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.custom = custom;
        nci.viewModelStore = viewModelStore;
        return nci;
    }
    
//这个方法在android.app.Activity里面,而mLastNonConfigurationInstances.activity实际就是就是上面方法中的nc
public Object getLastNonConfigurationInstance() {
    return mLastNonConfigurationInstances != null
            ? mLastNonConfigurationInstances.activity : null;
}    
```
//todo getLastNonConfigurationInstance原理  activity相关 重建 销毁
onRetainNonConfigurationInstance 与onSaveInstanceState类似，返回一个Object进行状态保存而不是bundle
类似的getLastNonConfigurationInstance从一个Object恢复状态，而onRestoreInstanceState从bundle恢复状态

没想到吧,Activity在横竖屏切换时悄悄保存了viewModelStore,放到了NonConfigurationInstances实例里面,横竖屏切换时保存了又恢复了回来,
  相当于ViewModel实例就还在啊,也就避免了横竖屏切换时的数据丢失.



viewModelProvider.get(UserModel.class)
下面我们来到那句构建ViewModel代码的后半段,它是ViewModelProvider的get()方法,看看实现,其实很简单
```
private static final String DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey";
  public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }

public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        //有缓存则用缓存
        ViewModel viewModel = mViewModelStore.get(key);
        if (modelClass.isInstance(viewModel)) {
            return (T) viewModel;
        } else {
            //noinspection StatementWithEmptyBody
            if (viewModel != null) {
            }
        }
        //无缓存  则重新通过mFactory构建
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) (mFactory)).create(key, modelClass);
        } else {
            viewModel = (mFactory).create(modelClass);
        }
        //缓存起来
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }    
```
大体思路是利用一个key来缓存ViewModel,有缓存则用缓存的,没有则重新构建.构建时使用的factory是上面ViewModelProvider的那个factory.


ViewModel.onCleared() 资源回收
既然ViewModel是生命周期感知的,那么何时应该清理ViewModel呢?
```
public ComponentActivity() {
 ...
         getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!isChangingConfigurations()) {
                        getViewModelStore().clear();
                    }
                }
            }
        });
 ...       
}
```
当activity处于destroy,并且没有处在重建中



再看 ViewModel
很多朋友可能就要问了,ViewModel到底是什么?
```
public abstract class ViewModel {
    // Can't use ConcurrentHashMap, because it can lose values on old apis (see b/37042460)
    //存储值的map
    private final Map<String, Object> mBagOfTags = new HashMap<>();
    private volatile boolean mCleared = false;
    protected void onCleared() {
    }

    @MainThread
    final void clear() {
        mCleared = true;
        if (mBagOfTags != null) {
            synchronized (mBagOfTags) {
                for (Object value : mBagOfTags.values()) {
                    //如果value是closeble语法的实现，在clear时进行清空 
                    closeWithRuntimeException(value);
                }
            }
        }
        onCleared();
    }

    <T> T setTagIfAbsent(String key, T newValue) {
        T previous;
        synchronized (mBagOfTags) {
            //noinspection unchecked
            previous = (T) mBagOfTags.get(key);
            if (previous == null) {
                mBagOfTags.put(key, newValue);
            }
        }
        T result = previous == null ? newValue : previous;
        if (mCleared) {
            // It is possible that we'll call close() multiple times on the same object, but
            // Closeable interface requires close method to be idempotent:
            // "if the stream is already closed then invoking this method has no effect." (c)
            closeWithRuntimeException(result);
        }
        return result;
    }

    <T> T getTag(String key) {
        //noinspection unchecked
        synchronized (mBagOfTags) {
            return (T) mBagOfTags.get(key);
        }
    }

    private static void closeWithRuntimeException(Object obj) {
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
```
其实很简单,就一个抽象类,里面就一个空方法??? 我擦,搞了半天,原来ViewModel不是主角....


AndroidViewModel
ViewModel有一个子类,是AndroidViewModel.它里面有一个Application的属性,仅此而已,为了方便在ViewModel里面使用Context.
```
public class AndroidViewModel extends ViewModel {
    @SuppressLint("StaticFieldLeak")
    private Application mApplication;

    public AndroidViewModel(@NonNull Application application) {
        mApplication = application;
    }

    /**
     * Return the application.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @NonNull
    public <T extends Application> T getApplication() {
        //noinspection unchecked
        return (T) mApplication;
    }
}
```



小结
ViewModel 的源码其实不多，理解起来比较容易，主要是官方FragmentActivity提供了技术实现，onRetainNonConfigurationInstance（）保存状态，
   getLastNonConfigurationInstance()恢复。

原来Activity还有这么2个玩意儿，之前我还只是知道onSaveInstanceState()和onRestoreInstanceState()，涨姿势了
