package sst.example.androiddemo.feature.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * The type Bitmap utils.
 */
public class BitmapUtils {
    public static final String TAG ="BitmapUtils";
    /**
     * Compress image. 将bitmap压缩到文件
     *
     * @param bitmap the bitmap
     * @param file   the file保存地址
     */
    public static void compressImage(Bitmap bitmap, File file) {
        //字节数组输出流
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);//将压缩的bitmap放到stream中
        while (stream.toByteArray().length / 1024 > 300) { //循环判断如果压缩后图片是否大于300kb,大于继续压缩
            stream.reset();//重置baos即清空baos
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, stream);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            //不断把stream的数据写文件输出流中去
            fileOutputStream.write(stream.toByteArray());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.w(TAG,"file error "+e.toString());
        }

    }

    /**
     * Read uri 2 file. 读取uri到文件
     */
    public static void readImgUri2File(Uri uri, String file){
        try {

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            //不断把stream的数据写文件输出流中去
            //todo  去除第三方包影响
//            fileOutputStream.write(FileIOUtils.readFile2BytesByStream(UriUtils.uri2File(uri)));
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.w(TAG, "file error "+e.toString());
        }
    }

    /**
     * Gets bitmap from uri. 从uri中读取bitmap
     *
     * @param uri the uri
     * @return the bitmap from uri
     * @throws FileNotFoundException the file not found exception
     */
    public static Bitmap getBitmapFromUri(Context context,Uri uri) throws FileNotFoundException {
        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
    }

    /**
     * 从路径读取图片
     * @param filePath
     * @return
     * @throws FileNotFoundException
     */
    public static Bitmap getBitmapFromFile(String filePath) throws FileNotFoundException {
        return BitmapFactory.decodeFile(filePath);
    }

    /**
    * @Description: base64 转为bitmap
    */
    public static @Nullable Bitmap base64ToBitmap(String base64Str){
        if(TextUtils.isEmpty(base64Str)){
            return null;
        }
        byte[] decodedString = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

}
