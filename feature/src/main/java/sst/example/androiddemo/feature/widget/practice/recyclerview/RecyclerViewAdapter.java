package sst.example.androiddemo.feature.widget.practice.recyclerview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import sst.example.androiddemo.feature.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ContentVH> {

    List<ItemBean> data;
    Context mContext;

    public RecyclerViewAdapter(List<ItemBean> itemBeans, Context context) {
        this.data = itemBeans;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ContentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.content_sticky, parent, false);
        return new ContentVH(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentVH holder, int position) {
        holder.actv.setText(data.get(position).text);
    }

    @Override
    public int getItemCount() {
        if (null != data) {
            return data.size();
        }
        return 0;
    }

    class ContentVH extends RecyclerView.ViewHolder {

        AppCompatTextView actv;

        public ContentVH(@NonNull View itemView) {
            super(itemView);
            actv = itemView.findViewById(R.id.content_text);
        }

    }
}


