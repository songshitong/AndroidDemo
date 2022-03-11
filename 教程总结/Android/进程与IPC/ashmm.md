// 查看相关原理  android12
ashmm Anonymous Shared Memory  android匿名共享内存
https://www.jianshu.com/p/d9bc9c668ba6
https://zhuanlan.zhihu.com/p/146671611

Ashmen(Anonymous Shared Memory) 匿名共享内存是 Android 的 Linux 内核实现的一个驱动, 它以驱动程序的形式实现在内核空间,
用于在进程间进行数据共享

ashmm的应用
1 surfaceflinger
在Android系统中，APP端View视图的数据是如何传递SurfaceFlinger服务的呢？View绘制的数据最终是按照一帧一帧显示到屏幕的，
而每一帧都会占用一定的存储空间，在APP端执行draw的时候，数据很明显是要绘制到APP的进程空间，但是视图窗口要经过SurfaceFlinger图层混排
 才会生成最终的帧，而SurfaceFlinger又运行在另一个独立的服务进程，那么View视图的数据是如何在两个进程间传递的呢，普通的Binder通信肯定不行，
 因为Binder不太适合这种数据量较大的通信，那么View数据的通信采用的是什么IPC手段呢？答案就是共享内存，更精确的说是匿名共享内存。
 共享内存是Linux自带的一种IPC机制，Android直接使用了该模型，不过做出了自己的改进，进而形成了Android的匿名共享内存（Anonymous Shared Memory-Ashmem）。通
 过Ashmem，APP进程同SurfaceFlinger共用一块内存，如此，就不需要进行数据拷贝，APP端绘制完毕，通知SurfaceFlinger端合成，
 再输出到硬件进行显示即可
2 进程间共享体积较大的数据，比如bitmap。
3 提升进程间传输数据的效率，比如ContentProvider基于共享内存进行数据传送。
4 借助Bitmap解码的inPurgeable属性，在android4.x及以下系统版本中实现内存在ashmem中分配，以节省Java堆内存。
  比如fresco图片加载库针对Android4.x及以下的机型对inPurgeable属性的使用。
5 MMKV 的 MemoryFile
//todo ashmm的应用

//todo tempfs
tempfs是Unix-like系统中一种基于内存的文件系统，具有极高的访问效率。
shmem是Linux自带的进程间通信机制：共享内存Shared Memory。
共享内存的虚拟文件记录在/proc/<pid>/maps文件中，pid表示打开这个共享内存文件的进程ID。

使用
Java层借助MemoryFile或者SharedMemory。
Native层借助MemoryHeapBase或者MemoryBase。
Native层直接调用libc的ashmem_create_region和mmap系统调用

其中
MemoryFile基于SharedMemory。 
MemoryBase基于MemoryHeapBase。 
SharedMemory、MemoryHeapBase都是基于ashmem_create_region/mmap。


MemoryFile
MemoryFile是对SharedMemory的包装，官方推荐直接使用SharedMemory。
Applications should generally prefer to use {@link SharedMemory} which offers more flexible access &
  control over the shared memory region than MemoryFile does.


SharedMemory
SharedMemory只能通过调用SharedMemory.create静态方法或者通过Parcel反序列化的方式进行创建。 SharedMemory的创建进程通过SharedMemory.create创建，
  使用进程通过Parcel反序列化创建。
因为SharedMemory类实现了Parcelable，所以可以通过binder跨进程传输。


Linux共享内存
首先看一下两个关键函数，
int shmget(key_t key, size_t size, int shmflg); 该函数用来创建共享内存
void *shmat(int shm_id, const void *shm_addr, int shmflg); 要想访问共享内存，必须将其映射到当前进程的地址空间
参考网上的一个demo，简单的看下，其中key_t是共享内存的唯一标识，可以说，Linux的共享内存其实是有名共享内存，而名字就是key，具体用法如下
读取进程
```
int main()  
{  
    void *shm = NULL;//分配的共享内存的原始首地址  
    struct shared_use_st *shared;//指向shm  
    int shmid;//共享内存标识符  
    //创建共享内存  
    shmid = shmget((key_t)12345, sizeof(struct shared_use_st), 0666|IPC_CREAT);   
    //将共享内存映射到当前进程的地址空间  
    shm = shmat(shmid, 0, 0);
    //设置共享内存  
    shared = (struct shared_use_st*)shm;  
    shared->written = 0;  
    //访问共享内存
    while(1){
        if(shared->written != 0)  { 
            printf("You wrote: %s", shared->text);
             if(strncmp(shared->text, "end", 3) == 0)  
                break;
            }}
    //把共享内存从当前进程中分离  
    if(shmdt(shm) == -1)  { }  
    //删除共享内存  
    if(shmctl(shmid, IPC_RMID, 0) == -1)   {  }  
    exit(EXIT_SUCCESS);  
}  
```

写进程
```
int main()  
{  
    void *shm = NULL;  
    struct shared_use_st *shared = NULL;  
    char buffer[BUFSIZ + 1];//用于保存输入的文本  
    int shmid;  
    //创建共享内存  
    shmid = shmget((key_t) 12345, sizeof(struct shared_use_st), 0666|IPC_CREAT);  
    //将共享内存连接到当前进程的地址空间  
    shm = shmat(shmid, (void*)0, 0);  
    printf("Memory attached at %X\n", (int)shm);  
    //设置共享内存  
    shared = (struct shared_use_st*)shm;  
    while(1)//向共享内存中写数据  
    {  
        //数据还没有被读取，则等待数据被读取,不能向共享内存中写入文本  
        while(shared->written == 1)  
        {  
            sleep(1);  
        }  
        //向共享内存中写入数据  
        fgets(buffer, BUFSIZ, stdin);  
        strncpy(shared->text, buffer, TEXT_SZ);  
        shared->written = 1;  
        if(strncmp(buffer, "end", 3) == 0)  
            running = 0;  
    }  
    //把共享内存从当前进程中分离  
    if(shmdt(shm) == -1)   {    }  
    sleep(2);  
    exit(EXIT_SUCCESS);  
} 
```
可以看到，Linux共享内存通信效率非常高，进程间不需要传递数据，便可以直接访问，缺点也很明显，Linux共享内存没有提供同步的机制，
在使用时，要借助其他的手段来处理进程间同步。Anroid本身在核心态是支持System V的功能，但是bionic库删除了glibc的shmget等函数，
使得android无法采用shmget的方式实现有名共享内存，当然，它也没想着用那个，Android在此基础上，创建了自己的匿名共享内存方式

