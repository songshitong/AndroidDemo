package sst.example.androiddemo.feature.service;

import androidx.appcompat.app.AppCompatActivity;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import sst.example.androiddemo.feature.R;

public class IntentServiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent_service);
        //activity中使用

        Intent intent = new Intent(this,MyService.class);
        intent.putExtra("task","播放音乐");
        startService(intent);
        intent.putExtra("task","播放视频");
        startService(intent);
        intent.putExtra("task","播放图片");
        startService(intent);
    }

    public class MyService extends IntentService {
        //这里必须有一个空参数的构造实现父类的构造,否则会报异常
        //java.lang.InstantiationException: java.lang.Class<***.MyService> has no zero argument constructor
        public MyService() {
            super("");
        }
        @Override
        public void onCreate() {
            System.out.println("onCreate");
            super.onCreate();
        }
        @Override
        public int onStartCommand( Intent intent, int flags, int startId) {
            System.out.println("onStartCommand");
            return super.onStartCommand(intent, flags, startId);

        }
        @Override
        public void onStart( Intent intent, int startId) {
            System.out.println("onStart");
            super.onStart(intent, startId);
        }
        @Override
        public void onDestroy() {
            System.out.println("onDestroy");
            super.onDestroy();
        }
        //这个是IntentService的核心方法,它是通过串行来处理任务的,也就是一个一个来处理
        @Override
        protected void onHandleIntent( Intent intent) {
            System.out.println("工作线程是: "+Thread.currentThread().getName());
            String task = intent.getStringExtra("task");
            System.out.println("任务是 :"+task);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}