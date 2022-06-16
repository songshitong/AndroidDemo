
context.getExternalCacheDir()
目录在/storage/emulated
context.getCacheDir()目录在/data/data/
/data/user/0/包名/cache
```
  if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
```  