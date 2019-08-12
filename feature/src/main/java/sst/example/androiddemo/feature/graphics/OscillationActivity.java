package sst.example.androiddemo.feature.graphics;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.practice.OscillationView;

public class OscillationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oscillation);
        OscillationView view = findViewById(R.id.oscillation);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.startAllAnimation();
            }
        });
    }
}