Android的匿名共享内存
Android可以使用Linux的一切IPC通信方式，包括共享内存，不过Android主要使用的方式是匿名共享内存Ashmem（Anonymous Shared Memory），
 跟原生的不太一样，比如它在自己的驱动中添加了互斥锁，另外通过fd的传递来实现共享内存的传递。

MemoryFile是Android为匿名共享内存而封装的一个对象， 这里通过使用MemoryFile来分析，Android中如何利用共享内存来实现大数据传递，
同时MemoryFile也是进程间大数据传递的一个手段，
开发的时候可以使用：
IMemoryAidlInterface.aidl
```
package com.snail.labaffinity;
import android.os.ParcelFileDescriptor;

interface IMemoryAidlInterface {
    ParcelFileDescriptor getParcelFileDescriptor();
}
```
MemoryFetchService
```
public class MemoryFetchService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MemoryFetchStub();
    }
    static class MemoryFetchStub extends IMemoryAidlInterface.Stub {
        @Override
        public ParcelFileDescriptor getParcelFileDescriptor() throws RemoteException {
            MemoryFile memoryFile = null;
            try {
                memoryFile = new MemoryFile("test_memory", 1024);
                memoryFile.getOutputStream().write(new byte[]{1, 2, 3, 4, 5});
                Method method = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
                FileDescriptor des = (FileDescriptor) method.invoke(memoryFile);
                //传递fd
                return ParcelFileDescriptor.dup(des);
            } catch (Exception e) {}
            return null;
     }}}
```

TestActivity.java
```
 Intent intent = new Intent(MainActivity.this, MemoryFetchService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                byte[] content = new byte[10];
                IMemoryAidlInterface iMemoryAidlInterface
                        = IMemoryAidlInterface.Stub.asInterface(service);
                try {
                    //获取fd
                    ParcelFileDescriptor parcelFileDescriptor = iMemoryAidlInterface.getParcelFileDescriptor();
                    FileDescriptor descriptor = parcelFileDescriptor.getFileDescriptor();
                    FileInputStream fileInputStream = new FileInputStream(descriptor);
                    fileInputStream.read(content);
                } catch (Exception e) {
                }}

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Service.BIND_AUTO_CREATE);   
```
以上是应用层使用匿名共享内存的方法，关键点就是文件描述符（FileDescriptor）的传递，文件描述符是Linux系统中访问与更新文件的主要方式。
从MemoryFile字面上看出，共享内存被抽象成了文件，不过本质也是如此，就是在tmpfs临时文件系统中创建一个临时文件，
（只是创建了节点，而没有看到实际的文件） 该文件与Ashmem驱动程序创建的匿名共享内存对应，可以直接去proc/pid下查看：
//todo tmpfs
android_ashmm_匿名内存共享文件

android_ashmm_匿名内存共享文件.webp

下面就基于MemoryFile主要分析两点，共享内存的分配与传递，先看下MemoryFile的构造函数
frameworks/base/core/java/android/os/MemoryFile.java
```
 public MemoryFile(String name, int length) throws IOException {
        try {
            mSharedMemory = SharedMemory.create(name, length);
            mMapping = mSharedMemory.mapReadWrite();
        } catch (ErrnoException ex) {
            ex.rethrowAsIOException();
        }
    }
```

MemoryFile主要是对SharedMemory的封装
SharedMemory创建
```
  public static @NonNull SharedMemory create(@Nullable String name, int size)
            throws ErrnoException {
        return new SharedMemory(nCreate(name, size));
    }
   private SharedMemory(FileDescriptor fd) {
        if (fd == null) {
            throw new IllegalArgumentException(
                    "Unable to create SharedMemory from a null FileDescriptor");
        }
        if (!fd.valid()) {
            throw new IllegalArgumentException(
                    "Unable to create SharedMemory from closed FileDescriptor");
        }
        mFileDescriptor = fd;
        mSize = nGetSize(mFileDescriptor);
        if (mSize <= 0) {
            throw new IllegalArgumentException("FileDescriptor is not a valid ashmem fd");
        }

        mMemoryRegistration = new MemoryRegistration(mSize);
        mCleaner = Cleaner.create(mFileDescriptor,
                new Closer(mFileDescriptor, mMemoryRegistration));
    }  
```
//todo 负责清理工作的Cleaner，MemoryRegistration
创建过程主要是通过nCreate获取fd

frameworks/base/core/jni/android_os_SharedMemory.cpp
```
jobject SharedMemory_nCreate(JNIEnv* env, jobject, jstring jname, jint size) {

    // Name is optional so we can't use ScopedUtfChars for this as it throws NPE on null
    const char* name = jname ? env->GetStringUTFChars(jname, nullptr) : nullptr;

    int fd = ashmem_create_region(name, size);

    // Capture the error, if there is one, before calling ReleaseStringUTFChars
    int err = fd < 0 ? errno : 0;

    if (name) {
        env->ReleaseStringUTFChars(jname, name);
    }

    if (fd < 0) {
        jniThrowErrnoException(env, "SharedMemory_create", err);
        return nullptr;
    }

    jobject jifd = jniCreateFileDescriptor(env, fd);
    if (jifd == nullptr) {
        close(fd);
    }
    return jifd;
}
```
主要是通过ashmem_create_region创建共享内存,并将fd返回


