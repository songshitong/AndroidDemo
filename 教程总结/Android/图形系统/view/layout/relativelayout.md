
可以参考https://juejin.cn/post/6844904065588002829


AndroidSdk\platforms\android-31\android.jar

设置gravity和忽略
android:gravity 设置子view的偏重
android:ignoreGravity 设置哪个view忽略，从初始位置进行布局
```
 <RelativeLayout
  android:gravity="center"
  android:ignoreGravity="@id/img_back">
```



RelativeLayout.LayoutParams 存储位置关系
```
public static final int LEFT_OF                  = 0;
public static final int ALIGN_PARENT_END         = 21;
private static final int VERB_COUNT              = 22;
private int[] mRules = new int[VERB_COUNT]; //存储依赖关系 一共有22种关系 从LEFT_OF到ALIGN_PARENT_END
 //其中0代表该种依赖关系不存在 !=0为存在 -1为默认或动态添加的，>0或<-1一般是xml的依赖的view.id
```

位置排序
```
 static class Node {
            View view; //一个节点对应一个view
            //被依赖的
            final ArrayMap<Node, DependencyGraph> dependents =
                    new ArrayMap<Node, DependencyGraph>();
            //依赖项
            final SparseArray<Node> dependencies = new SparseArray<Node>();
 }
 
 //依赖图
  private static class DependencyGraph {
        //存储所有的node
        private ArrayList<Node> mNodes = new ArrayList<Node>();
        //view.id 与node的对应关系
        private SparseArray<Node> mKeyNodes = new SparseArray<Node>();
        //root node
        private ArrayDeque<Node> mRoots = new ArrayDeque<Node>();
        
       
        void add(View view) {
            final int id = view.getId();
            //将view转为node
            final Node node = Node.acquire(view);
            //分别添加到mKeyNodes，mNodes
            if (id != View.NO_ID) {
                mKeyNodes.put(id, node);
            }
            mNodes.add(node);
        } 
        
        
        //寻找根节点 这个节点没有依赖项  可能被0-n个节点依赖
         private ArrayDeque<Node> findRoots(int[] rulesFilter) {
            final SparseArray<Node> keyNodes = mKeyNodes;
            final ArrayList<Node> nodes = mNodes;
            final int count = nodes.size();

            //先清除旧的依赖关系
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);
                node.dependents.clear();
                node.dependencies.clear();
            }

            //开始建立关系
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);

                final LayoutParams layoutParams = (LayoutParams) node.view.getLayoutParams();
                final int[] rules = layoutParams.mRules;
                final int rulesCount = rulesFilter.length;

                for (int j = 0; j < rulesCount; j++) {
                    final int rule = rules[rulesFilter[j]];
                    //查找到存在的rule
                    if (rule > 0 || ResourceId.isValid(rule)) {
                        //rule存在时指向依赖的view.id，根据id获取存储的node
                        final Node dependency = keyNodes.get(rule);
                        if (dependency == null || dependency == node) {
                            continue;
                        }
                        //标识被哪个node依赖了
                        dependency.dependents.put(node, this);
                        //标识依赖了哪个node
                        node.dependencies.put(rule, dependency);
                    }
                }
            }

            final ArrayDeque<Node> roots = mRoots;
            roots.clear();

            //查找没有依赖项的node
            for (int i = 0; i < count; i++) {
                final Node node = nodes.get(i);
                if (node.dependencies.size() == 0) roots.addLast(node);
            }
            return roots;
        }
        
        
        //根据rules进行排序   B -> A -> C  c依赖于A，A依赖于B  B先处理
        void getSortedViews(View[] sorted, int... rules) {
            //找到所有没有rule依赖项的node
            final ArrayDeque<Node> roots = findRoots(rules);
            int index = 0;

            Node node;
            //从队尾开始遍历
            while ((node = roots.pollLast()) != null) {
                final View view = node.view;
                final int key = view.getId();
                //存储顺序
                sorted[index++] = view;
                final ArrayMap<Node, DependencyGraph> dependents = node.dependents;
                final int count = dependents.size();
                for (int i = 0; i < count; i++) {
                    //拿到依赖当前node的节点
                    final Node dependent = dependents.keyAt(i);
                    final SparseArray<Node> dependencies = dependent.dependencies;
                    //移除当前的关系   
                    dependencies.remove(key);
                    if (dependencies.size() == 0) {
                        //当前节点没有依赖了，变为root
                        roots.add(dependent);
                    }
                }
            }
            ...
        }
 }
```


