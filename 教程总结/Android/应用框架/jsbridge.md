
2.0.0
https://github.com/lzyzsd/JsBridge/releases/tag/2.0.0


assets/WebViewJavascriptBridge.js
js方法的注入
// 创建消息index队列iframe
```
    function _createQueueReadyIframe() {
        messagingIframe = document.createElement('iframe');
        messagingIframe.style.display = 'none';
        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;   // yy://__QUEUE_MESSAGE__
        document.documentElement.appendChild(messagingIframe);
    }
```
//创建消息体队列iframe
```
    function _createQueueReadyIframe4biz() {
        bizMessagingIframe = document.createElement('iframe');
        bizMessagingIframe.style.display = 'none';
        document.documentElement.appendChild(bizMessagingIframe);
    }
```
//set default messageHandler  初始化默认的消息线程
```
var receiveMessageQueue = []; 
    function init(messageHandler) {
      ...
        _createQueueReadyIframe();
        _createQueueReadyIframe4biz();
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue; //默认空数组
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            //开始处理native的消息
            _dispatchMessageFromNative(receivedMessages[i]);
        }
        WebViewJavascriptBridge.inited = true;
    }
```

WebViewJavascriptBridge对象的属性设置
```
//提供给native调用,receiveMessageQueue 在会在页面加载完后赋值为null,所以
    function _handleMessageFromNative(messageJSON) {
        if (receiveMessageQueue) {
            receiveMessageQueue.push(messageJSON);
        }
        _dispatchMessageFromNative(messageJSON);

    }

    WebViewJavascriptBridge.init = init;
    WebViewJavascriptBridge.doSend = send;
    WebViewJavascriptBridge.registerHandler = registerHandler;
    WebViewJavascriptBridge.callHandler = callHandler;
    WebViewJavascriptBridge._handleMessageFromNative = _handleMessageFromNative;

    var readyEvent = document.createEvent('Events');
    var jobs = window.WVJBCallbacks || [];
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    window.WVJBCallbacks = []; //创建一个数组
    jobs.forEach(function (job) {
        job(WebViewJavascriptBridge)
    });
    document.dispatchEvent(readyEvent);
```


处理native的消息
```
var responseCallbacks = {}; //回调对象
var messageHandlers = {};  //messageHandlers对象
    function _dispatchMessageFromNative(messageJSON) {
        setTimeout(function() {
            //json解析 处理消息
            var message = JSON.parse(messageJSON);
            var responseCallback;
            //java call finished, now need to call js callback function
            if (message.responseId) { //消息存在回调ID
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                //执行回调
                responseCallback(message.responseData);
                //删除回调方法
                delete responseCallbacks[message.responseId];
            } else { //responseId没有，查询 callbackId
                //直接发送
                if (message.callbackId) {
                    var callbackResponseId = message.callbackId;
                    responseCallback = function(responseData) {
                         //发送
                        _doSend('response', responseData, callbackResponseId);
                    };
                }

                var handler = WebViewJavascriptBridge._messageHandler;
                if (message.handlerName) {
                   //查询存储的handler
                    handler = messageHandlers[message.handlerName];
                }
                //查找指定handler，并执行  
                try {
                    handler(message.data, responseCallback);
                } catch (exception) {
                    //js handler处理异常，抛出
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    }
```
发送消息   //sendMessage add message, 触发native处理 sendMessage
```
var sendMessageQueue = [];
    function _doSend(handlerName, message, responseCallback) {
        var callbackId;
        if(typeof responseCallback === 'string'){
            callbackId = responseCallback;
        } else if (responseCallback) {
            //构建callbackId
            callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message.callbackId = callbackId;
        }else{
            callbackId = ''; //没有回调
        }
        try { //执行js方法  WebViewJavascriptBridge.handlerName
             var fn = eval('WebViewJavascriptBridge.' + handlerName);
         } catch(e) {
             console.log(e);
         }
         if (typeof fn === 'function'){
             //调用fn并获取回调
             var responseData = fn.call(WebViewJavascriptBridge, JSON.stringify(message), callbackId);
             if(responseData){ //执行回调callback
                 responseCallback = responseCallbacks[callbackId];
                 if (!responseCallback) {
                     return;
                  }
                 responseCallback(responseData);
                 delete responseCallbacks[callbackId];
             }
         }
        //放到数组
        sendMessageQueue.push(message);
        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE; //   yy://__QUEUE_MESSAGE__
    }
```

