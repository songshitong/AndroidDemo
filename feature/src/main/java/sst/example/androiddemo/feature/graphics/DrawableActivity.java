package sst.example.androiddemo.feature.graphics;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.DrawableView;

public class DrawableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawable);
        final DrawableView dv = findViewById(R.id.drawable_view);
        dv.setClickable(true);
        dv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("DrawableView","click ======= ");
                dv.setSelected(!dv.isSelected());

            }
        });
    }
}
