package sst.example.androiddemo.feature.ffmpeg;

import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FFmpegCmd implements SurfaceHolder.Callback {
   private SurfaceHolder surfaceHolder;
//   static {
//      System.loadLibrary("native-lib");
//   }
   public static int run(String[] cmd){
      return run(cmd.length,cmd);
   }
   public  void startPlay(String path){
       if(null == path||0 == path.length()){
          Log.e("startPlay","path is null");
          return;
       }
//      nStartPlay(path,surfaceHolder.getSurface());
   }


   private static native int run(int cmdLen, String[] cmd);

   private static native void nStartPlay(String path, Surface surface);



   public void setSurfaceView(SurfaceView surfaceView){
      if(null != surfaceHolder){
         surfaceHolder.removeCallback(this);
      }
      this.surfaceHolder = surfaceView.getHolder();
      this.surfaceHolder.addCallback(this);
   }

   @Override
   public void surfaceCreated(SurfaceHolder holder) {
   }

   @Override
   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      this.surfaceHolder = holder;
   }

   @Override
   public void surfaceDestroyed(SurfaceHolder holder) {
      this.surfaceHolder.removeCallback(this);
   }
}
