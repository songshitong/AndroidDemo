todo 完成

http://gityuan.com/2017/03/26/load_library/    android_12.0.0_r3
C++动态库加载
所需要的头文件的#include, 最为核心的方法如下:
```
void *dlopen(const char * pathname,int mode);  //打开动态库  
void *dlsym(void *handle,const char *name);  //获取动态库对象地址  
char *dlerror(vid);   //错误检测  
int dlclose(void * handle); //关闭动态库   
```
对于动态库加载过程先通过dlopen()打开动态库文件，再通过dlsym()获取动态库对象地址，加载完成则需要dlclose()关闭动态库。

Java动态库加载
对于android上层的Java代码来说，将以上方法都做好封装，只需要一行代码就即可完成动态库的加载过程：
```
System.load("/data/local/tmp/libgityuan_jni.so");
System.loadLibrary("gityuan_jni");
```
以上两个方法都用于加载动态库，两者的区别如下：

加载的路径不同：System.load(String filename)是指定动态库的完整路径名；而System.loadLibrary(String libname)则只会从指定lib目录下查找，并加上lib前缀和.so后缀；
自动加载库的依赖库的不同：System.load(String filename)不会自动加载依赖库；而System.loadLibrary(String libname)会自动加载依赖库。



动态库加载过程
System.loadLibrary
libcore/ojluni/src/main/java/java/lang/System.java
```
 public static void loadLibrary(String libname) {
        Runtime.getRuntime().loadLibrary0(Reflection.getCallerClass(), libname);
    }
```

/libcore/ojluni/src/main/java/java/lang/Runtime.java
```
 void loadLibrary0(ClassLoader loader, String libname) {
          // Pass null for callerClass, we don't know it at this point. Passing null preserved
          // the behavior when we used to not pass the class.
          loadLibrary0(loader, null, libname);
      }
      
      
 private synchronized void loadLibrary0(ClassLoader loader, Class<?> callerClass, String libname) {
          if (libname.indexOf((int)File.separatorChar) != -1) {
              throw new UnsatisfiedLinkError(
      "Directory separator should not appear in library name: " + libname);
          }
          String libraryName = libname;
          // Android-note: BootClassLoader doesn't implement findLibrary(). http://b/111850480
          // Android's class.getClassLoader() can return BootClassLoader where the RI would
          // have returned null; therefore we treat BootClassLoader the same as null here.
          if (loader != null && !(loader instanceof BootClassLoader)) {
               //根据动态库名查看相应动态库的文件路径
              String filename = loader.findLibrary(libraryName);
              if (filename == null &&
                      (loader.getClass() == PathClassLoader.class ||
                       loader.getClass() == DelegateLastClassLoader.class)) {                
                  filename = System.mapLibraryName(libraryName);
              }
             ...
              String error = nativeLoad(filename, loader);
              ...
              return;
          }
          //默认路径System.getProperty("java.library.path");
          getLibPaths();
          //当loader为空的情况下执行
          String filename = System.mapLibraryName(libraryName);
          String error = nativeLoad(filename, loader, callerClass);
          if (error != null) {
              throw new UnsatisfiedLinkError(error);
          }
      }

private String[] getLibPaths() {
          if (mLibPaths == null) {
              synchronized(this) {
                  if (mLibPaths == null) {
                      mLibPaths = initLibPaths();
                  }
              }
          }
          return mLibPaths;
      }
  
      private static String[] initLibPaths() {
          String javaLibraryPath = System.getProperty("java.library.path");
          if (javaLibraryPath == null) {
              return EmptyArray.STRING;
          }
          String[] paths = javaLibraryPath.split(":");
          // Add a '/' to the end of each directory so we don't have to do it every time.
          for (int i = 0; i < paths.length; ++i) {
              if (!paths[i].endsWith("/")) {
                  paths[i] += "/";
              }
          }
          return paths;
      }      
```
该方法主要是找到目标库所在路径后调用nativeLoad来真正用于加载动态库，其中会根据loader是否为空中间过程略有不同，分两种情况：
当loader不为空时, 则通过loader.findLibrary()查看目标库所在路径;
当loader为空时, 则从默认目录mLibPaths下来查找是否存在该动态库;


