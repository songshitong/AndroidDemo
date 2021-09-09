package sst.example.androiddemo.feature;

import android.app.Application;


public class MApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //判断是否是主进程 阿里规范
//        if (mainProcess) {
//
//...
//
//        }
//
////仅在后台进程中初始化
//
//        if (bgProcess) {
//
//...
//
//        }
    }
}
