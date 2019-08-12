package sst.example.androiddemo.feature.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import com.blankj.utilcode.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;

public class MyUtils {
    public static final String TAG = "sst.example.androiddemo";


    public static void log(String msg){
        Log.d(TAG,msg);
    }
    //todo activity不显示后  bitmap 内存优化
    //todo surfaceview 自定义view  opengl
    //http://weishu.me/2016/12/23/dive-into-android-optimize-vm-heap/
    //todo view.bringToFront


    /**
     * Dp 2 px float.
     *
     * @param context the context
     * @param dp      the dp
     * @return the float
     */
    public static float dp2px(@NonNull Context context, float dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    /**
     * Sp 2 px float.
     *
     * @param context the context
     * @param dp      the dp
     * @return the float
     */
    public static float sp2px(@NonNull Context context, float dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp, dm);
    }

    /**
     * Show hide soft key bord.   隐藏和展示
     *
     * @param context the context
     */
    public static void showHideSoftKeyBord(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Get system album intent intent.
     *
     * @return the intent
     */
    public static Intent getSystemAlbumIntent() {
//        final String MIME_TYPE_IMAGE_JPEG = "image/*";
//        Intent getImage = new Intent();
//        if (Build.VERSION.SDK_INT <19) {
//            getImage.setAction(Intent.ACTION_GET_CONTENT);
//        }else {
//            getImage.setAction(Intent.ACTION_OPEN_DOCUMENT);
//        }
//        getImage.addCategory(Intent.CATEGORY_OPENABLE);
        Intent getImage = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getImage.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
//        getImage.setType(MIME_TYPE_IMAGE_JPEG);
        return getImage;
    }

    //多张图片
    public static Intent getSystemMultipleAlbumIntent(){
        // TODO: 2018/11/13 知识点
        Intent getImage = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getImage.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        getImage.putExtra(EXTRA_ALLOW_MULTIPLE,true);
        return getImage;
    }

    //视频
    public static Intent getAlbumVideoIntent(){
        Intent getImage = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getImage.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "video/*");
        return getImage;
    }

    //音频
    public static Intent getAlbumAudioIntent(){
        Intent getAduio = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getAduio.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "audio/*");
        return getAduio;
    }

    //使用外部播放器播放视频   Intent.createChooser 创建多个选择
    public static Intent getSysVideoPlayer(Uri path){
        Intent openVideo = new Intent(Intent.ACTION_VIEW);
        openVideo.setDataAndType(path, "video/*");
        return Intent.createChooser(openVideo,"选择播放器");
    }


    /**
     * Get sysstem img crop intent.
     *
     * @param uri the uri
     * @param out the out
     * @return the intent
     */
    public static Intent getSysstemImgCrop(Uri uri, Uri out) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        // crop为true是设置在开启的intent中设置显示的view可以剪裁
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX,outputY 是剪裁图片的宽高
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", false);
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("circleCrop", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, out);

        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        return intent;
    }

    /**
     * Get system brower intent.
     *
     * @param url the url
     * @return the intent
     */
    public static Intent getSystemBrower(String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        return Intent.createChooser(intent, "请选择浏览器");
    }


    /**
     * Get system email intent. 获取系统邮件发送
     *
     * @return the intent
     */
    public static Intent getSystemEmail(String emailUrl, String title, String content) {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse(String.format("mailto:%s", emailUrl)));
        data.putExtra(Intent.EXTRA_SUBJECT, title);
        data.putExtra(Intent.EXTRA_TEXT, content);
        return Intent.createChooser(data, "请选择发送方式");
    }

    public static final Intent getSystemAppDetailSetting(Context context) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        Build.VERSION.SDK_INT >= 9
        localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return localIntent;
    }


    /**
     * 创建目录.
     */
    public static void initDirectory(String file) {
        //todo 移除第三方util
        //头像目录
        if (!FileUtils.isFileExists(file)) {
            File target = new File(file);
            target.mkdir();
        }
    }
    /**
     * 创建文件.
     */
    public static void initFile(String file) {
        //todo 移除第三方util
        //头像目录
        if (!FileUtils.isFileExists(file)) {
            File target = new File(file);
            try {
                target.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
