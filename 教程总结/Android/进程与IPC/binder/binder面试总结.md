

binder通信模式 
oneway(aidl文件中方法声明)              客户端只需要把请求发送到服务端就可以立即返回，而不需要等待服务端的结果，这是一种非阻塞方式
```
方法调用有flag
mRemote.transact(Stub.TRANSACTION_invokeMethodInMyService, _data, null, android.os.IBinder.FLAG_ONEWAY);
```
普通模式/非oneway    客户端发起调用时，客户端一般会阻塞，直到服务端返回结果

https://mp.weixin.qq.com/s/2xcSvHlpxS3xgHP2PBQsBA