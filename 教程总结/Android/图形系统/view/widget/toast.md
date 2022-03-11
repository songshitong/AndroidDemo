https://www.jianshu.com/p/050ce052b873
android 12 
toast使用
```
Toast.makeText(context, text, duration).show();
```

总结
Toast显示主要NotificationManagerService管理多个以及计时，最后通过WindowManager.addView添加view


Toast创建
frameworks/base/core/java/android/widget/Toast.java
```
public static Toast makeText(Context context, CharSequence text, @Duration int duration) {
      return makeText(context, null, text, duration);
  }
 
public static Toast makeText(@NonNull Context context, @Nullable Looper looper,
      @NonNull CharSequence text, @Duration int duration) {
  //CHANGE_TEXT_TOASTS_IN_THE_SYSTEM     Toast将由SystemUI而不是应用程序中呈现，因此应用程序无法绕过Toast限制自定义背景
  if (Compatibility.isChangeEnabled(CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
      Toast result = new Toast(context, looper);
      result.mText = text;
      result.mDuration = duration;
      return result;
  } else {
      Toast result = new Toast(context, looper);
      View v = ToastPresenter.getTextToastView(context, text);
      result.mNextView = v;
      result.mDuration = duration;  
      return result;
  }
}

public Toast(@NonNull Context context, @Nullable Looper looper) {
          mContext = context;
          mToken = new Binder();
          looper = getLooper(looper);
          mHandler = new Handler(looper);
          //通过toast.addCallback()添加，可以监听taost的展示和消失
          mCallbacks = new ArrayList<>();
          //mTN是ITransientNotification的binder对象 
          mTN = new TN(context, context.getPackageName(), mToken,
                  mCallbacks, looper);
          mTN.mY = context.getResources().getDimensionPixelSize(
                  com.android.internal.R.dimen.toast_y_offset);
          mTN.mGravity = context.getResources().getInteger(
                  com.android.internal.R.integer.config_toastDefaultGravity);
      }
```
ToastPresenter主要负责toast的展示,看一下getTextToastView
获取默认的样式
/frameworks/base/core/java/android/widget/ToastPresenter.java
```
  public static final int TEXT_TOAST_LAYOUT = R.layout.transient_notification;
  public static View getTextToastView(Context context, CharSequence text) {
          View view = LayoutInflater.from(context).inflate(TEXT_TOAST_LAYOUT, null);
          TextView textView = view.findViewById(com.android.internal.R.id.message);
          textView.setText(text);
          return view;
      }
```

toast.show
```
    public void show() {
          if (Compatibility.isChangeEnabled(CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
              checkState(mNextView != null || mText != null, "You must either set a text or a view");
          } else {
              if (mNextView == null) {
                  throw new RuntimeException("setView must have been called");
              }
          }
          //获取INotificationManager
          INotificationManager service = getService();
          String pkg = mContext.getOpPackageName();
          TN tn = mTN;
          tn.mNextView = mNextView;
          final int displayId = mContext.getDisplayId();
  
          try {
              if (Compatibility.isChangeEnabled(CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
                  if (mNextView != null) {
                      // It's a custom toast
                      service.enqueueToast(pkg, mToken, tn, mDuration, displayId);
                  } else {
                      // It's a text toast
                      ITransientNotificationCallback callback =
                              new CallbackBinder(mCallbacks, mHandler);
                      service.enqueueTextToast(pkg, mToken, mText, mDuration, displayId, callback);
                  }
              } else {
                  service.enqueueToast(pkg, mToken, tn, mDuration, displayId);
              }
          } catch (RemoteException e) {
              // Empty
          }
      }
```
可以看到主要通过service实现显示逻辑,binder的服务端是NotificationManagerService
NotificationManagerService和ActivityManagerService类似，是系统的通知服务，
这就解释了为什么在有的手机上关掉应用的通知权限会导致Toast不显示

