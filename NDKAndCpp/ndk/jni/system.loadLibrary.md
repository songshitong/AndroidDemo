todo 完成

http://gityuan.com/2017/03/26/load_library/
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


android_12.0.0_r3
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
                  // Don't give up even if we failed to find the library in the native lib paths.
                  // The underlying dynamic linker might be able to find the lib in one of the linker
                  // namespaces associated with the current linker namespace. In order to give the
                  // dynamic linker a chance, proceed to load the library with its soname, which
                  // is the fileName.
                  // Note that we do this only for PathClassLoader  and DelegateLastClassLoader to
                  // minimize the scope of this behavioral change as much as possible, which might
                  // cause problem like b/143649498. These two class loaders are the only
                  // platform-provided class loaders that can load apps. See the classLoader attribute
                  // of the application tag in app manifest.
                  filename = System.mapLibraryName(libraryName);
              }
              if (filename == null) {
                  throw new UnsatisfiedLinkError(loader + " couldn't find \"" +
                                                 System.mapLibraryName(libraryName) + "\"");
              }
              String error = nativeLoad(filename, loader);
              if (error != null) {
                  throw new UnsatisfiedLinkError(error);
              }
              return;
          }
  
          // We know some apps use mLibPaths directly, potentially assuming it's not null.
          // Initialize it here to make sure apps see a non-null value.
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