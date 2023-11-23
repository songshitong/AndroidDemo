https://juejin.cn/post/7029344208234217485

已知的问题
1 android12 指定数据库目录在storage/0/非包名，插入数据后读取不到 版本3.3.1
2 监听数据库异常便于查询问题
  store.setDbExceptionListener
3 findFirst() 返回类型为nullable，如何得知是数据不存在还是数据库本身异常
  如果需要区分才能进行下一步  例如数据不存在执行下一步，异常不执行。。。，就没有办法区分了
  3.3版本内部各种抛出异常，需要进行try catch
4 unique字段
  由数据库字段unique保证唯一性，而不是由代码查询是否存在，然后执行什么操作   很明显代码稳定性不高，需要处理各种场景

注解与实体类
```
@Entity
data class BaseBean(
    @Id
    var id:Long = 0,
    var name: String? = null
)
```
1 必须有构造器，@id注解
2 构造器最好有默认值，For Kotlin data classes this can be achieved by adding default values for all parameters. 
(Technically this is only required if adding properties to the class body, like custom or transient properties or relations,
but it's a good idea to do it always.)
3 数据库升级时新增的字段需要声明为可空的，旧版本没有该字段，声明为非空的升级后会异常闪退


生成：Build > Make project
查看： app > objectbox-models
https://www.jianshu.com/p/57d5db61fc77
@Entity：对象持久化；
@Id：这个对象的主键,默认情况下，id是会被objectbox管理的，也就是自增id。手动管理id需要在注解的时候加上@Id(assignable = true)。
  当你在自己管理id的时候如果超过long的最大值，objectbox 会报错；id的值不能为负数；当id等于0时objectbox会认为这是一个新的实体对象,
  因此会新增到数据库表中；
@Index：这个对象中的索引。经常大量进行查询的字段创建索引，用于提高查询性能；
@Transient：某个字段不想被持久化，可以使用此注解，字段将不会保存到数据库；
@NameInDb：数据库中的字段自定义命名；
@ToOne：做一对一的关联注解 ，此外还有一对多，多对多的关联，例如Class的示例； ToOne<Customer>
@ToMany：做一对多的关联注解；  ToMany<Order>
@Backlink：表示反向关联。
@Unique：被标识的字段必须唯一，2.0+支持。在存入过程中，如果被@Unique标识的字段重复则会抛出UniqueViolationException异常。
```
https://docs.objectbox.io/entity-annotations
@Unique(onConflict = ConflictStrategy.REPLACE) 指明冲突时的策略   FAIL/REPLACE
The REPLACE strategy will add a new object with a different ID. As relations (ToOne/ToMany) reference objects by ID, 
if the previous object was referenced in any relations, these need to be updated manually.

//unique标识的需要进行try catch
try {
    box.put(User("Sam Flynn"))
} catch (e: UniqueViolationException) {
    // A User with that name already exists.
}
```


注意：
ObjectBox不支持List<String>,这样是一对多的关系，直接多条记录就可以
构造器需要时空的或者所有参数的并且有默认值的


数据库查看ObjectBox Admin   通知栏中存在前台服务
https://docs.objectbox.io/data-browser
debugImplementation "io.objectbox:objectbox-android-objectbrowser:$objectboxVersion"
apply(plugin = "io.objectbox") 放在dependen的后面
```
if (BuildConfig.DEBUG) { 
            val started = AndroidObjectBrowser(boxStore).start(context)
        }
```
初始化
```
applicaiton中
MyObjectBox.builder().androidContext(context.applicationContext).build()
```

核心API： https://www.jianshu.com/p/57d5db61fc7
MyObjectBox: 基于您的实体类生成，MyObjectBox 提供一个构建器为您的应用程序设置一个 BoxStore。
BoxStore: 使用 ObjectBox.BoxStore 的入口点是到数据库的直接接口，并管理 Boxes。
Box: 保存一个盒子并查询实体。对于每个实体，有一个 Box (由 BoxStore 提供)。


查询
```
//查询数量
inline fun <reified T> count(): Long =Store.box.boxFor(T::class.java).count()
//根据id查询
inline fun <reified T> get(id: Long): T = Store.box.boxFor(T::class.java).get(id)

 inline fun <reified T> getAll(): List<T> {
        return try {
            Store.box.boxFor(T::class.java).all
        }catch (e:RuntimeException){
            arrayListOf()
        }catch (e:java.lang.IllegalArgumentException){
            arrayListOf()
        }
    }
//根据某些条件查询
 inline fun <reified T> query(propertyQueryCondition: PropertyQueryCondition<T>): List<T> {
        var info: ArrayList<T> = ArrayList<T>()
        Store.box.boxFor(T::class.java).query(propertyQueryCondition).build().apply {
            info.addAll(find())
            close()
        }
        return info
    }   

int属性查询    
box.query().`in`(XX_.status, intArrayOf(1)).order(XX_.startTime).build().find(0,10) //查询前10条    
```
属性查询 查询某个属性的最大值，类似还有min,sum(),avg(),count()
```
Store.box.boxFor(T::class.java).query().build().property(XX_.startTime).max()
```

新增/更新
```
inline fun <reified T> put(bean: T) {
        try {
            Store.box.boxFor(T::class.java).put(bean)
        } catch (e: IllegalArgumentException) {
            toast("IllegalArgumentException")
            e.printStackTrace()
        }
    }

    inline fun <reified T> put(bean: List<T>) =
        Store.box.boxFor(T::class.java).put(bean)
```
删除
```
 inline fun <reified T> remove(id: Long) {
        try {
            Store.box.boxFor(T::class.java).remove(id)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    inline fun <reified T> remove(id: List<Long>) {
        try {
            Store.box.boxFor(T::class.java).removeByIds(id)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }


    inline fun <reified T> removeAll() {
        try {
            Store.box.boxFor(T::class.java).removeAll()
        }catch (e:RuntimeException){
            e.printStackTrace()
        }catch (e:java.lang.IllegalArgumentException){
            e.printStackTrace()
        }
    }
```

事务相关
```
runInTx: Runs the given runnable inside a transaction.
runInReadTx  多个读可以同时进行
runInTxAsync  异步事务
callInTx 类似runInTx，但是有返回值 
```

对响应式的支持 subscribe()方法  Task_是实体类Task生成的，实现接口EntityInfo<Task>
```
taskBox.query().equal(Task_.complete, false).build().subscribe(subscriptions).on(AndroidScheduler.mainThread()).observer(data -> updateUi(data));
线程切换  subscribe()在子线程执行，on切换为主线程
.subscribe().on(AndroidScheduler.mainThread()).observer(data -> updateUi(data));
 boxStore.subscribe(Task.class).observer(observer);//数据改变的监听
```
对LiveData的支持 ObjectBoxLiveData   liveData自动切换到ui线程
```
private ObjectBoxLiveData<Note> noteLiveData;  //只要数据变更就就会触发
    public ObjectBoxLiveData<Note> getNoteLiveData(Box<Note> notesBox) {
        if (noteLiveData == null) {
            // query all notes, sorted a-z by their text
            noteLiveData = new ObjectBoxLiveData<>(notesBox.query().order(Note_.text).build());
        }
        return noteLiveData;
    }
```
ObjectBoxLiveData继承LiveData
```
private final DataObserver<List<T>> listener = this::postValue;
 subscription = query.subscribe().observer(listener);
```




https://docs.objectbox.io/advanced/data-model-updates
数据库升级
1 ObjectBox新增或删除表字段
当我们数据库的升级需要新增和删除字段时，直接操作实体类即可，不需要做特殊的更改.
2 ObjectBox重命名表字段名称、字段的类型  使用注解UIDs(unique ID)
 默认会丢弃旧的数据，除非使用UIDS注解
2-1 添加空注解
2-2 使用build会提示要使用UIDS
```
error: [ObjectBox] UID operations for entity "MyName": 
[Rename] apply the current UID using @Uid(6645479796472661392L) -
[Change/reset] apply a new UID using @Uid(4385203238808477712L)
```
2-3 给UIDS增加数字，然后重命名字段
2-4 build工程