enqueueTextToast最后调用了enqueueToast
/frameworks/base/services/core/java/com/android/server/notification/NotificationManagerService.java
```
 private void enqueueToast(String pkg, IBinder token, @Nullable CharSequence text,
3193                  @Nullable ITransientNotification callback, int duration, int displayId,
3194                  @Nullable ITransientNotificationCallback textCallback) {
                  ...
3207              final int callingUid = Binder.getCallingUid();
3208              checkCallerIsSameApp(pkg);
                  //是否为系统级应用
3209              final boolean isSystemToast = isCallerSystemOrPhone()
3210                      || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg);
3211              boolean isAppRenderedToast = (callback != null);
3212              if (!checkCanEnqueueToast(pkg, callingUid, isAppRenderedToast, isSystemToast)) {
3213                  return;
3214              }
3215  
3216              synchronized (mToastQueue) {
3217                  int callingPid = Binder.getCallingPid();
3218                  final long callingId = Binder.clearCallingIdentity();
3219                  try {
3220                      ToastRecord record;
3221                      int index = indexOfToastLocked(pkg, token);
                          // 应用已经显示了Toast,执行更新操作 
3224                      if (index >= 0) {
3225                          record = mToastQueue.get(index);
3226                          record.update(duration);
3227                      } else {
                              // 应用未显示Toast
3228                          // Limit the number of toasts that any given package can enqueue.
3229                          // Prevents DOS attacks and deals with leaks.
3230                          int count = 0;
3231                          final int N = mToastQueue.size();
3232                          for (int i = 0; i < N; i++) {
3233                              final ToastRecord r = mToastQueue.get(i);
3234                              if (r.pkg.equals(pkg)) {
3235                                  count++;
                                      //最多5个  static final int MAX_PACKAGE_TOASTS = 5;
3236                                  if (count >= MAX_PACKAGE_TOASTS) {
3237                                      Slog.e(TAG, "Package has already queued " + count
3238                                              + " toasts. Not showing more. Package=" + pkg);
3239                                      return;
3240                                  }
3241                              }
3242                          }
3243  
3244                          Binder windowToken = new Binder();
3245                          mWindowManagerInternal.addWindowToken(windowToken, TYPE_TOAST, displayId,
3246                                  null /* options */);
                              //生成toast记录，添加到ArrayList<ToastRecord> mToastQueue 
3247                          record = getToastRecord(callingUid, callingPid, pkg, isSystemToast, token,
3248                                  text, callback, duration, windowToken, displayId, textCallback);
3249                          mToastQueue.add(record);
3250                          index = mToastQueue.size() - 1;
3251                          keepProcessAliveForToastIfNeededLocked(callingPid);
3252                      }
3253                      // If it's at index 0, it's the current toast.  It doesn't matter if it's
3254                      // new or just been updated, show it.
3255                      // If the callback fails, this will remove it from the list, so don't
3256                      // assume that it's valid after this.
3257                      if (index == 0) {
                              //处理下一个
3258                          showNextToastLocked(false);
3259                      }
3260                  } finally {
3261                      Binder.restoreCallingIdentity(callingId);
3262                  }
3263              }
3264          }
```

