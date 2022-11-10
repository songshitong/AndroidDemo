https://zhuanlan.zhihu.com/p/44912855

一、Fiddler

当启动fiddler，程序将会把自己作为一个代理，所以的http请求在达到目标服务器之前都会经过fiddler，同样的，所有的http响应都会在返回客户端之前流经fiddler。

Fiddler可以抓取支持http代理的任意程序的数据包，如果要抓取https会话，要先安装证书。

Fiddler的工作原理

Fiddler 是以代理web服务器(proxy)的形式工作的,它使用代理地址:127.0.0.1, 端口:8888. 当Fiddler会自动设置代理， 退出的时候它会自动注销代理，
这样就不会影响别的程序。不过如果Fiddler非正常退出，这时候因为Fiddler没有自动注销，会造成网页无法访问。解决的办法是重新启动下Fiddler.



fiddler 抓包https  https://wooyun.js.org/drops/%E6%B5%85%E6%9E%90%E6%89%8B%E6%9C%BA%E6%8A%93%E5%8C%85%E6%96%B9%E6%B3%95%E5%AE%9E%E8%B7%B5.html
注意防火墙，有的机器防火墙不允许访问，需要关闭
fiddler配置：
```
tools->connections  端口，allow remote computers to connect
```
WiFi配置代理到fiddler
手机打开  电脑ip:fiddler端口        下载cer证书，可以抓到https的请求了

wireshark 抓取fiddler的流量   捕获过滤器port fiddler端口
wireshark 现在可以抓取手机的流量了  此时相当于fiddler作为代理服务器直接APP服务器交互

fiddler host过滤
filters->hosts-> no zone filters;show only the following hosts   
域名配置 *.baidu.com.cn