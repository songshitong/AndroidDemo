
setResult不生效,手势退出的时候，可能是没有设置
```
 @Override public void onBackPressed() {
    setResult();
    super.onBackPressed();
  }
```


onActivityResult缺点必须在activity或fragment里面，不能脱离
多个intent的结果在同一个方法里面，很容易变得非常庞大

https://blog.csdn.net/guolin_blog/article/details/121063078
https://www.xlgz520.com/index.php/archives/26/
```
ActivityResultLauncher<Intent> requestDataLauncher;//java声明，调用launch方法一般要求参数是intent

//ActivityResultContracts.RequestPermission
//ActivityResultContracts.TakePicture
//ActivityResultContracts.GetContent
 private val requestDataLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data?.getStringExtra("data")
            // Handle data from SecondActivity
        }
    }
    
    val intent = Intent(this, SecondActivity::class.java)
    requestDataLauncher.launch(intent)
```
其他类型RequestMultiplePermissions(请求多个权限)，TakePicture(照相)等
使用要点
1. 需要实现ActivityResultCaller例如AppCompatActivity，Fragment(androidx)
2. registerForActivityResult放在start之前


选择图片
```
  galleryLauncher = registerForActivityResult(GetContent()) { uri ->}
  
  galleryLauncher?.launch("image/*")
```

拍照
```
   cameraLauncher = registerForActivityResult(TakePicture()) {}
   
    cameraLauncher?.launch(
            FileProvider.getUriForFile(
              context,
              "${activity?.packageName}.fileprovider",
              pathFile
            )
          ) 
```