https://juejin.cn/post/6844903630538031112
ItemDecoration 为每个item加文字

```
for (int i = 0; i < parent.getChildCount(); i++) {
    View view = parent.getChildAt(i);
    int position = parent.getChildAdapterPosition(view);
    String text = String.valueOf(position+1);
    float w = textPaint.measureText(text);
    c.drawText(text, 20 - w / 2, view.getBottom() - view.getHeight() / 2 + textPaint.getFontMetri().descent, textPaint);
}

```

https://www.jianshu.com/p/eac9b55023be
装饰设计模式 - RecyclerView添加头部和底部

装饰设计模式也称包装设计模式，使用一种透明的方式来动态的扩展对象的功能，也是继承关系的的一种替代方案之一。说个大白话就是，
在不使用的继承的方式下，采用装饰设计模式可以扩展一个对象的功能，可以使一个对象变得越来越强大。

2.2 模式的运用 RecyclerView 本身是不支持添加底部和头部的，那么采用装饰设计模式可以对其进行功能扩展，使其能够支持底部和头部的添加：

//todo 有footer，清空数据，再添加数据  展示异常，footer不展示，数据有重复
建议使用https://github.com/XRecyclerView/
```
可以添加头部底部的 WrapRecyclerAdapter
class WrapRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // 包装 adapter 是原来的 RecyclerView.Adapter 是并不支持添加头部和底部的
    private RecyclerView.Adapter mRealAdapter;
    ArrayList<View> mHeaderViews; // 头部
    ArrayList<View> mFooterViews; // 底部
    
    public WrapRecyclerAdapter(RecyclerView.Adapter adapter) {
        this.mRealAdapter = adapter;
        mHeaderViews = new ArrayList<>();
        mFooterViews = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        // Header (negative positions will throw an IndexOutOfBoundsException)
        int numHeaders = getHeadersCount();

        if (position < numHeaders) {
            return createFooterHeaderViewHolder(mHeaderViews.get(position));
        }

        // Adapter
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mRealAdapter != null) {
            adapterCount = mRealAdapter.getItemCount();
            if (adjPosition < adapterCount) {
                return mRealAdapter.onCreateViewHolder(parent, mRealAdapter.getItemViewType(adjPosition));
            }
        }

        // Footer (off-limits positions will throw an IndexOutOfBoundsException)
        return createFooterHeaderViewHolder(mFooterViews.get(adjPosition - adapterCount));
    }

    public int getHeadersCount() {
        return mHeaderViews.size();
    }

    private RecyclerView.ViewHolder createFooterHeaderViewHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Header (negative positions will throw an IndexOutOfBoundsException)
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return;
        }
        // Adapter
        final int adjPosition = position - numHeaders;
        if (mRealAdapter != null) {
            int adapterCount = mRealAdapter.getItemCount();
            if (adjPosition < adapterCount) {
                mRealAdapter.onBindViewHolder(holder, adjPosition);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    /**
     * 添加底部View
     * @param view
     */
    public void addFooterView(View view) {
        if (!mFooterViews.contains(view)) {
            mFooterViews.add(view);
            notifyDataSetChanged();
        }
    }

    /**
     * 添加头部View
     * @param view
     */
    public void addHeaderView(View view) {
        if (!mHeaderViews.contains(view)) {
            mHeaderViews.add(view);
            notifyDataSetChanged();
        }
    }

    /**
     * 移除底部View
     * @param view
     */
    public void removeFooterView(View view) {
        if (mFooterViews.contains(view)) {
            mFooterViews.remove(view);
            notifyDataSetChanged();
        }
    }

    /**
     * 移除头部View
     * @param view
     */
    public void removeHeaderView(View view) {
        if (mHeaderViews.contains(view)) {
            mHeaderViews.remove(view);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
      if (0 == mRealAdapter.getItemCount()) {
        return mHeaderViews.size();
      } else {
        //todo 真实adapter只有一个数据时，显示异常，没有显示fotter  只设置fotter的情况
        return mRealAdapter.getItemCount() + mHeaderViews.size() + mFooterViews.size();
      }
    }

}
```

