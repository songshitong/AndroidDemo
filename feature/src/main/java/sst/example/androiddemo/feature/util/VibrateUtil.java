package sst.example.androiddemo.feature.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

//震动
public class VibrateUtil {
  public void vibrate(View view){
    if(view.isHapticFeedbackEnabled()){
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }else {
      Log.e("VibrateUtil","不支持vibrate view "+view);
    }
  }

  //需要添加权限 <uses-permission android:name="android.permission.VIBRATE"/>
  @SuppressLint("MissingPermission") public void vibrateWithContext(Context context){
    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    // Vibrate for 500 milliseconds
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      //deprecated in API 26
      v.vibrate(500);
    }
  }
}
