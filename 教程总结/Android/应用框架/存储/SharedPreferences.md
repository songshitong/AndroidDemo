android sdk 29  android10.0
https://www.jianshu.com/p/c1c30753145a

sp文件
SharedPreferences实际上是一个xml文件，存储位置在
/data/data/应用包名/shared_prefs/xx.xml
SharedPreferences基本用法
```
val sp: SharedPreferences = context.getSharedPreferences("zx", Context.MODE_PRIVATE)
 val editor = sp.edit()
 editor.putString("key0", "11")
 editor.commit() //同步提交
 editor.apply() //异步提交
val result = sp.getString("key0", "")      
```

sp存储初认识
```
class ContextImpl extends Context {
    private static ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache;
  
    private ArrayMap<String, File> mSharedPrefsPaths;
}

class SharedPreferencesImpl implements SharedPreferences {
    private Map<String, Object> mMap;
}

class EditorImpl implements Editor {
    private final Map<String, Object> mModified = new HashMap<>();
}
```
从文件上说，SharedPreferences是xml文件，每个xml文件都对应一个存储键值对的map。
从内存上说，每个xml文件都会被加载进内存缓存起来，这样避免了频繁的I/O，提升了性能。所以先来了解一下缓存相关的几个map。
   mSharedPrefsPaths:保存了sp文件名-xml文件的映射关系
   sSharedPrefsCache:保存了xml文件对应的操作它的SharedPreferencesImpl的对象，据查源码，它内部只保存了一个键值对，key就是app的包名。
   mMap:保存了xml文件的内容
   mModified:调用put()方法时，临时保存修改内容，在调用commit/apply之后保存到mMap中，并且写进xml文件里。
好了，有个整体认识之后，开始源码解析。
获取SharePreferences
frameworks/base/core/java/android/app/ContextImpl.java
```
 public SharedPreferences getSharedPreferences(String name, int mode) {
        ...
        File file;
        synchronized (ContextImpl.class) {
            if (mSharedPrefsPaths == null) {
                mSharedPrefsPaths = new ArrayMap<>();
            }
            //先从缓存获取xml文件
            file = mSharedPrefsPaths.get(name);
            if (file == null) {
                //创建name.xml文件并放入缓存
                file = getSharedPreferencesPath(name);
                mSharedPrefsPaths.put(name, file);
            }
        }
        return getSharedPreferences(file, mode);
    }
 public File getSharedPreferencesPath(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }
  private File getPreferencesDir() {
        synchronized (mSync) {
            if (mPreferencesDir == null) {
                mPreferencesDir = new File(getDataDir(), "shared_prefs");
            }
            return ensurePrivateDirExists(mPreferencesDir);
        }
    }
 
 public SharedPreferences getSharedPreferences(File file, int mode) {
        SharedPreferencesImpl sp;
        synchronized (ContextImpl.class) {
            //从缓存sSharedPrefsCache获取sp
            final ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
            sp = cache.get(file);
            if (sp == null) {
                checkMode(mode);
                ...
                //实例化sp并放入缓存
                sp = new SharedPreferencesImpl(file, mode);
                cache.put(file, sp);
                return sp;
            }
        }
        ...
        return sp;
    }  
 
  private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
        if (sSharedPrefsCache == null) {
            sSharedPrefsCache = new ArrayMap<>();
        }
        final String packageName = getPackageName();
        //从缓存sSharedPrefsCache中查看是否有对应包名下的SharedPreferences
        ArrayMap<File, SharedPreferencesImpl> packagePrefs = sSharedPrefsCache.get(packageName);
        if (packagePrefs == null) {
            //没有的话创建一个空的map，然后放入缓存
            packagePrefs = new ArrayMap<>();
            sSharedPrefsCache.put(packageName, packagePrefs);
        }
        return packagePrefs;
    }          
```
getSharedPreferences()就是获取SharedPreferencesImpl对象，也就是真正实现sp的类，先从缓存sSharedPrefsCache获取，
 没有的话就new一个并且放入缓存。再来看一下ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache对象，
 key为包名，值是一个ArrayMap，值其实是在getSharedPreferences(File file, int mode)创建的，sSharedPrefsCache的size最多为1，
 值ArrayMap<File, SharedPreferencesImpl>存储的是xml文件-sp实现类SharedPreferencesImpl映射，我们知道xml是可以有多个的，
 所以sSharedPrefsCache的值可以有多个。

