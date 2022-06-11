https://juejin.cn/post/6844903914119102472
版本1.2.13
使用
```
//静态库
implementation 'com.tencent:mmkv:1.2.13'
//动态库
implementation 'com.tencent:mmkv-shared:1.2.13'
```
初始化
```
// 设置初始化的根目录
String dir = getFilesDir().getAbsolutePath() + "/mmkv_2";
String rootDir = MMKV.initialize(dir);
Log.i("MMKV", "mmkv root: " + rootDir);
```
获取实例
```
// 获取默认的全局实例
MMKV kv = MMKV.defaultMMKV();
// 根据业务区别存储, 附带一个自己的 ID
MMKV kv = MMKV.mmkvWithID("MyID");
// 多进程同步支持
MMKV kv = MMKV.mmkvWithID("MyID", MMKV.MULTI_PROCESS_MODE);
kv.close();
```
CURD
```
// 添加/更新数据
kv.encode(key, value);
// 获取数据
int tmp = kv.decodeInt(key);
// 删除数据
kv.removeValueForKey(key);
```
SP迁移
```
private void testImportSharedPreferences() {
    MMKV mmkv = MMKV.mmkvWithID("myData");
    SharedPreferences old_man = getSharedPreferences("myData", MODE_PRIVATE);
    // 迁移旧数据
    mmkv.importFromSharedPreferences(old_man);
    // 清空旧数据
    old_man.edit().clear().commit();
    ......
}
```
无缝替换  在Application，Activity，CP下复写该方法为如上，就能在上层使用了SP的方式，实际上底层却是调用了mmkv的方法
https://www.jianshu.com/p/ca1a2129523b
```
 override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val mmkv = MMKV.mmkvWithID(name,mode)
        if(mmkv.getBoolean("hasTransport",false)){
            var originPrefences = super.getSharedPreferences(name, mode)
            mmkv.importFromSharedPreferences(originPrefences)
            originPrefences.edit().clear().apply()
            mmkv.encode("hasTransport",true)
        }
        return mmkv
    }
```
可以在Application中自定义SharePreferences
```
public class MyApplication extends Application {
  @Override
  public SharedPreferences getSharedPreferences(String name, int mode)        
  {
     return SharedPreferencesImpl.getSharedPreferences(name, mode);
  }
}
```


源码分析
初始化
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKV.java
MMKV实现了SharedPreferences相关接口
可以使用getString(String key, @Nullable String defValue)也可以MMKV.decodeString(nativeHandle, key, defValue)
可以使用kv.edit().putString().commit() 也可以MMKV.encode("bool", true)
```
public class MMKV implements SharedPreferences, SharedPreferences.Editor {
  public static String initialize(Context context) {
        String root = context.getFilesDir().getAbsolutePath() + "/mmkv";
        MMKVLogLevel logLevel = BuildConfig.DEBUG ? MMKVLogLevel.LevelDebug : MMKVLogLevel.LevelInfo;
        //传入的LibLoader为null
        return initialize(context, root, null, logLevel);
    }
    public static String initialize(Context context, String rootDir, LibLoader loader, MMKVLogLevel logLevel) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            disableProcessModeChecker();
        } else {
            enableProcessModeChecker();
        }
        String cacheDir = context.getCacheDir().getAbsolutePath();
        return doInitialize(rootDir, cacheDir, loader, logLevel);
    }   
   
       private static String doInitialize(String rootDir, String cacheDir, LibLoader loader, MMKVLogLevel logLevel) {
        if (loader != null) {
            if (BuildConfig.FLAVOR.equals("SharedCpp")) {
                loader.loadLibrary("c++_shared");
            }
            loader.loadLibrary("mmkv");
        } else {
            if (BuildConfig.FLAVOR.equals("SharedCpp")) {
                System.loadLibrary("c++_shared");
            }
            System.loadLibrary("mmkv");
        }
        //jni初始化
        jniInitialize(rootDir, cacheDir, logLevel2Int(logLevel));
        MMKV.rootDir = rootDir;
        return MMKV.rootDir;
    } 
    private static native void jniInitialize(String rootDir, String cacheDir, int level);
}
```
MMKV 的初始化, 主要是将根目录通过 jniInitialize 传入了 Native 层, 接下来看看 Native 的初始化操作
Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
```
MMKV_JNI void jniInitialize(JNIEnv *env, jobject obj, jstring rootDir, jstring cacheDir, jint logLevel) {
    if (!rootDir) {
        return;
    }
    const char *kstr = env->GetStringUTFChars(rootDir, nullptr);
    if (kstr) {
        //通过MMKV::initializeMMKV初始化
        MMKV::initializeMMKV(kstr, (MMKVLogLevel) logLevel);
        env->ReleaseStringUTFChars(rootDir, kstr);

        g_android_tmpDir = jstring2string(env, cacheDir);
    }
}
```
Core/MMKV.cpp
```
unordered_map<string, MMKV *> *g_instanceDic;
ThreadLock *g_instanceLock;
MMKVPath_t g_rootDir;

void initialize() {
    //获取一个 unordered_map, 类似于 Java 中的 HashMap
    g_instanceDic = new unordered_map<string, MMKV *>;
    //初始化线程锁
    g_instanceLock = new ThreadLock();
    g_instanceLock->initialize();

    mmkv::DEFAULT_MMAP_SIZE = mmkv::getPageSize();
    MMKVInfo("version %s, page size %d, arch %s", MMKV_VERSION, DEFAULT_MMAP_SIZE, MMKV_ABI);
   ...
}

  
ThreadOnceToken_t once_control = ThreadOnceUninitialized;
void MMKV::initializeMMKV(const MMKVPath_t &rootDir, MMKVLogLevel logLevel) {
    g_currentLogLevel = logLevel;
     // 由 Linux Thread 互斥锁和条件变量保证 initialize 函数在一个进程内只会执行一次  
    // https://blog.csdn.net/zhangxiao93/article/details/51910043
    //完成后执行initalize的callback
    ThreadLock::ThreadOnce(&once_control, initialize);
    //将根目录保存到全局变量
    g_rootDir = rootDir;
    //创建对应的目录
    mkPath(g_rootDir);
    MMKVInfo("root dir: " MMKV_PATH_FORMAT, g_rootDir.c_str());
}
```
可以看到 initializeMMKV 中主要任务是初始化数据, 以及创建根目录
1 pthread_once_t: 类似于 Java 的单例, 其 initialize 方法在进程内只会执行一次
   创建 MMKV 对象的缓存散列表 g_instanceDic
   创建一个线程锁 g_instanceLock
