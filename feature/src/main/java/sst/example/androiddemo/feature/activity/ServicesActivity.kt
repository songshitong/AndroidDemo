package sst.example.androiddemo.feature.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_services.*
import sst.example.androiddemo.feature.service.StartService






class ServicesActivity : AppCompatActivity() {
    private val TAG = "ServicesActivity"
   var connection = MyServiceConnection()
    private var myBinder: StartService.MyBinder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(sst.example.androiddemo.feature.R.layout.activity_services)
        val serviceIntent = Intent(this,StartService::class.java)
        startService.setOnClickListener {
            startService(serviceIntent)
        }
        stopService.setOnClickListener {
            stopService(serviceIntent)
        }

        bindServiceBtn.setOnClickListener {
            bindService(serviceIntent,connection, Context.BIND_AUTO_CREATE)
        }

        unbindServiceBtn.setOnClickListener {
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


