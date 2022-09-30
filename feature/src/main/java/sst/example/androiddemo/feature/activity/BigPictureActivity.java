package sst.example.androiddemo.feature.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowMetrics;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;
import java.io.IOException;
import java.io.InputStream;

import sst.example.androiddemo.feature.R;

public class BigPictureActivity extends AppCompatActivity {
    private static final String TAG ="BigPictureActivity";

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_big_picture);
        ImageView imageView = findViewById(R.id.bigPictureSampleSize);
        InputStream is = null;
        try {
             is = getAssets().open("8k壁纸.jpeg");
             int width = getWindowManager().getCurrentWindowMetrics().getBounds().width();
             int height = 100*3;
             Log.d(TAG,"width "+width+" height "+height);
             Bitmap bitmap = decodeSampleFromBitmap(is,width,height);
             if(null != bitmap){
                 Log.d(TAG,"bitmap size: "+bitmap.getByteCount()/1000/1000+"M");
                 imageView.setImageBitmap(bitmap);
             }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(null !=is){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap decodeSampleFromBitmap(InputStream is,  int reqWidth, int reqHeight) throws IOException {
        //创建一个位图工厂的设置选项
        BitmapFactory.Options options = new BitmapFactory.Options();
        //设置该属性为true,解码时只能获取width、height、mimeType  可以用来优化获取图片大小的速度和空间
        options.inJustDecodeBounds = true;
        //解码  native层将解码获取的大小设置到Options  将图片大小写入option
        BitmapFactory.decodeStream(is, null, options);
        //计算采样比例        根据option计算采样
        int inSampleSize = options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.d(TAG,"inSampleSize "+inSampleSize);
        //设置该属性为false，实现真正解码
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        //重置流，再次解析
        is.reset();
        //解码
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
        return bitmap;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 原始大小
        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.d(TAG,"原图width "+width+" 原图height "+height);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            //采样时每次放大2,直到小于等于目标大小
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}