小结：在ContextImpl中通过缓存获取或者创建了SharedPreferencesImpl对象，它是sp真正的核心类


核心类SharedPreferencesImpl
它实现了SharedPreferences接口，sp的初始化、获取、写入数据都是在这里完成的
android.app.SharedPreferencesImpl
```
final class SharedPreferencesImpl implements SharedPreferences {
  SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false; //是否加载xml文件成功，默认false
        mMap = null;
        mThrowable = null;
        //关键是这里
        startLoadFromDisk();
    }
}
```
startLoadFromDisk
```
private void startLoadFromDisk() {
        synchronized (mLock) {
            mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            public void run() {
             //开启子线程，从磁盘加载、解析xml文件
                loadFromDisk();
            }
        }.start();
    }

  private void loadFromDisk() {
        synchronized (mLock) {
            if (mLoaded) {
            //如果加载过了直接返回
                return;
            }
            if (mBackupFile.exists()) {
                mFile.delete();
                mBackupFile.renameTo(mFile);
            }
        }
        Map<String, Object> map = null;
        StructStat stat = null;
        Throwable thrown = null;
        try {
            stat = Os.stat(mFile.getPath());
            if (mFile.canRead()) {
                BufferedInputStream str = null;
                try {
                  //解析xml后，需要缓存到内存中，用map来保存
                    str = new BufferedInputStream(
                            new FileInputStream(mFile), 16 * 1024);
                    map = (Map<String, Object>) XmlUtils.readMapXml(str);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot read " + mFile.getAbsolutePath(), e);
                } finally {
                    IoUtils.closeQuietly(str);
                }
            }
        } catch (ErrnoException e) {
            
        } catch (Throwable t) {
            thrown = t;
        }
        //加载成功之后，设置一下标识位，并且唤醒线程
        synchronized (mLock) {
            mLoaded = true;
            mThrowable = thrown;
            try {
                if (thrown == null) {
                    if (map != null) {
                        mMap = map;
                        mStatTimestamp = stat.st_mtim;
                        mStatSize = stat.st_size;
                    } else {
                         //这个就是之前提到过的SharedPreferencesImpl中的成员变量mMap  ,这里给它复值
                        mMap = new HashMap<>();
                    }
                }
            } catch (Throwable t) {
                mThrowable = t;
            } finally {
                 //这里很关键，加载完成之后唤醒等待阻塞的线程
                mLock.notifyAll();
            }
        }
    }
```
这里的逻辑比较简单，通过XmlUtils工具类去读取xml文件内容，其实就是用了XmlParser解析，从xml文件取到数据之后就缓存到mMap中，
  后面就可以直接从内存读取了，这样就提高了读取效率



读取数据
```
##SharedPreferencesImpl
public String getString(String key, @Nullable String defValue) {
        //这里之所以要加锁，是因为awaitLoadedLocked()里面用了wait()等待阻塞，wait必须要加锁才能用
        synchronized (mLock) {
            //如果xml文件没有加载完成，就一直等待阻塞
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

        //循环等待xml文件加载完成，加载完成之后会唤醒阻塞
        while (!mLoaded) {
            try {
                mLock.wait();
            } catch (InterruptedException unused) {
            }
        }
        if (mThrowable != null) {
            throw new IllegalStateException(mThrowable);
        }
    }
```
这里以getString()为例，读取的时候会先加锁，如果xml文件没有加载完成，就一直等待阻塞；如果加载完成会自动唤醒阻塞线程，并且从内存获取数据。


