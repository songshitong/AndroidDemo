//todo https://muyangmin.github.io/glide-docs-cn/int/recyclerview.html

使用RecyclerViewPreloader进行预加载
```
    PreloadSizeProvider sizeProvider = 
        new FixedPreloadSizeProvider(imageWidthPixels, imageHeightPixels);
    PreloadModelProvider modelProvider = new MyPreloadModelProvider();
    RecyclerViewPreloader<Photo> preloader = 
        new RecyclerViewPreloader<>(
            Glide.with(this), modelProvider, sizeProvider, 10 /*maxPreload*/);

    RecyclerView myRecyclerView = (RecyclerView) result.findViewById(R.id.recycler_view);
    myRecyclerView.addOnScrollListener(preloader);
```