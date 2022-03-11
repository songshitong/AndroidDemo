
https://juejin.cn/post/6844904136937324552#heading-55
Android与 js 是如何交互的
Android调js
WebView.loadUrl("javascript:js中的方法名")
  这种方法的优点是很简洁，缺点是没有返回值，如果需要拿到js方法的返回值则需要js调用Android中的方法来拿到这个返回值
WebView.evaluateJavaScript("javascript:js中的方法名",ValueCallback)
  这种方法比 loadUrl 好的是可以通过 ValueCallback 这个回调拿到 js方法的返回值。缺点是这个方法 Android4.4 才有，兼容性较差。

js调Android
WebView.addJavascriptInterface()。
  这是官方解决 js 调用 Android 方法的方案，需要注意的是要在供 js 调用的 Android 方法上加上 @JavascriptInterface 注解，
  以避免安全漏洞。这种方案的缺点是 Android4.2 以前会有安全漏洞，不过在 4.2 以后已经修复了。
重写 WebViewClient的shouldOverrideUrlLoading()方法来拦截url， 拿到 url 后进行解析，如果符合双方的规定，即可调用 Android 方法。
  优点是避免了 Android4.2 以前的安全漏洞，缺点也很明显，无法直接拿到调用 Android 方法的返回值，
  只能通过 Android 调用 js 方法来获取返回值
重写 WebChromClient 的 onJsPrompt() 方法，同前一个方式一样，拿到 url 之后先进行解析，如果符合双方规定，即可调用Android方法。
  最后如果需要返回值，通过 result.confirm("Android方法返回值") 即可将 Android 的返回值返回给 js。方法的优点是没有漏洞，
  也没有兼容性限制，同时还可以方便的获取 Android 方法的返回值。
  其实这里需要注意的是在 WebChromeClient 中除 了 onJsPrompt 之外还有 onJsAlert 和 onJsConfirm 方法。那么为什么不选择另两个方法呢？
   原因在于 onJsAlert 是没有返回值的，而 onJsConfirm 只有 true 和 false 两个返回值，同时在前端开发中 prompt 方法基本不会被调用，
   所以才会采用 onJsPrompt。