修改数据
修改数据其实分为2部分，临时存放修改数据（put）和提交修改数据（commit、apply）。

临时存放数据Editor
通常向SharedPreferences存放数据的时候，是通过如下方式完成的
```
 val editor: SharedPreferences.Editor = sp.edit()
 editor.putString("key0", "11")
```
首先调用SharedPreferences的edit()方法获取Editor对象，然后调用put()方法，注意：Editor是一个接口，它的实现类是EditorImpl。
android.app.SharedPreferencesImpl
```
public Editor edit() {
        synchronized (mLock) {
            awaitLoadedLocked();
        }
        return new EditorImpl();
    }
public final class EditorImpl implements Editor {
      private final Object mEditorLock = new Object();
      //临时存储要提交数据的map
      private final Map<String, Object> mModified = new HashMap<>();
      public Editor putString(String key, @Nullable String value) {
            //上锁
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
      }
}
```
这里代码很简单，创建Editor对象之前会先判断xml文件是否加载完成，每次sp.edit()调用都是创建一个新的对象。put()方法是线程安全的，
它只是把数据存放到一个临时的Map集合，并没有提交到内存缓存和磁盘


提交修改数据：commit()和apply()
```
 public boolean commit() {
            //将Editor修改的内容提交到内存缓存mMap
            MemoryCommitResult mcr = commitToMemory();
            //把数据写入磁盘文件
            SharedPreferencesImpl.this.enqueueDiskWrite(
                mcr, null /* sync write on this thread okay */);
            try {
                //CountDownLatch阻塞等待  
                mcr.writtenToDiskLatch.await();
            } catch (InterruptedException e) {
                return false;
            } finally {
                ...
            }
            //回调通知
            notifyListeners(mcr);
            //写入磁盘文件是否成功
            return mcr.writeToDiskResult;
        }
        
  public void apply() {
            //修改内存缓存mMap
            final MemoryCommitResult mcr = commitToMemory();
            //等待写入文件完成的任务
            final Runnable awaitCommit = new Runnable() {
                    @Override
                    public void run() {
                        try {
                        /阻塞等待写入文件完成,否则阻塞在这
                        //利用CountDownLatch来等待任务的完成
                        //后面执行enqueueDiskWrite写入文件成功后会把writtenToDiskLatch多线程计数器减1，
                         //这样的话下面的阻塞代码就可以通过了.
                            mcr.writtenToDiskLatch.await();
                        } catch (InterruptedException ignored) {
                        }
                      ...
                    }
                };
            /QueuedWork是用来确保SharedPrefenced的写操作在Activity 销毁前执行完的一个全局队列. 
            //QueuedWork里面的队列是通过LinkedList实现的，LinkedList不仅可以做链表，也可以做队列
            //添加到全局的工作队列中
            QueuedWork.addFinisher(awaitCommit);
            //这个任务是等待磁盘写入完成，然后从队列中移除任务
            Runnable postWriteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        //执行阻塞任务
                        awaitCommit.run();
                        //阻塞完成之后，从队列中移除任务 
                        QueuedWork.removeFinisher(awaitCommit);
                    }
                };
            //异步执行磁盘文件写入，注意这里和commit不同的是postWriteRunnable不为空
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);
            最终回调给SharedPreferencesImpl注册监听registerOnSharedPreferenceChangeListener
            notifyListeners(mcr);
        }    

//回调给mcr.listeners.onSharedPreferenceChanged  
private void notifyListeners(final MemoryCommitResult mcr) {
            if (mcr.listeners == null || mcr.keysModified == null ||
                mcr.keysModified.size() == 0) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                    final String key = mcr.keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                        }
                    }
                }
            } else {
                // Run this function on the main thread.
                ActivityThread.sMainThreadHandler.post(() -> notifyListeners(mcr));
            }
        }           
```


