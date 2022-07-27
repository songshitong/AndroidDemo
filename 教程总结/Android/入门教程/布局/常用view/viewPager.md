
监听page变换
ViewPager.registerOnPageChangeCallback
```
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        //位置，百分比，位移像素
        Log.i(TAG,"position "+position+" positionOffset "+positionOffset+" positionOffsetPixels "+positionOffsetPixels);
      }
```

https://www.jianshu.com/p/25aa5cacbfb9?u_atoken=3e023bde-7740-4694-86f4-49d3dbf005b5&u_asession=01HaK5plD18AzF5oHaPHmw3mRvOnrNWIpSuZ4WKq4YJxTjcdb9_6UjRjPZPJOVxZvVX0KNBwm7Lovlpxjd_P_q4JsKWYrT3W_NKPr8w6oU7K9SMLtRxhgzj19vDa21lwFwPpcarp92QKzyJKyYjREPlmBkFo3NEHBv0PZUm6pbxQU&u_asig=05QQGENPHj3y0qRSj9RI0XflFR4SOOHvhJDt6TS02YFbxsRpDUwGY-vFw0dPAu0GKktltuYcNdkYcdfHTTX5h4K10tzPYx9Y26G4RG2KS4hYmnYfVzsew-F4zj2nPFyfAbKnEzA4hunyMsX-KexHp5UfSd3VtFUiW_vUIqOR9EgHf9JS7q8ZD7Xtz2Ly-b0kmuyAKRFSVJkkdwVUnyHAIJzUCD6yD0bRLlqbRsLQ1G3yIe3TBf03doKt7dxpLDIfZ6WPRPQyB_SKrj-61LB_f61u3h9VXwMyh6PgyDIVSG1W_nWg-Dil4Gai6hqAfDRN4toKwYsY3UV1TNWqkdmJ-OLjWznleWNev4MAG5BwCCjuCQyep4YT3tqAK-SUu5DIpfmWspDxyAEEo4kbsryBKb9Q&u_aref=GlY5uPGUkLZouM08872ICt01s1E%3D
ViewPager2+view
```
viewPager2 = findViewById(R.id.viewpager2);
viewPager2.setAdapter(new ViewPagerAdapter());

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.CardViewHolder> {
    ...

    @NonNull
    @Override
    public ViewPagerAdapter.CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPagerAdapter.CardViewHolder holder, int position) {
        holder.textView.setText(mDatas.get(position));
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_content);
        }
    }
}
```
viewpager2+fragment
```
viewPager.setAdapter(new ViewPagerFragmentStateAdapter(),colors);

public class ViewPagerFragmentStateAdapter extends FragmentStateAdapter {
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return PageFragment.newInstance(colors, position);
    }
    @Override
    public int getItemCount() {
        return colors.size();
    }
}
```