mapReadWrite共享内存映射到当前进程空间
frameworks/base/core/java/android/os/SharedMemory.java
```
 public @NonNull ByteBuffer mapReadWrite() throws ErrnoException {
        return map(OsConstants.PROT_READ | OsConstants.PROT_WRITE, 0, mSize);
    }
 public @NonNull ByteBuffer map(int prot, int offset, int length) throws ErrnoException {
        checkOpen();
        validateProt(prot);
        ... 
        long address = Os.mmap(0, length, prot, OsConstants.MAP_SHARED, mFileDescriptor, offset);
        boolean readOnly = (prot & OsConstants.PROT_WRITE) == 0;
        Runnable unmapper = new Unmapper(address, length, mMemoryRegistration.acquire());
        return new DirectByteBuffer(length, address, mFileDescriptor, unmapper, readOnly);
    }   
```
mapReadWrite调用mmap将共享内存映射到当前进程空间，之后Java层就能利用FileDescriptor，像访问文件一样访问共享内存
//todo 一个文件在不同的进程的文件描述符fd不同， 拿到后不能直接使用


ashmem_create_region这个函数是如何向Linux申请一块共享内存的呢？
system/core/libcutils/ashmem-dev.cpp
```
//name 一般在/proc/pid/maps
int ashmem_create_region(const char *name, size_t size)
{
    int ret, save_errno;
    if (has_memfd_support()) {
        return memfd_create_region(name ? name : "none", size);
    }
    int fd = __ashmem_open();
    if (fd < 0) {
        return fd;
    }
    if (name) {
        char buf[ASHMEM_NAME_LEN] = {0};

        strlcpy(buf, name, sizeof(buf));
        ret = TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_SET_NAME, buf));
        if (ret < 0) {
            goto error;
        }
    }

    ret = TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_SET_SIZE, size));
    if (ret < 0) {
        goto error;
    }

    return fd;
error:
    save_errno = errno;
    close(fd);
    errno = save_errno;
    return ret;
}

//todo 默认大小 bionic/libc/kernel/uapi/linux/ashmem.h
#define __ASHMEMIOC 0x77
#define ASHMEM_SET_SIZE _IOW(__ASHMEMIOC, 3, size_t)
```
第一步是打开文件并获取fd,默认路径为/dev/ashmem
第二步通过ioctl设置共享内存大小
可以看到，其实并未真正的分配内存，这也符合Linux的风格，只有等到真正的使用的时候，才会通过缺页中断分配内存，接着mmap函数，它会分配内存吗？

__ashmem_open相关
```
static int __ashmem_open()
{
    int fd;
    pthread_mutex_lock(&__ashmem_lock);
    fd = __ashmem_open_locked();
    pthread_mutex_unlock(&__ashmem_lock);
    return fd;
}
static int __ashmem_open_locked()
{
    //获取文件路径  默认是"/dev/ashmem" + boot_id
    static const std::string ashmem_device_path = get_ashmem_device_path();

    if (ashmem_device_path.empty()) {
        return -1;
    }
    //使用open获取fd
    int fd = TEMP_FAILURE_RETRY(open(ashmem_device_path.c_str(), O_RDWR | O_CLOEXEC));

    // fallback for APEX w/ use_vendor on Q, which would have still used /dev/ashmem
    if (fd < 0) {
        int saved_errno = errno;
        fd = TEMP_FAILURE_RETRY(open("/dev/ashmem", O_RDWR | O_CLOEXEC));
        if (fd < 0) {
            /* Q launching devices and newer must not reach here since they should have been
             * able to open ashmem_device_path */
            ALOGE("Unable to open ashmem device %s (error = %s) and /dev/ashmem(error = %s)",
                  ashmem_device_path.c_str(), strerror(saved_errno), strerror(errno));
            return fd;
        }
    }
    struct stat st;
    int ret = TEMP_FAILURE_RETRY(fstat(fd, &st));
    if (ret < 0) {
        int save_errno = errno;
        close(fd);
        errno = save_errno;
        return ret;
    }
    if (!S_ISCHR(st.st_mode) || !st.st_rdev) {
        close(fd);
        errno = ENOTTY;
        return -1;
    }

    __ashmem_rdev = st.st_rdev;
    return fd;
}

static std::string get_ashmem_device_path() {
    static const std::string boot_id_path = "/proc/sys/kernel/random/boot_id";
    std::string boot_id;
    if (!android::base::ReadFileToString(boot_id_path, &boot_id)) {
        ALOGE("Failed to read %s: %s.\n", boot_id_path.c_str(), strerror(errno));
        return "";
    };
    boot_id = android::base::Trim(boot_id);
    return "/dev/ashmem" + boot_id;
}
```
open的驱动层    common-android-mainline内核
common/drivers/staging/android/ashmem.c   
```
static int ashmem_open(struct inode *inode, struct file *file)
{
	struct ashmem_area *asma;
	int ret;

	ret = generic_file_open(inode, file);
	if (ret)
		return ret;

	asma = kmem_cache_zalloc(ashmem_area_cachep, GFP_KERNEL);
	if (!asma)
		return -ENOMEM;
    //初始化unpinned_list
	INIT_LIST_HEAD(&asma->unpinned_list);
	memcpy(asma->name, ASHMEM_NAME_PREFIX, ASHMEM_NAME_PREFIX_LEN);
	asma->prot_mask = PROT_MASK;
	file->private_data = asma;

	return 0;
}
```
主要是创建一个ashmem_area对象

mmap相关
libcore/luni/src/main/java/android/system/Os.java
```
    public static long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException {
     return Libcore.os.mmap(address, byteCount, prot, flags, fd, offset);
      }
```
Libcore.os 指向linux
libcore/luni/src/main/java/libcore/io/Libcore.java
```
public final class Libcore {
    /**
     * Direct access to syscalls. Code should strongly prefer using {@link #os}
     * unless it has a strong reason to bypass the helpful checks/guards that it
     * provides.
     */
    public static final Os rawOs = new Linux();
 }
```

