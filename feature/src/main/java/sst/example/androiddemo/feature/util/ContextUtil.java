package sst.example.androiddemo.feature.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;


public class ContextUtil {

  //根据context获取activity
  static Activity findActivity(Context context) {
    do {
      if (context instanceof Activity) {
        return (Activity) context;
      } else if (context instanceof ContextWrapper){
        context = ((ContextWrapper) context).getBaseContext();
      } else {
        return null;
      }
    } while (context != null);
    return null;
  }
}
