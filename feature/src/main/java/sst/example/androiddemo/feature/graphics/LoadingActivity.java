package sst.example.androiddemo.feature.graphics;

import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.practice.LoadingView;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        ViewGroup vg = (ViewGroup) getWindow().getDecorView().getRootView();
        View view = new LoadingView(this);
        view.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        vg.addView(view);
    }
}