commit()方法很简单，就3步，写入内存缓存、写入磁盘文件、返回写入文件结果。apply的流程和commit差不多，只是apply没有返回值。
 二者都会调用enqueueDiskWrite写入文件，commit的postWriteRunnable参数为null，apply是有值的
```
frameworks/base/core/java/android/app/SharedPreferencesImpl.java 可以注册监听
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(mLock) {
            mListeners.put(listener, CONTENT);
        }
    }

private MemoryCommitResult commitToMemory() {
     long memoryStateGeneration;
    boolean keysCleared = false;
    List<String> keysModified = null;
    Set<OnSharedPreferenceChangeListener> listeners = null;
    Map<String, Object> mapToWriteToDisk;

    synchronized (SharedPreferencesImpl.this.mLock) {
        if (mDiskWritesInFlight > 0) {
            mMap = new HashMap<String, Object>(mMap);
        }
        mapToWriteToDisk = mMap;
        mDiskWritesInFlight++;
        boolean hasListeners = mListeners.size() > 0;
        if (hasListeners) {
            keysModified = new ArrayList<String>();
            listeners = new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
        }
        synchronized (mEditorLock) {
            boolean changesMade = false;

            if (mClear) {
                if (!mapToWriteToDisk.isEmpty()) {
                    changesMade = true;
                    mapToWriteToDisk.clear();
                }
                keysCleared = true;
                mClear = false;
            }
            //mModified就是之前edit().put()来存放的数据
            for (Map.Entry<String, Object> e : mModified.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                // 如果执行了remove，则v对应的this，将这些key/value从mMap移除
                if (v == this || v == null) {
                    if (!mapToWriteToDisk.containsKey(k)) {
                        continue;
                    }
                    mapToWriteToDisk.remove(k);
                } else {
                    //将mModified中要修改的数据写到内存缓存mMap中 
                    if (mapToWriteToDisk.containsKey(k)) {
                        Object existingValue = mapToWriteToDisk.get(k);
                        if (existingValue != null && existingValue.equals(v)) {
                            continue;
                        }
                    }
                    mapToWriteToDisk.put(k, v);
                }

                changesMade = true;
                if (hasListeners) {
                    keysModified.add(k);
                }
            }
             //清空该次修改的记录
            mModified.clear();

            if (changesMade) {
                mCurrentMemoryStateGeneration++;
            }
            memoryStateGeneration = mCurrentMemoryStateGeneration;
        }
    }
    return new MemoryCommitResult(memoryStateGeneration, keysCleared, keysModified,
            listeners, mapToWriteToDisk);          
}

private static class MemoryCommitResult {
        final long memoryStateGeneration;
        final boolean keysCleared;
        final List<String> keysModified;
        final Set<OnSharedPreferenceChangeListener> listeners;
        final Map<String, Object> mapToWriteToDisk;
        //文件写入磁盘的等待，只有1
        final CountDownLatch writtenToDiskLatch = new CountDownLatch(1);

        volatile boolean writeToDiskResult = false;
        boolean wasWritten = false;
        
        private MemoryCommitResult(long memoryStateGeneration, boolean keysCleared,
                @Nullable List<String> keysModified,
                @Nullable Set<OnSharedPreferenceChangeListener> listeners,
                Map<String, Object> mapToWriteToDisk) {
            this.memoryStateGeneration = memoryStateGeneration;
            this.keysCleared = keysCleared;
            this.keysModified = keysModified;
            this.listeners = listeners;
            this.mapToWriteToDisk = mapToWriteToDisk;
        }
        //CountDownLatch的计数归零，不在进行await
        void setDiskWriteResult(boolean wasWritten, boolean result) {
            this.wasWritten = wasWritten;
            writeToDiskResult = result;
            writtenToDiskLatch.countDown();
        }
 }
```
commitToMemory就是将mModified中的修改写入到内存缓存中，接着看一下写入文件方法enqueueDiskWrite()。

