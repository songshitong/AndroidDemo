Debug.dumpHprofData(fileName) 将内存信息保存到文件，但是存在性能问题，APP处于冻结状态,可能会触发GC
```
VMDebug.dumpHprofData(fileName);
```
native调用
```
// art/runtime/native/dalvik_system_VMDebug.cc
static void VMDebug_dumpHprofData(JNIEnv* env, jclass, jstring javaFilename, jint javaFd) {
  // Only one of these may be null.
  // 忽略一些判断代码
  hprof::DumpHeap(filename.c_str(), fd, false);
}

// art/runtime/hprof/hprof.cc
void DumpHeap(const char* filename, int fd, bool direct_to_ddms) {
  // 忽略一些判断代码
  ScopedSuspendAll ssa(__FUNCTION__, true /* long suspend */);
  Hprof hprof(filename, fd, direct_to_ddms);
  // 开始执行 Dump 操作
  hprof.Dump();
}
```
从源码中，我们可以看到在进行 Dump 操作之前，会构造一个 ScopedSuspendAll 对象，用来暂停所有的线程，然后再析构方法中恢复：
```
// 暂停所有线程
ScopedSuspendAll::ScopedSuspendAll(const char* cause, bool long_suspend) {
  Runtime::Current()->GetThreadList()->SuspendAll(cause, long_suspend);
}

// 恢复所有线程
ScopedSuspendAll::~ScopedSuspendAll() {
  Runtime::Current()->GetThreadList()->ResumeAll();
}
```
这个暂停操作，对用户体验是种极大的伤害，可以通过取巧的方式去规避，例如，新开个进程来显示 loading 页面，APP 退到后台去执行 dump
  等等，但并没有真正解决这个问题
https://juejin.cn/post/6867335105322188813