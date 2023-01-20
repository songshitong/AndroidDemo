
https://medium.com/mobile-app-development-publication/when-your-android-studio-breakpoint-doesnt-stop-cf0331e63dd2
多进程情况，断点不停住
1 代码中添加Debug.waitForDebugger()  例如一启动就debug,仍然需要绑定到进程
2 attachToProcess  绑定到特定的进程