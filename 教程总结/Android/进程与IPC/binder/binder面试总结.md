

binder通信模式 
oneway(aidl文件中方法声明)              客户端只需要把请求发送到服务端就可以立即返回，而不需要等待服务端的结果，这是一种非阻塞方式
```
方法调用有flag
mRemote.transact(Stub.TRANSACTION_invokeMethodInMyService, _data, null, android.os.IBinder.FLAG_ONEWAY);
```
普通模式/非oneway    客户端发起调用时，客户端一般会阻塞，直到服务端返回结果

https://mp.weixin.qq.com/s/2xcSvHlpxS3xgHP2PBQsBA


https://www.jianshu.com/p/adaa1a39a274
ipcSetDataReference  https://juejin.cn/post/6868901776368926734

bbinder到Java层  https://juejin.cn/post/6890088205916307469#heading-3


https://weishu.me/2016/01/12/binder-index-for-newer/
https://github.com/xfhy/Android-Notes/tree/499b9832a344aff12c9976922da7860058204dd1/Blogs/Android/%E5%A4%9A%E8%BF%9B%E7%A8%8B
bpbinder与bbinder 在驱动层的关系https://juejin.cn/post/6867139592739356686#heading-7