2 mkPath: 根据字符串创建文件目录

接下来我们看看这个目录创建的过程

目录的创建
Core/MemoryFile.cpp
```
extern bool mkPath(const MMKVPath_t &str) {
    char *path = strdup(str.c_str());
    // 定义 stat 结构体用于描述linux文件的属性  
    struct stat sb = {};
    bool done = false;
    // 指向字符串起始地址  
    char *slash = path;

    while (!done) {
        // 移动到第一个非 "/" 的下标处
        slash += strspn(slash, "/");
        // 移动到第一个 "/" 下标出处
        slash += strcspn(slash, "/");

        done = (*slash == '\0');
        *slash = '\0';

        if (stat(path, &sb) != 0) {
            //执行创建文件夹的操作, C 中无 mkdirs 的操作, 需要一个一个文件夹的创建
            if (errno != ENOENT || mkdir(path, 0777) != 0) {
                MMKVWarning("%s : %s", path, strerror(errno));
                free(path);
                return false;
            }
        } else if (!S_ISDIR(sb.st_mode)) {
            // 若非文件夹, 则说明为非法路径
            MMKVWarning("%s: %s", path, strerror(ENOTDIR));
            free(path);
            return false;
        }

        *slash = '/';
    }
    free(path);

    return true;
}
```

