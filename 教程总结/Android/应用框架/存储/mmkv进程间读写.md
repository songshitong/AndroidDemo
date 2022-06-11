todo
https://www.jianshu.com/p/c12290a9a3f7
https://www.jianshu.com/p/f4837fd9b3b4  pb的编写
这里有几个关于MMKV使用的注意事项：
1.保证每一个文件存储的数据都比较小，也就说需要把数据根据业务线存储分散。这要就不会把虚拟内存消耗过快。
2.还需要在适当的时候释放一部分内存数据，比如在App中监听onTrimMemory方法，在Java内存吃紧的情况下进行MMKV的trim操作(不准确，我们暂时以此为信号，最好自己监听进程中内存使用情况)。
2.在不需要使用的时候，最好把MMKV给close掉。甚至调用exit方法


实现进程间通信是通过mmap函数
```
bool MemoryFile::mmap() {
    m_ptr = (char *) ::mmap(m_ptr, m_size, PROT_READ | PROT_WRITE, MAP_SHARED, m_diskFile.m_fd, 0);
    if (m_ptr == MAP_FAILED) {
        MMKVError("fail to mmap [%s], %s", m_diskFile.m_path.c_str(), strerror(errno));
        m_ptr = nullptr;
        return false;
    }
    return true;
}
```
映射的路径：
context.getFilesDir().getAbsolutePath() + "/mmkv"

https://juejin.cn/post/6844903914119102472
进程读写的同步
说起进程间读写同步, 我们很自然的想到 Linux 的信号量, 但是这种方式有一个弊端, 那就是当持有锁的进程意外死亡的时候, 并不会释放其拥有的信号量,
若多进程之间存在竞争, 那么阻塞的进程将不会被唤醒, 这是非常危险的    todo linux信号量的缺点

MMKV 是采用 文件锁 的方式来进行进程间的同步操作
LOCK_SH(共享锁): 多个进程可以使用同一把锁, 常被用作读共享锁
LOCK_EX(排他锁): 同时只允许一个进程使用, 常被用作写锁
LOCK_UN: 释放锁


接下来我看看 MMKV 加解锁的操作
一) 文件共享锁
Core/MMKV.cpp
```
MMKV::MMKV(const string &mmapID, MMKVMode mode, string *cryptKey, MMKVPath_t *rootPath)
    : m_mmapID(mmapID)
     ...
    // 创建文件锁的描述
    , m_fileLock(new FileLock(m_metaFile->getFd()))
    // 描述共享锁   InterProcessLock类型
    , m_sharedProcessLock(new InterProcessLock(m_fileLock, SharedLockType))
     // 描述排它锁  InterProcessLock类型
    , m_exclusiveProcessLock(new InterProcessLock(m_fileLock, ExclusiveLockType))
    //判断是否为进程间通信   m_isInterProcess为bool类型
    , m_isInterProcess((mode & MMKV_MULTI_PROCESS) != 0) {
        。。。
        // 根据是否跨进程操作确定共享锁和排它锁的开关
        m_sharedProcessLock->m_enable = m_isInterProcess;
        m_exclusiveProcessLock->m_enable = m_isInterProcess;
          // sensitive zone
    {
    // 文件读操作, 启用了文件共享锁
        SCOPED_LOCK(m_sharedProcessLock);
        loadFromFile();
    }
    }
```
可以看到在我们前面分析过的构造函数中, MMKV 对文件锁进行了初始化, 并且创建了共享锁和排它锁, 并在跨进程操作时开启, 当进行读操作时,
启动了共享锁

二) 文件排它锁
Core/MMKV_IO.cpp
```
bool MMKV::fullWriteback(AESCrypt *newCrypter) {
    ...
    auto preparedData = m_crypter ? prepareEncode(*m_dicCrypt) : prepareEncode(*m_dic);
    auto sizeOfDic = preparedData.second;
    // 启动了排它锁
    SCOPED_LOCK(m_exclusiveProcessLock);
    if (sizeOfDic > 0) {
        auto fileSize = m_file->getFileSize();
        if (sizeOfDic + Fixed32Size <= fileSize) {
            return doFullWriteBack(move(preparedData), newCrypter);
        } else {
            assert(0);
            assert(newCrypter == nullptr);
            // ensureMemorySize will extend file & full rewrite, no need to write back again
            return ensureMemorySize(sizeOfDic + Fixed32Size - fileSize);
        }
    }
    return false;
}
```
在进行数据回写的函数中, 启动了排它锁

