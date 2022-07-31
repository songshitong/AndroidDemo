

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
1. AppCompatActivity
2. registerForActivityResult防止start之前