https://www.jianshu.com/p/c4fa942d8153
SharedPreference有哪些隐患或风险？
卡顿、丢帧、甚至ANR、占用内存过高

为什么SharedPreference会造成卡顿甚至ANR？
第一次从SharedPreference获取值的时候，可能阻塞主线程，造成卡顿／丢帧。
看如下代码，我第一次从sp取数据竟然花费了11ms。这还是我的数据很少的情况下，很多时候，一个迭代了很多版本的项目存放的数据会远比我的要大，
  耗时也会更长
```
   var startTime = System.currentTimeMillis()
        val sp: SharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE)
        sp.edit().putString("a","11111").apply();
        val value: String? = sp.getString("a", "")
        Log.e(TAG, "func1  :  ${System.currentTimeMillis() - startTime}")
```
结果放了一个值，然后获取用了4ms
有人会说SharedPreferences 的加载是不是在子线程吗，为什么还会阻塞主线程呢？这个问题，我们要从源码中寻找答案。
```
public String getString(String key, @Nullable String defValue) {
        synchronized (mLock) {
            //阻塞等待加载、解析xml文件完成
            awaitLoadedLocked();
            //从内存获取
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
private void awaitLoadedLocked() {
        if (!mLoaded) {
            BlockGuard.getThreadPolicy().onReadFromDisk();
        }
        while (!mLoaded) {
            try {
                //关键点，object对象的wait()来阻塞等待
                mLock.wait();
            } catch (InterruptedException unused) {
            }
        }
    }    
```
awaitLoadedLocked()会循环等待，直到mLoaded为true，那什么时候mLoaded为true呢？答案是从磁盘加载、解析xml完成之后，
具体是在SharedPreferencesImpl#loadFromDisk()方法内，这里不展开了，可以去上一篇源码分析文章看。
小结，第一次获取数据的时候会阻塞主线程，原因是主线程会等待从文件加载sp完成，这是一个耗时操作，尤其是xml中数据比较大的时候更明显；
  注意：只有第一次才会，后面不会，因为加载文件成功后会在内存缓存数据，下次就不需要等待了。
怎么解决？尽可能早的去完成sp对象的初始化，通常在Application是最合适的。



多次commit、apply
我见过很多这样的代码,每次写入数据都会创建一个Editor对象，调用一次commit/apply。
```
 val sp: SharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE)
 sp.edit().putString("key0", "11").apply()
 sp.edit().putString("key1", "11").apply()
 sp.edit().putString("key2", "11").apply()
```
创建Editor对象和put方法并不怎么耗时，但是多次commit()/apply()有多耗时，您心里没数吗？不信就来看看下面这组数据。

存储方式	   数据量	耗时（ms）
多次commit	20	   116
多次apply	20	   5
一次性commit	20	   6
一次性apply	20	   1
所以，请把你的代码改成这样。另外在不需要返回值的时候，请你使用apply()，官方也是这样推荐的。
```
val sp: SharedPreferences = getSharedPreferences("sp", Context.MODE_PRIVATE)
var editor = sp.edit()
editor.putString("key0", "11")
    .putString("key1", "11")
    .putString("key2", "11")
    .apply()
```

commit和apply有什么区别？
commit()是同步且有返回值的；apply()方法是异步没有返回值的；
commit()在主线程写入文件，会造成UI卡顿；apply()在子线程写入文件，也有可能卡UI；
apply可能造成UI卡顿的原因:
```
public void apply() {
  ...
   QueuedWork.addFinisher(awaitCommit);
  ...
   SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);
}
```
apply把写入文件的任务放入一个队列中，在QueuedWork内部会通过HandlerThread串行的执行。
到这里，看上去还没有问题，在子线程写文件并不会造成UI线程卡顿，但是我们来看一下ActivityThread的handleStopActivity方法
```
frameworks/base/core/java/android/app/ActivityThread.java
//activity stop时调用
public void handleStopActivity(IBinder token, boolean show, int configChanges,
            PendingTransactionActions pendingActions, boolean finalStateRequest, String reason) {
      ...省略无关代码
        // Make sure any pending writes are now committed.
        //sdk>=11
        if (!r.isPreHoneycomb()) {
            QueuedWork.waitToFinish();
        }
    }
 //service stop时调用   
private void handleStopService(IBinder token) {
   ...
    QueuedWork.waitToFinish();
   ...
}    
android.app.QueuedWork
public static void waitToFinish() {
            while (true) {
                Runnable finisher;
                synchronized (sLock) {
                    //从队列取任务
                    finisher = sFinishers.poll();
                }
                if (finisher == null) {
                    break;
                }
                finisher.run();
            }
    }    
```
这个方法很简单，循环地从sFinishers这个队列中取任务执行，直到任务为空。这个任务就是之前apply中的awaitCommit，它是用来等待
 写入文件的线程执行完毕的。现在试想一下，在onPause之后，如果因为你多次使用了apply，那就意味着写入任务会在这里排队，
 但是写入文件那里只有一个HandlerThread在串行的执行，那是不是就卡顿了？