libcore/luni/src/main/java/libcore/io/Linux.java
```
public native long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException;
```
libcore/luni/src/main/native/libcore_io_Linux.cpp
```
static jlong Linux_mmap(JNIEnv* env, jobject, jlong address, jlong byteCount, jint prot, jint flags, jobject javaFd, jlong offset) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    void* suggestedPtr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    void* ptr = mmap64(suggestedPtr, byteCount, prot, flags, fd, offset);
    if (ptr == MAP_FAILED) {
        throwErrnoException(env, "mmap");
    }
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(ptr));
}
```
又调用了mmap64

bionic/libc/bionic/mmap.cpp
```
void* mmap64(void* addr, size_t size, int prot, int flags, int fd, off64_t offset) {
  if (offset < 0 || (offset & ((1UL << MMAP2_SHIFT)-1)) != 0) {
    errno = EINVAL;
    return MAP_FAILED;
  }

  // prevent allocations large enough for `end - start` to overflow
  size_t rounded = __BIONIC_ALIGN(size, PAGE_SIZE);
  if (rounded < size || rounded > PTRDIFF_MAX) {
    errno = ENOMEM;
    return MAP_FAILED;
  }

  bool is_private_anonymous =
      (flags & (MAP_PRIVATE | MAP_ANONYMOUS)) == (MAP_PRIVATE | MAP_ANONYMOUS);
  bool is_stack_or_grows_down = (flags & (MAP_STACK | MAP_GROWSDOWN)) != 0;

  void* result = __mmap2(addr, size, prot, flags, fd, offset >> MMAP2_SHIFT);

  if (result != MAP_FAILED && kernel_has_MADV_MERGEABLE &&
      is_private_anonymous && !is_stack_or_grows_down) {
    ErrnoRestorer errno_restorer;
    int rc = madvise(result, size, MADV_MERGEABLE);
    if (rc == -1 && errno == EINVAL) {
      kernel_has_MADV_MERGEABLE = false;
    }
  }

  return result;
}

//todo extern "C"
// mmap2(2) is like mmap(2), but the offset is in 4096-byte blocks, not bytes.
extern "C" void*  __mmap2(void*, size_t, int, int, int, size_t);

#define MMAP2_SHIFT 12 // 2**12 == 4096
```
mmap的驱动层  通用内核
common/drivers/staging/android/ashmem.c
```
static int ashmem_mmap(struct file *file, struct vm_area_struct *vma)
{
	static struct file_operations vmfile_fops;
	struct ashmem_area *asma = file->private_data;
	int ret = 0;
	mutex_lock(&ashmem_mutex);
    ...
	if (!asma->file) {
	    //"dev/ashmem"
		char *name = ASHMEM_NAME_DEF;
		struct file *vmfile;
		struct inode *inode;

		if (asma->name[ASHMEM_NAME_PREFIX_LEN] != '\0')
			name = asma->name;

        // 这里创建的临时文件其实是备份用的临时文件，之类的临时文件有文章说只对内核态可见，用户态不可见，我们也没有办法通过命令查询到 ，
        //可以看做是个隐藏文件，用户空间看不到！！
        <!--校准真正操作的文件--
		vmfile = shmem_file_setup(name, asma->size, vma->vm_flags);
		if (IS_ERR(vmfile)) {
			ret = PTR_ERR(vmfile);
			goto out;
		}
		vmfile->f_mode |= FMODE_LSEEK;
		inode = file_inode(vmfile);
		lockdep_set_class(&inode->i_rwsem, &backing_shmem_inode_class);
		asma->file = vmfile;
		/*
		 * override mmap operation of the vmfile so that it can't be
		 * remapped which would lead to creation of a new vma with no
		 * asma permission checks. Have to override get_unmapped_area
		 * as well to prevent VM_BUG_ON check for f_ops modification.
		 */
		if (!vmfile_fops.mmap) {
			vmfile_fops = *vmfile->f_op;
			vmfile_fops.mmap = ashmem_vmfile_mmap;
			vmfile_fops.get_unmapped_area =
					ashmem_vmfile_get_unmapped_area;
		}
		vmfile->f_op = &vmfile_fops;
	}
	get_file(asma->file);

	/*
	 * XXX - Reworked to use shmem_zero_setup() instead of
	 * shmem_set_file while we're in staging. -jstultz
	 */
	if (vma->vm_flags & VM_SHARED) {
		ret = shmem_zero_setup(vma);
		if (ret) {
			fput(asma->file);
			goto out;
		}
	} else {
		vma_set_anonymous(vma);
	}

	vma_set_file(vma, asma->file);
	/* XXX: merge this with the get_file() above if possible */
	fput(asma->file);

out:
	mutex_unlock(&ashmem_mutex);
	return ret;
}
```
其实这里就复用了Linux的共享内存机制，虽然说是匿名共享内存，但底层其实还是给共享内存设置了名称（前缀ASHMEM_NAME_DEF+名字），
如果名字未设置，那就默认使用ASHMEM_NAME_DEF作为名称。不过，在这里没直接看到内存分配的函数。但是，有两个函数shmem_file_setup与
shmem_zero_setup很重要，也是共享内存比较不好理解的地方，shmem_file_setup是原生linux的共享内存机制，不过Android也修改Linux共享内存的驱动代码，
匿名共享内存其实就是在Linux共享内存的基础上做了改进

