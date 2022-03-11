package sst.example.androiddemo.feature.SystemBug;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;

import java.lang.reflect.Field;

import sst.example.androiddemo.feature.R;

public class ToastBugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toast_bug);
        Button btn = findViewById(R.id.toast_bug);
        //7.1.1 BadTokenException       7.0没复现
//        E/AndroidRuntime: FATAL EXCEPTION: main
//        Process: sst.example.androiddemo.feature, PID: 3530
//        android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@db6768c is not valid; is your activity running?
//        at android.view.ViewRootImpl.setView(ViewRootImpl.java:679)
//        at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:342)
//        at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:93)
//        at android.widget.Toast$TN.handleShow(Toast.java:459)
//        at android.widget.Toast$TN$2.handleMessage(Toast.java:342)
//        at android.os.Handler.dispatchMessage(Handler.java:102)
//        at android.os.Looper.loop(Looper.java:154)
//        at android.app.ActivityThread.main(ActivityThread.java:6119)
//        at java.lang.reflect.Method.invoke(Native Method)
//        at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:886)
//        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:776)
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SafeToast.show(ToastBugActivity.this);
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class SafeToast {
        public static void show(Context context){
            Toast toast = Toast.makeText(context,"toast",Toast.LENGTH_SHORT);
            hook(toast);
            toast.show();
        }

        private static void hook(Toast toast){
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                try {
                    //noinspection JavaReflectionMemberAccess
                    Field mTNField = Toast.class.getDeclaredField("mTN");
                    mTNField.setAccessible(true);
                    //获取当前的mTN
                    Object mTN = mTNField.get(toast);
                    Field mTNmHandlerField = mTNField.getType().getDeclaredField("mHandler");
                    mTNmHandlerField.setAccessible(true);
                    //获取mTN的mHandler
                    Handler tnHandler = (Handler) mTNmHandlerField.get(mTN);
                    //mHandler替换为new SafeHandler()
                    mTNmHandlerField.set(mTN, new SafeHandler(tnHandler));
                } catch (Exception ignored) { /**/ }
            }
        }

        private static class SafeHandler extends Handler {
            private Handler impl;

            SafeHandler(Handler impl) {
                this.impl = impl;
            }

            @Override
            public void handleMessage(Message msg) {
                //消息处理为hook的handler
                impl.handleMessage(msg);
            }

            @Override
            public void dispatchMessage(Message msg) {
                //对dispatchMessage进行异常捕获
                try {
                    impl.dispatchMessage(msg);
                } catch (Exception e) {
                    Log.e("SafeToast", e.toString());
                }
            }
        }
    }


}