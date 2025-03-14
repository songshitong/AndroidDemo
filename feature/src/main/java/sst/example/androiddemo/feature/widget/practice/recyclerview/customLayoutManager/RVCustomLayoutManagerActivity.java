package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;

import android.graphics.Bitmap;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

public class RVCustomLayoutManagerActivity extends AppCompatActivity {
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
        Button saveToGalleryBtn = findViewById(R.id.saveToGalleryBtn);
        saveToGalleryBtn.setOnClickListener(v->{
          Bitmap bitmap = BitmapActivity.shotRecyclerView(mRecyclerView);
          BitmapActivity.saveImageToGallery(getBaseContext(),bitmap);
        });
    }

    private void initData() {
        for (int i = 0; i < 20; i++) {
            mList.add(new Bean("content = " + i, Color.parseColor(ColorUtils.generateRandomColor())));
        }
    }
}