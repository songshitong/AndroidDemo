package sst.example.androiddemo.feature.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.example.androiddemo.IMyBinder

// 官方文档关于binder和message接口的区别
//为什么aidl复杂，涉及到进程间通信，序列化
//Android的IPC机制  进程间通信总结

class AidlService : Service() {
    private val TAG ="AidlService"
    override fun onBind(intent: Intent): IBinder {
        //返回MyBind对象
        return MyBinder()
    }

    private fun methodInMyService(str:String) {
        Log.d(TAG,"服务里的方法执行了 ====")
    }

    /**
     * 直接继承IMyBinder.Stub
     */
    private inner class MyBinder : IMyBinder.Stub() {
        override fun basicTypes(
            anInt: Int,
            aLong: Long,
            aBoolean: Boolean,
            aFloat: Float,
            aDouble: Double,
            aString: String?
        ) {
        }

        @Throws(RemoteException::class)
        override fun invokeMethodInMyService(str:String) {
            methodInMyService(str)
        }
    }
}