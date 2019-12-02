package sst.example.androiddemo.feature.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import sst.example.androiddemo.feature.R;

//定义DialogActivity

//xmlmanifest 设置dialog主题
public class DialogActivity extends AppCompatActivity {
    private static final String TAG = "DialogActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        Log.d(TAG," onCreate ==== ");
    }


}