common/mm/shmem.c  common内核
```
//get an unlinked file living in tmpfs
//name for dentry (to be seen in /proc/<pid>/maps
struct file *shmem_file_setup(const char *name, loff_t size, unsigned long flags)
{
	return __shmem_file_setup(shm_mnt, name, size, flags, 0);
}
EXPORT_SYMBOL_GPL(shmem_file_setup);

static struct file *__shmem_file_setup(struct vfsmount *mnt, const char *name, loff_t size,
				       unsigned long flags, unsigned int i_flags)
{
	struct inode *inode;
	struct file *res;
   ...
   //分配inode，分配成功就好比建立了文件，也许并未存在真实文件映射
	inode = shmem_get_inode(mnt->mnt_sb, NULL, S_IFREG | S_IRWXUGO, 0,
				flags);
	if (unlikely(!inode)) {
		shmem_unacct_size(flags, size);
		return ERR_PTR(-ENOSPC);
	}
	inode->i_flags |= i_flags;
	inode->i_size = size;
	clear_nlink(inode);	/* It is unlinked */
	//todo这是啥
	res = ERR_PTR(ramfs_nommu_expand_for_mapping(inode, size));
	if (!IS_ERR(res))
	    //绑定，并指定该文件操作指针为shmem_file_operations
		res = alloc_file_pseudo(inode, mnt, name, O_RDWR,
				&shmem_file_operations);
	if (IS_ERR(res))
		iput(inode);
	return res;
}
```
//todo 驱动没咋看懂   libc怎么到的驱动层   缺页中断
通过shmem_file_setup在tmpfs临时文件系统中创建一个临时文件（也许只是内核中的一个inode节点），该文件与Ashmem驱动程序创建的匿名共享内存对应，
不过用户态并不能看到该临时文件，之后就能够使用该临时文件了，注意共享内存机制真正使用map的对象其实是这个临时文件，而不是ashmem设备文件，
这里之所以是一次mmap，主要是通过vma->vm_file = asma->file完成map对象的替换，当映射的内存引起缺页中断的时候，
就会调用shmem_file_setup创建的对象的函数，而不是ashmem的，看下临时文件的对应的hook函数
```
int shmem_zero_setup(struct vm_area_struct *vma)
{
	struct file *file;
	loff_t size = vma->vm_end - vma->vm_start;
	/*
	 * Cloning a new file under mmap_lock leads to a lock ordering conflict
	 * between XFS directory reading and selinux: since this file is only
	 * accessible to the user through its mapping, use S_PRIVATE flag to
	 * bypass file security, in the same way as shmem_kernel_file_setup().
	 */
	file = shmem_kernel_file_setup("dev/zero", size, vma->vm_flags);
	if (IS_ERR(file))
		return PTR_ERR(file);

	if (vma->vm_file)
		fput(vma->vm_file);
	vma->vm_file = file;
	//设置一些操作  例如缺页中断shmem_fault
	vma->vm_ops = &shmem_vm_ops;

	if (IS_ENABLED(CONFIG_TRANSPARENT_HUGEPAGE) &&
			((vma->vm_start + ~HPAGE_PMD_MASK) & HPAGE_PMD_MASK) <
			(vma->vm_end & HPAGE_PMD_MASK)) {
		khugepaged_enter(vma, vma->vm_flags);
	}

	return 0;
}
```


到这里回到之前的MemoryFile，看一下写操作：
frameworks/base/core/java/android/os/MemoryFile.java
```
  public void writeBytes(byte[] buffer, int srcOffset, int destOffset, int count)
            throws IOException {
        beginAccess();
        try {
            mMapping.position(destOffset);
            mMapping.put(buffer, srcOffset, count);
        } finally {
            endAccess();
        }
    }
 private void beginAccess() throws IOException {
        checkActive();
        if (mAllowPurging) {
            if (native_pin(mSharedMemory.getFileDescriptor(), true)) {
                throw new IOException("MemoryFile has been purged");
            }
        }
    }
    private void endAccess() throws IOException {
        if (mAllowPurging) {
            native_pin(mSharedMemory.getFileDescriptor(), false);
        }
    }    
```
读写操作主要通过ByteBuffer直接读写内存
```
libcore/ojluni/src/main/java/java/nio/DirectByteBuffer.java
 private ByteBuffer put(long a, byte x) {
        Memory.pokeByte(a, x);
        return this;
    }
libcore/luni/src/main/java/libcore/io/Memory.java
public static native void pokeByte(long address, byte value);

libcore/luni/src/main/native/libcore_io_Memory.cpp 
static void Memory_pokeByte(JNIEnv*, jclass, jlong dstAddress, jbyte value) {
    *cast<jbyte*>(dstAddress) = value;
}   
```
//todo java nio的实现  Memory的操作还有 GetByteArrayRegion  unaligned未对齐内存


