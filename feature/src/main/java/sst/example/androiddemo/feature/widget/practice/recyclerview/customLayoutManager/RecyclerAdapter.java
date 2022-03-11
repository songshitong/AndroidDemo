package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import sst.example.androiddemo.feature.R;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder> {


    private List<Bean> mList;

    public RecyclerAdapter(List<Bean> list) {
        mList = list;
    }


    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_custom_layout_manager, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        holder.itemView.setBackgroundColor(mList.get(position).getColor());
        holder.mTextView.setText(mList.get(position).getContent());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.rvCustomLmTextView);
        }
    }
}