```
private void enqueueDiskWrite(final MemoryCommitResult mcr,
                                  final Runnable postWriteRunnable) {
        //commit和apply的区别，commit 的postWriteRunnable参数为null，而apply是有值的                          
        final boolean isFromSyncCommit = (postWriteRunnable == null);
        //写入磁盘文件的任务
        final Runnable writeToDiskRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (mWritingToDiskLock) {
                     //就是将MemoryCommitResult的mapToWriteToDisk写入到文件，其实就是mMap的内容
                     // 写入文件完成之后，会调用writtenToDiskLatch.countDown()将计数器-1，这样就不会阻塞了
                        writeToFile(mcr, isFromSyncCommit);
                    }
                    synchronized (mLock) {
                        mDiskWritesInFlight--;
                    }
                    //apply才执行,apply的postWriteRunnable不为空
                    if (postWriteRunnable != null) {
                        // 写文件成功后则执行移除全局队列中的任务的任务.
                        // 此时waitCommit 任务就不会阻塞了, 因为writtenToDiskLatch==0 了.
                        // 不阻塞 QueuedWork.removeFinisher(awaitCommit); 就会被调用, 也就是说该任务执行完了
                       // 会将该任务从队列中移除
                        postWriteRunnable.run();
                    }
                }
            };

        //commit才执行，即在UI线程写入文件,postWriteRunnable为null
        if (isFromSyncCommit) {
            boolean wasEmpty = false;
            synchronized (mLock) {
                 // mDiskWritesInFlight 会在commitToMemory() 方法中进行+1 操作
                wasEmpty = mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
               // 在当前线程执行写任务
                //这里是直接调用Runnable的run方法，就是普通的方法调用，而不是开启子线程，懂了吧
                writeToDiskRunnable.run();
                return;
            }
        }
        //apply会调用这个，里面会通过HandlerThread去执行writeToDiskRunnable    isFromSyncCommit=false !isFromSyncCommit=true
        QueuedWork.queue(writeToDiskRunnable, !isFromSyncCommit);
    }
    
 //通过XmlUtils将内存的MemoryCommitResult写入xml,并mcr.setDiskWriteResult通知调用成功    
 private void writeToFile(MemoryCommitResult mcr, boolean isFromSyncCommit) {
        ...
        boolean fileExists = mFile.exists();

        // Rename the current file so it may be used as a backup during the next read
        if (fileExists) {
            boolean needsWrite = false;

            // Only need to write if the disk state is older than this commit
            if (mDiskStateGeneration < mcr.memoryStateGeneration) {
                if (isFromSyncCommit) {
                    needsWrite = true;
                } else {
                    synchronized (mLock) {
                        // No need to persist intermediate states. Just wait for the latest state to
                        // be persisted.
                        if (mCurrentMemoryStateGeneration == mcr.memoryStateGeneration) {
                            needsWrite = true;
                        }
                    }
                }
            }

            if (!needsWrite) {
                mcr.setDiskWriteResult(false, true);
                return;
            }

            boolean backupFileExists = mBackupFile.exists();
            if (!backupFileExists) {
                if (!mFile.renameTo(mBackupFile)) {
                    mcr.setDiskWriteResult(false, false);
                    return;
                }
            } else {
                mFile.delete();
            }
        }

        try {
            //创建输出流
            FileOutputStream str = createFileOutputStream(mFile);
            if (str == null) {
                mcr.setDiskWriteResult(false, false);
                return;
            }
            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
            writeTime = System.currentTimeMillis();
            FileUtils.sync(str);
            fsyncTime = System.currentTimeMillis();
            str.close();
            //设置文件权限
            ContextImpl.setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
            try {
                final StructStat stat = Os.stat(mFile.getPath());
                synchronized (mLock) {
                    mStatTimestamp = stat.st_mtim;
                    mStatSize = stat.st_size;
                }
            } catch (ErrnoException e) {
            }
            //写入成功，删除备份文件
            mBackupFile.delete();
            mDiskStateGeneration = mcr.memoryStateGeneration;
            mcr.setDiskWriteResult(true, true);
            long fsyncDuration = fsyncTime - writeTime;
            mSyncTimes.add((int) fsyncDuration);
            mNumSync++;
            return;
        } catch (XmlPullParserException e) {
        } catch (IOException e) {
        }
        //写入成功
        mcr.setDiskWriteResult(false, false);
    }    
    
```
小结：apply的逻辑稍微复杂了一点，大家仔细看。
apply是异步操作，每次调用apply会把写入任务放在QueuedWork的一个LinkedList实现的队列中，然后在QueuedWork中通过一个HandlerThread串行的执行写入
 文件的任务。排队是通过CountDownLatch来实现的，它其实是一个多线程计数器，调用它的await可以阻塞等待写入文件的子线程（HandlerThread）完成，
 在写入文件完成之后会调用它的countDown()将计数器-1,这样writtenToDiskLatch==0就不会阻塞等待了，然后再移除队列中的该任务。

