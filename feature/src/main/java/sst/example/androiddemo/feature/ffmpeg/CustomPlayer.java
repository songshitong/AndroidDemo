package sst.example.androiddemo.feature.ffmpeg;

import android.view.Surface;

public class CustomPlayer {

    //
    private native void native_prepare(String dataSource);
    private native void native_start();

    //设置播放的surface
    private native void native_set_surface(Surface surface);


}