实例化
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKV.java
```
 public static MMKV mmkvWithID(String mmapID) throws RuntimeException {
        ...
        long handle = getMMKVWithID(mmapID, SINGLE_PROCESS_MODE, null, null);
        return checkProcessMode(handle, mmapID, SINGLE_PROCESS_MODE);
    }
 private native static long getMMKVWithID(String mmapID, int mode, @Nullable String cryptKey, @Nullable String rootPath);
 
  private static MMKV checkProcessMode(long handle, String mmapID, int mode) throws RuntimeException {
        if (handle == 0) {
            throw new RuntimeException("Fail to create an MMKV instance [" + mmapID + "] in JNI");
        }
        if (!isProcessModeCheckerEnabled) {
            return new MMKV(handle);
        }
        synchronized (checkedHandleSet) {
          ....
        }
        //创建MMKV对象
        return new MMKV(handle);
    }   
 
 private final long nativeHandle;

    private MMKV(long handle) {
        nativeHandle = handle;
    }    
```
Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
```
MMKV_JNI jlong getMMKVWithID(JNIEnv *env, jobject, jstring mmapID, jint mode, jstring cryptKey, jstring rootPath) {
    MMKV *kv = nullptr;
    if (!mmapID) {
        return (jlong) kv;
    }
    //获取独立存储 id
    string str = jstring2string(env, mmapID);

    bool done = false;
    if (cryptKey) {
        // 获取秘钥
        string crypt = jstring2string(env, cryptKey);
        if (crypt.length() > 0) {
            if (rootPath) {
                // 获取相对路径
                string path = jstring2string(env, rootPath);
                // 通过 mmkvWithID 函数获取一个 MMKV 的对象
                kv = MMKV::mmkvWithID(str, DEFAULT_MMAP_SIZE, (MMKVMode) mode, &crypt, &path);
            } else {
                kv = MMKV::mmkvWithID(str, DEFAULT_MMAP_SIZE, (MMKVMode) mode, &crypt, nullptr);
            }
            done = true;
        }
    }
    ...
    // 强转成句柄, 返回到 Java   
    return (jlong) kv;
}
```
可以看到最终通过 MMKV::mmkvWithID 函数获取到 MMKV 的对象
Core/MMKV.cpp
```
#ifndef MMKV_ANDROID
MMKV *MMKV::mmkvWithID(const string &mmapID, MMKVMode mode, string *cryptKey, MMKVPath_t *rootPath) {

    if (mmapID.empty()) {
        return nullptr;
    }
    SCOPED_LOCK(g_instanceLock);
    // 1. 通过 mmapID 和 relativePath, 组成最终的 mmap 文件路径的 key   
    auto mmapKey = mmapedKVKey(mmapID, rootPath);
    // 2. 从全局缓存中查找
    auto itr = g_instanceDic->find(mmapKey);
    if (itr != g_instanceDic->end()) {
        MMKV *kv = itr->second;
        return kv;
    }
    // 3. 创建缓存文件
    if (rootPath) {
        //MMKV_PATH_SLASH="/"   SPECIAL_CHARACTER_DIRECTORY_NAME = L"specialCharacter";
        MMKVPath_t specialPath = (*rootPath) + MMKV_PATH_SLASH + SPECIAL_CHARACTER_DIRECTORY_NAME;
        // 不存在则创建一个文件
        if (!isFileExist(specialPath)) {
            mkPath(specialPath);
        }
        MMKVInfo("prepare to load %s (id %s) from rootPath %s", mmapID.c_str(), mmapKey.c_str(), rootPath->c_str());
    }
    // 4. 创建实例对象    相对路径(android中是 data/data/包名/files/mmkv) + / + mmkvID
    auto kv = new MMKV(mmapID, mode, cryptKey, rootPath);
    kv->m_mmapKey = mmapKey;
    // 5. 缓存这个 mmapKey
    (*g_instanceDic)[mmapKey] = kv;
    return kv;
}
#endif

string mmapedKVKey(const string &mmapID, const MMKVPath_t *rootPath) {
    if (rootPath && g_rootDir != (*rootPath)) {
        return md5(*rootPath + MMKV_PATH_SLASH + string2MMKVPath_t(mmapID));
    }
    return mmapID;
}
static string md5(const basic_string<T> &value) {
    uint8_t md[MD5_DIGEST_LENGTH] = {};
    char tmp[3] = {}, buf[33] = {};
    openssl::MD5((const uint8_t *) value.c_str(), value.size() * (sizeof(T) / sizeof(uint8_t)), md);
    for (auto ch : md) {
        snprintf(tmp, sizeof(tmp), "%2.2x", ch);
        strcat(buf, tmp);
    }
    return {buf};
}
```
MMKV的创建流程
```
MMKVPath_t mappedKVPathWithID(const string &mmapID, MMKVMode mode, const MMKVPath_t *rootPath) {
#ifndef MMKV_ANDROID
    if (rootPath) {
#else
    if (mode & MMKV_ASHMEM) {
        return ashmemMMKVPathWithID(encodeFilePath(mmapID));
    } else if (rootPath) {
#endif
        return *rootPath + MMKV_PATH_SLASH + encodeFilePath(mmapID);
    }
    //根据mmapID编码文件
    return g_rootDir + MMKV_PATH_SLASH + encodeFilePath(mmapID);
}

static MMKVPath_t encodeFilePath(const string &mmapID) {
    const char *specialCharacters = "\\/:*?\"<>|";
    string encodedID;
    bool hasSpecialCharacter = false;
    for (auto ch : mmapID) {
        if (strchr(specialCharacters, ch) != nullptr) {
            encodedID = md5(mmapID);
            hasSpecialCharacter = true;
            break;
        }
    }
    if (hasSpecialCharacter) {
        static ThreadOnceToken_t once_control = ThreadOnceUninitialized;
        ThreadLock::ThreadOnce(&once_control, mkSpecialCharacterFileDirectory);
        return MMKVPath_t(SPECIAL_CHARACTER_DIRECTORY_NAME) + MMKV_PATH_SLASH + string2MMKVPath_t(encodedID);
    } else {
        return string2MMKVPath_t(mmapID);
    }
}

#ifndef MMKV_ANDROID
MMKV::MMKV(const string &mmapID, MMKVMode mode, string *cryptKey, MMKVPath_t *rootPath)
    : m_mmapID(mmapID)
     // 拼装文件的路径     MMKVPath_t m_path
    , m_path(mappedKVPathWithID(m_mmapID, mode, rootPath))
    // 拼装 .crc 文件路径
    , m_crcPath(crcPathWithID(m_mmapID, mode, rootPath))
    ...
    // 1. 将文件摘要信息映射到内存， 4 kb 大小
    , m_metaFile(new MemoryFile(m_crcPath))
    , m_metaInfo(new MMKVMetaInfo())
    , m_crypter(nullptr)
    , m_lock(new ThreadLock())
    , m_fileLock(new FileLock(m_metaFile->getFd()))
    , m_sharedProcessLock(new InterProcessLock(m_fileLock, SharedLockType))
    , m_exclusiveProcessLock(new InterProcessLock(m_fileLock, ExclusiveLockType))
    , m_isInterProcess((mode & MMKV_MULTI_PROCESS) != 0) {
    m_actualSize = 0;
    m_output = nullptr;

#    ifndef MMKV_DISABLE_CRYPT
    if (cryptKey && cryptKey->length() > 0) {
        // 根据 cryptKey 创建 AES 加解密的引擎 
        m_dicCrypt = new MMKVMapCrypt();
        m_crypter = new AESCrypt(cryptKey->data(), cryptKey->length());
    } else {
        //using MMKVMap = std::unordered_map<NSString *, mmkv::KeyValueHolder, KeyHasher, KeyEqualer>;
        m_dic = new MMKVMap();
    }
#    else
    m_dic = new MMKVMap();
#    endif
   ....
    // sensitive zone
    {
        SCOPED_LOCK(m_sharedProcessLock);
        // 2. 根据 m_mmapID 来加载文件中的数据
        loadFromFile();
    }
}
#endif
```
这里我们主要关注两个关键点
1 m_metaFile 文件摘要的映射
2 loadFromFile 数据的载入
接下来我们先看看, 文件摘要信息的映射
Core/MemoryFile.cpp
```
File::File(MMKVPath_t path, OpenFlag flag) : m_path(std::move(path)), m_fd(-1), m_flag(flag) {
    open();
}
bool File::open() {
#    ifdef MMKV_ANDROID
    if (m_fileType == MMFILE_TYPE_ASHMEM) {
        return isFileValid();
    }
#    endif
    if (isFileValid()) {
        return true;
    }
    //打开文件并得到m_fd
    m_fd = ::open(m_path.c_str(), OpenFlag2NativeFlag(m_flag), S_IRWXU);
    ...
    return true;
}
//m_diskFile是自定义的File类型
MemoryFile::MemoryFile(MMKVPath_t path) : m_diskFile(std::move(path), OpenFlag::ReadWrite | OpenFlag::Create), m_ptr(nullptr), m_size(0) {
    reloadFromFile();
}
void MemoryFile::reloadFromFile() {
    ... //打开文件
    if (!m_diskFile.open()) {
        MMKVError("fail to open:%s, %s", m_diskFile.m_path.c_str(), strerror(errno));
    } else {
        //创建文件锁  todo
        FileLock fileLock(m_diskFile.m_fd);
        InterProcessLock lock(&fileLock, ExclusiveLockType);
        SCOPED_LOCK(&lock);
        // 获取文件的信息
        mmkv::getFileSize(m_diskFile.m_fd, m_size);
        // round up to (n * pagesize)
        // 验证文件的大小是否小于一个内存页, 一般为 4kb
        if (m_size < DEFAULT_MMAP_SIZE || (m_size % DEFAULT_MMAP_SIZE != 0)) {
            size_t roundSize = ((m_size / DEFAULT_MMAP_SIZE) + 1) * DEFAULT_MMAP_SIZE;
            truncate(roundSize);
        } else {
            //通过 mmap 将文件映射到内存, 获取内存首地址
            auto ret = mmap();
            if (!ret) {
                doCleanMemoryCache(true);
            }
        }
...
    }
}

bool MemoryFile::mmap() {
    //mmap映射的内存指针存在MemoryFile.m_ptr
    m_ptr = (char *) ::mmap(m_ptr, m_size, PROT_READ | PROT_WRITE, MAP_SHARED, m_diskFile.m_fd, 0);
    ....
    return true;
}

bool MemoryFile::truncate(size_t size) {
   ...
    auto oldSize = m_size;
    m_size = size;
    // round up to (n * pagesize)
    if (m_size < DEFAULT_MMAP_SIZE || (m_size % DEFAULT_MMAP_SIZE != 0)) {
        m_size = ((m_size / DEFAULT_MMAP_SIZE) + 1) * DEFAULT_MMAP_SIZE;
    }
    //通过 ftruncate 将文件大小对其到内存页 
    if (::ftruncate(m_diskFile.m_fd, static_cast<off_t>(m_size)) != 0) {
        MMKVError("fail to truncate [%s] to size %zu, %s", m_diskFile.m_path.c_str(), m_size, strerror(errno));
        m_size = oldSize;
        return false;
    }
    if (m_size > oldSize) {
    //通过 zeroFillFile 将文件对其后的空白部分用 0 填充   
        if (!zeroFillFile(m_diskFile.m_fd, oldSize, m_size - oldSize)) {
            MMKVError("fail to zeroFile [%s] to size %zu, %s", m_diskFile.m_path.c_str(), m_size, strerror(errno));
            m_size = oldSize;
            return false;
        }
    }
    
    if (m_ptr) {
        if (munmap(m_ptr, oldSize) != 0) {
            MMKVError("fail to munmap [%s], %s", m_diskFile.m_path.c_str(), strerror(errno));
        }
    }
    //文件映射
    auto ret = mmap();
    if (!ret) {
        doCleanMemoryCache(true);
    }
    return ret;
}

//填充文件为0
bool zeroFillFile(int fd, size_t startPos, size_t size) {
    ...
    if (lseek(fd, static_cast<off_t>(startPos), SEEK_SET) < 0) {
        MMKVError("fail to lseek fd[%d], error:%s", fd, strerror(errno));
        return false;
    }

    static const char zeros[4096] = {};
    while (size >= sizeof(zeros)) {
        if (write(fd, zeros, sizeof(zeros)) < 0) {
            MMKVError("fail to write fd[%d], error:%s", fd, strerror(errno));
            return false;
        }
        size -= sizeof(zeros);
    }
    if (size > 0) {
        if (write(fd, zeros, size) < 0) {
            MMKVError("fail to write fd[%d], error:%s", fd, strerror(errno));
            return false;
        }
    }
    return true;
}
```
//todo 没看懂内存操作 https://www.jianshu.com/p/c12290a9a3f7
能看到在这个过程中实际上还是通过ftruncate进行扩容，接着调用zeroFillFile，先通过lseek把指针移动当前容量的最后，
并把剩余的部分都填充空数据'\0'。最后映射指向的地址是有效的，会先解开后重新进行映射。