还可能造成ANR https://www.jianshu.com/p/ca1a2129523b
当我们的SP写入耗时过大，就会造成Activity 暂停时候卡住，从而导致AMS服务那边的倒计时超时爆了ANR。而这种情况可能很会见的不少，
因为SP本身就全量写

Google为何这么设计呢？字节跳动技术团队的这篇文章中做出了如下猜测：  https://mp.weixin.qq.com/s/qAHburd-_S8ZJsr69kAtcA
无论是 commit 还是 apply 都会产生 ANR，但从 Android 之初到目前 Android8.0，Google 一直没有修复此 bug，我们贸然处理会产生什么问题呢。
  Google 在 Activity 和 Service 调用 onStop 之前阻塞主线程来处理 SP，我们能猜到的唯一原因是尽可能的保证数据的持久化。
  因为如果在运行过程中产生了 crash，也会导致 SP 未持久化，持久化本身是 IO 操作，也会失败。

字节的解决策略  清空等待队列,不进行序列化
//todo hook技术
SP 操作仅仅把 commit 替换为 apply 不是万能的，apply 调用次数过多容易引起 ANR。所有此类 ANR 都是经由 QueuedWork.waitToFinish() 触发的，
如果在调用此函数之前，将其中保存的队列手动清空，那么是不是能解决问题呢，答案是肯定的。
Activity 的 onStop，以及 Service 的 onStop 和 onStartCommand 都是通过 ActivityThread 触发的，
ActivityThread 中有一个 Handler 变量，我们通过 Hook 拿到此变量，给此 Handler 设置一个 callback，
Handler 的 dispatchMessage 中会先处理 callback。
在 Callback 中调用队列的清理工作 
https://mp.weixin.qq.com/s?__biz=MzI1MzYzMjE0MQ==&mid=2247484387&idx=1&sn=e3c8d6ef52520c51b5e07306d9750e70&scene=21#wechat_redirect
实验验证
我们清理了等待锁队列，会对数据持久化造成什么影响呢，下面我们通过一组实验来验证。
进程启动的时候，产生一个随机数字。用 commit 和 apply 两种方式来存此变量。第二次进程启动，获取以两种方式存取的值并做比较，
如果相同表示 apply 持久化成功，如果不相同表示 apply 持久化失败。
实验一：开启等待锁队列的清理。
实验二：关闭等待锁队列的清理。
线上同时开启两个实验，在实验规模相同的情况下，统计 apply 失败率。
实验一，失败率为 1.84%。
实验二，失败率为为 1.79%
可见，apply 机制本身的失败率就比较高，清理等待锁队列对持久化造成的影响不大

如何解决sp造成的界面卡顿、掉帧问题？ 如何正确的使用sp
1.初始化sp放在application；
2.不要频繁的commit/apply,尽量使用一次事物提交；
3.优先选择用apply而不是commit，因为commit会卡UI；
4.sp是轻量级的存储工具，所以请你不要存放太大的数据，不要存json等；   因为文件太大初始化会很耗时，而且文件内容会一直缓存在内存中，得不到释放；
5.单个sp文件不要太大，如果数据量很大，请把关联性比较大的，高频操作的放在单独的sp文件，做sp拆分
6. 如果优化了apply还出现卡顿，就用commit吧，但是需要自己进行异步处理，至于用Thread还是线程池或者其它看你自己业务

