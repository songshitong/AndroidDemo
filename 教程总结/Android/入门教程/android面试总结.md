


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
        // handler 在子线程中初始化事，此处代码在子线程中执行
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