showNextToastLocked 处理下一个
```
     void showNextToastLocked(boolean lastToastWasTextRecord) {
7760          if (mIsCurrentToastShown) {
7761              return; // Don't show the same toast twice.
7762          }
7763  
7764          ToastRecord record = mToastQueue.get(0);
7765          while (record != null) {
7766              int userId = UserHandle.getUserId(record.uid);
7767              boolean rateLimitingEnabled =
7768                      !mToastRateLimitingDisabledUids.contains(record.uid);
7769              boolean isWithinQuota =
7770                      mToastRateLimiter.isWithinQuota(userId, record.pkg, TOAST_QUOTA_TAG)
7771                              || isExemptFromRateLimiting(record.pkg, userId);
7772              boolean isPackageInForeground = isPackageInForegroundForToast(record.uid);
7773  
7774              if (tryShowToast(
7775                      record, rateLimitingEnabled, isWithinQuota, isPackageInForeground)) {
                      //显示时长计时  
7776                  scheduleDurationReachedLocked(record, lastToastWasTextRecord);
7777                  mIsCurrentToastShown = true;
7778                  if (rateLimitingEnabled && !isPackageInForeground) {
7779                      mToastRateLimiter.noteEvent(userId, record.pkg, TOAST_QUOTA_TAG);
7780                  }
7781                  return;
7782              }
7783              //移除第一个显示的
7784              int index = mToastQueue.indexOf(record);
7785              if (index >= 0) {
7786                  mToastQueue.remove(index);
7787              }
7788              record = (mToastQueue.size() > 0) ? mToastQueue.get(0) : null;
7789          }
7790      }
7791  
```
先取出第一个record，然后执行tryShowToast，开始对toast显示进行计时，最后将第一个移除

看下toast的tryShowToast
```
  private boolean tryShowToast(ToastRecord record, boolean rateLimitingEnabled,
7794              boolean isWithinQuota, boolean isPackageInForeground) {
7795          if (rateLimitingEnabled && !isWithinQuota && !isPackageInForeground) {
7796              reportCompatRateLimitingToastsChange(record.uid);
7797              Slog.w(TAG, "Package " + record.pkg + " is above allowed toast quota, the "
7798                      + "following toast was blocked and discarded: " + record);
7799              return false;
7800          }
7801          if (blockToast(record.uid, record.isSystemToast, record.isAppRendered(),
7802                  isPackageInForeground)) {
7803              Slog.w(TAG, "Blocking custom toast from package " + record.pkg
7804                      + " due to package not in the foreground at the time of showing the toast");
7805              return false;
7806          }
7807          return record.show();
7808      }
```
主要调用record的show方法

ToastRecord是抽象类,有两种构成TextToastRecord，CustomToastRecord
```
  private ToastRecord getToastRecord(int uid, int pid, String packageName, boolean isSystemToast,
3154              IBinder token, @Nullable CharSequence text, @Nullable ITransientNotification callback,
3155              int duration, Binder windowToken, int displayId,
3156              @Nullable ITransientNotificationCallback textCallback) {
3157          if (callback == null) {
3158              return new TextToastRecord(this, mStatusBar, uid, pid, packageName,
3159                      isSystemToast, token, text, duration, windowToken, displayId, textCallback);
3160          } else {
3161              return new CustomToastRecord(this, uid, pid, packageName,
3162                      isSystemToast, token, callback, duration, windowToken, displayId);
3163          }
3164      }
```

TextToastRecord的show方法  //todo mStatusBar相关
/frameworks/base/services/core/java/com/android/server/notification/toast/TextToastRecord.java
```
public boolean show() {
61          if (mStatusBar == null) {
62              Slog.w(TAG, "StatusBar not available to show text toast for package " + pkg);
63              return false;
64          }
65          mStatusBar.showToast(uid, pkg, token, text, windowToken, getDuration(), mCallback);
66          return true;
67      }
68  
```

