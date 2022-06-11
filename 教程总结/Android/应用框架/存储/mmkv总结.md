
待学习  记录crc校验，aes，pb的编解码

示例代码  
```
String otherDir = getFilesDir().getAbsolutePath() + "/mmkv_3";
MMKV kv = testMMKV("test/AES", "Tencent MMKV", false, otherDir);
```
生成的目录  生成文件名称：指定目录/md5(文件绝对路径(/data/user/0/com.tencent.mmkvdemo/files/mmkv_3/test/AES)) 
```
MMKV: <MMKV_Android.cpp:156::mmkvWithID> prepare to load test/AES (id 127fcc03e0a8845bd5664e6e7e25f805) from rootPath /data/user/0/com.tencent.mmkvdemo/files/mmkv_3
ogging MMKV: <MemoryFile.cpp:97::open> open fd[0x22], /data/user/0/com.tencent.mmkvdemo/files/mmkv_3/127fcc03e0a8845bd5664e6e7e25f805
logging MMKV: <MemoryFile.cpp:97::open> open fd[0x23], /data/user/0/com.tencent.mmkvdemo/files/mmkv_3/127fcc03e0a8845bd5664e6e7e25f805.crc
```
echo -n "/data/user/0/com.tencent.mmkvdemo/files/mmkv_3/test/AES"|md5sum 生成字符md5
结果是
```
127fcc03e0a8845bd5664e6e7e25f805
```

https://www.jianshu.com/p/c12290a9a3f7
由于MMKV读写是直接读写到mmap文件映射的内存上，绕开了普通读写io需要进入内核，写到磁盘的过程。光是这种级别优化，都可以拉开三个数量级的性能差距。
但是也诞生了一个很大的问题，一个进程在32位的机子中有4g的虚拟内存限制，而我们把文件映射到虚拟内存中，
如果文件过大虚拟内存就会出现大量的消耗最后出现异常，对于不熟悉Linux的朋友就无法理解这种现象
这里有几个关于MMKV使用的注意事项：

1.保证每一个文件存储的数据都比较小，也就说需要把数据根据业务线存储分散。这要就不会把虚拟内存消耗过快。

2.还需要在适当的时候释放一部分内存数据，比如在App中监听onTrimMemory方法，在Java内存吃紧的情况下进行MMKV的trim操作
   (不准确，我们暂时以此为信号，最好自己监听进程中内存使用情况)。

2.在不需要使用的时候，最好把MMKV给close掉。甚至调用exit方法
