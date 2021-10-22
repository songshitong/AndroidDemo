//todo 进阶  LiveEventBus

版本 eventbus 3.2

使用方式 不使用注解处理器
使用注解处理器
https://greenrobot.org/eventbus/documentation/subscriber-index/
初始化注解处理器生成的文件？？ 为啥不自动加载？？
EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
// Now the default instance uses the given index. Use it like this:
EventBus eventBus = EventBus.getDefault();

EventBus in 3 steps
1 Define events:

public static class MessageEvent {
 /* Additional fields if needed */
  }

2 Prepare subscribers: Declare and annotate your subscribing method, optionally specify a thread mode:

@Subscribe(threadMode = ThreadMode.MAIN)  
public void onMessageEvent(MessageEvent event) {/* Do something */};
Register and unregister your subscriber. For example on Android, activities and fragments should 
usually register according to their life cycle:

 @Override
 public void onStart() {
     super.onStart();
     EventBus.getDefault().register(this);
 }

 @Override
 public void onStop() {
     super.onStop();
     EventBus.getDefault().unregister(this);
 }
 
3. Post events:

 EventBus.getDefault().post(new MessageEvent());
 
 
 流程分析
 注册，注解处理，确定订阅关系，消息分发 
 反注册        
 特殊的注解处理器，粘性事件




订阅的注解
'''
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Subscribe {
    ThreadMode threadMode() default ThreadMode.POSTING;

    //粘性事件  将以前的消息发送给订阅者
    boolean sticky() default false;

    //订阅者优先级影响事件传递的顺序。
    //在同一个交付线程（{@link ThreadMode}）内，更高优先级的订阅者会先收到事件
    int priority() default 0;
}
'''

//线程模式
public enum ThreadMode {
    /**
     * Subscriber will be called directly in the same thread, which is posting the event. This is the default. Event delivery
     * implies the least overhead because it avoids thread switching completely. Thus this is the recommended mode for
     * simple tasks that are known to complete in a very short time without requiring the main thread. Event handlers
     * using this mode must return quickly to avoid blocking the posting thread, which may be the main thread.
     */
    POSTING, //POSTING外派 外出点和工作的公司是一个组织

    /**
     * On Android, subscriber will be called in Android's main thread (UI thread). If the posting thread is
     * the main thread, subscriber methods will be called directly, blocking the posting thread. Otherwise the event
     * is queued for delivery (non-blocking). Subscribers using this mode must return quickly to avoid blocking the main thread.
     * If not on Android, behaves the same as {@link #POSTING}.
     */
    MAIN,

    /**
     * On Android, subscriber will be called in Android's main thread (UI thread). Different from {@link #MAIN},
     * the event will always be queued for delivery. This ensures that the post call is non-blocking.
     */
    MAIN_ORDERED,

    /**
     * On Android, subscriber will be called in a background thread. If posting thread is not the main thread, subscriber methods
     * will be called directly in the posting thread. If the posting thread is the main thread, EventBus uses a single
     * background thread, that will deliver all its events sequentially. Subscribers using this mode should try to
     * return quickly to avoid blocking the background thread. If not on Android, always uses a background thread.
     */
    BACKGROUND,

    /**
     * Subscriber will be called in a separate thread. This is always independent from the posting thread and the
     * main thread. Posting events never wait for subscriber methods using this mode. Subscriber methods should
     * use this mode if their execution might take some time, e.g. for network access. Avoid triggering a large number
     * of long running asynchronous subscriber methods at the same time to limit the number of concurrent threads. EventBus
     * uses a thread pool to efficiently reuse threads from completed asynchronous subscriber notifications.
     */
    ASYNC
}

//发布事件
interface Poster {

    /**
     * Enqueue an event to be posted for a particular subscription.
     *
     * @param subscription Subscription which will receive the event.
     * @param event        Event that will be posted to subscribers.
     */
    void enqueue(Subscription subscription, Object event);
}