MMKV 的文件锁并没有表面上那么简单, 因为文件锁为状态锁, 无论加了多少次锁, 一个解锁操作就全解除, 显然无法应对子函数嵌套调用的问题,
MMKV 内部通过了自行实现计数器来实现锁的可重入性, 更多的细节可以查看 wiki
https://github.com/Tencent/MMKV/wiki/android_ipc  todo   其他特性




MMKV 是支持 Ashmem 共享内存的, 当我们不想将文件写入磁盘，但是又想进行跨进程通信，就可以使用 MMKV 提供的 MMAP_ASHMEM
createNativeBuffer
```
//例子来自mmkv框架自带的demo，具体实现可以查看官方示例
// first of all, init ashmem mmkv in main process
MMKV.mmkvWithAshmemID(this, AshmemMMKV_ID, AshmemMMKV_Size, MMKV.MULTI_PROCESS_MODE, cryptKey);

// then other process can get by ContentProvider
Intent intent = new Intent(this, MyService.class);
intent.putExtra(BenchMarkBaseService.CMD_ID, BenchMarkBaseService.CMD_PREPARE_ASHMEM_BY_CP);
startService(intent);

intent = new Intent(this, MyService_1.class);
intent.putExtra(BenchMarkBaseService.CMD_ID, BenchMarkBaseService.CMD_PREPARE_ASHMEM_BY_CP);
startService(intent);
```
mmkvWithAshmemID  需要传入mmapID
```
public static MMKV mmkvWithAshmemID(Context context, String mmapID, int size, int mode, @Nullable String cryptKey)
        throws RuntimeException {
         ...
        //获取进程名字 
        String processName = MMKVContentProvider.getProcessNameByPID(context, android.os.Process.myPid());
       ...
       //进程名字带有:
        if (processName.contains(":")) {
            //MMKVContentProvider实现了ContentProvider
            //拿到ContentProvider的uri
            Uri uri = MMKVContentProvider.contentUri(context);
            ...
            Bundle extras = new Bundle();
            extras.putInt(MMKVContentProvider.KEY_SIZE, size);
            extras.putInt(MMKVContentProvider.KEY_MODE, mode);
            if (cryptKey != null) {
                extras.putString(MMKVContentProvider.KEY_CRYPT, cryptKey);
            }
            ContentResolver resolver = context.getContentResolver();
            //通过ContentResolver获取序列化的mmkv
            Bundle result = resolver.call(uri, MMKVContentProvider.FUNCTION_NAME, mmapID, extras);
            if (result != null) {
                result.setClassLoader(ParcelableMMKV.class.getClassLoader());
                //ParcelableMMKV实现了Parcelable，里面包含MMKV的关键信息
                ParcelableMMKV parcelableMMKV = result.getParcelable(MMKVContentProvider.KEY);
                if (parcelableMMKV != null) {
                    //反序列化得到MMKV  通过MMKV.mmkvWithAshmemFD(mmapID, ashmemFD, ashmemMetaFD, cryptKey)进行构造
                    MMKV mmkv = parcelableMMKV.toMMKV();
                    if (mmkv != null) {
                        simpleLog(MMKVLogLevel.LevelInfo,
                                  mmkv.mmapID() + " fd = " + mmkv.ashmemFD() + ", meta fd = " + mmkv.ashmemMetaFD());
                        return mmkv;
                    }
                }
            }
        }
        simpleLog(MMKVLogLevel.LevelInfo, "getting mmkv in main process");
        //构建ASHMEM_MODE的MMKV
        mode = mode | ASHMEM_MODE;
        long handle = getMMKVWithIDAndSize(mmapID, size, mode, cryptKey);
        if (handle != 0) {
            return new MMKV(handle);
        }
        throw new IllegalStateException("Fail to create an Ashmem MMKV instance [" + mmapID + "]");
    }     
    
  public static MMKV mmkvWithAshmemFD(String mmapID, int fd, int metaFD, String cryptKey) throws RuntimeException {
        long handle = getMMKVWithAshmemFD(mmapID, fd, metaFD, cryptKey);
        。。。
        return new MMKV(handle);
    }    
    private native static long getMMKVWithAshmemFD(String mmapID, int fd, int metaFD, @Nullable String cryptKey);    
```
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKVContentProvider.java
```
public Bundle call(@NonNull String method, @Nullable String mmapID, @Nullable Bundle extras) {
        if (method.equals(MMKVContentProvider.FUNCTION_NAME)) {
            if (extras != null) {
                int size = extras.getInt(MMKVContentProvider.KEY_SIZE);
                int mode = extras.getInt(MMKVContentProvider.KEY_MODE);
                String cryptKey = extras.getString(MMKVContentProvider.KEY_CRYPT);
                try {
                    //构建bundle
                    return mmkvFromAshmemID(mmapID, size, mode, cryptKey);
                } catch (Exception e) {
                    Log.e("MMKV", e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

  private Bundle mmkvFromAshmemID(String ashmemID, int size, int mode, String cryptKey) throws RuntimeException {
        //根据ashmemId构建MMKV  通过ashmemID获取了ashmemFD
        MMKV mmkv = MMKV.mmkvWithAshmemID(getContext(), ashmemID, size, mode, cryptKey);
        ParcelableMMKV parcelableMMKV = new ParcelableMMKV(mmkv);
        Log.i("MMKV", ashmemID + " fd = " + mmkv.ashmemFD() + ", meta fd = " + mmkv.ashmemMetaFD());
        Bundle result = new Bundle();
        result.putParcelable(MMKVContentProvider.KEY, parcelableMMKV);
        return result;
    }    
```
反序列化
ParcelableMMKV.java
```
public final class ParcelableMMKV implements Parcelable {
    private final String mmapID;
    private int ashmemFD = -1;
    private int ashmemMetaFD = -1;
    private String cryptKey = null;

    public ParcelableMMKV(MMKV mmkv) {
        mmapID = mmkv.mmapID();
        ashmemFD = mmkv.ashmemFD();
        ashmemMetaFD = mmkv.ashmemMetaFD();
        cryptKey = mmkv.cryptKey();
    }
    
     public MMKV toMMKV() {
        if (ashmemFD >= 0 && ashmemMetaFD >= 0) {
            return MMKV.mmkvWithAshmemFD(mmapID, ashmemFD, ashmemMetaFD, cryptKey);
        }
        return null;
    }
```


Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
```
MMKV_JNI jlong getMMKVWithAshmemFD(JNIEnv *env, jobject obj, jstring mmapID, jint fd, jint metaFD, jstring cryptKey) {
    MMKV *kv = nullptr;
    ...
    string id = jstring2string(env, mmapID);

    if (cryptKey) {
        string crypt = jstring2string(env, cryptKey);
        if (crypt.length() > 0) {
            kv = MMKV::mmkvWithAshmemFD(id, fd, metaFD, &crypt);
        }
    }
    if (!kv) {
        kv = MMKV::mmkvWithAshmemFD(id, fd, metaFD, nullptr);
    }

    return (jlong) kv;
}
```
Core/MMKV_Android.cpp
```
MMKV *MMKV::mmkvWithAshmemFD(const string &mmapID, int fd, int metaFD, string *cryptKey) {
    ...
    SCOPED_LOCK(g_instanceLock);
    //先从g_instanceDic查询
    auto itr = g_instanceDic->find(mmapID);
    if (itr != g_instanceDic->end()) {
        MMKV *kv = itr->second;
#    ifndef MMKV_DISABLE_CRYPT
        kv->checkReSetCryptKey(fd, metaFD, cryptKey);
#    endif
        return kv;
    }
    //构造并存储
    auto kv = new MMKV(mmapID, fd, metaFD, cryptKey);
    (*g_instanceDic)[mmapID] = kv;
    return kv;
}

MMKV::MMKV(const string &mmapID, int ashmemFD, int ashmemMetaFD, string *cryptKey)
    : m_mmapID(mmapID)
    , m_path(mappedKVPathWithID(m_mmapID, MMKV_ASHMEM, nullptr))
    , m_crcPath(crcPathWithID(m_mmapID, MMKV_ASHMEM, nullptr))
    , m_dic(nullptr)
    , m_dicCrypt(nullptr)
    //根据ashmemFD创建MemoryFile
    , m_file(new MemoryFile(ashmemFD))
    , m_metaFile(new MemoryFile(ashmemMetaFD))
    ...
    , m_isInterProcess(true) {
     ...
     {
        SCOPED_LOCK(m_sharedProcessLock);
        loadFromFile();
    } 
}

MMKVPath_t ashmemMMKVPathWithID(const MMKVPath_t &mmapID) {
    //匿名共享内存路径 constexpr char ASHMEM_NAME_DEF[] = "/dev/ashmem";
    return MMKVPath_t(ASHMEM_NAME_DEF) + MMKV_PATH_SLASH + mmapID;
}

Core/MemoryFile_Android.cpp
MemoryFile::MemoryFile(int ashmemFD) : m_diskFile(ashmemFD), m_ptr(nullptr), m_size(0), m_fileType(MMFILE_TYPE_ASHMEM) {
   ....
        m_size = m_diskFile.m_size;
        MMKVInfo("ashmem name:%s, size:%zu", m_diskFile.m_path.c_str(), m_size);
        auto ret = mmap();
        if (!ret) {
            doCleanMemoryCache(true);
        }
}
//Core/MemoryFile.h   m_diskFile是定义在MemoryFile头文件的File
class MemoryFile {
    File m_diskFile;
}
class File {
   explicit File(MMKVPath_t path, OpenFlag flag);
   bool open();
}
```
匿名共享文件的创建
```
Core/MemoryFile_Android.cpp
File::File(MMKVPath_t path, OpenFlag flag, size_t size, FileType fileType)
    : m_path(std::move(path)), m_fd(-1), m_flag(flag), m_size(0), m_fileType(fileType) {
    if (m_fileType == MMFILE_TYPE_FILE) {
        //普通模式
        open();
    } else {
        //ASHMEM模式
        // round up to (n * pagesize)
        if (size < DEFAULT_MMAP_SIZE || (size % DEFAULT_MMAP_SIZE != 0)) {
            size = ((size / DEFAULT_MMAP_SIZE) + 1) * DEFAULT_MMAP_SIZE;
        }
        auto filename = m_path.c_str();
        auto ptr = strstr(filename, ASHMEM_NAME_DEF);
        if (ptr && ptr[sizeof(ASHMEM_NAME_DEF) - 1] == '/') {
            filename = ptr + sizeof(ASHMEM_NAME_DEF);
        }
        //创建SharedMemory文件并返回FD
        m_fd = ASharedMemory_create(filename, size);
        if (isFileValid()) {
            m_size = size;
        }
    }
}

typedef int (*AShmem_create_t)(const char *name, size_t size);
int ASharedMemory_create(const char *name, size_t size) {
    int fd = -1;
    if (g_android_api >= __ANDROID_API_O__) {
        static auto handle = loadLibrary();
        static AShmem_create_t funcPtr =
            (handle != nullptr) ? reinterpret_cast<AShmem_create_t>(dlsym(handle, "ASharedMemory_create")) : nullptr;
        if (funcPtr) {
            fd = funcPtr(name, size);
            ...
    }
    if (fd < 0) {
        fd = open(ASHMEM_NAME_DEF, O_RDWR | O_CLOEXEC);
        if (fd < 0) {
            MMKVError("fail to open ashmem:%s, %s", name, strerror(errno));
        } else {
            if (ioctl(fd, ASHMEM_SET_NAME, name) != 0) {
                MMKVError("fail to set ashmem name:%s, %s", name, strerror(errno));
            } else if (ioctl(fd, ASHMEM_SET_SIZE, size) != 0) {
                MMKVError("fail to set ashmem:%s, size %zu, %s", name, size, strerror(errno));
            }
        }
    }
    return fd;
}
```


ashmemFD查询
```
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKV.java
public native int ashmemFD();

Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
MMKV_JNI jint ashmemFD(JNIEnv *env, jobject instance) {
    MMKV *kv = getMMKV(env, instance);
    if (kv) {
        return kv->ashmemFD();
    }
    return -1;
}

Core/MMKV_Android.cpp
int MMKV::ashmemFD() {
    return (m_file->m_fileType & mmkv::MMFILE_TYPE_ASHMEM) ? m_file->getFd() : -1;
}
```

