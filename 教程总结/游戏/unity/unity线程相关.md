
https://imgtec.eetrend.com/d6-imgtec/blog/2017-01/9176.html
不能在子线程中操作UnityEngineSDK
1 UnityEngine的API与对象都不能在子线程中运行。
2 UnityEngine定义的基本结构(int,float,Struct定义的数据类型)可以在子线程中运行。例如Vector3(Struct)可以，但Texture2d对象则不可以，
  因为其根父类为UnityEngine.Object)。

为什么要做这个限制？
因为游戏中逻辑更新和画面更新的时间点要求有确定性，必须按照帧序列严格保持同步，否则就会出现游戏中的对象不同步的现象。
  多线程也能保证这个效果，但如果引入多线程，会加大同步处理的难度与游戏的不稳定性。

unity使用子线程：
场景：
　　大量耗时的数据计算
　　网络请求
　　复杂密集的I/O操作
方式：
1 Unity3D的NativePlugin中可以新建子线程。通过NativePlugin可以接入移动端iOS与Android中的成熟库，可以是Objective C,
Java, C++三种语言交叉混合的方式组成NativePlugin，然后使用Android或者iOS的SDK开辟子线程
2 Loom插件



il2cpp 源码
unity导出工程  https://www.jianshu.com/p/e0e62650cb47
unityLibrary/src/main/Il2CppOutputProject/IL2CPP/libil2cpp/mono/ThreadPool/threadpool-ms-io.cpp