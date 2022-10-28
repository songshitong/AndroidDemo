

外部公共文档
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
androidQ val insertUri = contentResolver.insert(Downloads.EXTERNAL_CONTENT_URI, value)
/storage/emulated/0/Download

Environment.getExternalStorageDirectory().path
/storage/emulated/0

context.getExternalCacheDir()
目录在/storage/emulated  例如/storage/emulated/0/Android/data/com.example.xx/cache

外部文档
getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
/storage/emulated/0/Android/data/com.example.xx/files/Documents

getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
/storage/emulated/0/Android/data/com.example.aibadge/files/Download

context.getCacheDir()目录在/data/data/       Cache目录在部分手机升级时文件会被丢弃，不要存放重要文件，只存放缓存
/data/user/0/包名/cache
```
  if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
```  
getDataDir  data目录就是包名下面的 
/data/data/包名/ 

getFilesDir
/data/data/包名/files/

文件监听  
//2022-07-06先监听后创建目录不生效的。。。。  
```
 observer = new FileObserver(BleManager.getInstance("/d') {
      @Override public void onEvent(int event, @Nullable String path) {
        switch (event){
            case FileObserver.CREATE:
              Logger.i(TAG+"文件改变");
              ...
              break;
          }
      }
    };
    observer.startWatching();
        
    observer.stopWatching();
    
```