onMeasure   
```
  private View[] mSortedHorizontalChildren; //存储排序完的node
  private View[] mSortedVerticalChildren;
    
 private void sortChildren() {
        final int count = getChildCount();
        ...
        final DependencyGraph graph = mGraph;
        graph.clear();

        //将view转为node，添加进graph 
        for (int i = 0; i < count; i++) {
            graph.add(getChildAt(i));
        }
        //按照规则进行排序
        graph.getSortedViews(mSortedVerticalChildren, RULES_VERTICAL);
        graph.getSortedViews(mSortedHorizontalChildren, RULES_HORIZONTAL);
    }
    
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDirtyHierarchy) {
            mDirtyHierarchy = false; 
            //执行完requesLayout后需要排序
            sortChildren();
        }
       ...
        for (int i = 0; i < count; i++) {
            View child = views[i];
            if (child.getVisibility() != GONE) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int[] rules = params.getRules(layoutDirection);
                //根据rule调整child的left right 
                applyHorizontalSizeRules(params, myWidth, rules);
                //执行child.measure
                measureChildHorizontal(child, params, myWidth, myHeight);
                //根据child的宽度调整left right
                if (positionChildHorizontal(child, params, myWidth, isWrapContentWidth)) {
                    offsetHorizontalAxis = true;
                }
            }
        }  
        ...
          for (int i = 0; i < count; i++) {
            final View child = views[i];
            if (child.getVisibility() != GONE) {
                final LayoutParams params = (LayoutParams) child.getLayoutParams();
                //根据rule确定child的top,bottom
                applyVerticalSizeRules(params, myHeight, child.getBaseline());
                //执行child.measure
                measureChild(child, params, myWidth, myHeight);
                //根据child的height重新确定top,bottom
                if (positionChildVertical(child, params, myHeight, isWrapContentHeight)) {
                    offsetVerticalAxis = true;
                }
                //wrapContent情况下的取最大的宽度   
                if (isWrapContentWidth) {
                    if (isLayoutRtl()) {
                      ...
                    } else {
                        if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                            width = Math.max(width, params.mRight);
                        } else {
                            width = Math.max(width, params.mRight + params.rightMargin);
                        }
                    }
                }
                //wrapHeight的情况下 取最大高度
                if (isWrapContentHeight) {
                    if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
                        height = Math.max(height, params.mBottom);
                    } else {
                        height = Math.max(height, params.mBottom + params.bottomMargin);
                    }
                }

                if (child != ignore || verticalGravity) {
                    left = Math.min(left, params.mLeft - params.leftMargin);
                    top = Math.min(top, params.mTop - params.topMargin);
                }

                if (child != ignore || horizontalGravity) {
                    right = Math.max(right, params.mRight + params.rightMargin);
                    bottom = Math.max(bottom, params.mBottom + params.bottomMargin);
                }
            }
        }
        ...
         if (isWrapContentWidth) {
            width += mPaddingRight;

            if (mLayoutParams != null && mLayoutParams.width >= 0) {
                width = Math.max(width, mLayoutParams.width);
            }

            width = Math.max(width, getSuggestedMinimumWidth());
            width = resolveSize(width, widthMeasureSpec);
            //end与parent对齐的情况
            if (offsetHorizontalAxis) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        final int[] rules = params.getRules(layoutDirection);
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                            centerHorizontal(child, params, width);
                        } else if (rules[ALIGN_PARENT_RIGHT] != 0) {
                            final int childWidth = child.getMeasuredWidth();
                            params.mLeft = width - mPaddingRight - childWidth;
                            params.mRight = params.mLeft + childWidth;
                        }
                    }
                }
            }
        }
        ...
         //增加paddingBottom
         if (isWrapContentHeight) {
            height += mPaddingBottom;
            if (mLayoutParams != null && mLayoutParams.height >= 0) {
                height = Math.max(height, mLayoutParams.height);
            }

            height = Math.max(height, getSuggestedMinimumHeight());
            height = resolveSize(height, heightMeasureSpec);
            //bottom与parent对齐
            if (offsetVerticalAxis) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        final int[] rules = params.getRules(layoutDirection);
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
                            centerVertical(child, params, height);
                        } else if (rules[ALIGN_PARENT_BOTTOM] != 0) {
                            final int childHeight = child.getMeasuredHeight();
                            params.mTop = height - mPaddingBottom - childHeight;
                            params.mBottom = params.mTop + childHeight;
                        }
                    }
                }
            }
        }
        ...
        // RelativeLayout是否设置了gravity属性
         if (horizontalGravity || verticalGravity) {
            final Rect selfBounds = mSelfBounds;
            selfBounds.set(mPaddingLeft, mPaddingTop, width - mPaddingRight,
                    height - mPaddingBottom);

            final Rect contentBounds = mContentBounds;
            Gravity.apply(mGravity, right - left, bottom - top, selfBounds, contentBounds,
                    layoutDirection);
             //计算child需要的偏移量
            final int horizontalOffset = contentBounds.left - left;
            final int verticalOffset = contentBounds.top - top;
            if (horizontalOffset != 0 || verticalOffset != 0) {
                for (int i = 0; i < count; i++) {
                    final View child = views[i];
                    if (child.getVisibility() != GONE && child != ignore) {
                        final LayoutParams params = (LayoutParams) child.getLayoutParams();
                        if (horizontalGravity) {
                            params.mLeft += horizontalOffset;
                            params.mRight += horizontalOffset;
                        }
                        if (verticalGravity) {
                            params.mTop += verticalOffset;
                            params.mBottom += verticalOffset;
                        }
                    }
                }
            }
        }
        ...
        setMeasuredDimension(width, height);设置测量的宽高
 }
```
0 requestLayout情况对child进行排序
1 执行child.measure 根据rule确定child宽度
2 执行child.measure 根据确定child高度
3 WrapContentWidth 确定自己的宽度
4 WrapContentHeight 确定自己的高度
6 设置gravity后对child位置进行调整
7 设置测量的宽高



onLayout布局 执行child的layout
```
 protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                RelativeLayout.LayoutParams st =
                        (RelativeLayout.LayoutParams) child.getLayoutParams();
                child.layout(st.mLeft, st.mTop, st.mRight, st.mBottom);
            }
        }
    }
```