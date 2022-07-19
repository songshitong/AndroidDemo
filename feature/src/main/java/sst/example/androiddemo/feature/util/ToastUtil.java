package sst.example.androiddemo.feature.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.google.android.material.snackbar.Snackbar;
import sst.example.androiddemo.feature.R;

public class ToastUtil {

  public static void show(Context context,String msg,int duration){
    Toast toast = customToast(context,msg);
    toast.setDuration(duration);
    toast.show();
  }

  public static void show(Context context,@StringRes int resId,int duration){
    Toast toast = customToast(context,context.getString(resId));
    toast.setDuration(duration);
    toast.show();
  }

  //"方法被Android废弃了"
  @Deprecated()
  private static Toast customToast(Context context,String msg){
    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    //自定义布局
    View view = inflater.inflate(R.layout.toast_custom_layout, null);
    //自定义toast文本
    TextView mTextView = (TextView)view.findViewById(R.id.toast_msg);
    mTextView.setText(msg);
    Toast mToast = new Toast(context);
    //设置toast居中显示
    //setGravity不支持文字了       setGravity() shouldn't be called on text toasts, the values won't be used
    mToast.setGravity(Gravity.CENTER, 0, 0);
    //该方法被废弃
    mToast.setView(view);
    return mToast;
  }

  //https://juejin.cn/post/7029216153469714445
  public static Snackbar customSnackbar(Activity activity,String str,int length){
    Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), str, length);
    View snackBarView = snackbar.getView();
    //从View中找出当前窗口最外层视图，然后在其底部显示
    //设置布局居中
    int diffW = (int) MyUtils.dp2px(activity,10f);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(snackBarView.getLayoutParams().width-diffW, snackBarView.getLayoutParams().height);
    params.gravity = Gravity.CENTER;
    snackBarView.setLayoutParams(params);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
      TextView message = (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
      //View.setTextAlignment需要SDK>=17
      message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
      message.setGravity(Gravity.CENTER);
    }
    snackbar.show();
    return snackbar;
  }
}
