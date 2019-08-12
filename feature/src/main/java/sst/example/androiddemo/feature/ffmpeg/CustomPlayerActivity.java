package sst.example.androiddemo.feature.ffmpeg;

import android.Manifest;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import sst.example.androiddemo.feature.R;

public class CustomPlayerActivity extends AppCompatActivity {
    int progress;
    SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_player);
        //保持屏幕常亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SurfaceView surfaceView = findViewById(R.id.custom_player_surfaceView);
        seekBar = findViewById(R.id.custom_player_seekbar);
        checkPermission();
    }

    //检查读内存权限
    private void checkPermission() {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission_group.STORAGE
        },0);
    }
}