ClassLoader一般来说都是PathClassLoader，从该对象的findLibrary说起. 由于PathClassLoader继承于 BaseDexClassLoader对象, 
并且没有覆写该方法, 故调用其父类所对应的方法.
findLibrary
libcore/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
```
public String findLibrary(String name) {
          return pathList.findLibrary(name);
     }
```
/libcore/dalvik/src/main/java/dalvik/system/DexPathList.java
//dexPath一般是指apk所在路径
```
 DexPathList(ClassLoader definingContext, String dexPath,
              String librarySearchPath, File optimizedDirectory, boolean isTrusted) {
         ...
  
          this.definingContext = definingContext;
  
          ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
          //记录所有的dexFile文件
          this.dexElements = makeDexElements(splitDexPath(dexPath), optimizedDirectory,
                                             suppressedExceptions, definingContext, isTrusted);
          //app目录的native库                                   
          this.nativeLibraryDirectories = splitPaths(librarySearchPath, false);
          //系统目录的native库
          this.systemNativeLibraryDirectories =
                  splitPaths(System.getProperty("java.library.path"), true);
          //记录所有的Native动态库        
          this.nativeLibraryPathElements = makePathElements(getAllNativeLibraryDirectories());
            ....
      }

  public String findLibrary(String libraryName) {
          String fileName = System.mapLibraryName(libraryName);
  
          for (NativeLibraryElement element : nativeLibraryPathElements) {
              String path = element.findNativeLibrary(fileName);
 
              if (path != null) {
                  return path;
              }
          }
  
          return null;
      }
```
从所有的动态库nativeLibraryPathElements(包含两个系统路径)查询是否存在匹配的。一般地，64位系统的nativeLibraryPathElements取值:
/data/app/[packagename]-1/lib/arm64
/vendor/lib64
/system/lib64


System.mapLibraryName  是个native方法
/libcore/ojluni/src/main/native/System.c
```
 System_mapLibraryName(JNIEnv *env, jclass ign, jstring libname)
  {
      int len;
      //"lib"
      int prefix_len = (int) strlen(JNI_LIB_PREFIX);
      //".so"
      int suffix_len = (int) strlen(JNI_LIB_SUFFIX);
      jchar chars[256];
     ..
      len = (*env)->GetStringLength(env, libname);
     ..
      cpchars(chars, JNI_LIB_PREFIX, prefix_len);
      (*env)->GetStringRegion(env, libname, 0, len, chars + prefix_len);
      len += prefix_len;
      cpchars(chars + len, JNI_LIB_SUFFIX, suffix_len);
      len += suffix_len;
  
      return (*env)->NewString(env, chars, len);
  }
```
该方法的功能是将xxx动态库的名字转换为libxxx.so，比如前面传递过来的nickname为gityuan_jni, 经过该方法处理后返回的名字为libgityuan_jni.so.



Element.findNativeLibrary
/libcore/dalvik/src/main/java/dalvik/system/DexPathList.java
```
  public String findNativeLibrary(String name) {
              maybeInit();
              if (zipDir == null) {
                  String entryPath = new File(path, name).getPath();
                  if (IoUtils.canOpenReadOnly(entryPath)) {
                      return entryPath;
                  }
              } else if (urlHandler != null) {              
                  String entryName = zipDir + '/' + name;
                  if (urlHandler.isEntryStored(entryName)) {
                    return path.getPath() + zipSeparator + entryName;
                  }
              }
              return null;
          }
 /libcore/luni/src/main/java/libcore/io/ClassPathURLStreamHandler.java          
 public boolean isEntryStored(String entryName) {
      ZipEntry entry = jarFile.getEntry(entryName);
      return entry != null && entry.getMethod() == ZipEntry.STORED;
    }          
```
遍历查询,一旦找到则返回所找到的目标动态库.