为什么要做最后这个步骤呢？如果阅读过我解析的mmap的源码一文，实际上就能明白，file使用MAP_SHARED的模式本质上是给file结构体
绑定一段vma映射好的内存。ftruncate只是给file结构体进行了扩容，但是还没有对对应绑定虚拟内存进行扩容，因此需要解开一次映射后，
重新mmap一次。


MemoryFile 的构造函数处理的事务如下
1 打开指定的文件
2 创建这个文件锁
3 修正文件大小, 最小为 4kb
  前 4kb 用于统计数据总大小
4 通过 mmap 将文件映射到内存

好的, 通过 MemoryFile 的构造函数, 我们便能够获取到映射后的内存首地址了, 操作这块内存时 Linux 内核会负责将内存中的数据同步到文件中
比起 SP 的数据同步, mmap 显然是要优雅的多, 即使进程意外死亡, 也能够通过 Linux 内核的保护机制, 将进行了文件映射的内存数据刷入到文件中, 
  提升了数据写入的可靠性


接下来看看数据的载入
Core/MMKV_IO.cpp
```
void MMKV::loadFromFile() {
    //若已经进行了文件映射  则获取相关数据
    if (m_metaFile->isFileValid()) {
        m_metaInfo->read(m_metaFile->getMemory());
    }
    ...
    if (!m_file->isFileValid()) {
        m_file->reloadFromFile();
    }
    if (!m_file->isFileValid()) {
        MMKVError("file [%s] not valid", m_path.c_str());
    } else {
        // error checking
        bool loadFromFile = false, needFullWriteback = false;
        //读取内存文件的前 32 位, 获取存储数据的真实大小,验证文件的长度,验证文件 CRC 的正确性
        checkDataValid(loadFromFile, needFullWriteback);
        MMKVInfo("loading [%s] with %zu actual size, file size %zu, InterProcess %d, meta info "
                 "version:%u",
                 m_mmapID.c_str(), m_actualSize, m_file->getFileSize(), m_isInterProcess, m_metaInfo->m_version);
        //文件地址
        auto ptr = (uint8_t *) m_file->getMemory();
        // loading
        //需要从文件获取数据
        if (loadFromFile && m_actualSize > 0) {
            MMKVInfo("loading [%s] with crc %u sequence %u version %u", m_mmapID.c_str(), m_metaInfo->m_crcDigest,
                     m_metaInfo->m_sequence, m_metaInfo->m_version);
            //构建输入缓存         
            MMBuffer inputBuffer(ptr + Fixed32Size, m_actualSize, MMBufferNoCopy);
            if (m_crypter) {
                clearDictionary(m_dicCrypt);
            } else {
                clearDictionary(m_dic);
            }
            if (needFullWriteback) {
#ifndef MMKV_DISABLE_CRYPT
                if (m_crypter) {
                    MiniPBCoder::greedyDecodeMap(*m_dicCrypt, inputBuffer, m_crypter);
                } else
#endif
                {
                    MiniPBCoder::greedyDecodeMap(*m_dic, inputBuffer);
                }
            } else {
#ifndef MMKV_DISABLE_CRYPT
                if (m_crypter) {
                   // 解密输入缓冲中的数据  todo protoBuffer的编码和解码
                    MiniPBCoder::decodeMap(*m_dicCrypt, inputBuffer, m_crypter);
                } else
#endif
                {
                  // 从输入缓冲中将数据读入 m_dic
                    MiniPBCoder::decodeMap(*m_dic, inputBuffer);
                }
            }
            //构建输出数据
            m_output = new CodedOutputData(ptr + Fixed32Size, m_file->getFileSize() - Fixed32Size);
            m_output->seek(m_actualSize);
            //进行重整回写，剔除重复的数据
            if (needFullWriteback) {
                fullWriteback();
            }
        } else {
            //说明文件中没有数据, 或者校验失败了
            // file not valid or empty, discard everything
            SCOPED_LOCK(m_exclusiveProcessLock);

            m_output = new CodedOutputData(ptr + Fixed32Size, m_file->getFileSize() - Fixed32Size);
            //清空文件中的数据 
            if (m_actualSize > 0) {
                writeActualSize(0, 0, nullptr, IncreaseSequence);
                sync(MMKV_SYNC);
            } else {
                writeActualSize(0, 0, nullptr, KeepSequence);
            }
        }
        auto count = m_crypter ? m_dicCrypt->size() : m_dic->size();
        MMKVInfo("loaded [%s] with %zu key-values", m_mmapID.c_str(), count);
    }

    m_needLoadFromFile = false;
}
```
可以看到 loadFromFile 中对于 CRC 验证通过的文件, 会将文件中的数据读入到 m_dic 中缓存, 否则则会清空文件