SharedPreference如何跨进程通信
有人寄希望于在初始化sp的时候，设置flag为MODE_MULTI_PROCESS来跨进程通信，但是很遗憾，这种方式已经被废弃。
```
getSharedPreferences("sp", Context.MODE_MULTI_PROCESS)

* @deprecated MODE_MULTI_PROCESS does not work reliably in
     * some versions of Android, and furthermore does not provide any
     * mechanism for reconciling concurrent modifications across
     * processes.  Applications should not attempt to use it.  Instead,
     * they should use an explicit cross-process data management
     * approach such as {@link android.content.ContentProvider ContentProvider}.
     */
    @Deprecated
    public static final int MODE_MULTI_PROCESS = 0x0004;
```
如果要跨进程通信，需要在sp外面包裹一层ContentProvider，当然用mmkv性能上更佳。
比如使用文件锁，保证每次只有一个进程在访问这个文件；或者对于Android开发而言，ContentProvider作为官方倡导的跨进程组件，
  其它进程通过定制的ContentProvider用于访问SharedPreferences，同样可以保证SharedPreferences的进程安全，等等。
文件锁的使用
```
try {
            FileInputStream fis = new FileInputStream("");
            //通过FileChannel获取FileLock
            FileLock fl = fis.getChannel().tryLock();
            fl.release();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
```




https://mp.weixin.qq.com/s/qAHburd-_S8ZJsr69kAtcA
优点
设计与实现：建立基本结构
对于21世纪初，那个Android系统诞生的时代而言，使用xml文件保存应用轻量级的数据绝对是一个不错的主意。那个时代的json才刚刚出生不久，
  虽然也渐渐成为了主流的 轻量级数据交换格式 ，但是其更多的优势还是在于 可读性，这也是笔者猜测没有使用json而使用xml保存的原因之一。

读操作的优化
通过这样的方式，虽然我们建立了一个最简单的 文件存储系统，但是性能实在不敢恭维，每次读取一个key对应的值都要重新对文件进行一次读的操作？
 显然需要尽量避免笨重的I/O操作。
因此设计者针对读操作进行了简单的优化，当SharedPreferences对象第一次通过Context.getSharedPreferences()进行初始化时，
 对xml文件进行一次读取，并将文件内所有内容（即所有的键值对）缓到内存的一个Map中，这样，接下来所有的读操作，只需要从这个Map中取就可以了
```
 final class SharedPreferencesImpl implements SharedPreferences {
  private final File mFile;             // 对应的xml文件
  private Map<String, Object> mMap;     // Map中缓存了xml文件中所有的键值对
}
```
读者不禁会有疑问，虽然节省了I/O的操作，但另一个视角分析，当xml中数据量过大时，这种 内存缓存机制 是否会产生 高内存占用 的风险？
这也正是很多开发者诟病SharedPreferences的原因之一，那么，从事物的两面性上来看，高内存占用 真的是设计者的问题吗？
不尽然，因为SharedPreferences的设计初衷是数据的 轻量级存储 ，对于类似应用的简单的配置项（比如一个boolean或者int类型），
  即使很多也并不会对内存有过高的占用；而对于复杂的数据（比如复杂对象序列化后的字符串），开发者更应该使用类似Room这样的解决方案，
  而非一股脑存储到SharedPreferences中。
因此，相对于「SharedPreferences会导致内存使用过高」的说法，笔者更倾向于更客观的进行总结：
  虽然 内存缓存机制 表面上看起来好像是一种 空间换时间 的权衡，实际上规避了短时间内频繁的I/O操作对性能产生的影响，而通过良好的代码规范，
  也能够避免该机制可能会导致内存占用过高的副作用，所以这种设计是 值得肯定 的。

写操作的优化
针对写操作，设计者同样设计了一系列的接口，以达到优化性能的目的。
我们知道对键值对进行更新是通过mSharedPreferences.edit().putString().commit()进行操作的——edit()是什么，commit()又是什么，
   为什么不单纯的设计初mSharedPreferences.putString()这样的接口？
