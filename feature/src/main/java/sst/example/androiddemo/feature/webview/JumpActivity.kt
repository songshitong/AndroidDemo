package sst.example.androiddemo.feature.webview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_jump.*
import sst.example.androiddemo.feature.R

//scheme跳转方式
class JumpActivity : AppCompatActivity() {
    val url = "sstdemo://www.sstdemo.com:80/mypath?key=mykey"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jump)
        jumpBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            }catch (e:Exception){
              //拉不起来可以执行其他操作...比例打开备用页面
              Log.e("JumpActivity",e.printStackTrace().toString())
              //错误信息
              //   android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.VIEW dat=sstdemo://www.sstdemo.com:80/mypath?key=mykey }
            }
        }
    }

    // 判断是否可以拉起 代码来自flutter-UrlLauncher插件
    private fun canLaunch(url: String):Boolean {
        val launchIntent = Intent(Intent.ACTION_VIEW)
        launchIntent.data = Uri.parse(url)
        val componentName = launchIntent.resolveActivity(this.getPackageManager())
        val canLaunch =
            componentName != null && "{com.android.fallback/com.android.fallback.Fallback}" != componentName.toShortString()
        return canLaunch
    }

    //本地起服务器，应对浏览器限制加载不了本地页面
    //https://stackoverflow.com/questions/54697024/view-index-html-found-in-the-assets-folder-using-nanohttpd-server-embedded-withi
    //http://programminglife.io/android-http-server-with-nanohttpd/
}