js注册handler  // 注册线程 往数组里面添加值
```
    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
    }
```
js调用原生的handler
```
// 调用线程
    function callHandler(handlerName, data, responseCallback) {
        // 如果方法不需要参数，只有回调函数，简化JS中的调用
        if (arguments.length == 2 && typeof data == 'function') {
			responseCallback = data;
			data = null;
		}
        _doSend(handlerName, data, responseCallback);
    }
```



android测
android测有两种使用方式,继承webview和使用BridgeHelper    
两者都继承WebViewJavascriptBridge，实现发送消息  必须在主线程，但是没有抛出异常，没有进行线程切换
两者的逻辑并不完全相同
1 转义的逻辑不同BridgeHelper手动转义，webview使用JSONObject.quote进行转义
2 webView对于总长度大于2097152并且android4.4以上，使用evaluateJavascript，否则使用loadUrl
BridgeHelper发送消息全部由loadUrl实现

缺点：
监听页面完成才进行js注入，web侧可能初始化时拿不到对象  页面完成监听不稳定  注意多次调用finish会多次注入，js判断了初始化后不执行

com/github/lzyzsd/jsbridge/WebViewJavascriptBridge.java
```
public interface WebViewJavascriptBridge {
    //发送消息给web
	void sendToWeb(String data);
	void sendToWeb(String data, OnBridgeCallback responseCallback);
	void sendToWeb(String function, Object... values);
}
```

先看bridgeHelper的逻辑
com/github/lzyzsd/jsbridge/BridgeHelper.java
```
  @Override
    public void sendToWeb(String data, OnBridgeCallback responseCallback) {
        doSend(null, data, responseCallback);
    }
    
 
 private void doSend(String handlerName, String data, OnBridgeCallback responseCallback) {
        Message m = new Message();
        //创建消息 设置data
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        //设置CallbackId  保存CallbackId与responseCallback  //线程不安全，不过发消息要求线程在主线程
        if (responseCallback != null) {
           //JAVA_CB_累加id_时间戳
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        //设置消息对应js的handlerName
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        //开始发送
        queueMessage(m);
    }   
    

 private void queueMessage(Message m) {
        if (startupMessage != null) {
            startupMessage.add(m); //List<Message> startupMessage 
            //页面结束时onPageFinished，将未触发的消息调用
        } else {
            dispatchMessage(m);
        }
    }     
    
 
  private void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
        messageJson = messageJson.replaceAll("%7B", URLEncoder.encode("%7B"));
        messageJson = messageJson.replaceAll("%7D", URLEncoder.encode("%7D"));
        messageJson = messageJson.replaceAll("%22", URLEncoder.encode("%22"));
        //调用js方法  javascript:WebViewJavascriptBridge._handleMessageFromNative(messsage);
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    } 
    
  private void loadUrl(String jsUrl) {
        webView.loadUrl(jsUrl);
    }     
```

