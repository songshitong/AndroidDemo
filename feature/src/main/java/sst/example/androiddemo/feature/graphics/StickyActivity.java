package sst.example.androiddemo.feature.graphics;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.widget.practice.recyclerview.ItemBean;
import sst.example.androiddemo.feature.widget.practice.recyclerview.RecyclerViewAdapter;
import sst.example.androiddemo.feature.widget.practice.recyclerview.StickyItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class StickyActivity extends AppCompatActivity {

    List<ItemBean> itemBeans = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticky);
        initDatas();
        RecyclerView recyclerView = findViewById(R.id.stickyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,false));
        recyclerView.setAdapter(new RecyclerViewAdapter(itemBeans,this));
        recyclerView.addItemDecoration(new StickyItemDecoration(itemBeans));
    }

    private void initDatas() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                itemBeans.add(new ItemBean(i,"group "+i+" item "+j));
            }
        }

    }

}
