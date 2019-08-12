package sst.example.androiddemo.feature.webview

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_jump.*
import sst.example.androiddemo.feature.R
import java.lang.Exception

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
              Log.e("JumpActivity",e.printStackTrace().toString())
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
}
