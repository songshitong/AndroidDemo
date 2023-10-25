
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

https://juejin.cn/post/6844903901913677837  todo

https://www.jianshu.com/p/3a0d0ce4e649
Myers算法
DiffUtil进行差量计算采用的是著名的Myers算法
https://www.codeproject.com/Articles/42279/Investigating-Myers-diff-algorithm-Part-1-of-2
使用DiffTutorial
https://www.codeproject.com/script/Membership/LogOn.aspx?rp=%2fKB%2frecipes%2fDiffTutorial_2%2fDiffTutorial_bin.zip&download=true

DiffUtils代码实现主要集中在diffPartial方法里面。
diffPartial方法主要是来寻找一条snake，它的核心也就是Myers算法
calculateDiff方法是不断的调用diffPartial方法，然后将寻找到的snake放入一个数组里面，最后就是创建一个DiffResult对象，将所有的snake作为参数传递过去

在DiffResult类的内部，分别有两个数组来存储状态，分别是：mOldItemStatuses,用来的旧Item的状态；mNewItemStatuses,用来存储新Item的状态
recyclerview-1.2.1-sources.jar\androidx\recyclerview\widget\DiffUtil.java
```
 public static class DiffResult {
        //状态
        // item stayed the same.
        private static final int FLAG_NOT_CHANGED = 1;
        // item stayed in the same location but changed.
        private static final int FLAG_CHANGED = FLAG_NOT_CHANGED << 1;
        // Item has moved and also changed.
        private static final int FLAG_MOVED_CHANGED = FLAG_CHANGED << 1;
        // Item has moved but did not change.
        private static final int FLAG_MOVED_NOT_CHANGED = FLAG_MOVED_CHANGED << 1;
        // Item moved
        private static final int FLAG_MOVED = FLAG_MOVED_CHANGED | FLAG_MOVED_NOT_CHANGED;

        // since we are re-using the int arrays that were created in the Myers' step, we mask
        // change flags
        private static final int FLAG_OFFSET = 4;

        private static final int FLAG_MASK = (1 << FLAG_OFFSET) - 1;

        DiffResult(Callback callback, List<Diagonal> diagonals, int[] oldItemStatuses,
                int[] newItemStatuses, boolean detectMoves) {
            mDiagonals = diagonals;
            mOldItemStatuses = oldItemStatuses;
            mNewItemStatuses = newItemStatuses;
            Arrays.fill(mOldItemStatuses, 0);
            Arrays.fill(mNewItemStatuses, 0);
            mCallback = callback;
            mOldListSize = callback.getOldListSize();
            mNewListSize = callback.getNewListSize();
            mDetectMoves = detectMoves;
            addEdgeDiagonals();
            findMatchingItems(); //处理数据
        }
      
      private void findMatchingItems() {
            for (Diagonal diagonal : mDiagonals) {
                for (int offset = 0; offset < diagonal.size; offset++) {
                    int posX = diagonal.x + offset;
                    int posY = diagonal.y + offset;
                    final boolean theSame = mCallback.areContentsTheSame(posX, posY);
                    final int changeFlag = theSame ? FLAG_NOT_CHANGED : FLAG_CHANGED;
                    mOldItemStatuses[posX] = (posY << FLAG_OFFSET) | changeFlag;
                    mNewItemStatuses[posY] = (posX << FLAG_OFFSET) | changeFlag;
                }
            }
            //查找移动的
            if (mDetectMoves) {
                // traverse each addition / removal from the end of the list, find matching
                // addition removal from before
                findMoveMatches();
            }
        }
  
     
     private void findMoveMatches() {
            // for each removal, find matching addition
            int posX = 0;
            for (Diagonal diagonal : mDiagonals) {
                while (posX < diagonal.x) {
                    if (mOldItemStatuses[posX] == 0) {
                        // there is a removal, find matching addition from the rest
                        findMatchingAddition(posX);
                    }
                    posX++;
                }
                // snap back for the next diagonal
                posX = diagonal.endX();
            }
        }
        
      private void findMatchingAddition(int posX) {
            int posY = 0;
            final int diagonalsSize = mDiagonals.size();
            for (int i = 0; i < diagonalsSize; i++) {
                final Diagonal diagonal = mDiagonals.get(i);
                while (posY < diagonal.y) {
                    // found some additions, evaluate
                    if (mNewItemStatuses[posY] == 0) { // not evaluated yet
                        //是同一个item
                        boolean matching = mCallback.areItemsTheSame(posX, posY);
                        if (matching) {
                            // 判断item是否更新
                            boolean contentsMatching = mCallback.areContentsTheSame(posX, posY);
                            final int changeFlag = contentsMatching ? FLAG_MOVED_NOT_CHANGED
                                    : FLAG_MOVED_CHANGED;
                            // once we process one of these, it will mark the other one as ignored.
                            mOldItemStatuses[posX] = (posY << FLAG_OFFSET) | changeFlag;
                            mNewItemStatuses[posY] = (posX << FLAG_OFFSET) | changeFlag;
                            return;
                        }
                    }
                    posY++;
                }
                posY = diagonal.endY();
            }
        }   
}
```