因此用户恶意修改文件之后, 会破坏 CRC 的值, 这个存储数据便会被作废, 这一点要尤为注意
从文件中读取数据到 m_dic 之后, 会将 mdic 回写到文件中, 其重写的目的是为了剔除重复的数据

关于为什么会出现重复的数据, 在后面 encode 操作中再分析

MMKV构建回顾
到这里 MMKV 实例的构建就完成了, 有了 m_dic 这个内存缓存, 我们进行数据查询的效率就大大提升了
从最终的结果来看它与 SP 是一致的, 都是初次加载时会将文件中所有的数据加载到散列表中, 不过 MMKV 多了一步数据回写的操作, 
因此当数据量比较大时, 对实例构建的速度有一定的影响
```
// 写入 1000 条数据之后, MMVK 和 SharedPreferences 实例化的时间对比
E/TAG: create MMKV instance time is 4 ms
E/TAG: create SharedPreferences instance time is 1 ms
```
从结果上来看, MMVK 的确在实例构造速度上有一定的劣势, 不过得益于是将 m_dic 中的数据写入到 mmap 的内存, 
其真正进行文件写入的时机由 Linux 内核决定, 再加上文件的页缓存机制, 所以速度上虽有劣势, 但不至于无法接受



encode
关于 encode 即数据的添加与更新的流程, 这里以 encodeString 为例
```
 public boolean encode(String key, @Nullable String value) {
        return encodeString(nativeHandle, key, value);
    }
private native boolean encodeString(long handle, String key, @Nullable String value);    
```
Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
```
MMKV_JNI jboolean encodeString(JNIEnv *env, jobject, jlong handle, jstring oKey, jstring oValue) {
    MMKV *kv = reinterpret_cast<MMKV *>(handle);
    if (kv && oKey) {
        string key = jstring2string(env, oKey);
        if (oValue) {
            // 若是 value 非 NULL，通过set函数, 将数据存入
            string value = jstring2string(env, oValue);
            return (jboolean) kv->set(value, key);
        } else {
             // 若是 value 为 NULL, 则移除 key 对应的 value 值
            kv->removeValueForKey(key);
            return (jboolean) true;
        }
    }
    return (jboolean) false;
}
```
Core/MMKV.cpp
```
bool MMKV::set(bool value, MMKVKey_t key) {
    if (isKeyEmpty(key)) {
        return false;
    }
    //将数据编码成 ProtocolBuffer   todo 没看懂
    size_t size = pbBoolSize();  //返回1
    MMBuffer data(size);
    CodedOutputData output(data.getPtr(), size);
    output.writeBool(value);
    //更新键值对   
    return setDataForKey(move(data), key);
}
```
这里主要分为两步操作
数据编码
更新键值对

