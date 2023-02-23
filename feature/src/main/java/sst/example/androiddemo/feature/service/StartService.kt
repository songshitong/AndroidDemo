package sst.example.androiddemo.feature.service

import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log

public  class StartService : Service(){
    private val TAG = "StartService"
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG,"onBind ====")
        //bindService才会调用
        return MyBinder()
    }

    override fun onCreate() {
        Log.d(TAG,"onCreate ====")
        super.onCreate()
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        Log.d(TAG,"onStart ====")
        super.onStart(intent, startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onStartCommand ====")
        //startService才会调用
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG,"onRebind ====")

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG,"onUnbind ====")

        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy ====")

        super.onDestroy()
    }


    interface MyIBinder {
        fun invokeMethodInMyService()
    }

    inner class MyBinder : Binder(), MyIBinder {

        fun stopService(serviceConnection: ServiceConnection) {
            unbindService(serviceConnection)
        }

        override fun invokeMethodInMyService() {
            for (i in 0..19) {
                println("service is opening")
            }
        }
    }
}