/frameworks/base/services/core/java/com/android/server/notification/toast/CustomToastRecord.java
```
  public boolean show() {
52          try {
53              callback.show(windowToken);
54              return true;
55          } catch (RemoteException e) {
56              Slog.w(TAG, "Object died trying to show custom toast " + token + " in package "
57                      + pkg);
58              mNotificationManager.keepProcessAliveForToastIfNeeded(pid);
59              return false;
60          }
61      }
```
主要是调用了callback的show方法，也就是ITransientNotification类型的TN
/frameworks/base/core/java/android/widget/Toast.java
```
private static class TN extends ITransientNotification.Stub {
    public void show(IBinder windowToken) {
              if (localLOGV) Log.v(TAG, "SHOW: " + this);
              mHandler.obtainMessage(SHOW, windowToken).sendToTarget();
          }
mHandler = new Handler(looper, null) {
626                  @Override
627                  public void handleMessage(Message msg) {
628                      switch (msg.what) {
629                          case SHOW: {
630                              IBinder token = (IBinder) msg.obj;
631                              handleShow(token);
632                              break;
633                          }
634                          case HIDE: {
635                              handleHide();
636                              // Don't do this in handleHide() because it is also invoked by
637                              // handleShow()
638                              mNextView = null;
639                              break;
640                          }
641                          case CANCEL: {
642                              handleHide();
643                              // Don't do this in handleHide() because it is also invoked by
644                              // handleShow()
645                              mNextView = null;
646                              try {
647                                  getService().cancelToast(mPackageName, mToken);
648                              } catch (RemoteException e) {
649                              }
650                              break;
651                          }
652                      }
653                  }
654              };
655          }          
     public void handleShow(IBinder windowToken) {
690              // If a cancel/hide is pending - no need to show - at this point
691              // the window token is already invalid and no need to do any work.
692              if (mHandler.hasMessages(CANCEL) || mHandler.hasMessages(HIDE)) {
693                  return;
694              }
695              if (mView != mNextView) {
696                  // remove the old view if necessary
697                  handleHide();
698                  mView = mNextView;
699                  mPresenter.show(mView, mToken, windowToken, mDuration, mGravity, mX, mY,
700                          mHorizontalMargin, mVerticalMargin,
701                          new CallbackBinder(getCallbacks(), mHandler));
702              }
703          }        
}      
```
可以看到主要通过handler的消息SHOW，使用ToastPresenter进行展示
/frameworks/base/core/java/android/widget/ToastPresenter.java
```
     public void show(View view, IBinder token, IBinder windowToken, int duration, int gravity,
223              int xOffset, int yOffset, float horizontalMargin, float verticalMargin,
224              @Nullable ITransientNotificationCallback callback, boolean removeWindowAnimations) {
225          checkState(mView == null, "Only one toast at a time is allowed, call hide() first.");
226          mView = view;
227          mToken = token;
228  
229          adjustLayoutParams(mParams, windowToken, duration, gravity, xOffset, yOffset,
230                  horizontalMargin, verticalMargin, removeWindowAnimations);
231          addToastView();
232          trySendAccessibilityEvent(mView, mPackageName);
233          if (callback != null) {
234              try {
235                  callback.onToastShown();
236              } catch (RemoteException e) {
237                  Log.w(TAG, "Error calling back " + mPackageName + " to notify onToastShow()", e);
238              }
239          }
240      }

//将view添加到WindowManager
private void addToastView() {
302          if (mView.getParent() != null) {
303              mWindowManager.removeView(mView);
304          }
305          try {
306              mWindowManager.addView(mView, mParams);
307          } catch (WindowManager.BadTokenException e) {
308              // Since the notification manager service cancels the token right after it notifies us
309              // to cancel the toast there is an inherent race and we may attempt to add a window
310              // after the token has been invalidated. Let us hedge against that.
311              Log.w(TAG, "Error while attempting to show toast from " + mPackageName, e);
312              return;
313          }
314      }
```
ToastPresenter的show主要用于处理LayoutParam,添加view到WindowManager，然后回调callback.onToastShown,此时对toast的显示监听就收到了


