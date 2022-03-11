package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import sst.example.androiddemo.feature.R;

public class RVCutsomLayoutManagerActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;

    List<Bean> mList = new ArrayList<>();
    RecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rvcutsom_layout_manager);
        initData();
        mRecyclerView = findViewById(R.id.Rv_Custom_LM);
        mLayoutManager = new CustomLayoutManger(1.5f, 0.85f);
        mAdapter = new RecyclerAdapter(mList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    private void initData() {
        for (int i = 0; i < 100; i++) {
            mList.add(new Bean("content = " + i, Color.parseColor(ColorUtils.generateRandomColor())));
        }
    }
}