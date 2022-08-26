WebView启动优化。
1 WebView第一次创建比较耗时，可以预先创建WebView，提前将其内核初始化。
2 使用WebView缓存池，用到WebView的地方都从缓存池取，缓存池中没有缓存再创建，注意内存泄漏问题。
3 本地预置html和css，WebView创建的时候先预加载本地html，之后通过js脚本填充内容部分。

内存泄露
单独开一个进程,避免其他进程影响
1 单独进程的activity
在androidmanifest.xml的activity标签里加上android:process="packagename.web"
2 使用ApplicationContext代码创建webView
WebView mWebView = new WebView(getApplicationContext());




https://juejin.cn/post/6844904149608300552#heading-3
WebView后台耗电问题
在WebView加载页面的时候，WebView会自动开启线程去加载，如果没有很好地将WebView销毁的话，这些残余的线程会一直在后台运行，由此导致你的应用程序耗电量居高不下
解决方案：
在Activity的onstop和onresume里分别把setJavaScriptEnabled()；给设置成false和true。