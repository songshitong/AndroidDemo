package sst.example.androiddemo.feature;

import android.app.Application;
import android.os.BaseBundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;


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
