https://www.jianshu.com/p/3a0d0ce4e649

AsyncDifferConfig用来配置线程池

优点
1 性能好
2 不用区分notifyItem或者notifyAll

在ListAdapter中使用,ListAdapter做了建议封装
```
 class UserAdapter extends ListAdapter<User, UserViewHolder> {
       public UserAdapter() {
           super(User.DIFF_CALLBACK);
       }
        @Override
       public void onBindViewHolder(UserViewHolder holder, int position) {
           holder.bindTo(getItem(position));
       }
       public static final DiffUtil.ItemCallback<User> DIFF_CALLBACK =
               new DiffUtil.ItemCallback<User>() {
            @Override
           public boolean areItemsTheSame(
                    @NonNull User oldUser,  @NonNull User newUser) {
               // User properties may have changed if reloaded from the DB, but ID is fixed
               return oldUser.getId() == newUser.getId();
           }
            @Override
           public boolean areContentsTheSame(
                    @NonNull User oldUser,  @NonNull User newUser) {
               // NOTE: if you use equals, your object must properly override Object#equals()
               // Incorrectly returning false here will result in too many animations.
               return oldUser.equals(newUser);
           }
       }
   }
```
使用 adapter中使用
```
   class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
       private final AsyncListDiffer<User> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);
        @Override
       public int getItemCount() {
           return mDiffer.getCurrentList().size();
       }
       public void submitList(List<User> list) {
           mDiffer.submitList(list);
       }
    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
           User user = mDiffer.getCurrentList().get(position);
           holder.bindTo(user);
       }
       public static final DiffUtil.ItemCallback<User> DIFF_CALLBACK
               = new DiffUtil.ItemCallback<User>() {
            @Override
           public boolean areItemsTheSame(
                    @NonNull User oldUser,  @NonNull User newUser) {
               // User properties may have changed if reloaded from the DB, but ID is fixed
               return oldUser.getId() == newUser.getId();
           }
            @Override
           public boolean areContentsTheSame(
                    @NonNull User oldUser,  @NonNull User newUser) {
               // NOTE: if you use equals, your object must properly override Object#equals()
               // Incorrectly returning false here will result in too many animations.
               return oldUser.equals(newUser);
           }
       }
   }
```

常见问题
1 更新itemObject的属性后不更新
更新itemObject的属性后，仍为同一个对象，equals判等返回true，此时不更新
赋值为新的对象copy，或者使用其他属性进行标记，例如单独的flag(需要进行标记和复位)等
```
override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as AudioRecordData
      if (isSliding != other.isSliding) return false
      ...各种属性判等
      return true
    }
```