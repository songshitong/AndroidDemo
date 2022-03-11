package sst.example.androiddemo.feature.activity.scrollNested;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;

public class ScrollNestedActivity extends AppCompatActivity {
    public static final boolean handleNested=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_nested);
    }



}