package sst.example.androiddemo.feature.util;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;

public class MyUtils {
    public static final String TAG = "sst.example.androiddemo";


    public static void log(String msg){
        Log.d(TAG,msg);
    }
    //todo activity不显示后  bitmap 内存优化
    //todo surfaceview 自定义view  opengl
    //http://weishu.me/2016/12/23/dive-into-android-optimize-vm-heap/



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
        //getImage.setType(MIME_TYPE_IMAGE_JPEG);
        return getImage;
    }

    //多张图片
    public static Intent getSystemMultipleAlbumIntent(){
        Intent getImage = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getImage.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        //允许选择多个  部分相册，点击直接返回单个，长按开始文件选择
        getImage.putExtra(EXTRA_ALLOW_MULTIPLE,true);
        return getImage;
    }

    //限制图片为JPG  视频为mp4
    //https://stackoverflow.com/questions/4922037/android-let-user-pick-image-or-video-from-gallery
    public static Intent getMediaIntent(){
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            //intent = new Intent(Intent.ACTION_GET_CONTENT);
            //intent.setType("image/* video/*");
            //ACTION_PICK存在多个选择器，需要手动选择原生相册，系统相册，三方相册等
             intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(android.provider
                .MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/jpg video/mp4");
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            //CATEGORY_OPENABLE 只选择能通过ContentResolver.openFileDescriptor(Uri, String)打开的文件
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/jpg");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/jpg", "video/mp4"});
        }
        return intent;
    }

    //选择文件
    public Intent getFileIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/ogg");
        return intent;
    }

    //拿到intent后，查询文件的大小和名字
    public static void getContentProviderInfo(Context context,Uri uri){
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        while (cursor.moveToNext()){
            Log.d(TAG,"file size: " + cursor.getInt(sizeIndex));
            Log.d(TAG,"file name: " + cursor.getString(nameIndex));
        }
        cursor.close();
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

    /**
     * app详细设置
     */
    public static final Intent getSystemAppDetailSetting(Context context) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        Build.VERSION.SDK_INT >= 9
        localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return localIntent;
    }

    /**
     * 打开应用市场
     */
    public static Intent getMarket(Context context){
        Uri uri = Uri.parse("market://details?id="+context.getPackageName());
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return Intent.createChooser(intent,"请选择应用市场");
    }

    /**
     * 打开壁纸设定  并将wallPaperServiceClass类绘制的内容作为预览
     * @param context
     * @param wallPaperServiceClass
     * @return
     */
    public static  Intent getWallPaper(Context context,Class wallPaperServiceClass){
        Intent intent = new Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(context, wallPaperServiceClass));
        return  intent;
    }

    //打电话
    public static Intent dialPhone(String num){
        Intent intent = new Intent(Intent.ACTION_DIAL);
        Uri data = Uri.parse("tel:" + num);
        intent.setData(data);
        return intent;
    }

    //直接打电话，需要权限<uses-permission android:name="android.permission.CALL_PHONE" />
    //使用时动态请求  ActivityCompat.requestPermissions(this,new String []{Manifest.permission.CALL_PHONE},1);
    public static Intent directCall(String num){
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + num);
        intent.setData(data);
        return intent;
    }

    /**
     * 创建目录.
     */
    public static void initDirectory(String file) {
        //头像目录
        File target = new File(file);
        if (!target.exists()) {
            target.mkdir();
        }
    }
    /**
     * 创建文件.
     */
    public static void initFile(String file) {
        //头像目录
        File target = new File(file);
        if (!target.exists()) {

            try {
                target.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