显示时长计时scheduleDurationReachedLocked
```
   private void scheduleDurationReachedLocked(ToastRecord r, boolean lastToastWasTextRecord)
7875      {
7876          mHandler.removeCallbacksAndMessages(r);
              //static final int MESSAGE_DURATION_REACHED = 2;
7877          Message m = Message.obtain(mHandler, MESSAGE_DURATION_REACHED, r);
              //  static final int LONG_DELAY = TOAST_WINDOW_TIMEOUT - TOAST_WINDOW_ANIM_BUFFER; // 3.5 seconds
              //     static final int SHORT_DELAY = 2000; // 2 seconds
7878          int delay = r.getDuration() == Toast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
7879          // Accessibility users may need longer timeout duration. This api compares original delay
7880          // with user's preference and return longer one. It returns original delay if there's no
7881          // preference.
7882          delay = mAccessibilityManager.getRecommendedTimeoutMillis(delay,
7883                  AccessibilityManager.FLAG_CONTENT_TEXT);
7884  
7885          if (lastToastWasTextRecord) {
7886              delay += 250; // delay to account for previous toast's "out" animation
7887          }
7888          if (r instanceof TextToastRecord) {
7889              delay += 333; // delay to account for this toast's "in" animation
7890          }
7891  
7892          mHandler.sendMessageDelayed(m, delay);
7893      }
```
主要通过handler的延时消息实现
```
protected class WorkerHandler extends Handler
8181      {
8186          @Override
8187          public void handleMessage(Message msg)
8188          {
8189              switch (msg.what)
8190              {
8191                  case MESSAGE_DURATION_REACHED:
8192                      handleDurationReached((ToastRecord) msg.obj);
8193                      break;
            ...
        }   
   private void handleDurationReached(ToastRecord record)
7896      {
7898          synchronized (mToastQueue) {
7899              int index = indexOfToastLocked(record.pkg, record.token);
7900              if (index >= 0) {
7901                  cancelToastLocked(index);
7902              }
7903          }
7904      }
}

    void cancelToastLocked(int index) {
7837          ToastRecord record = mToastQueue.get(index);
7838          record.hide();
7839  
7840          if (index == 0) {
7841              mIsCurrentToastShown = false;
7842          }
7843  
7844          ToastRecord lastToast = mToastQueue.remove(index);
7845  
7846          mWindowManagerInternal.removeWindowToken(lastToast.windowToken, false /* removeWindows */,
7847                  lastToast.displayId);
7848          // We passed 'false' for 'removeWindows' so that the client has time to stop
7849          // rendering (as hide above is a one-way message), otherwise we could crash
7850          // a client which was actively using a surface made from the token. However
7851          // we need to schedule a timeout to make sure the token is eventually killed
7852          // one way or another.
7853          scheduleKillTokenTimeout(lastToast);
7854  
7855          keepProcessAliveForToastIfNeededLocked(record.pid);
7856          if (mToastQueue.size() > 0) {
7857              // Show the next one. If the callback fails, this will remove
7858              // it from the list, so don't assume that the list hasn't changed
7859              // after this point.
7860              showNextToastLocked(lastToast instanceof TextToastRecord);
7861          }
7862      }
```
取消toast的主要逻辑是record.hide()，从ToastQueue移除当前，开始处理下一个

//todo 
/frameworks/base/services/core/java/com/android/server/notification/toast/TextToastRecord.java
```
  public void hide() {
71          // If it's null, show() would have returned false
72          checkNotNull(mStatusBar, "Cannot hide toast that wasn't shown");
73  
74          mStatusBar.hideToast(pkg, token);
75      }
```

/frameworks/base/services/core/java/com/android/server/notification/toast/CustomToastRecord.java
```
  public void hide() {
65          try {
66              callback.hide();
67          } catch (RemoteException e) {
71          }
72      }
```

后面的逻辑与show相似了,最终调用mWindowManager将view移除，然后执行toast的隐藏监听onToastHidden()
/frameworks/base/core/java/android/widget/ToastPresenter.java
```
    public void hide(@Nullable ITransientNotificationCallback callback) {
250          checkState(mView != null, "No toast to hide.");
251  
252          if (mView.getParent() != null) {
253              mWindowManager.removeViewImmediate(mView);
254          }
255          try {
256              mNotificationManager.finishToken(mPackageName, mToken);
257          } catch (RemoteException e) {
               ...
259          }
260          if (callback != null) {
261              try {
262                  callback.onToastHidden();
263              } catch (RemoteException e) {
                 ...
266              }
267          }
268          mView = null;
269          mToken = null;
270      }
```