数据编码
创建MMBuffer
Core/MMBuffer.cpp
```
 static constexpr size_t SmallBufferSize() {
        return sizeof(MMBuffer) - offsetof(MMBuffer, paddedBuffer);  
 }
    
MMBuffer::MMBuffer(size_t length) {
    if (length > SmallBufferSize()) {
        //normal类型存储在heap memory
        type = MMBufferType_Normal;
        isNoCopy = MMBufferCopy;
        size = length;
        ptr = malloc(size);
        ...
#ifdef MMKV_APPLE
        m_data = nil;
#endif
    } else {
        //small类型存储在stack memory
        type = MMBufferType_Small;
        paddedSize = static_cast<uint8_t>(length);
    }
}
```
创建CodedOutputData
Core/CodedOutputData.cpp
```
class CodedOutputData {
    uint8_t *const m_ptr;
    size_t m_size;
    size_t m_position;
}
CodedOutputData::CodedOutputData(void *ptr, size_t len) : m_ptr((uint8_t *) ptr), m_size(len), m_position(0) {
    MMKV_ASSERT(m_ptr);
}
void CodedOutputData::writeBool(bool value) {
    //bool转为1或0
    this->writeRawByte(static_cast<uint8_t>(value ? 1 : 0));
}
void CodedOutputData::writeRawByte(uint8_t value) {
    //存储在m_ptr
    m_ptr[m_position++] = value;
}
```

