package sst.example.androiddemo.feature.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sst.example.androiddemo.feature.R
import sst.example.androiddemo.feature.service.StartService






class ServicesActivity : AppCompatActivity() {
    private val TAG = "ServicesActivity"
   var connection = MyServiceConnection()
    private var myBinder: StartService.MyBinder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(sst.example.androiddemo.feature.R.layout.activity_services)
        val serviceIntent = Intent(this,StartService::class.java)
      findViewById<View>(R.id.startService).setOnClickListener {
            startService(serviceIntent)
        }
        findViewById<View>(R.id.stopService).setOnClickListener {
            stopService(serviceIntent)
        }

        findViewById<View>(R.id.bindServiceBtn).setOnClickListener {
            bindService(serviceIntent,connection, Context.BIND_AUTO_CREATE)
        }

        findViewById<View>(R.id.unbindServiceBtn).setOnClickListener {
            unbindService(connection)
        }
    }


     inner class MyServiceConnection : ServiceConnection {

        //这里的第二个参数IBinder就是Service中的onBind方法返回的
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "onServiceConnected ====")
            myBinder = service as StartService.MyBinder
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "onServiceDisconnected ====")
        }
    }



}


