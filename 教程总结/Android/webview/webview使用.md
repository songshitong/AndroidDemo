
创建context
实例化webview的Context必须是activity，内部弹出alert需要activity context


加载url  
loadUrl 注意url不能太长
Refusing to load URL as it exceeds 2097152 characters.
//由loadUrl改为evaluateJavascript

常用配置
```
 settings.javaScriptEnabled = true
    settings.setGeolocationEnabled(true)
    settings.domStorageEnabled = true
    // settings.setAppCacheEnabled(false)
    //暂不用缓存，防止页面异常
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    //运行https的url加载http的内容，默认不允许
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    settings.databaseEnabled = true
    settings.loadWithOverviewMode = true
    settings.userAgentString = xxx
    if(com.autohome.carbooksdk.BuildConfig.DEBUG){
      setWebContentsDebuggingEnabled(true)
    }
```


webview设置背景色
```
在布局文件中的webview设置background（这个一定要设置）。然后在java代码中添加如下代码：
wv.setBackgroundColor(0);//设置背景色
wv.getBackground().setAlpha(0);//设置填充透明度（布局中一定要设置background，不然getbackground会是null）
```

销毁
```
 private void destroyWebView() {
    stopLoading();
    loadUrl("about:blank");
    clearHistory();
    removeAllViews();
    ViewGroup viewGroup = (ViewGroup) getParent();
    if (null != viewGroup) {
      viewGroup.removeView(this);
    }
    destroy();
    webview=null;
  }
```

WebViewClient  一般监听控制webview行为
shouldOverrideUrlLoading
onPageStarted
onPageFinished
onReceivedError 页面加载失败

onPageFinished的多次调用
https://www.jb51.net/article/183447.htm
一般是由于重定向导致，可以通过url和webview.progress==100进行过滤


WebChromeClient
onReceivedTitle()
onProgressChanged()
onJsAlert()


https://juejin.cn/post/6844904136937324552#heading-55
Android与 js 是如何交互的
Android调js
WebView.loadUrl("javascript:js中的方法名")
  这种方法的优点是很简洁，缺点是没有返回值，如果需要拿到js方法的返回值则需要js调用Android中的方法来拿到这个返回值
WebView.evaluateJavaScript("javascript:js中的方法名",ValueCallback)
  这种方法比 loadUrl 好的是可以通过 ValueCallback 这个回调拿到 js方法的返回值。缺点是这个方法 Android4.4 才有，兼容性较差。
注意：需要在主线程调用！！！   unity->android->webview中 unity->android可能切换线程，需要注意


js调Android
WebView.addJavascriptInterface(Object obj, String interfaceName)。
  这是官方解决 js 调用 Android 方法的方案，需要注意的是要在供 js 调用的 Android 方法上加上 @JavascriptInterface 注解，
  以避免安全漏洞。这种方案的缺点是 Android4.2 以前会有安全漏洞，不过在 4.2 以后已经修复了。
  向js注入一个interfaceName的对象， js通过window.interfaceName调用相关方法
   
重写 WebViewClient的shouldOverrideUrlLoading()方法来拦截url， 拿到 url 后进行解析，如果符合双方的规定，即可调用 Android 方法。
  优点是避免了 Android4.2 以前的安全漏洞，缺点也很明显，无法直接拿到调用 Android 方法的返回值，
  只能通过 Android 调用 js 方法来获取返回值
重写 WebChromClient 的 onJsPrompt() 方法，同前一个方式一样，拿到 url 之后先进行解析，如果符合双方规定，即可调用Android方法。
  最后如果需要返回值，通过 result.confirm("Android方法返回值") 即可将 Android 的返回值返回给 js。方法的优点是没有漏洞，
  也没有兼容性限制，同时还可以方便的获取 Android 方法的返回值。
  其实这里需要注意的是在 WebChromeClient 中除 了 onJsPrompt 之外还有 onJsAlert 和 onJsConfirm 方法。那么为什么不选择另两个方法呢？
   原因在于 onJsAlert 是没有返回值的，而 onJsConfirm 只有 true 和 false 两个返回值，同时在前端开发中 prompt 方法基本不会被调用，
   所以才会采用 onJsPrompt。


https://www.itranslater.com/qa/details/2582595927299064832
清除webview内容
```
webView.loadUrl("about:blank")
```

打印webview的log  WebChromeClient
```
//public void onConsoleMessage(String message, int lineNumber, String sourceID) {
//    Log.d("MyApplication", message + " -- From line "
//                         + lineNumber + " of "
//                         + sourceID);
//    return true;
//  }
//两个内容相同，使用下面的即可  
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        DevUtil.i("console", "["+consoleMessage.messageLevel()+"] "+ consoleMessage.message() + "(" +consoleMessage.sourceId()  + ":" + consoleMessage.lineNumber()+")");
        return super.onConsoleMessage(consoleMessage);
    }  
```
部分手机console.log可能不打印  需要自定义接口但是拿不到文件和行号
https://blog.csdn.net/lanxingfeifei/article/details/50502436  也可以调用console.log先测试是否可以打印，然后使用替换的接口
```
// 首先,定一个类,叫什么名称都可以,但是里面的方法名必须与
// Javascript的console中的方法名对应
private class Console{
    private static final String TAG="[WebView]";
    public void log(String msg){
        Log.i(TAG,msg);
    }
	// 还可以添加其他的方法,比如: warn,assert等等
}
 
// 然后,为WebView添加对应的接口
webView.addJavascriptInterface(new Console, "console");
```


远程调试  webview debug   
https://developer.chrome.com/docs/devtools/remote-debugging/webviews/
```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
    { WebView.setWebContentsDebuggingEnabled(true); }
}
```
chrome查看 chrome://inspect 选择usb设备还是network设备
点击设备的inspect

webview chrome变更
https://web.dev/appcache-removal/#android-webview
Chrome 85 开始，默认情况下，Chrome 中将不再提供 AppCache,建议用Service Workers替代
https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API/Using_Service_Workers
settings.setAppCacheEnabled(false);


webview使用LocalStorage
```
 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("window.localStorage.setItem('"+ key +"','"+ val +"');", null);
            webView.evaluateJavascript("window.localStorage.setItem('"+ key2 +"','"+ val2 +"');", null);
        } else {
            webView.loadUrl("javascript:localStorage.setItem('"+ key +"','"+ val +"');");
            webView.loadUrl("javascript:localStorage.setItem('"+ key2 +"','"+ val2 +"');");
        }
```


webview键盘相关
1 无法弹出键盘
window设置为softmode为可以获焦， 并且手动获取焦点requestFocus()
2 布局遮挡
  由h5进行移动