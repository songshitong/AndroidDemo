
https://juejin.cn/post/6844904136937324552#heading-63
RecyclerView与ListView 对比：缓存机制
1. 层级不同：RecyclerView比ListView多两级缓存，支持多个离屏ItemView缓存，
支持开发者自定义缓存处理逻辑，支持所有RecyclerView共用同一个RecyclerViewPool(缓存池)。

具体来说：
ListView(两级缓存)：
ListView  是否需要回调createView   是否需要回调bindView   生命周期          备注
mActiveView     否                   否              onLayout函数周期内  用于屏幕内itemView快速重用
mScrapViews     否                   是              与mAdapter一致，当
                                                    mAdapter被更换时，
                                                     mScrapViews即被清空


RecyclerView(四级缓存)：
RecyclerView      是否需要回调createView   是否需要回调bindView   生命周期          备注
mAttachedScrap         否                   否              onLayout函数周期内  用于屏幕内itemView快速重用
mCacheViews            否                   是              与mAdapter一致，当mAdapter被更换时mCacheViews    
                                                           即被缓存至mRecyclerPool  默认上限为2，即缓存屏幕外2个itemView
mViewCacheExtension                                        不直接使用，需要用户再定制，默认不实现
mRecyclerPool          否                   是              与自身生命周期一致，不再被引用时即被释放
                                                            默认上限为5,技术上可以实现所有RecyclerViewPool公用一个

ListView和RecyclerView缓存机制基本一致：
1). mActiveViews和mAttachedScrap功能相似，意义在于快速重用屏幕上可见的列表项ItemView，而不需要重新createView和bindView；
2). mScrapView和mCachedViews + mReyclerViewPool功能相似，意义在于缓存离开屏幕的ItemView，目的是让即将进入屏幕的ItemView重用.
3). RecyclerView的优势在于
  a.mCacheViews的使用，可以做到屏幕外的列表项ItemView进入屏幕内时也无须bindView快速重用；
  b.mRecyclerPool可以供多个RecyclerView共同使用，在特定场景下，如viewpager+多个列表页下有优势.客观来说，
      RecyclerView在特定场景下对ListView的缓存机制做了补强和完善。

2. 缓存不同：
   1). RecyclerView缓存RecyclerView.ViewHolder，抽象可理解为：
     View + ViewHolder(避免每次createView时调用findViewById) + flag(标识状态)；
   2). ListView缓存View。