android.app.QueuedWork
```
private static final LinkedList<Runnable> sWork = new LinkedList<>();
private static boolean sCanDelay = true;

public static void queue(Runnable work, boolean shouldDelay) {
        //获取子线程的Handler，通过HandlerThread创建的
        Handler handler = getHandler();
        synchronized (sLock) {
            //Runnable加入list排队
            sWork.add(work);
            if (shouldDelay && sCanDelay) {
              //apply传入的shouldDelay为true
              //apply执行这句，默认延迟100ms执行任务 
             handler.sendEmptyMessageDelayed(QueuedWorkHandler.MSG_RUN, DELAY);
            } else {
                handler.sendEmptyMessage(QueuedWorkHandler.MSG_RUN);
            }
        }
    }

//懒加载创建子线程的Handler，后面的写入文件的任务都在子线程完成  名字是"queued-work-looper"
private static Handler getHandler() {
        synchronized (sLock) {
            if (sHandler == null) {
                HandlerThread handlerThread = new HandlerThread("queued-work-looper",
                        Process.THREAD_PRIORITY_FOREGROUND);
                handlerThread.start();

                sHandler = new QueuedWorkHandler(handlerThread.getLooper());
            }
            return sHandler;
        }
    }

private static class QueuedWorkHandler extends Handler {
        static final int MSG_RUN = 1;

        QueuedWorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == MSG_RUN) {
                //处理排队的写文件任务
                processPendingWork();
            }
        }
    }
    
   private static final LinkedList<Runnable> sFinishers = new LinkedList<>();    
   public static void removeFinisher(Runnable finisher) {
        synchronized (sLock) {
            sFinishers.remove(finisher);
        }
    }  
  //添加待完成的runnalbe  
  public static void addFinisher(Runnable finisher) {
    synchronized (sLock) {
        sFinishers.add(finisher);
    }
  }   
```
这里代码量不少，但是逻辑却很简单，把apply提交的任务加到一个LinkedList中，然后开启子线程去串行的执行任务。
```
private static void processPendingWork() {
       synchronized (sProcessingWork) {
            LinkedList<Runnable> work;
            synchronized (sLock) {
                work = (LinkedList<Runnable>) sWork.clone();
                sWork.clear();

                // Remove all msg-s as all work will be processed now
                getHandler().removeMessages(QueuedWorkHandler.MSG_RUN);
            }
            //太简单了，串行处理排队的写文件任务
            //注意：这里已经在HandlerThread这个子线程中了
            if (work.size() > 0) {
                for (Runnable w : work) {
                    w.run();
                }
            }
        }
    }
```
好了，到这里SharedPreferences源码就分析完了。

总结
1、初始化：通过XmlUtils这个工具类读取、解析xml文件，并且把数据加载到内存缓存中，这样避免了频繁的 I/O，提升了读取数据的效率；
2、写操作：通过Editor把数据存放到临时map集合，当调用commit()/apply()的时候，再把数据分别提交到内存和文件；
3、读操作：需要先阻塞等待加载文件完成，然后再从内存中读取数据；第一次会比较慢，以后就很快；
4、commit()有返回值，是在主线程写入文件；apply()没有返回值，在子线程写入文件。


