http://gityuan.com/2016/01/01/handler-message-usage/

用途简化Handler的创建过程，方便在子线程中使用

使用
HandlerThread配合handler使用
```
// Step 1: 创建并启动HandlerThread线程，内部包含Looper
HandlerThread handlerThread = new HandlerThread("gityuan.com");
handlerThread.start();
// Step 2: 创建Handler
Handler handler = new Handler(handlerThread.getLooper());
// Step 3: 发送消息
handler.post(new Runnable() {
        @Override
        public void run() {
            System.out.println("thread id="+Thread.currentThread().getId());
        }
    });
```
子线程中使用handler的原始方法  需要创建和启动looper
```
class LooperThread extends Thread {
    public Handler mHandler;
    public void run() {
        Looper.prepare();
        // Step 1: 创建Handler
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                //处理即将发送过来的消息
                System.out.println("thread id="+Thread.currentThread().getId());
            }
        };

        Looper.loop();
    }
}
// Step 2: 创建并启动LooperThread线程，内部包含Looper
LooperThread looperThread = new LooperThread("thread");
looperThread.start();

// Step 3: 发送消息
LooperThread.mHandler.sendEmptyMessage(10);
```




源码实现
/frameworks/base/core/java/android/os/HandlerThread.java
获取HandlerThread线程中的Looper对象
```
public Looper getLooper() {
    // 当线程没有启动或者已经结束时，则返回null
    if (!isAlive()) {
        return null;
    }

    //当线程已经启动，则等待直到looper创建完成
    synchronized (this) {
        while (isAlive() && mLooper == null) {
            try {
                wait(); //休眠等待
            } catch (InterruptedException e) {
            }
        }
    }
    return mLooper;
}
```

执行HandlerThread的run()    假设先getLooper然后在启动start线程，getLooper不就阻塞了吗
```
public void run() {
    mTid = Process.myTid();  //获取线程的tid
    Looper.prepare();   // 创建Looper对象
    synchronized (this) {
        mLooper = Looper.myLooper(); //获取looper对象
        notifyAll(); //唤醒等待线程
    }
    Process.setThreadPriority(mPriority);
    onLooperPrepared();  // 该方法可通过覆写，实现自己的逻辑
    Looper.loop();   //进入循环模式
    mTid = -1;
}
```

Looper退出
```
public boolean quit() {
    Looper looper = getLooper();
    if (looper != null) {
        looper.quit(); //普通退出
        return true;
    }
    return false;
}

public boolean quitSafely() {
    Looper looper = getLooper();
    if (looper != null) {
        looper.quitSafely(); //安全退出
        return true;
    }
    return false;
}
```
quit()与quitSafely()的区别，仅仅在于是否移除当前正在处理的消息。移除当前正在处理的消息可能会出现不安全的行为
//todo messageQueue quit