webView的实现方式
com/github/lzyzsd/jsbridge/BridgeWebView.java
```
public void callHandler(String handlerName, String data, OnBridgeCallback callBack) {
        doSend(handlerName, data, callBack);
    }

    private void doSend(String handlerName, Object data, OnBridgeCallback responseCallback) {
        if (!(data instanceof String) && mGson == null){
            return;
        }
        //构建JSRequest
        JSRequest request = new JSRequest();
        if (data != null) {
            //非str格式，需要进行json格式化
            request.data = data instanceof String ? (String) data : mGson.toJson(data);
        }
        if (responseCallback != null) {
            //构建callbackId 并保存
            String callbackId = String.format(BridgeUtil.CALLBACK_ID_FORMAT, (++mUniqueId) + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            mCallbacks.put(callbackId, responseCallback);
            request.callbackId = callbackId;
        }
        //存储handlerName
        if (!TextUtils.isEmpty(handlerName)) {
            request.handlerName = handlerName;
        }
        queueMessage(request);
    }
    

 private void dispatchMessage(Object message) {
        if (mGson == null){
            return;
        }
        String messageJson = mGson.toJson(message);
        //escape special characters for json string  为json字符串转义特殊字符

		  // 系统原生 API 做 Json转义，没必要自己正则替换，而且替换不一定完整
        messageJson = JSONObject.quote(messageJson);
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            //对于总长度大于2097152或者android4.4以上 
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT&&javascriptCommand.length()>=URL_MAX_CHARACTER_NUM) {
				this.evaluateJavascript(javascriptCommand,null);
			}else {
				this.loadUrl(javascriptCommand);
			}
        }
    }    
```


额外知识 BridgeWebView与Client
BridgeWebView内置BridgeWebViewClient，其对WebViewClient功能增强，新加额外的功能，外部可以正常设置client
java/com/github/lzyzsd/jsbridge/BridgeWebView.java
```
   private void init() {
        mClient = new BridgeWebViewClient(this);
        super.setWebViewClient(mClient);
    }
 
   @Override
    public void setWebViewClient(WebViewClient client) {
        mClient.setWebViewClient(client);
    }   
```
java/com/github/lzyzsd/jsbridge/BridgeWebViewClient.java
```
class BridgeWebViewClient extends WebViewClient {
    public void setWebViewClient(WebViewClient client) {
        mClient = client;
    }
    
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mClient != null) {
            mClient.onPageStarted(view, url, favicon);
        } else {
            super.onPageStarted(view, url, favicon);
        }
    }
    
     @Override
    public void onPageFinished(WebView view, String url) {
        if (mClient != null) {
            mClient.onPageFinished(view, url);
        } else {
            super.onPageFinished(view, url);
        }
        mListener.onLoadStart();
        BridgeUtil.webViewLoadLocalJs(view, BridgeUtil.JAVA_SCRIPT);
        mListener.onLoadFinished();
    }
}
```

总结：
android向js发送消息 一般由loadUrl或者evaluateJavascript实现
 js方法的注册 保存方法和方法名，evaluateJavascript执行时触发对应的方法
js向Android发送消息
 android方法的注册 webView.addJavascriptInterface();  方法由@JavascriptInterface标记  
    //类似的可以做框架设计，反射拿到所有方法及注解，标有注解的进行特殊处理 
    //注解需要一个调用的方法名常量，这样使用时会方便许多
 android的方法如何添加到WebViewJavascriptBridge?
    webview.addJavascriptInterface(BridgeWebView.BaseJavascriptInterface,"WebViewJavascriptBridge") 将对象注入到js

js的iframe中，src有什么用？
https://www.jianshu.com/p/ce47bee0034f


js库的注入  监听页面完成才进行js注入，web侧可能初始化时拿不到对象, 注意多次调用finish会多次注入，js判断了初始化后不执行
SalesChampionQISDK/src/main/java/com/cubic/xgcar/component/jsbridge/BridgeWebViewClient.java
```
 @Override
    public void onPageFinished(WebView view, String url) {
       ... //加载asset的WebViewJavascriptBridge.js
        BridgeUtil.webViewLoadLocalJs(view, BridgeUtil.JAVA_SCRIPT);
       ...
    }
```
//加载js方法
java/com/cubic/xgcar/component/jsbridge/BridgeUtil.java
```
  public static void webViewLoadLocalJs(WebView view, String path){
        String jsContent = assetFile2Str(view.getContext(), path);
		view.loadUrl("javascript:" + jsContent);
    }
```