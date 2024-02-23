package sst.example.androiddemo.feature.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.List;
import android.os.Process;

//https://juejin.cn/post/6877127949452050446
public class ProcessUtil {
  private static String currentProcessName;

  /**
   * @return 当前进程名
   */
  @Nullable
  public static String getCurrentProcessName(@NonNull Context context)  {
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //1)通过Application的API获取当前进程名  android9及以上
    currentProcessName = getCurrentProcessNameByApplication();
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //2)通过反射ActivityThread获取当前进程名  android8及以下
    currentProcessName = getCurrentProcessNameByActivityThread();
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //3 cmdLine获取
    currentProcessName = getCurrentProcessNameByCmdLine();
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //4)通过ActivityManager获取当前进程名  兜底获取
    currentProcessName = getCurrentProcessNameByActivityManager(context);

    return currentProcessName;
  }

  //通过cmdLine获取进程名  https://stackoverflow.com/questions/19631894/is-there-a-way-to-get-current-process-name-in-android
  public static String getCurrentProcessNameByCmdLine()
      {
    BufferedReader cmdlineReader = null;
    StringBuilder processName = new StringBuilder();
    try {
      cmdlineReader = new BufferedReader(new InputStreamReader(
          new FileInputStream(
              "/proc/" + android.os.Process.myPid() + "/cmdline"),
          "iso-8859-1"));
      int c;
      while ((c = cmdlineReader.read()) > 0) {
        processName.append((char) c);
      }
      return processName.toString();
    }catch ( IOException e){

    }
    finally {
      if (cmdlineReader != null) {
        try {
          cmdlineReader.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return processName.toString();
  }

  /**
   * 通过Application新的API获取进程名，无需反射，无需IPC，效率最高。
   */
  public static String getCurrentProcessNameByApplication() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      return Application.getProcessName();
    }
    return null;
  }

  /**
   * 通过反射ActivityThread获取进程名，避免了ipc
   */
  public static String getCurrentProcessNameByActivityThread() {
    String processName = null;
    try {
      //高版本限制api调用 只在低版本使用
     /* @SuppressLint("PrivateApi")*/ final Method declaredMethod = Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader())
          .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
      declaredMethod.setAccessible(true);
      final Object invoke = declaredMethod.invoke(null, new Object[0]);
      if (invoke instanceof String) {
        processName = (String) invoke;
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return processName;
  }

  /**
   * 通过ActivityManager 获取进程名，需要IPC通信
   */
  public static String getCurrentProcessNameByActivityManager( Context context) {
    if (context == null) {
      return null;
    }
    int pid = Process.myPid();
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    if (am != null) {
      List<ActivityManager.RunningAppProcessInfo> runningAppList = am.getRunningAppProcesses();
      if (runningAppList != null) {
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList) {
          if (processInfo.pid == pid) {
            return processInfo.processName;
          }
        }
      }
    }
    return null;
  }
}