https://www.jianshu.com/p/ca1a2129523b   android10.0
跨进程设置：
frameworks/base/core/java/android/app/SharedPreferencesImpl.java
```
 private void writeToFile(MemoryCommitResult mcr, boolean isFromSyncCommit) {
    ...
       FileOutputStream str = createFileOutputStream(mFile);
        if (str == null) {
            mcr.setDiskWriteResult(false, false);
            return;
        }
        XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
        writeTime = System.currentTimeMillis();
        FileUtils.sync(str);
        fsyncTime = System.currentTimeMillis();
        str.close();
      //配置权限  
     ContextImpl.setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
    ...
 }
```
frameworks/base/core/java/android/app/ContextImpl.java
```
   static void setFilePermissionsFromMode(String name, int mode,
            int extraPermissions) {
        //设置用户和组的rw权限 read,write   
        int perms = FileUtils.S_IRUSR|FileUtils.S_IWUSR
            |FileUtils.S_IRGRP|FileUtils.S_IWGRP
            |extraPermissions;
        //标记位做与运算    public static final int MODE_WORLD_READABLE = 0x0001; 二进制0001    context中
        //SP设置多进程读写时候的标志位：mode  MODE_MULTI_PROCESS = 0x0004; 二进制0100 context中
        //0001 & 0100 =0;  添加其他人权限
        if ((mode&MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        //public static final int MODE_WORLD_WRITEABLE = 0x0002; 二进制0010    context中
        //0100 & 0010 = 0  添加了其他人权限
        if ((mode&MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        ...
        FileUtils.setPermissions(name, perms, -1, -1);
    }
```
frameworks/base/core/java/android/os/FileUtils.java
```
//用户  7是rwx权限 4=r 2=w 1=x    linux文件权限为3列，分别是用户，组，其他人
  public static final int S_IRWXU = 00700;
  public static final int S_IRUSR = 00400;
  public static final int S_IWUSR = 00200;
  public static final int S_IXUSR = 00100;
//组
  public static final int S_IRWXG = 00070;
  public static final int S_IRGRP = 00040;
  public static final int S_IWGRP = 00020;
  public static final int S_IXGRP = 00010;
//其他人
  public static final int S_IRWXO = 00007;
  public static final int S_IROTH = 00004;
  public static final int S_IWOTH = 00002;
  public static final int S_IXOTH = 00001;
 public static int setPermissions(String path, int mode, int uid, int gid) {
        try {
            Os.chmod(path, mode);
        } catch (ErrnoException e) {
            Slog.w(TAG, "Failed to chmod(" + path + "): " + e);
            return e.errno;
        }

        if (uid >= 0 || gid >= 0) {
            try {
                Os.chown(path, uid, gid);
            } catch (ErrnoException e) {
                Slog.w(TAG, "Failed to chown(" + path + "): " + e);
                return e.errno;
            }
        }
        return 0;
    }
```
这个过程中就是通过chmod方法设置了文件的在系统中的权限。从名称就能知道，这个方法每一次读写之后都会默认给用户和组都能够进行读写
根据位运算，SP在Android 10.0根本没有进行多进程读写文件的互斥处理，给其他人授予了权限
我们看看MODE_MULTI_PROCESS的注释：
```
MODE_MULTI_PROCESS does not work reliably in
     * some versions of Android, and furthermore does not provide any
     * mechanism for reconciling concurrent modifications across
     * processes.  Applications should not attempt to use it.  Instead,
     * they should use an explicit cross-process data management
     * approach such as {@link android.content.ContentProvider ContentProvider}.
```
这里面已经说明了，在某些Android版本中SP将不会提供跨进程读写文件的保护，如果有需求，请使用ContentProvider

//todo linux文件权限