当使用Ashmem分配了一块内存，部分不被使用时，就可以将这块内存unpin掉，内核可以将unpin对应的物理页面回收,回收后的内存还可以再次被获得
(通过缺页handler)，因为unpin操作并不会改变已经mmap的地址空间，不过，MemoryFile只会操作整个共享内存，而不会分块访问，
所以pin与unpin对于它没多大意义，可以看做整个区域都是pin或者unpin的，首次通过env->GetByteArrayRegion访问会引发缺页中断，
进而调用tmpfs 文件的相应操作，分配物理页，在Android现在的内核中，缺页中断对应的vm_operations_struct中的函数是fault
common/mm/shmem.c
```
static const struct vm_operations_struct shmem_vm_ops = {
	.fault		= shmem_fault,
	.map_pages	= filemap_map_pages,
#ifdef CONFIG_NUMA
	.set_policy     = shmem_set_policy,
	.get_policy     = shmem_get_policy,
#endif
};
```
当mmap的tmpfs文件引发缺页中断时， 就会调用shmem_fault函数，
```
static vm_fault_t shmem_fault(struct vm_fault *vmf)
{
	struct vm_area_struct *vma = vmf->vma;
	struct inode *inode = file_inode(vma->vm_file);
	gfp_t gfp = mapping_gfp_mask(inode->i_mapping);
	int err;
	vm_fault_t ret = VM_FAULT_LOCKED;

	/*
	 * Trinity finds that probing a hole which tmpfs is punching can
	 * prevent the hole-punch from ever completing: which in turn
	 * locks writers out with its hold on i_rwsem.  So refrain from
	 * faulting pages into the hole while it's being punched.  Although
	 * shmem_undo_range() does remove the additions, it may be unable to
	 * keep up, as each new page needs its own unmap_mapping_range() call,
	 * and the i_mmap tree grows ever slower to scan if new vmas are added.
	 *
	 * It does not matter if we sometimes reach this check just before the
	 * hole-punch begins, so that one fault then races with the punch:
	 * we just need to make racing faults a rare case.
	 *
	 * The implementation below would be much simpler if we just used a
	 * standard mutex or completion: but we cannot take i_rwsem in fault,
	 * and bloating every shmem inode for this unlikely case would be sad.
	 */
	if (unlikely(inode->i_private)) {
		struct shmem_falloc *shmem_falloc;

		spin_lock(&inode->i_lock);
		shmem_falloc = inode->i_private;
		if (shmem_falloc &&
		    shmem_falloc->waitq &&
		    vmf->pgoff >= shmem_falloc->start &&
		    vmf->pgoff < shmem_falloc->next) {
			struct file *fpin;
			wait_queue_head_t *shmem_falloc_waitq;
			DEFINE_WAIT_FUNC(shmem_fault_wait, synchronous_wake_function);

			ret = VM_FAULT_NOPAGE;
			fpin = maybe_unlock_mmap_for_io(vmf, NULL);
			if (fpin)
				ret = VM_FAULT_RETRY;

			shmem_falloc_waitq = shmem_falloc->waitq;
			prepare_to_wait(shmem_falloc_waitq, &shmem_fault_wait,
					TASK_UNINTERRUPTIBLE);
			spin_unlock(&inode->i_lock);
			schedule();

			/*
			 * shmem_falloc_waitq points into the shmem_fallocate()
			 * stack of the hole-punching task: shmem_falloc_waitq
			 * is usually invalid by the time we reach here, but
			 * finish_wait() does not dereference it in that case;
			 * though i_lock needed lest racing with wake_up_all().
			 */
			spin_lock(&inode->i_lock);
			finish_wait(shmem_falloc_waitq, &shmem_fault_wait);
			spin_unlock(&inode->i_lock);

			if (fpin)
				fput(fpin);
			return ret;
		}
		spin_unlock(&inode->i_lock);
	}

	err = shmem_getpage_gfp(inode, vmf->pgoff, &vmf->page, SGP_CACHE,
				  gfp, vma, vmf, &ret);
	if (err)
		return vmf_error(err);
	return ret;
}
```
到这里，就可以看到会调用shmem_getpage_gfp函数分配真实的物理页，具体的分配策略比较复杂，不在分析
//todo Linux的内存映射



Android匿名共享内存的pin与unpin
pin本身的意思是压住，定住，ashmem_pin_region和ashmem_unpin_region这两个函数从字面上来说，就是用来对匿名共享内存锁定和解锁，
标识哪些内存正在使用需要锁定，哪些内存是不使用的，这样，ashmem驱动程序可以一定程度上辅助内存管理，提供一定的内存优化能力。
匿名共享内存创建之初时，所有的内存都是pinned状态，只有用户主动申请，才会unpin一块内存，只有对于unpinned状态的内存块，
用户才可以重新pin。现在仔细梳理一下驱动，看下pin与unpin的实现

native_pin
```
frameworks/base/core/java/android/os/MemoryFile.java
private static native boolean native_pin(FileDescriptor fd, boolean pin) throws IOException;

frameworks/base/core/jni/android_os_MemoryFile.cpp
static jboolean android_os_MemoryFile_pin(JNIEnv* env, jobject clazz, jobject fileDescriptor,
        jboolean pin) {
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    int result = (pin ? ashmem_pin_region(fd, 0, 0) : ashmem_unpin_region(fd, 0, 0));
    if (result < 0) {
        jniThrowException(env, "java/io/IOException", NULL);
    }
    return result == ASHMEM_WAS_PURGED;
}

int ashmem_pin_region(int fd, size_t offset, size_t len)
{
    if (!pin_deprecation_warn || debug_log) {
        ALOGE("Pinning is deprecated since Android Q. Please use trim or other methods.\n");
        pin_deprecation_warn = true;
    }

    if (has_memfd_support() && !memfd_is_ashmem(fd)) {
        return 0;
    }

    // TODO: should LP64 reject too-large offset/len?
    ashmem_pin pin = { static_cast<uint32_t>(offset), static_cast<uint32_t>(len) };
    return __ashmem_check_failure(fd, TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_PIN, &pin)));
}
int ashmem_unpin_region(int fd, size_t offset, size_t len)
{
    if (!pin_deprecation_warn || debug_log) {
        ALOGE("Pinning is deprecated since Android Q. Please use trim or other methods.\n");
        pin_deprecation_warn = true;
    }

    if (has_memfd_support() && !memfd_is_ashmem(fd)) {
        return 0;
    }

    // TODO: should LP64 reject too-large offset/len?
    ashmem_pin pin = { static_cast<uint32_t>(offset), static_cast<uint32_t>(len) };
    return __ashmem_check_failure(fd, TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_UNPIN, &pin)));
}
```
可以看到主要通过ioctl 实现pin或unpin的功能

看一下驱动层
common/drivers/staging/android/ashmem.c
```
static int __init ashmem_init(void)
{
	int ret = -ENOMEM;
   <!--创建 ahemem_area 高速缓存-->
	ashmem_area_cachep = kmem_cache_create("ashmem_area_cache",
					       sizeof(struct ashmem_area),
					       0, 0, NULL);
	if (!ashmem_area_cachep) {
		pr_err("failed to create slab cache\n");
		goto out;
	}
    <!--创建 ahemem_range高速缓存-->
	ashmem_range_cachep = kmem_cache_create("ashmem_range_cache",
						sizeof(struct ashmem_range),
						0, SLAB_RECLAIM_ACCOUNT, NULL);
	if (!ashmem_range_cachep) {
		pr_err("failed to create slab cache\n");
		goto out_free1;
	}
    <!--注册杂项设备-->
	ret = misc_register(&ashmem_misc);
	if (ret) {
		pr_err("failed to register misc device!\n");
		goto out_free2;
	}

	ret = register_shrinker(&ashmem_shrinker);
	if (ret) {
		pr_err("failed to register shrinker!\n");
		goto out_demisc;
	}

	pr_info("initialized\n");

	return 0;

out_demisc:
	misc_deregister(&ashmem_misc);
out_free2:
	kmem_cache_destroy(ashmem_range_cachep);
out_free1:
	kmem_cache_destroy(ashmem_area_cachep);
out:
	return ret;
}
device_initcall(ashmem_init);
```

