
https://juejin.cn/post/6844903630538031112
ItemDecoration  为每个item加文字
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

2.2 模式的运用
RecyclerView 本身是不支持添加底部和头部的，那么采用装饰设计模式可以对其进行功能扩展，使其能够支持底部和头部的添加：
```
可以添加头部底部的 WrapRecyclerAdapter
class WrapRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // 包装 adapter 是原来的 RecyclerView.Adapter 是并不支持添加头部和底部的
    private RecyclerView.Adapter mRealAdapter;
    ArrayList<View> mHeaderViews; // 头部
    ArrayList<View> mFooterViews; // 底部
    
    public WrapRecyclerAdapter(RecyclerView.Adapter adapter) {
        mRealAdapter = adapter;
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
        return mRealAdapter.getItemCount() + mHeaderViews.size() + mFooterViews.size();
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
        super.setAdapter(mWrapAdapter);
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