package sst.example.androiddemo.feature.webview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import sst.example.androiddemo.feature.R
import android.net.Proxy.getHost
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.marginTop
import kotlinx.android.synthetic.main.activity_scheme.*


//在Android平台而言，URI主要分三个部分：scheme, authority,path, queryString。其中authority又分为host和port。格式如下：
//scheme://host:port/path?qureyParameter=queryString
//举个例子：
//http://www.sstdemo.com:80/aaa?id=hello

//Manifest配置  <intent-filter>配置项中有<data>配置
//<!--需要添加下面的intent-filter配置-->
//<intent-filter>
//<action android:name="android.intent.action.VIEW"/>
//<category android:name="android.intent.category.DEFAULT"/>
//<category android:name="android.intent.category.BROWSABLE"/>
//<data android:scheme="myscheme"/>
//</intent-filter>
//<data android:host=""
//android:mimeType=""
//android:path=""
//android:pathPattern=""
//android:pathPrefix=""
//android:port=""
//android:scheme=""
//android:ssp=""
//android:sspPattern=""
//android:sspPrefix=""/>
class SchemeActivity : AppCompatActivity() {
    val  TAG = "SchemeActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheme)

        //xml 中配置的scheme是sstdemo  host是www.sstdemo.com
        val intent = intent
        Log.d(TAG, "scheme:" + intent.scheme!!)
        val uri = intent.data
        Log.d(TAG, "scheme: " + uri!!.scheme)
        Log.d(TAG, "host: " + uri.host)
        Log.d(TAG, "port: " + uri.port)
        Log.d(TAG, "path: " + uri.path)
        Log.d(TAG, "queryString: " + uri.query)
        Log.d(TAG, "queryParameter: " + uri.getQueryParameter("key"))
        schemeContent.text = "scheme ${uri.scheme} \n host  ${uri.host} \n port ${uri.port} \n path ${uri.path} \n queryString ${uri.query}"
    }
}