nativeLoad   native方法
/libcore/ojluni/src/main/native/Runtime.c
```
Runtime_nativeLoad(JNIEnv* env, jclass ignored, jstring javaFilename,
                     jobject javaLoader, jclass caller)
  {
      return JVM_NativeLoad(env, javaFilename, javaLoader, caller);
  }
```
/art/openjdkjvm/OpenjdkJvm.cc
```
  JNIEXPORT jstring JVM_NativeLoad(JNIEnv* env,
                                   jstring javaFilename,
                                   jobject javaLoader,
                                   jclass caller) {
    ScopedUtfChars filename(env, javaFilename);
   ...
    std::string error_msg;
    {
      art::JavaVMExt* vm = art::Runtime::Current()->GetJavaVM();
      bool success = vm->LoadNativeLibrary(env,
                                           filename.c_str(),
                                           javaLoader,
                                           caller,
                                           &error_msg);
      ...
    }
   ...
    return env->NewStringUTF(error_msg.c_str());
  }
```
/art/runtime/jni/java_vm_ext.cc
```
bool JavaVMExt::LoadNativeLibrary(JNIEnv* env,
                                   const std::string& path,
                                   jobject class_loader,
                                   jclass caller_class,
                                   std::string* error_msg) {
   error_msg->clear();
   SharedLibrary* library;
   //检查该动态库是否已加载
   Thread* self = Thread::Current();
   {
     MutexLock mu(self, *Locks::jni_libraries_lock_);
     library = libraries_->Get(path);
   }
   。。。
   if (library != nullptr) {
     // Use the allocator pointers for class loader equality to avoid unnecessary weak root decode.
     if (library->GetClassLoaderAllocator() != class_loader_allocator) {
       // The library will be associated with class_loader. The JNI
       // spec says we can't load the same library into more than one
       // class loader.
       //
       // This isn't very common. So spend some time to get a readable message.
       auto call_to_string = [&](jobject obj) -> std::string {
         if (obj == nullptr) {
           return "null";
         }
         // Handle jweaks. Ignore double local-ref.
         ScopedLocalRef<jobject> local_ref(env, env->NewLocalRef(obj));
         if (local_ref != nullptr) {
           ScopedLocalRef<jclass> local_class(env, env->GetObjectClass(local_ref.get()));
           jmethodID to_string = env->GetMethodID(local_class.get(),
                                                  "toString",
                                                  "()Ljava/lang/String;");
           DCHECK(to_string != nullptr);
           ScopedLocalRef<jobject> local_string(env,
                                                env->CallObjectMethod(local_ref.get(), to_string));
           if (local_string != nullptr) {
             ScopedUtfChars utf(env, reinterpret_cast<jstring>(local_string.get()));
             if (utf.c_str() != nullptr) {
               return utf.c_str();
             }
           }
           if (env->ExceptionCheck()) {
             // We can't do much better logging, really. So leave it with a Describe.
             env->ExceptionDescribe();
             env->ExceptionClear();
           }
           return "(Error calling toString)";
         }
         return "null";
       };
       std::string old_class_loader = call_to_string(library->GetClassLoader());
       std::string new_class_loader = call_to_string(class_loader);
       StringAppendF(error_msg, "Shared library \"%s\" already opened by "
           "ClassLoader %p(%s); can't open in ClassLoader %p(%s)",
           path.c_str(),
           library->GetClassLoader(),
           old_class_loader.c_str(),
           class_loader,
           new_class_loader.c_str());
       LOG(WARNING) << *error_msg;
       return false;
     }
     VLOG(jni) << "[Shared library \"" << path << "\" already loaded in "
               << " ClassLoader " << class_loader << "]";
     if (!library->CheckOnLoadResult()) {
       StringAppendF(error_msg, "JNI_OnLoad failed on a previous attempt "
           "to load \"%s\"", path.c_str());
       return false;
     }
     return true;
   }
    ....
    ...
    void* handle = android::OpenNativeLibrary(
        env,
        runtime_->GetTargetSdkVersion(),
        path_str,
        class_loader,
        (caller_location.empty() ? nullptr : caller_location.c_str()),
        library_path.get(),
        &needs_native_bridge,
        &nativeloader_error_msg);
    //找到JNI_OnLoad
    void* sym = library->FindSymbol("JNI_OnLoad", nullptr);
    if (sym == nullptr) {
      was_successful = true;
    } else {
      //调用JNI_OnLoad
      ScopedLocalRef<jobject> old_class_loader(env, env->NewLocalRef(self->GetClassLoaderOverride()));
      self->SetClassLoaderOverride(class_loader);
      using JNI_OnLoadFn = int(*)(JavaVM*, void*);
      JNI_OnLoadFn jni_on_load = reinterpret_cast<JNI_OnLoadFn>(sym);
      int version = (*jni_on_load)(this, nullptr);
    。。。
    return was_successful;
  }
  
```
首先判断so是否已经加载过了，并且可用，则直接返回true
如果是第一次加载，则转调android::OpenNativeLibrary，此处返回值handle即为so的入口地址

