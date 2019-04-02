package sst.example.androiddemo.feature.graphics;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.PictureView;

public class PictureDrawableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_drawable);
        final PictureView pictureView = findViewById(R.id.pictureView);
        pictureView.postDelayed(new Runnable() {
            @Override
            public void run() {
                pictureView.setShowContent(true);
            }
        }, 5000);
    }
}