设计者希望，在复杂的业务中，有时候一次操作会导致多个键值对的更新，这时，与其多次更新文件，我们更倾向将这些更新 合并到一次写操作中，以达到性能的优化。
因此，对于SharedPreferences的写操作，设计者抽象出了一个Editor类，不管某次操作通过若干次调用putXXX()方法，更新了几个xml中的键值对，
   只有调用了commit()方法，最终才会真正写入文件：
```
// 简单的业务，一次更新一个键值对
sharedPreferences.edit().putString().commit();

// 复杂的业务，一次更新多个键值对，仍然只进行一次IO操作（文件的写入）
Editor editor = sharedPreferences.edit();
editor.putString();
editor.putBoolean().putInt();
editor.commit();   // commit()才会更新文件
```
了解到这一点，读者应该明白，通过简单粗暴的封装，以达到类似SPUtils.putXXX()这种所谓代码量的节省，从而忽略了Editor.commit()的设计理念和使用场景，
  往往是不可取的，从设计上来讲，这甚至是一种倒退 。
另外一个值得思考的角度是，本质上文件的I/O是一个非常重的操作，直接放在主线程中的commit()方法某些场景下会导致ANR（比如数据量过大），
  因此更合理的方式是应该将其放入子线程执行。
因此设计者还为Editor提供了一个apply()方法，用于异步执行文件数据的同步，并推荐开发者使用apply()而非commit()。
  当前apply也存在其他问题。


保证复杂流程代码的可读性
为了保证SharedPreferences是线程安全的，Google的设计者一共使用了3把锁：
```
final class SharedPreferencesImpl implements SharedPreferences {
  // 1、使用注释标记锁的顺序
  // Lock ordering rules:
  //  - acquire SharedPreferencesImpl.mLock before EditorImpl.mLock
  //  - acquire mWritingToDiskLock before EditorImpl.mLock

  // 2、通过注解标记持有的是哪把锁
  @GuardedBy("mLock")
  private Map<String, Object> mMap;

  @GuardedBy("mWritingToDiskLock")
  private long mDiskStateGeneration;

  public final class EditorImpl implements Editor {
    @GuardedBy("mEditorLock")
    private final Map<String, Object> mModified = new HashMap<>();
  }
}
```
对于这样复杂的类而言，如何提高代码的可读性？SharedPreferencesImpl做了一个很好的示范：通过注释明确写明加锁的顺序，
  并为被加锁的成员使用@GuardedBy注解。


文件损坏 & 备份机制
由于不可预知的原因（比如内核崩溃或者系统突然断电），xml文件的写操作异常中止，Android系统本身的文件系统虽然有很多保护措施，
  但依然会有数据丢失或者文件损坏的情况。

作为设计者，如何规避这样的问题呢？答案是对文件进行备份，SharedPreferences的写入操作正式执行之前，首先会对文件进行备份，
  将初始文件重命名为增加了一个.bak后缀的备份文件：
```
SharedPreferencesImpl(File file, int mode) {
   mFile = file;//最终的文件
   mBackupFile = makeBackupFile(file); //备份文件名
}
static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }
// 尝试写入文件,提前备份
private void writeToFile(...) {
  if (!backupFileExists) {
      !mFile.renameTo(mBackupFile);
  }
}
```
这之后，尝试对文件进行写入操作，写入成功时，则将备份文件删除：
```
// 写入成功，立即删除存在的备份文件
// Writing was successful, delete the backup file if there is one.
mBackupFile.delete();
```
反之，若因异常情况（比如进程被杀）导致写入失败，进程再次启动后，若发现存在备份文件，则将备份文件重名为源文件，原本未完成写入的文件就直接丢弃：
```
// 从磁盘初始化加载时执行
private void loadFromDisk() {
    synchronized (mLock) {
        if (mBackupFile.exists()) {
            mFile.delete();
            mBackupFile.renameTo(mFile);
        }
    }
  }
```
现在，通过文件备份机制，我们能够保证数据只会丢失最后的更新，而之前成功保存的数据依然能够有效。