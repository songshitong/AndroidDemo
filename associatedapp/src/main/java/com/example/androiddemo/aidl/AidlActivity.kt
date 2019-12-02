package com.example.androiddemo.aidl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.androiddemo.IMyBinder
import kotlinx.android.synthetic.main.activity_aidl.*



class AidlActivity : AppCompatActivity() {
    private val TAG ="AidlActivity"
    private var conn: MyConn? = null
    private var aidlIntent: Intent? = null
    private var myBinder: IMyBinder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.androiddemo.R.layout.activity_aidl)
        callAidlBtn.setOnClickListener {
            start(it)
        }
    }

    //开启服务按钮的点击事件
    fun start(view: View) {
        aidlIntent = Intent()
        aidlIntent?.action = "com.example.aidlservice"
        aidlIntent?.setPackage("sst.example.androiddemo.feature")

        conn = MyConn()
        //绑定服务，
        // 第一个参数是intent对象，表面开启的服务。
        // 第二个参数是绑定服务的监听器
        // 第三个参数一般为BIND_AUTO_CREATE常量，表示自动创建bind
        bindService(aidlIntent, conn!!, Context.BIND_AUTO_CREATE)
    }

    operator fun invoke() {
        try {
            myBinder?.invokeMethodInMyService()
        } catch (e: RemoteException) {
            Log.e(TAG,"call aidl err "+e.printStackTrace())
        }

    }


    inner class MyConn : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            //通过Stub得到接口类型
            myBinder = IMyBinder.Stub.asInterface(iBinder)
            invoke()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            myBinder= null
        }
    }

    //获取隐式intent  todo Android5.0以后不让使用隐式intent
    // 1 getExplicitIntent将隐式转为显示
    // 2 拿到intent所在的包名和service名
    fun getExplicitIntent(context: Context, implicitIntent: Intent): Intent? {
        // Retrieve all services that can match the given intent
        val pm = context.packageManager
        val resolveInfo = pm.queryIntentServices(implicitIntent, 0)
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size != 1) {
            return null
        }
        // Get component info and create ComponentName
        val serviceInfo = resolveInfo[0]
        val packageName = serviceInfo.serviceInfo.packageName
        val className = serviceInfo.serviceInfo.name
        val component = ComponentName(packageName, className)
        // Create a new intent. Use the old one for extras and such reuse
        val explicitIntent = Intent(implicitIntent)
        // Set the component to be explicit
        explicitIntent.component = component
        return explicitIntent
    }
}
