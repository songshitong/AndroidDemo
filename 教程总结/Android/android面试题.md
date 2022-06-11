线程间通信
View.post
```
// post 方法在子线程中调用
textView.post(new Runnable() {
    @Override
    public void run() {
        // 此处代码会在 UI 线程执行
    }
});
```
Activity.runOnUiThread
```
// runOnUiThread 方法在子线程中调用
activity.runOnUiThread(new Runnable() {
    @Override
    public void run() {
        // 此处代码会在 UI 线程执行
    }
});
//最终通过handler执行
   public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }
```
handler.post
```
// post 方法在子线程中调用
handler.post(new Runnable() {
    @Override
    public void run() {
        // handler 在主线程中初始化时，此处代码在主线程中执行
    }
});
```
AsyncTask
```
private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {
     protected Long doInBackground(URL... urls) {
         int count = urls.length;
         long totalSize = 0;
         for (int i = 0; i < count; i++) {
             totalSize += Downloader.downloadFile(urls[i]);
             //手动调用更新进度
             publishProgress((int) ((i / (float) count) * 100));
             // Escape early if cancel() is called
             if (isCancelled()) break;
         }
         return totalSize;
     }
     protected void onProgressUpdate(Integer... progress) {
         setProgressPercent(progress[0]);
     }
     protected void onPostExecute(Long result) {
         showDialog("Downloaded " + result + " bytes");
     }
 }
```



view的事件分发
点击事件由上而下的分发传递规则
当点击事件产生后会由DecorView传递给Activity来处理，再传递给Window再传递给顶层的ViewGroup，一般在事件传递中只考虑ViewGroup的onInterceptTouchEvent()方法，
因为一般情况我们不会去重写dispatchTouchEvent()方法。
对于根ViewGroup，点击事件首先传递给它的dispatchTouchEvent()方法，如果该ViewGroup的onInterceptTouchEvent()方法返回true，
则表示它要拦截这个事件，这个事件就会交给它的onTouchEvent()方法处理，如果onInterceptTouchEvent()方法返回false，
则表示它不拦截这个事件，则交给它的子元素的dispatchTouchEvent()来处理，如此的反复下去。如果传递给最底层的View，View是没有子View的，
就会调用View的dispatchTouchEvent()方法，一般情况下最终会调用View的onTouchEvent()方法。

点击事件由下而上的处理传递规则
点击事件传给最底层的View，如果他的onTouchEvent()方法返回true，则事件由最底层的View消耗并处理了，如果返回false则表示该View不做处理，
则传递给父View的onTouchEvent()处理，如果父View的onTouchEvent()仍旧返回返回false，则继续传递给改父View的父View处理，如此的反复下去。
```
public boolean dispatchTouchEvent(MotionEvent ev) {
  boolean result=false;
  if(onInterceptTouchEvent(ev)){
        result=onTouchEvent(ev);
   }else{
        result=child.dispatchTouchEvent(ev);
  }
return result;
```

handler源码总结

view的绘制流程
视图绘制的起点在 ViewRootImpl 类的 performTraversals()方法，
在这个方法内其实是按照顺序依次调用了 mView.measure()、mView.layout()、mView.draw()
View的绘制流程分为3步：测量、布局、绘制，分别对应3个方法 measure、layout、draw。
测量阶段。view.measure->view.onMeasure 在measure 方法中做一些优化和准备工作后会调用 onMeasure 方法进行实际的自我测量。
onMeasure方法在View和ViewGroup做的事情是不一样的：
View。 View 中的 onMeasure 方法会计算自己的尺寸并通过 setMeasureDimension 保存。
ViewGroup。 ViewGroup 中的 onMeasure 方法会调用所有子view的measure 方法进行自我测量并保存。
   然后通过子View的尺寸和位置计算出自己的尺寸并保存。
widthMeasureSpec, heightMeasureSpec
2个bit用于区分不同的布局模式，30个bit存放的是尺寸的数据
int widthMode = MeasureSpec.getMode(widthMeasureSpec);
int widthSize = MeasureSpec.getSize(widthMeasureSpec);
测量模式	    表示意思
UNSPECIFIED	父容器没有对当前View有任何限制，当前View可以任意取尺寸   通常用于系统内部，应用开发中很少用到
EXACTLY	当前的尺寸就是当前View应该取的尺寸  match_parent或固定尺寸
AT_MOST	当前尺寸是当前View能取的最大尺寸    wrap_content

布局阶段。view.layout->View.onLayout layout 方法会保存父 View 传进来的尺寸和位置，并调用 onLayout 进行实际的内部布局。
onLayout 在 View 和 ViewGroup 中做的事情也是不一样的：
View。 因为 View 是没有子 View 的，所以View的onLayout里面什么都不做。  
ViewGroup。 ViewGroup 中的 onLayout 方法会调用所有子 View 的 layout 方法，把尺寸和位置传给他们，让他们完成自我的内部布局。

绘制阶段。draw 方法会做一些调度工作，然后会调用 onDraw 方法进行 View 的自我绘制。
draw 方法的调度流程大致是这样的：
绘制背景。对应 drawBackground(Canvas)方法。
绘制主体。对应 onDraw(Canvas)方法。
绘制子View。 对应 dispatchDraw(Canvas)方法。     ViewGroup重写了该方法，调用drawChild->child.draw
绘制滑动相关和前景。 对应 onDrawForeground(Canvas)   




bitmap内存变迁
1.0->native->2.3 Dalvik ->7.1->8.0 native->

bitmap计算  放在资源目录下  ARGB_8888 一个像素4字节
```
scale = targetDensity / density

widthPix = originalWidth * scale

heightPix = orignalHeight * scale

Bitmap Memory = widthPix * scale * heightPix * scale * 一个像素大小
```


图片压缩
BitmapFactory.Options.inSampleSize 采样
BitmapFactory.Options.inPreferredConfig= Bitmap.Config.RGB_565
Bitmap.compress  todo
图片复用
BitmapFactory.Options.inBitmap