更新键值对
Core/MMKV_IO.cpp
```
bool MMKV::setDataForKey(MMBuffer &&data, MMKVKey_t key, bool isDataHolder) {
   ...
   auto itr = m_dic->find(key);
        if (itr != m_dic->end()) {
            //将键值对写入mmap文件映射的内存中
            auto ret = appendDataWithKey(data, itr->second, isDataHolder);
            if (!ret.first) {
                return false;
            }
            itr->second = std::move(ret.second);
        } else {
            auto ret = appendDataWithKey(data, key, isDataHolder);
            if (!ret.first) {
                return false;
            }
            // 写入成功, 更新散列数据  
            m_dic->emplace(key, std::move(ret.second));
        }
   ...     
}

KVHolderRet_t MMKV::appendDataWithKey(const MMBuffer &data, MMKVKey_t key, bool isDataHolder) {
...
#else
    auto keyData = MMBuffer((void *) key.data(), key.size(), MMBufferNoCopy);
#endif
    return doAppendDataWithKey(data, keyData, isDataHolder, static_cast<uint32_t>(keyData.length()));
}

KVHolderRet_t MMKV::appendDataWithKey(const MMBuffer &data, const KeyValueHolder &kvHolder, bool isDataHolder) {
    SCOPED_LOCK(m_exclusiveProcessLock);
    //计算 key + value 的 ProtocolBuffer 编码后的长度
    uint32_t keyLength = kvHolder.keySize;
    // size needed to encode the key
    size_t rawKeySize = keyLength + pbRawVarint32Size(keyLength);

    // ensureMemorySize() might change kvHolder.offset, so have to do it early
    {
        auto valueLength = static_cast<uint32_t>(data.length());
        if (isDataHolder) {
            valueLength += pbRawVarint32Size(valueLength);
        }
        auto size = rawKeySize + valueLength + pbRawVarint32Size(valueLength);
        //验证是否有足够的空间, 不足则进行数据重整与扩容操作
        bool hasEnoughSize = ensureMemorySize(size);
        if (!hasEnoughSize) {
            return make_pair(false, KeyValueHolder());
        }
    }
    //m_file->getMemory()获取mmap映射的地址
    auto basePtr = (uint8_t *) m_file->getMemory() + Fixed32Size;
    MMBuffer keyData(basePtr + kvHolder.offset, rawKeySize, MMBufferNoCopy);
    //keyData持有内存映射的地址
    return doAppendDataWithKey(data, keyData, isDataHolder, keyLength);
}

KVHolderRet_t
MMKV::doAppendDataWithKey(const MMBuffer &data, const MMBuffer &keyData, bool isDataHolder, uint32_t originKeyLength) {
    auto isKeyEncoded = (originKeyLength < keyData.length());
    auto keyLength = static_cast<uint32_t>(keyData.length());
    auto valueLength = static_cast<uint32_t>(data.length());
    if (isDataHolder) {
        valueLength += pbRawVarint32Size(valueLength);
    }
    // size needed to encode the key
    size_t size = isKeyEncoded ? keyLength : (keyLength + pbRawVarint32Size(keyLength));
    // size needed to encode the value
    size += valueLength + pbRawVarint32Size(valueLength);

    SCOPED_LOCK(m_exclusiveProcessLock);
    //验证是否有足够的空间, 不足则进行数据重整与扩容操作
    bool hasEnoughSize = ensureMemorySize(size);
    if (!hasEnoughSize || !isFileValid()) {
        return make_pair(false, KeyValueHolder());
    }

....
#ifndef MMKV_DISABLE_CRYPT
    if (m_crypter) {
        if (KeyValueHolderCrypt::isValueStoredAsOffset(valueLength)) {
            m_crypter->getCurStatus(t_status);
        }
    }
#endif
    try {
        // 将 key 和编码后的 value 写入到文件映射的内存
        if (isKeyEncoded) {
            //使用memcpy将keyData拷贝到CodedOutputData的m_ptr  数据通过mmap由linux自动写到文件
            m_output->writeRawData(keyData);
        } else {
            m_output->writeData(keyData);
        }
        if (isDataHolder) {
            m_output->writeRawVarint32((int32_t) valueLength);
        }
        m_output->writeData(data); // note: write size of data
    } catch (std::exception &e) {
        MMKVError("%s", e.what());
        return make_pair(false, KeyValueHolder());
    }

    auto offset = static_cast<uint32_t>(m_actualSize);
    //获取文件映射内存当前 <key, value> 的起始位置
    auto ptr = (uint8_t *) m_file->getMemory() + Fixed32Size + m_actualSize;
#ifndef MMKV_DISABLE_CRYPT
    if (m_crypter) {
         // 加密这块区域
        m_crypter->encrypt(ptr, ptr, size);
    }
#endif
    m_actualSize += size;
    //更新 CRC
    updateCRCDigest(ptr, size);

    return make_pair(true, KeyValueHolder(originKeyLength, valueLength, offset));
}

Core/CodedOutputData.cpp
void CodedOutputData::writeRawData(const MMBuffer &data) {
    size_t numberOfBytes = data.length();
    ...
    memcpy(m_ptr + m_position, data.getPtr(), numberOfBytes);
    m_position += numberOfBytes;
}
```
可以看到更新键值对的操作还是比较复杂的, 首先将键值对数据写入到文件映射的内存中, 写入成功之后更新散列数据

关于写入到文件映射的过程, 上面代码中的注释也非常的清晰, 接下来我们 ensureMemorySize 是如何进行数据的重整与扩容的

```
bool MMKV::ensureMemorySize(size_t newSize) {
    ...
    // 数据重写: 
    // 1. 文件剩余空闲空间少于新的键值对
    // 2. 散列为空
    if (newSize >= m_output->spaceLeft() || (m_crypter ? m_dicCrypt->empty() : m_dic->empty())) {
        // try a full rewrite to make space
        auto fileSize = m_file->getFileSize();
        auto preparedData = m_crypter ? prepareEncode(*m_dicCrypt) : prepareEncode(*m_dic);
        auto sizeOfDic = preparedData.second;
        // 计算所需的数据空间
        size_t lenNeeded = sizeOfDic + Fixed32Size + newSize;
        size_t dicCount = m_crypter ? m_dicCrypt->size() : m_dic->size();
        // 计算每个键值对的平均大小
        size_t avgItemSize = lenNeeded / std::max<size_t>(1, dicCount);
        // 计算未来可能会使用的大小(类似于 1.5 倍)
        size_t futureUsage = avgItemSize * std::max<size_t>(8, (dicCount + 1) / 2);
        // 1. no space for a full rewrite, double it
        // 2. or space is not large enough for future usage, double it to avoid frequently full rewrite
        // 1. 所需空间 >= 当前文件总大小
        // 2. 所需空间的 1.5 倍 >= 当前文件总大小  
        if (lenNeeded >= fileSize || (lenNeeded + futureUsage) >= fileSize) {
           //扩容为2倍
            size_t oldSize = fileSize;
            do {
                fileSize *= 2;
            } while (lenNeeded + futureUsage >= fileSize);
            MMKVInfo("extending [%s] file size from %zu to %zu, incoming size:%zu, future usage:%zu", m_mmapID.c_str(),
                     oldSize, fileSize, newSize, futureUsage);
         ...
        }
        return doFullWriteBack(move(preparedData), nullptr);
    }
    return true;
}
```
从上面的代码我们可以了解到
数据的重写时机
  1 文件剩余空间少于新的键值对大小
  2  散列为空
文件扩容时机
 1 所需空间的 1.5 倍超过了当前文件的总大小时, 扩容为之前的两倍

回顾
至此 encode 的流程我们就走完了, 回顾一下整个 encode 的流程
使用 ProtocolBuffer 编码 value
将 key 和 编码后的 value 使用 ProtocolBuffer 的格式 append 到文件映射区内存的尾部
    文件空间不足
        判断是否需要扩容
         进行数据的回写
    即在文件后进行追加