/art/libnativeloader/native_loader.cpp
```
void* OpenNativeLibrary(JNIEnv* env, int32_t target_sdk_version, const char* path,
                          jobject class_loader, const char* caller_location, jstring library_path,
                          bool* needs_native_bridge, char** error_msg) {
...
return OpenNativeLibraryInNamespace(ns, path, needs_native_bridge, error_msg);
...
}

 void* OpenNativeLibraryInNamespace(NativeLoaderNamespace* ns, const char* path,
                                     bool* needs_native_bridge, char** error_msg) {
    auto handle = ns->Load(path);
    ...
    if (needs_native_bridge != nullptr) {
      *needs_native_bridge = ns->IsBridged();
    }
    return handle.ok() ? *handle : nullptr;
  }
```
/art/libnativeloader/native_loader_namespace.cpp
```
 Result<void*> NativeLoaderNamespace::Load(const char* lib_name) const {
    if (!IsBridged()) {
      android_dlextinfo extinfo;
      extinfo.flags = ANDROID_DLEXT_USE_NAMESPACE;
      extinfo.library_namespace = this->ToRawAndroidNamespace();
      void* handle = android_dlopen_ext(lib_name, RTLD_NOW, &extinfo);
      if (handle != nullptr) {
        return handle;
      }
    } ...
    return Error() << GetLinkerError(IsBridged());
  }
```
/bionic/libdl/libdl.cpp
```
 void* android_dlopen_ext(const char* filename, int flag, const android_dlextinfo* extinfo) {
    const void* caller_addr = __builtin_return_address(0);
    return __loader_android_dlopen_ext(filename, flag, extinfo, caller_addr);
  }
```
/bionic/linker/dlfcn.cpp
```
void* __loader_android_dlopen_ext(const char* filename,
                             int flags,
                             const android_dlextinfo* extinfo,
                             const void* caller_addr) {
    return dlopen_ext(filename, flags, extinfo, caller_addr);
  }
  

  static void* dlopen_ext(const char* filename,
                          int flags,
                          const android_dlextinfo* extinfo,
                          const void* caller_addr) {
    ScopedPthreadMutexLocker locker(&g_dl_mutex);
    g_linker_logger.ResetState();
    void* result = do_dlopen(filename, flags, extinfo, caller_addr);
    ...
    return result;
  }  
```
/bionic/linker/linker.cpp
```
void* do_dlopen(const char* name, int flags,
                  const android_dlextinfo* extinfo,
                  const void* caller_addr) {
   ....
    ProtectedDataGuard guard;
    soinfo* si = find_library(ns, translated_name, flags, extinfo, caller);
...
    return nullptr;
  }
  

static soinfo* find_library(android_namespace_t* ns,
                              const char* name, int rtld_flags,
                              const android_dlextinfo* extinfo,
                              soinfo* needed_by) {
    ... if (!find_libraries(ns,
                               needed_by,
                               &name,
                               1,
                               &si,
                               nullptr,
                               0,
                               rtld_flags,
                               extinfo,
                               false /* add_as_children */)) {
      if (si != nullptr) {
        soinfo_unload(si);
      }
      return nullptr;
    }
  
    si->increment_ref_count();
  
    return si;
  }  
```