更新到adapter
```
   public void dispatchUpdatesTo(@NonNull final RecyclerView.Adapter adapter) {
            //对adapter包装
            dispatchUpdatesTo(new AdapterListUpdateCallback(adapter));
        }
        
 
public void dispatchUpdatesTo(@NonNull ListUpdateCallback updateCallback) {
             //将多个操作合并进行回调
            final BatchingListUpdateCallback batchingCallback;

            if (updateCallback instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback) updateCallback;
            } else {
                batchingCallback = new BatchingListUpdateCallback(updateCallback);
                // replace updateCallback with a batching callback and override references to
                // updateCallback so that we don't call it directly by mistake
                //noinspection UnusedAssignment
                updateCallback = batchingCallback;
            }
            // track up to date current list size for moves
            // when a move is found, we record its position from the end of the list (which is
            // less likely to change since we iterate in reverse).
            // Later when we find the match of that move, we dispatch the update
            int currentListSize = mOldListSize;
            // list of postponed moves
            final Collection<PostponedUpdate> postponedUpdates = new ArrayDeque<>();
            // posX and posY are exclusive
            int posX = mOldListSize;
            int posY = mNewListSize;
            // iterate from end of the list to the beginning.
            // this just makes offsets easier since changes in the earlier indices has an effect
            // on the later indices.
            for (int diagonalIndex = mDiagonals.size() - 1; diagonalIndex >= 0; diagonalIndex--) {
                final Diagonal diagonal = mDiagonals.get(diagonalIndex);
                int endX = diagonal.endX();
                int endY = diagonal.endY();
                // dispatch removals and additions until we reach to that diagonal
                // first remove then add so that it can go into its place and we don't need
                // to offset values
                while (posX > endX) {
                    posX--;
                    // REMOVAL
                    int status = mOldItemStatuses[posX];
                    if ((status & FLAG_MOVED) != 0) {
                        int newPos = status >> FLAG_OFFSET;
                        // get postponed addition
                        PostponedUpdate postponedUpdate = getPostponedUpdate(postponedUpdates,
                                newPos, false);
                        if (postponedUpdate != null) {
                            // this is an addition that was postponed. Now dispatch it.
                            int updatedNewPos = currentListSize - postponedUpdate.currentPos;
                            batchingCallback.onMoved(posX, updatedNewPos - 1);
                            if ((status & FLAG_MOVED_CHANGED) != 0) {
                                Object changePayload = mCallback.getChangePayload(posX, newPos);
                                batchingCallback.onChanged(updatedNewPos - 1, 1, changePayload);
                            }
                        } else {
                            // first time we are seeing this, we'll see a matching addition
                            postponedUpdates.add(new PostponedUpdate(
                                    posX,
                                    currentListSize - posX - 1,
                                    true
                            ));
                        }
                    } else {
                        // simple removal
                        batchingCallback.onRemoved(posX, 1);
                        currentListSize--;
                    }
                }
                while (posY > endY) {
                    posY--;
                    // ADDITION
                    int status = mNewItemStatuses[posY];
                    if ((status & FLAG_MOVED) != 0) {
                        // this is a move not an addition.
                        // see if this is postponed
                        int oldPos = status >> FLAG_OFFSET;
                        // get postponed removal
                        PostponedUpdate postponedUpdate = getPostponedUpdate(postponedUpdates,
                                oldPos, true);
                        // empty size returns 0 for indexOf
                        if (postponedUpdate == null) {
                            // postpone it until we see the removal
                            postponedUpdates.add(new PostponedUpdate(
                                    posY,
                                    currentListSize - posX,
                                    false
                            ));
                        } else {
                            // oldPosFromEnd = foundListSize - posX
                            // we can find posX if we swap the list sizes
                            // posX = listSize - oldPosFromEnd
                            int updatedOldPos = currentListSize - postponedUpdate.currentPos - 1;
                            batchingCallback.onMoved(updatedOldPos, posX);
                            if ((status & FLAG_MOVED_CHANGED) != 0) {
                                Object changePayload = mCallback.getChangePayload(oldPos, posY);
                                batchingCallback.onChanged(posX, 1, changePayload);
                            }
                        }
                    } else {
                        // simple addition
                        batchingCallback.onInserted(posX, 1);
                        currentListSize++;
                    }
                }
                // now dispatch updates for the diagonal
                posX = diagonal.x;
                posY = diagonal.y;
                for (int i = 0; i < diagonal.size; i++) {
                    // dispatch changes
                    if ((mOldItemStatuses[posX] & FLAG_MASK) == FLAG_CHANGED) {
                        Object changePayload = mCallback.getChangePayload(posX, posY);
                        batchingCallback.onChanged(posX, 1, changePayload);
                    }
                    posX++;
                    posY++;
                }
                // snap back for the next diagonal
                posX = diagonal.x;
                posY = diagonal.y;
            }
            batchingCallback.dispatchLastEvent();
        }        
```