对这个键值对区域进行统一的加密
更新 CRC 的值
将 key 和 value 对应的 ProtocolBuffer 编码内存区域, 更新到散列表 m_dic 中

通过 encode 的分析, 我们得知 MMKV 文件的存储方式如下
//todo 验证格式



接下来看看 decode 的流程
decode 的过程同样以 decodeString 为例
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKV.java
```
  public String getString(String key, @Nullable String defValue) {
        return decodeString(nativeHandle, key, defValue);
    }
    private native String decodeString(long handle, String key, @Nullable String defaultValue);    
```
Android/MMKV/mmkv/src/main/cpp/native-bridge.cpp
```
MMKV_JNI jstring decodeString(JNIEnv *env, jobject obj, jlong handle, jstring oKey, jstring oDefaultValue) {
    MMKV *kv = reinterpret_cast<MMKV *>(handle);
    if (kv && oKey) {
        string key = jstring2string(env, oKey);  //mmkv 自己封装的 通过env->GetStringUTFChars将Java String转string
        string value;
        // 通过 getString, 将数据输出到传出参数中 value 中
        bool hasValue = kv->getString(key, value);
        if (hasValue) {
            return string2jstring(env, value);  ////mmkv 自己封装的 通过env->NewStringUTF将string转java String
        }
    }
    return oDefaultValue;
}
```
Core/MMKV.cpp
```
bool MMKV::getString(MMKVKey_t key, string &result) {
    if (isKeyEmpty(key)) {
        return false;
    }
    SCOPED_LOCK(m_lock);
    // 1. 从内存缓存中获取数据
    auto data = getDataForKey(key);
    if (data.length() > 0) {
        try {
            // 2. 解析 data 对应的 ProtocolBuffer 数据
            CodedInputData input(data.getPtr(), data.length());
            result = input.readString();
            return true;
        } catch (std::exception &exception) {
            MMKVError("%s", exception.what());
        }
    }
    return false;
}

MMBuffer MMKV::getDataForKey(MMKVKey_t key) {
    checkLoadData();
#ifndef MMKV_DISABLE_CRYPT
    if (m_crypter) {
        auto itr = m_dicCrypt->find(key);
        if (itr != m_dicCrypt->end()) {
            auto basePtr = (uint8_t *) (m_file->getMemory()) + Fixed32Size;
            return itr->second.toMMBuffer(basePtr, m_crypter);
        }
    } else
#endif
    {
        // 从散列表中获取 key 对应的 value
        auto itr = m_dic->find(key);
        if (itr != m_dic->end()) {
            auto basePtr = (uint8_t *) (m_file->getMemory()) + Fixed32Size;
            return itr->second.toMMBuffer(basePtr);
        }
    }
    MMBuffer nan;
    return nan;
}
```
可以看到 decode 的流程比较简单, 先从内存缓存中获取 key 对应的 value 的 ProtocolBuffer 内存区域, 再解析这块内存区域,
从中获取真正的 value 值

思考
看到这里可能会有一个疑问, 为什么 m_dic 不直接存储 key 和 value 原始数据呢, 这样查询效率不是更快吗?
如此一来查询效率的确会更快, 因为少了 ProtocolBuffer 解码的过程
https://github.com/Tencent/MMKV/wiki/assets/profile_android.png
从图上的结果可以看出, MMKV 的读取性能时略低于 SharedPreferences 的, 这里笔者给出自己的思考

m_dic 在数据重整中也起到了非常重要的作用, 需要依靠 m_dic 将数据写入到 mmap 的文件映射区, 这个过程是非常耗时的, 若是原始的 value,
则需要对所有的 value 再进行一次 ProtocolBuffer 编码操作, 尤其是当数据量比较庞大时, 其带来的性能损耗更是无法忽略的

既然 m_dic 还承担着方便数据复写的功能, 那能否再添加一个内存缓存专门用于存储原始的 value 呢?

当然可以, 这样 MMKV 的读取定是能够达到 SharedPreferences 的水平, 不过 value 的内存消耗则会加倍, MMKV 作为一个轻量级缓存的框架, 
查询时时间的提升幅度还不足以用内存加倍的代价去换取, 我想这是 Tencent 在进行多方面权衡之后, 得到的一个比较合理的解决方案



Sp迁移相关
Android/MMKV/mmkv/src/main/java/com/tencent/mmkv/MMKV.java
读取SP的所有，根据类型再次存储
```
public int importFromSharedPreferences(SharedPreferences preferences) {
        Map<String, ?> kvs = preferences.getAll();
        if (kvs == null || kvs.size() <= 0) {
            return 0;
        }

        for (Map.Entry<String, ?> entry : kvs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }

            if (value instanceof Boolean) {
                encodeBool(nativeHandle, key, (boolean) value);
            } else if (value instanceof Integer) {
                encodeInt(nativeHandle, key, (int) value);
            } else if (value instanceof Long) {
                encodeLong(nativeHandle, key, (long) value);
            } else if (value instanceof Float) {
                encodeFloat(nativeHandle, key, (float) value);
            } else if (value instanceof Double) {
                encodeDouble(nativeHandle, key, (double) value);
            } else if (value instanceof String) {
                encodeString(nativeHandle, key, (String) value);
            } else if (value instanceof Set) {
                encode(key, (Set<String>) value);
            } else {
                simpleLog(MMKVLogLevel.LevelError, "unknown type: " + value.getClass());
            }
        }
        return kvs.size();
    }
```

