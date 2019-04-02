package sst.example.androiddemo.feature.graphics;

import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.practice.ParticleView;

public class ParticleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle);
        ((ViewGroup)getWindow().getDecorView().getRootView()).addView(new ParticleView(this));
    }
}