打开ashem的时候 ，会利用ashmem_area_cachep告诉缓存新建ashmem_area对象，并初始化unpinned_list，开始肯定为null
```
static int ashmem_open(struct inode *inode, struct file *file)
{
    ...
	asma = kmem_cache_zalloc(ashmem_area_cachep, GFP_KERNEL);
	if (!asma)
		return -ENOMEM;
    //初始化unpinned_list
	INIT_LIST_HEAD(&asma->unpinned_list);
	...
	return 0;
}
```
ashmem_unpin功能
```
static int ashmem_unpin(struct ashmem_area *asma, size_t pgstart, size_t pgend,
			struct ashmem_range **new_range)
{
	struct ashmem_range *range, *next;
	unsigned int purged = ASHMEM_NOT_PURGED;
restart:
	list_for_each_entry_safe(range, next, &asma->unpinned_list, unpinned) {
		/* short circuit: this is our insertion point */
		if (range_before_page(range, pgstart))
			break;

		/*
		 * The user can ask us to unpin pages that are already entirely
		 * or partially pinned. We handle those two cases here.
		 */
		if (page_range_subsumed_by_range(range, pgstart, pgend))
			return 0;
		if (page_range_in_range(range, pgstart, pgend)) {
			pgstart = min(range->pgstart, pgstart);
			pgend = max(range->pgend, pgend);
			purged |= range->purged;
			range_del(range);
			goto restart;
		}
	}

	range_alloc(asma, range, purged, pgstart, pgend, new_range);
	return 0;
}
```
这个函数主要作用是创建一个ashmem_range ，并插入ashmem_area的unpinned_list，在插入的时候可能会有合行为，这个时候要首先
  删除原来的unpin ashmem_range，之后新建一个合并后的ashmem_range插入unpinned_list。
android_ashmm_内存unpin操作.webp


下面来看一下pin函数的实现，先理解了unpin，pin就很好理解了，其实就是将一块共享内存投入使用，如果它位于unpinedlist，就将它摘下来：
android_ashmm_内存pin操作.webp
```
static int ashmem_pin(struct ashmem_area *asma, size_t pgstart, size_t pgend,
		      struct ashmem_range **new_range)
{
	struct ashmem_range *range, *next;
	int ret = ASHMEM_NOT_PURGED;

	list_for_each_entry_safe(range, next, &asma->unpinned_list, unpinned) {
		/* moved past last applicable page; we can short circuit */
		if (range_before_page(range, pgstart))
			break;

		/*
		 * The user can ask us to pin pages that span multiple ranges,
		 * or to pin pages that aren't even unpinned, so this is messy.
		 *
		 * Four cases:
		 * 1. The requested range subsumes an existing range, so we
		 *    just remove the entire matching range.
		 * 2. The requested range overlaps the start of an existing
		 *    range, so we just update that range.
		 * 3. The requested range overlaps the end of an existing
		 *    range, so we just update that range.
		 * 4. The requested range punches a hole in an existing range,
		 *    so we have to update one side of the range and then
		 *    create a new range for the other side.
		 */
		if (page_range_in_range(range, pgstart, pgend)) {
			ret |= range->purged;

			/* Case #1: Easy. Just nuke the whole thing. */
			if (page_range_subsumes_range(range, pgstart, pgend)) {
				range_del(range);
				continue;
			}

			/* Case #2: We overlap from the start, so adjust it */
			if (range->pgstart >= pgstart) {
				range_shrink(range, pgend + 1, range->pgend);
				continue;
			}

			/* Case #3: We overlap from the rear, so adjust it */
			if (range->pgend <= pgend) {
				range_shrink(range, range->pgstart,
					     pgstart - 1);
				continue;
			}

			/*
			 * Case #4: We eat a chunk out of the middle. A bit
			 * more complicated, we allocate a new range for the
			 * second half and adjust the first chunk's endpoint.
			 */
			range_alloc(asma, range, range->purged,
				    pgend + 1, range->pgend, new_range);
			range_shrink(range, range->pgstart, pgstart - 1);
			break;
		}
	}

	return ret;
}
```


