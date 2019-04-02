package sst.example.androiddemo.feature.graphics;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.MatrixView;

public class BitmapActivity extends AppCompatActivity {
    private static final String TAG = BitmapActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap);
        //BitmapFactory.decodeResource
        Bitmap bitmap = getBitmap(this,R.mipmap.ic_launcher_round);
        //Bitmap.crate生成一个空白bitmap
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        //裁剪
        Bitmap widthBt = Bitmap.createBitmap(bitmap,0,0,100,100);
        //缩放
        Bitmap scaleBt = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth()/2,bitmap.getHeight()/2,false);

        Matrix matrix = new Matrix();
        matrix.setRotate(180);
        //操作，旋转,缩放，移动，
        Bitmap matrixBt = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,false);
        ImageView iv = findViewById(R.id.bitmap1);

        //设置某个像素的颜色
        scaleBt.setPixel(10,10,Color.BLUE);
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
                Log.i(TAG,progress+"");
                mv.setSaturation((float) (((float) progress)/100));
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
        Bitmap bitmap=null;
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP){
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        }else {
            //该方法不能将vector的xml解析为bitmap   api21以上
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }

        return bitmap;
    }

    /**
     * 把view生成bitmap
     * @param v
     * @return
     */
    public static Bitmap createBitmapFormView(View v){
          Bitmap result = Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.RGB_565);
          Canvas canvas = new Canvas(result);
          v.draw(canvas);
          return result;
    }
}
