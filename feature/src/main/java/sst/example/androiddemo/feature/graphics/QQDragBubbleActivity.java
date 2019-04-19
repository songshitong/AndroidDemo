package sst.example.androiddemo.feature.graphics;

import android.view.ViewGroup;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.practice.QQDragBubbleView;

public class QQDragBubbleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qqdrag_bubble);
        ViewGroup vg = (ViewGroup) getWindow().getDecorView().getRootView();
        QQDragBubbleView qqdbView = new QQDragBubbleView(this);
        qqdbView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        vg.addView(qqdbView);
    }
}
