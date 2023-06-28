package sst.example.androiddemo.feature;

import android.app.Application;
import android.content.Context;
import androidx.annotation.Nullable;

public class MApplication extends Application {
    int i;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
