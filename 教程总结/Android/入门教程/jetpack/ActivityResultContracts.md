
onActivityResult缺点必须在activity或fragment里面，不能脱离
多个intent的结果在同一个方法里面，很容易变得非常庞大

https://blog.csdn.net/guolin_blog/article/details/121063078
https://www.xlgz520.com/index.php/archives/26/
```
 private val requestDataLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data?.getStringExtra("data")
            // Handle data from SecondActivity
        }
    }
    
    val intent = Intent(this, SecondActivity::class.java)
    requestDataLauncher.launch(intent)
```
使用要点
1. 需要实现ActivityResultCaller例如AppCompatActivity，Fragment(androidx)
2. registerForActivityResult放在start之前