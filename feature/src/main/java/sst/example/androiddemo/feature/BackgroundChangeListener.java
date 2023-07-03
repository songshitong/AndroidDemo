package sst.example.androiddemo.feature;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//前后台监听，根据各个activity的生命周期计算
public class BackgroundChangeListener
    implements Application.ActivityLifecycleCallbacks {
  private int activityCount = 0;
  private OnBackgroundChangeListener listener;

  public void setListener(OnBackgroundChangeListener listener) {
    this.listener = listener;
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

  }

  @Override public void onActivityStarted(@NonNull Activity activity) {
    activityCount++;
    if (1 == activityCount && null != listener) {
      listener.onBackgroundChange(false);
    }
  }

  @Override public void onActivityResumed(@NonNull Activity activity) {

  }

  @Override public void onActivityPaused(@NonNull Activity activity) {

  }

  @Override public void onActivityStopped(@NonNull Activity activity) {
    activityCount--;
    if (0 == activityCount && null != listener) {
      listener.onBackgroundChange(true);
    }
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

  }

  @Override public void onActivityDestroyed(@NonNull Activity activity) {

  }

  public interface OnBackgroundChangeListener {
    void onBackgroundChange(boolean background);
  }
}