```
支持添加底部和头部的 RecyclerView
public class WrapRecyclerView extends RecyclerView{
    // 支持添加头部和底部的 RecyclerView.Adapter
    private WrapRecyclerAdapter mWrapAdapter;

    public WrapRecyclerView(Context context) {
        super(context);
    }

    public WrapRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        // 这里做一个替换
        mWrapAdapter = new WrapRecyclerAdapter(adapter);
        registerListener(adapter);
        super.setAdapter(mWrapAdapter);
    }
    
    //监听真实adapter的数据改变
    private void registerListener(Adapter adapter){
       mRealAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
      @Override public void onChanged() {
        super.onChanged();
        if(null != mWrapAdapter){
          mWrapAdapter.notifyDataSetChanged();
        }
      }

      @Override public void onItemRangeChanged(int positionStart, int itemCount) {
        super.onItemRangeChanged(positionStart, itemCount);
        if(null != mWrapAdapter){
          mWrapAdapter.notifyItemRangeChanged(positionStart,itemCount);
        }
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
        super.onItemRangeChanged(positionStart, itemCount, payload);
        if(null != mWrapAdapter){
          mWrapAdapter.notifyItemRangeChanged(positionStart,itemCount);
        }
      }

      @Override public void onItemRangeInserted(int positionStart, int itemCount) {
        super.onItemRangeInserted(positionStart, itemCount);
        if(null != mWrapAdapter){
          mWrapAdapter.notifyItemRangeInserted(positionStart,itemCount);
        }
      }

      @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
        super.onItemRangeRemoved(positionStart, itemCount);
        if(null != mWrapAdapter){
          mWrapAdapter.notifyItemRangeRemoved(positionStart,itemCount);
        }
      }

      @Override public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount);
        if(null != mWrapAdapter){
          mWrapAdapter.notifyItemMoved(fromPosition,toPosition);
        }
      }
    });
    
    }

    /**
     * 添加头部View
     * @param view
     */
    public void addHeaderView(View view){
        if(mWrapAdapter != null){
            mWrapAdapter.addHeaderView(view);
        }
    }

    /**
     * 添加底部View
     * @param view
     */
    public void addFooterView(View view){
        if(mWrapAdapter != null){
            mWrapAdapter.addFooterView(view);
        }
    }

    /**
     * 移除头部View
     * @param view
     */
    public void removeHeaderView(View view){
        if(mWrapAdapter != null){
            mWrapAdapter.removeHeaderView(view);
        }
    }

    /**
     * 移除底部View
     * @param view
     */
    public void removeFooterView(View view){
        if(mWrapAdapter != null){
            mWrapAdapter.removeFooterView(view);
        }
    }
    
    //https://github.com/XRecyclerView/XRecyclerView/blob/master/xrecyclerview/src/main/java/com/jcodecraeer/xrecyclerview/XRecyclerView.java
    //observer逻辑分发给实际的adapter，方式dataNotify不生效
     @Override
    public void registerAdapterDataObserver(AdapterDataObserver observer) {
        adapter.registerAdapterDataObserver(observer);
    }
     @Override
    public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
      if(null != mRealAdapter){
        mRealAdapter.registerAdapterDataObserver(observer);
      }
    }
    @Override
    public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
      if(null != mRealAdapter){
        mRealAdapter.unregisterAdapterDataObserver(observer);
      }
    }
    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
      if(null != mRealAdapter){
        mRealAdapter.onViewDetachedFromWindow(holder);
      }
    }
    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
      if(null != mRealAdapter){
        mRealAdapter.onViewRecycled(holder);
      }
    }
    @Override
    public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
      if(null != mRealAdapter){
        return mRealAdapter.onFailedToRecycleView(holder);
      }else{
        return  super.onFailedToRecycleView(holder);
      }
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
      super.onAttachedToRecyclerView(recyclerView);
      if(null != mRealAdapter){
        mRealAdapter.onAttachedToRecyclerView(recyclerView);
      }
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
      if(null != mRealAdapter){
        mRealAdapter.onDetachedFromRecyclerView(recyclerView);
      }
    }
    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
      super.onViewAttachedToWindow(holder);
      if(null != mRealAdapter){
        mRealAdapter.onViewAttachedToWindow(holder);
      }
    }
   
}
```

使用

```
// 实例化头部View
View headerView = LayoutInflater.from(this).inflate(R.layout.layout_rc_header, mRecyclerView, false);
// 设置适配器
mRecyclerView.setAdapter(new RecyclerViewAdapter());
// 添加头部
mRecyclerView.addHeaderView(headerView);
```