find_libraries
```
bool find_libraries(android_namespace_t* ns,
                      soinfo* start_with,
                      const char* const library_names[],
                      size_t library_names_count,
                      soinfo* soinfos[],
                      std::vector<soinfo*>* ld_preloads,
                      size_t ld_preloads_count,
                      int rtld_flags,
                      const android_dlextinfo* extinfo,
                      bool add_as_children,
                      std::vector<android_namespace_t*>* namespaces) {
  // Step 0: prepare.                    
   LoadTaskList load_tasks;
    for (size_t i = 0; i < library_names_count; ++i) {
      const char* name = library_names[i];
      load_tasks.push_back(LoadTask::create(name, start_with, ns, &readers_map));
    }                    
    // Step 1: expand the list of load_tasks to include
   // all DT_NEEDED libraries (do not load them just yet) 
   ...
   // Step 2: Load libraries in random order (see b/24047022)
   ...
   // Step 3: pre-link all DT_NEEDED libraries in breadth first order.
   ...
   //Step 4: Construct the global group. DF_1_GLOBAL bit is force set for LD_PRELOADed libs because
     // they must be added to the global group. Note: The DF_1_GLOBAL bit for a library is normally set
     // in step 3. 
   ..
   //Step 5: Collect roots of local_groups
   ..
   // Step 6: Link all local groups
   ..
   // Step 7: Mark all load_tasks as linked and increment refcounts
    // for references between load_groups (at this point it does not matter if
    // referenced load_groups were loaded by previous dlopen or as part of this
    // one on step 6)                  
 }
```
find_libraries整个函数分为8个步骤：
（Step 0: prepare.）此步将需要加载的so封装为LoadTask，并存放在名为load_tasks的容器中
step1  展开本so的所有依赖so的依赖so，此处的so依赖是一个树形结构
step2  加载so
dlopen加载so不是Linux内核提供的能力，而是libc采用Linux syscall封装而来的。理论上来说，Linux所有可执行文件与so都是ELF格式，
而进程加载so主要是将so按照ELF约定好的段（Segment）加载到自己的虚拟内存空间中，在内核中采用struct vm_area_struct与其对应起来
（Linux Kernel为用户层提供了procfs伪文件系统，通过/proc/[pid]/maps便可以查看加载进来的so的struct vm_area_struct），
然后对PLT/GOT等进行重定位。更进一步，将文件与内存对应起来，并使之对应于内核中的struct vm_area_struct，最常规的方式就是Linux syscall mmap
```
   for (auto&& task : load_list) {
      address_space_params* address_space =
          (reserved_address_recursive || !task->is_dt_needed()) ? &extinfo_params : &default_params;
      if (!task->load(address_space)) {
        return false;
      }
    }
  
   
    bool load(address_space_params* address_space) {
      ElfReader& elf_reader = get_elf_reader();
      if (!elf_reader.Load(address_space)) {
        return false;
      }
      ... 
      return true;
    }  
```
/bionic/linker/linker_phdr.cpp
```
bool ElfReader::Load(address_space_params* address_space) {
    ...
    if (ReserveAddressSpace(address_space) && LoadSegments() && FindPhdr() &&
        FindGnuPropertySection()) {
      did_load_ = true;
   ....
    }
  
    return did_load_;
  }
```