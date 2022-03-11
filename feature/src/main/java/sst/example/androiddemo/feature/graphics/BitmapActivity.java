package sst.example.androiddemo.feature.graphics;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.MatrixView;

public class BitmapActivity extends AppCompatActivity {
    private static final String TAG = BitmapActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap);
        //BitmapFactory.decodeResource
        Bitmap bitmap = getBitmap(this, R.mipmap.ic_launcher_round);
        //Bitmap.crate生成一个空白bitmap
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        //裁剪
        Bitmap widthBt = Bitmap.createBitmap(bitmap, 0, 0, 100, 100);
        //缩放
        Bitmap scaleBt = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, false);

        Matrix matrix = new Matrix();
        matrix.setRotate(180);
        //操作，旋转,缩放，移动，
        Bitmap matrixBt = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        ImageView iv = findViewById(R.id.bitmap1);
        //设置某个像素的颜色
        scaleBt.setPixel(10, 10, Color.BLUE);
//        压缩
//        scaleBt.compress(Bitmap.CompressFormat.JPEG,10,null);

        iv.setImageBitmap(scaleBt);
        //获取bitmap的alpha图
        Bitmap extracBt = bitmap.extractAlpha();
        ImageView iv_extracBt = findViewById(R.id.bitmap2);
        iv_extracBt.setImageBitmap(extracBt);

//        //巨图加载
//        BitmapRegionDecoder brd = BitmapRegionDecoder.newInstance(false);
//        brd.decodeRegion(new Rect(),BitmapFactory.Options);


        final MatrixView mv = findViewById(R.id.matrix);
        final SeekBar seekBar_mv = findViewById(R.id.seekbar_matrix);
        seekBar_mv.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i(TAG, progress + "");
                mv.setSaturation((float) (((float) progress) / 100));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public static Bitmap getBitmap(Context context, int vectorDrawableId) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        } else {
            //该方法不能将vector的xml解析为bitmap   api21以上
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }

        return bitmap;
    }

    /**
     * 把view生成bitmap
     *
     * @param v
     * @return
     */
    public static Bitmap createBitmapFormView(View v) {
        Bitmap result = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(result);
        v.draw(canvas);
        return result;
    }


    public byte[] getBitmapByte(Bitmap bitmap) {   //将bitmap转化为byte[]类型也就是转化为二进制
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }


    public Bitmap getBitmapFromByte(byte[] temp) {   //将二进制转化为bitmap
        if (temp != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(temp, 0, temp.length);
            return bitmap;
        } else {
            return null;
        }
    }


    //    Java代码  drawable转为bitmap
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    //todo bitmap 写入文件


   @RequiresApi(api = Build.VERSION_CODES.O)
   private void getBitMapOption(){
        //它是每个像素储存一个透明度值，每个像素是占1个字节（8位），也就是后面8的由来。其次，它不储存任何颜色信息，
       // 只单纯做透明度的处理  一般遮盖的效果
     Bitmap.Config a = Bitmap.Config.ALPHA_8;
     //a,r,g,b 每个通道4位 一种16位即2字节  ARGB_4444和RGB_565
     Bitmap.Config b = Bitmap.Config.ARGB_4444;
     //r 5位，g 6位 b 5位  一种16位=2字节  缺点不支持透明度
     Bitmap.Config c = Bitmap.Config.RGB_565;
     ///32位=4字节  可以显示的颜色值=256*256*256=16777216  2的8次方=256
     ///通常也被简称为1600万色或千万色。也称为24位色(2 的24次方)
     Bitmap.Config d = Bitmap.Config.ARGB_8888;
     //它每个像素占用8个字节 但是它是以半浮点数存储的 f代表float
       // 这个属性非常适合用于广色域宽屏和HDR(高动态范围的图片)
     Bitmap.Config e = Bitmap.Config.RGBA_F16;
     //硬件位图 Android8.0后增加的存储在GPU的图片格式
     //优点
     //  因为硬件位图仅储存像素数据的一份副本。一般情况下，应用内存中有一份像素数据（即像素字节数组），而在显存中还有一份副本（在像素被上传到 GPU之后）。
       //  而硬件位图仅持有 GPU 中的副本，因此：
     //  硬件位图仅需要一半于其他位图配置的内存；
      // 硬件位图可避免绘制时上传纹理导致的内存抖动。
     ///缺点
      // 在显存中存储像素数据意味着这些数据不容易访问到,读取,变更需要使用PixelCopy进行复制
     //https://muyangmin.github.io/glide-docs-cn/
     Bitmap.Config f = Bitmap.Config.HARDWARE;


     //比如1920*1080像素的一张图，以ARGB_8888的方式来存储颜色值的话。
       // 那它的大小就是1920*1080*4(字节) = 8294400（bytes） =  7.91兆（M）大约值
   }
}