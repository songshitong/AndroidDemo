package sst.example.androiddemo.feature.activity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.PrecomputedText;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pools;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