Android进程共享内存的传递-fd文件描述符的传递
开头的例子，进程通信通过传递fd,实现内存共享
原生Linux共享内存是通过传递已知的key来处理的，但是Android中不存在这种机制，Android是怎么处理的呢？那就是通过Binder传递文件描述符来处理，
Android的Binder对于fd的传递也做了适配，原理其实就是在内核层为要传递的目标进程转换fd，因为在linux中fd只是对本进程是有效、且唯一，
进程A打开一个文件得到一个fd，不能直接为进程B使用，因为B中那个fd可能压根无效、或者对应其他文件，不过，虽然同一个文件可以有多个文件描述符，
但是文件只有一个，在内核层也只会对应一个inode节点与file对象，这也是内核层可以传递fd的基础，Binder驱动通过当前进程的fd找到对应的文件，
然后为目标进程新建fd，并传递给目标进程，核心就是把进程A中的fd转化成进程B中的fd，看一下Android中binder的实现
//todo fd的介绍
common/drivers/android/binder.c
```
static void binder_transaction(struct binder_proc *proc,
			       struct binder_thread *thread,
			       struct binder_transaction_data *tr, int reply,
			       binder_size_t extra_buffers_size)
{
case BINDER_TYPE_FD: {
           struct binder_fd_object *fp = to_binder_fd_object(hdr);
			binder_size_t fd_offset = object_offset +
				(uintptr_t)&fp->fd - (uintptr_t)fp;
			int ret = binder_translate_fd(fp->fd, fd_offset, t,
						      thread, in_reply_to);

			fp->pad_binder = 0;
			if (ret < 0 ||
			    binder_alloc_copy_to_buffer(&target_proc->alloc,
							t->buffer,
							object_offset,
							fp, sizeof(*fp))) {
				return_error = BR_FAILED_REPLY;
				return_error_param = ret;
				return_error_line = __LINE__;
				goto err_translate_failed;
			}
		} break;
}

static int binder_translate_fd(u32 fd, binder_size_t fd_offset,
			       struct binder_transaction *t,
			       struct binder_thread *thread,
			       struct binder_transaction *in_reply_to)
{
	struct binder_proc *proc = thread->proc;
	struct binder_proc *target_proc = t->to_proc;
	struct binder_txn_fd_fixup *fixup;
	struct file *file;
	int ret = 0;
	bool target_allows_fd;

	if (in_reply_to)
		target_allows_fd = !!(in_reply_to->flags & TF_ACCEPT_FDS);
	else
		target_allows_fd = t->buffer->target_node->accept_fds;
	...
    <!--关键点1 可以根据fd在当前进程获取到file ，多个进程打开同一文件，在内核中对应的file是一样-->
	file = fget(fd);
	if (!file) {
		binder_user_error("%d:%d got transaction with invalid fd, %d\n",
				  proc->pid, thread->pid, fd);
		ret = -EBADF;
		goto err_fget;
	}
	ret = security_binder_transfer_file(proc->cred, target_proc->cred, file);
	if (ret < 0) {
		ret = -EPERM;
		goto err_security;
	}

	/*
	 * Add fixup record for this transaction. The allocation
	 * of the fd in the target needs to be done from a
	 * target thread.
	 */
	fixup = kzalloc(sizeof(*fixup), GFP_KERNEL);
	if (!fixup) {
		ret = -ENOMEM;
		goto err_alloc;
	}
	fixup->file = file;
	fixup->offset = fd_offset;
	trace_binder_transaction_fd_send(t, fd, fixup->offset);
	list_add_tail(&fixup->fixup_entry, &t->fd_fixups);

	return ret;
....
}

```
//todo没找到
<!--关键点2,为目标进程获取空闲fd-->
<!--关键点3将目标进程的空闲fd与file绑定-->
Android_ashmm_fd转换.webp


为什么看不到匿名共享内存对应的文件呢
为什么Android用户看不到共享内存对应的文件，Google到的说法是：在内核没有定义defined(CONFIG_TMPFS) 情况下，tmpfs对用户不可见：
If CONFIG_TMPFS is not set, the user visible part of tmpfs is not build. But the internal mechanisms are always present.
而在Android的shmem.c驱动中确实没有defined(CONFIG_TMPFS) ，这里只是猜测，也许还有其他解释，如有了解，望能指导。


匿名共享内存的优点也是BUG
匿名共享内存不会占用Dalvik Heap与Native Heap，不会导致OOM，这是优点，同时也是缺点，因为如果肆意使用，会导致系统资源不足，性能下降
//这里说的是占用仍然占用内存
Android_ashmm_内存占用.webp


另外共享存占用空间的计算，只会计算到第一个创建它的进程中，其他进程不将ashmem计算在内。

总结
Android匿名共享内存是基于Linux共享内存的，都是在tmpfs文件系统上新建文件，并将其映射到不同的进程空间，从而达到共享内存的目的，
只是，Android在Linux的基础上进行了改造，并借助Binder+fd文件描述符实现了共享内存的传递。




评论
为什么看不到匿名共享内存对应的文件呢？
关键原因:
ashmem_mmap 函数里 调用 shmem_file_setup 建立匿名共享内存的文件是外部看不到的，可以看函数说明:
/**
* shmem_kernel_file_setup - get an unlinked file living in tmpfs which must be
* kernel internal. There will be NO LSM permission checks against the
* underlying inode. So users of this interface must do LSM checks at a
* higher layer. The one user is the big_key implementation. LSM checks
* are provided at the key level rather than the inode level.
* @name: name for dentry (to be seen in /proc/<pid>/maps
* @size: size to be set for the file
* @flags: VM_NORESERVE suppresses pre-accounting of the entire object size
  */
  struct file *shmem_kernel_file_setup(const char *name, loff_t size, unsigned long flags)
  我们传给 该函数的参数类似 dev/ashmem/xxx
  实际上 shmem 建了一个文件系统挂载， 挂载在/目录下，并且使用的是tmpfs文件系统，shmem_kernel_file_setup 其实就是在 /目录下 
  新建了 dev/ashmem/xxx的文件，tmpfs是一个内存文件系统，sheme建立文件系统挂载时 使用的 是 kern_mount函数，挂载时，
  并未加入到进程的namespace中，即taskstruct ->nsproxy -> mnt_ns， 所以看 进程的mounts 信息，根本看不到这个挂载，
  也就是说 通过 cat /proc/pid/mounts 看不到这个挂载，/proc/mounts 其实是软链到 /proc/self/mounts 而已，因此看不到这个挂载信息 


linux源码结构，系统调用和驱动的区别
https://sharrychoo.github.io/blog/android-source/dc-ashmem#%E4%BA%8C-open-%E7%B3%BB%E7%BB%9F%E8%B0%83%E7%94%A8
kmem_cache 描述物理内存缓冲区, 使用的是 Linux 中分配小物理内存时的 slab allocator 技术, 与通过伙伴系统分配大内存不同, 
使用 kmem_cache 进行物理内存分配的策略会更加的高效, 内核中描述进程的 task_struct 也是通过 kmem_cache 来快速分配物理页面的, 
这里就不再赘述了

这里在创建 kmem_cache 时, 指定了每一个区域的大小指定为 ashmem_area 的大小, 也就是说, 
当我们在使用 Ashmem 共享内存创建 ashmem_area 结构体时, 就可以高效的使用 kmem_cache 来完成了