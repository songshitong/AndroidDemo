//todo 查看相关原理
ashmm Anonymous Shared Memory  android匿名共享内存
https://www.jianshu.com/p/d9bc9c668ba6
https://zhuanlan.zhihu.com/p/146671611

使用
Java层借助MemoryFile或者SharedMemory。
Native层借助MemoryHeapBase或者MemoryBase。
Native层直接调用libc的ashmem_create_region和mmap系统调用