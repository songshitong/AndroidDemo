

使用方式
fragmentManager.beginTransaction().add(centerFragment, "center").commitAllowingStateLoss()


androidx.fragment\fragment\1.5.1


fragmentManager是从FragmentActivity获取的
```
public class FragmentActivity extends .. {
 //管理fragment以及与FragmentActivity的交互
 final FragmentController mFragments = FragmentController.createController(new HostCallbacks());

 //获取FragmentManager
 public FragmentManager getSupportFragmentManager() {
        return mFragments.getSupportFragmentManager();
    }
}
```
androidx/fragment/app/FragmentController.java 
```
   public FragmentManager getSupportFragmentManager() {
        return mHost.mFragmentManager;
    }
    
     //FragmentController的逻辑交由mFragmentManager实现，例如
      @Nullable
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        return mHost.mFragmentManager.getLayoutInflaterFactory()
                .onCreateView(parent, name, context, attrs);
    }  
```
androidx/fragment/app/FragmentHostCallback.java
```
public abstract class FragmentHostCallback<E> extends FragmentContainer {
   //创建FragmentManager
    final FragmentManager mFragmentManager = new FragmentManagerImpl();  //内部使用，隐藏了FragmentManager的创建

 //FragmentHostCallback主要用户fragment调用host方法的解耦与转发，例如
     public void onStartActivityFromFragment(
            @NonNull Fragment fragment, @SuppressLint("UnknownNullness") Intent intent,
            int requestCode, @Nullable Bundle options) {
        ...
        ContextCompat.startActivity(mContext, intent, options);
    }
}
```
androidx/fragment/app/FragmentManager.java
```
public abstract class FragmentManager{
}

//包内可见class
class FragmentManagerImpl extends FragmentManager {
  //空类  逻辑都在父类
}
```


beginTransaction().add(centerFragment, "center").commit的逻辑
androidx/fragment/app/FragmentManager.java
```
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }
```
androidx/fragment/app/FragmentTransaction.java  创建FragmentTransaction
```
  //常见的操作
  static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;
    static final int OP_SET_PRIMARY_NAV = 8;
    static final int OP_UNSET_PRIMARY_NAV = 9;
    static final int OP_SET_MAX_LIFECYCLE = 10;

    static final class Op {
     Op(int cmd, Fragment fragment) {
            this.mCmd = cmd;
            this.mFragment = fragment;
            this.mFromExpandedOp = false;
            this.mOldMaxState = Lifecycle.State.RESUMED;
            this.mCurrentMaxState = Lifecycle.State.RESUMED;
        }
    }
    

    public final FragmentTransaction add(@NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args, @Nullable String tag)  {
        return add(createFragment(fragmentClass, args), tag);
    }
    
     private Fragment createFragment(@NonNull Class<? extends Fragment> fragmentClass,
            @Nullable Bundle args) {
        if (mFragmentFactory == null) {
            throw new IllegalStateException("Creating a Fragment requires that this "
                    + "FragmentTransaction was built with FragmentManager.beginTransaction()");
        }
        if (mClassLoader == null) {
            throw new IllegalStateException("The FragmentManager must be attached to its"
                    + "host to create a Fragment");
        }
        //根据class创建Fragment
        Fragment fragment = mFragmentFactory.instantiate(mClassLoader, fragmentClass.getName());
        if (args != null) {
            fragment.setArguments(args); //使用无参构造器时  设置参数
        }
        return fragment;
    }    
```
androidx/fragment/app/FragmentFactory.java
```
 public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        try {
            Class<? extends Fragment> cls = loadFragmentClass(classLoader, className);
            return cls.getConstructor().newInstance(); //创建的无参构造器 注意fragment实现代码！！！
        } catch (java.lang.InstantiationException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": calling Fragment constructor caused an exception", e);
        }
    }
    
  @NonNull
    public static Class<? extends Fragment> loadFragmentClass(@NonNull ClassLoader classLoader,
            @NonNull String className) {
        try {
            Class<?> clazz = loadClass(classLoader, className);
            return (Class<? extends Fragment>) clazz;
        } catch (ClassNotFoundException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists", e);
        } catch (ClassCastException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class is a valid subclass of Fragment", e);
        }
    }
    
   @NonNull
    private static Class<?> loadClass(@NonNull ClassLoader classLoader,
            @NonNull String className) throws ClassNotFoundException {
        //将classLoader fragment的class进行存储，防止多次创建   
        SimpleArrayMap<String, Class<?>> classMap = sClassCacheMap.get(classLoader);
        if (classMap == null) {
            classMap = new SimpleArrayMap<>();
            sClassCacheMap.put(classLoader, classMap);
        }
        Class<?> clazz = classMap.get(className);
        if (clazz == null) {
            // Class not found in the cache, see if it's real, and try to add it
            clazz = Class.forName(className, false, classLoader);
            classMap.put(className, clazz);
        }
        return clazz;
    }     
```


androidx/fragment/app/FragmentTransaction.java
add后续
```
    public FragmentTransaction add(@NonNull Fragment fragment, @Nullable String tag)  {
        doAddOp(0, fragment, tag, OP_ADD);
        return this;
    }
    
     void doAddOp(int containerViewId, Fragment fragment, @Nullable String tag, int opcmd) {
        ...
        //各种校验
        final Class<?> fragmentClass = fragment.getClass();
        final int modifiers = fragmentClass.getModifiers();
        if (fragmentClass.isAnonymousClass() || !Modifier.isPublic(modifiers)
                || (fragmentClass.isMemberClass() && !Modifier.isStatic(modifiers))) {
            throw new IllegalStateException("Fragment " + fragmentClass.getCanonicalName()
                    + " must be a public static class to be  properly recreated from"
                    + " instance state.");
        }

        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment "
                        + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }

        if (containerViewId != 0) {
            if (containerViewId == View.NO_ID) {
                throw new IllegalArgumentException("Can't add fragment "
                        + fragment + " with tag " + tag + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment "
                        + fragment + ": was " + fragment.mFragmentId
                        + " now " + containerViewId);
            }
            fragment.mContainerId = fragment.mFragmentId = containerViewId;
        }

        addOp(new Op(opcmd, fragment));
    }
    
     void addOp(Op op) {
        mOps.add(op); //将op添加到ArrayList<Op> mOps 
        op.mEnterAnim = mEnterAnim;
        op.mExitAnim = mExitAnim;
        op.mPopEnterAnim = mPopEnterAnim;
        op.mPopExitAnim = mPopExitAnim;
    }   
```
commit/commitAllowingStateLoss    //与commitNow/commitNowAllowingStateLoss的区别，与commitNow没有做线程切换，要求在主线程执行
androidx/fragment/app/BackStackRecord.java
```
 int commitInternal(boolean allowStateLoss) {
        if (mCommitted) throw new IllegalStateException("commit already called");
       ....
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex(); //mBackStackIndex增长1
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex; //返回index
    }
```
androidx/fragment/app/FragmentManager.java
```
void enqueueAction(@NonNull OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            if (mHost == null) {
                if (mDestroyed) { //FragmentManager销毁后不允许执行
                    throw new IllegalStateException("FragmentManager has been destroyed");
                } else { //FragmentManager没有attach到host时不允许执行
                    throw new IllegalStateException("FragmentManager has not been attached to a "
                            + "host.");
                }
            }
            checkStateLoss(); //不允许onSaveInstanceState后执行
        }
        synchronized (mPendingActions) { //锁上集合
            if (mHost == null) {
                if (allowStateLoss) { //host为空，允许状态丢失时，退出
                    // This FragmentManager isn't attached, so drop the entire transaction.
                    return;
                }
                throw new IllegalStateException("Activity has been destroyed");
            }
            mPendingActions.add(action); //添加到ArrayList<OpGenerator> mPendingActions
            scheduleCommit();
        }
    }

    private void checkStateLoss() {
        if (isStateSaved()) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
    }  
    
     void scheduleCommit() {
        synchronized (mPendingActions) {
            boolean pendingReady = mPendingActions.size() == 1;
            if (pendingReady) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
                updateOnBackPressedCallbackEnabled(); //打开OnBackPressed监听
            }
        }
    }  
  
     //切换主线程执行
     private Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions(true);
        }
    };  
    
    boolean execPendingActions(boolean allowStateLoss) {
        ensureExecReady(allowStateLoss);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();

        return didSomething;
    }
    
 private boolean generateOpsForPendingActions(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isPop) {
        boolean didSomething = false;
        synchronized (mPendingActions) {
            if (mPendingActions.isEmpty()) {
                return false;
            }
            try {
                final int numActions = mPendingActions.size();
                for (int i = 0; i < numActions; i++) { //添加到records
                    didSomething |= mPendingActions.get(i).generateOps(records, isPop);
                }
            } finally {
                // Whether generateOps succeeds or not, we clear the pending actions
                // to avoid re-processing the same set of actions a second time
                mPendingActions.clear(); //移除
                mHost.getHandler().removeCallbacks(mExecCommit);
            }
        }
        return didSomething;
    }   
    
 
 androidx/fragment/app/BackStackRecord.java
  @Override
    public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop) {
        ...
        records.add(this);
        isRecordPop.add(false);
        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
        return true;
    }    
```

removeRedundantOperationsAndExecute 执行op
```
 private void removeRedundantOperationsAndExecute(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop) {
        ...
        final int numRecords = records.size();
        int startIndex = 0;
        for (int recordNum = 0; recordNum < numRecords; recordNum++) {
            final boolean canReorder = records.get(recordNum).mReorderingAllowed;
            if (!canReorder) {
                // execute all previous transactions
                if (startIndex != recordNum) {
                    executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                }
                // execute all pop operations that don't allow reordering together or
                // one add operation
                int reorderingEnd = recordNum + 1;
                if (isRecordPop.get(recordNum)) {
                    while (reorderingEnd < numRecords
                            && isRecordPop.get(reorderingEnd)
                            && !records.get(reorderingEnd).mReorderingAllowed) {
                        reorderingEnd++;
                    }
                }
                executeOpsTogether(records, isRecordPop, recordNum, reorderingEnd);
                startIndex = reorderingEnd;
                recordNum = reorderingEnd - 1;
            }
        }
        if (startIndex != numRecords) {
            executeOpsTogether(records, isRecordPop, startIndex, numRecords);
        }
    }
    
  private void executeOpsTogether(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
         ...
        if (!allowReordering && mCurState >= Fragment.CREATED) {
            // When reordering isn't allowed, we may be operating on Fragments that haven't
            // been made active
            for (int index = startIndex; index < endIndex; index++) {
                BackStackRecord record = records.get(index);
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null && fragment.mFragmentManager != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        //将fragmentStateManager与fragment一一对应存储到mFragmentStore        
                        mFragmentStore.makeActive(fragmentStateManager);
                    }
                }
            }
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        // The last operation determines the overall direction, this ensures that operations
        // such as push, push, pop, push are correctly considered a push
        boolean isPop = isRecordPop.get(endIndex - 1);
        // Ensure that Fragments directly affected by operations
        // are moved to their expected state in operation order
        for (int index = startIndex; index < endIndex; index++) {
            BackStackRecord record = records.get(index);
            if (isPop) {
                // Pop operations get applied in reverse order
                for (int opIndex = record.mOps.size() - 1; opIndex >= 0; opIndex--) {
                    FragmentTransaction.Op op = record.mOps.get(opIndex);
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            } else {
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            }

        }
        // And only then do we move all other fragments to the current state
        moveToState(mCurState, true);
        Set<SpecialEffectsController> changedControllers = collectChangedControllers(
                records, startIndex, endIndex);
        for (SpecialEffectsController controller : changedControllers) {
            controller.updateOperationDirection(isPop);
            controller.markPostponedState();
            controller.executePendingOperations();
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            isPop = isRecordPop.get(recordNum);
            if (isPop && record.mIndex >= 0) {
                record.mIndex = -1;
            }
            record.runOnCommitRunnables();
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    } 
 
     private static void executeOps(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            if (isPop) {
                record.bumpBackStackNesting(-1);
                record.executePopOps();
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }     
```
androidx/fragment/app/BackStackRecord.java
```
 void executeOps() {  //执行不同的op
        final int numOps = mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            final Op op = mOps.get(opNum);
            final Fragment f = op.mFragment;
            if (f != null) {
                f.mBeingSaved = mBeingSaved;
                f.setPopDirection(false);
                f.setNextTransition(mTransition);
                f.setSharedElementNames(mSharedElementSourceNames, mSharedElementTargetNames);
            }
            switch (op.mCmd) {
                case OP_ADD:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.addFragment(f); //添加fragment到FragmentStore
                    break;
                case OP_REMOVE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.removeFragment(f);
                    break;
                case OP_HIDE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.hideFragment(f);
                    break;
                case OP_SHOW:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.showFragment(f);
                    break;
                case OP_DETACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.detachFragment(f);
                    break;
                case OP_ATTACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.attachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(f);
                    break;
                case OP_UNSET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(null);
                    break;
                case OP_SET_MAX_LIFECYCLE:
                    mManager.setMaxLifecycle(f, op.mCurrentMaxState);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
        }
    }
```

//执行fragment的状态流转
```
 void moveToState(int newState, boolean always) {
        if (mHost == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }

        if (!always && newState == mCurState) {
            return;
        }

        mCurState = newState;
        mFragmentStore.moveToExpectedState();
        startPendingDeferredFragments();

        if (mNeedMenuInvalidate && mHost != null && mCurState == Fragment.RESUMED) {
            mHost.onSupportInvalidateOptionsMenu();
            mNeedMenuInvalidate = false;
        }
    }
```
androidx/fragment/app/FragmentStore.java
```
void moveToExpectedState() {
        // Must add them in the proper order. mActive fragments may be out of order
        for (Fragment f : mAdded) {
            FragmentStateManager fragmentStateManager = mActive.get(f.mWho);
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();
            }
        }

        // Now iterate through all active fragments. These will include those that are removed
        // and detached.
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();

                Fragment f = fragmentStateManager.getFragment();
                boolean beingRemoved = f.mRemoving && !f.isInBackStack();
                if (beingRemoved) {
                    if (f.mBeingSaved && !mSavedState.containsKey(f.mWho)) {
                        // In cases where the Fragment never got attached
                        // (i.e., add transaction + saveBackStack())
                        // we still want to save the bare minimum of state
                        // relating to this Fragment
                        fragmentStateManager.saveState();
                    }
                    makeInactive(fragmentStateManager);
                }
            }
        }
    }
```
androidx/fragment/app/FragmentStateManager.java
```
//计算fragment的状态
int computeExpectedState() {
        // If the FragmentManager is null, disallow changing the state at all
        if (mFragment.mFragmentManager == null) {
            return mFragment.mState;
        }
        // Assume the Fragment can go as high as the FragmentManager's state
        int maxState = mFragmentManagerState;

        // Don't allow the Fragment to go above its max lifecycle state
        switch (mFragment.mMaxState) {
            case RESUMED:
                // maxState can't go any higher than RESUMED, so there's nothing to do here
                break;
            case STARTED:
                maxState = Math.min(maxState, Fragment.STARTED);
                break;
            case CREATED:
                maxState = Math.min(maxState, Fragment.CREATED);
                break;
            case INITIALIZED:
                maxState = Math.min(maxState, Fragment.ATTACHED);
                break;
            default:
                maxState = Math.min(maxState, Fragment.INITIALIZING);
        }

        // For fragments that are created from a layout using the <fragment> tag (mFromLayout)
        if (mFragment.mFromLayout) {
            if (mFragment.mInLayout) {
                // Move them immediately to VIEW_CREATED when they are
                // actually added to the layout (mInLayout).
                maxState = Math.max(mFragmentManagerState, Fragment.VIEW_CREATED);
                // But don't move to higher than VIEW_CREATED until the view is added to its parent
                // and the LayoutInflater call has returned
                if (mFragment.mView != null && mFragment.mView.getParent() == null) {
                    maxState = Math.min(maxState, Fragment.VIEW_CREATED);
                }
            } else {
                if (mFragmentManagerState < Fragment.ACTIVITY_CREATED) {
                    // But while they are not in the layout, don't allow their
                    // state to progress upward until the FragmentManager state
                    // is at least ACTIVITY_CREATED. This ensures they get the onInflate()
                    // callback before being attached or created.
                    maxState = Math.min(maxState, mFragment.mState);
                } else {
                    // Once the FragmentManager state is at least ACTIVITY_CREATED
                    // their state can progress up to CREATED as we assume that
                    // they are not ever going to be in layout
                    maxState = Math.min(maxState, Fragment.CREATED);
                }
            }
        }
        // Fragments that are not currently added will sit in the CREATED state.
        if (!mFragment.mAdded) {
            maxState = Math.min(maxState, Fragment.CREATED);
        }
        SpecialEffectsController.Operation.LifecycleImpact awaitingEffect = null;
        if (mFragment.mContainer != null) {
            SpecialEffectsController controller = SpecialEffectsController.getOrCreateController(
                    mFragment.mContainer, mFragment.getParentFragmentManager());
            awaitingEffect = controller.getAwaitingCompletionLifecycleImpact(this);
        }
        if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.ADDING) {
            // Fragments awaiting their enter effects cannot proceed beyond that state
            maxState = Math.min(maxState, Fragment.AWAITING_ENTER_EFFECTS);
        } else if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.REMOVING) {
            // Fragments that are in the process of being removed shouldn't go below that state
            maxState = Math.max(maxState, Fragment.AWAITING_EXIT_EFFECTS);
        } else if (mFragment.mRemoving) {
            if (mFragment.isInBackStack()) {
                // Fragments on the back stack shouldn't go higher than CREATED
                maxState = Math.min(maxState, Fragment.CREATED);
            } else {
                // While removing a fragment, we always move to INITIALIZING
                maxState = Math.min(maxState, Fragment.INITIALIZING);
            }
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (mFragment.mDeferStart && mFragment.mState < Fragment.STARTED) {
            maxState = Math.min(maxState, Fragment.ACTIVITY_CREATED);
        }
        ...
        return maxState;
    }
    
    
    void moveToExpectedState() {
        ...
        try {
            mMovingToState = true;

            boolean stateWasChanged = false;
            int newState;
            while ((newState = computeExpectedState()) != mFragment.mState) { //依次循环执行，直到目标状态
                stateWasChanged = true;
                if (newState > mFragment.mState) {
                    // Moving upward
                    int nextStep = mFragment.mState + 1;
                    switch (nextStep) {
                        case Fragment.ATTACHED:
                            attach();
                            break;
                        case Fragment.CREATED:
                            create();
                            break;
                        case Fragment.VIEW_CREATED:
                            ensureInflatedView();
                            createView();
                            break;
                        case Fragment.AWAITING_EXIT_EFFECTS:
                            activityCreated();
                            break;
                        case Fragment.ACTIVITY_CREATED:
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                int visibility = mFragment.mView.getVisibility();
                                SpecialEffectsController.Operation.State finalState =
                                        SpecialEffectsController.Operation.State.from(visibility);
                                controller.enqueueAdd(finalState, this);
                            }
                            mFragment.mState = Fragment.ACTIVITY_CREATED;
                            break;
                        case Fragment.STARTED:
                            start();
                            break;
                        case Fragment.AWAITING_ENTER_EFFECTS:
                            mFragment.mState = Fragment.AWAITING_ENTER_EFFECTS;
                            break;
                        case Fragment.RESUMED:
                            resume();
                            break;
                    }
                } else { //逆向状态
                    // Moving downward
                    int nextStep = mFragment.mState - 1;
                    switch (nextStep) {
                        case Fragment.AWAITING_ENTER_EFFECTS:
                            pause();
                            break;
                        case Fragment.STARTED:
                            mFragment.mState = Fragment.STARTED;
                            break;
                        case Fragment.ACTIVITY_CREATED:
                            stop();
                            break;
                        case Fragment.AWAITING_EXIT_EFFECTS:
                            ..
                            if (mFragment.mBeingSaved) {
                                saveState();
                            } else if (mFragment.mView != null) {
                                // Need to save the current view state if not done already
                                // by saveInstanceState()
                                if (mFragment.mSavedViewState == null) {
                                    saveViewState();
                                }
                            }
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                controller.enqueueRemove(this);
                            }
                            mFragment.mState = Fragment.AWAITING_EXIT_EFFECTS;
                            break;
                        case Fragment.VIEW_CREATED:
                            mFragment.mInLayout = false;
                            mFragment.mState = Fragment.VIEW_CREATED;
                            break;
                        case Fragment.CREATED:
                            destroyFragmentView();
                            mFragment.mState = Fragment.CREATED;
                            break;
                        case Fragment.ATTACHED:
                            if (mFragment.mBeingSaved
                                    && mFragmentStore.getSavedState(mFragment.mWho) == null) {
                                saveState();
                            }
                            destroy();
                            break;
                        case Fragment.INITIALIZING:
                            detach();
                            break;
                    }
                }
            }
            if (!stateWasChanged && mFragment.mState == Fragment.INITIALIZING) {
                // If the state wasn't changed and the Fragment should be removed
                // then we need to do the work of destroy()+detach() here
                // to ensure the FragmentManager is in a cleaned up state
                if (mFragment.mRemoving && !mFragment.isInBackStack() && !mFragment.mBeingSaved) {
                    。。。
                    mFragmentStore.getNonConfig().clearNonConfigState(mFragment);
                    mFragmentStore.makeInactive(this);
                   ...
                    mFragment.initState();
                }
            }
            if (mFragment.mHiddenChanged) {
                if (mFragment.mView != null && mFragment.mContainer != null) {
                    // Get the controller and enqueue the show/hide
                    SpecialEffectsController controller = SpecialEffectsController
                            .getOrCreateController(mFragment.mContainer,
                                    mFragment.getParentFragmentManager());
                    if (mFragment.mHidden) {
                        controller.enqueueHide(this);
                    } else {
                        controller.enqueueShow(this);
                    }
                }
                if (mFragment.mFragmentManager != null) {
                    mFragment.mFragmentManager.invalidateMenuForFragment(mFragment);
                }
                mFragment.mHiddenChanged = false;
                mFragment.onHiddenChanged(mFragment.mHidden);
                mFragment.mChildFragmentManager.dispatchOnHiddenChanged();
            }
        } finally {
            mMovingToState = false;
        }
    }
    
 
     void attach() {
        。。。
        if (targetFragmentStateManager != null) {
            targetFragmentStateManager.moveToExpectedState();
        }
        mFragment.mHost = mFragment.mFragmentManager.getHost();
        mFragment.mParentFragment = mFragment.mFragmentManager.getParent();
        mDispatcher.dispatchOnFragmentPreAttached(mFragment, false);
        mFragment.performAttach(); //执行fragment的attach
        mDispatcher.dispatchOnFragmentAttached(mFragment, false);
    }   
```

androidx/fragment/app/Fragment.java
```
    void performAttach() {
        for (OnPreAttachedListener listener: mOnPreAttachedListeners) {
            listener.onPreAttached();
        }
        mOnPreAttachedListeners.clear();
        mChildFragmentManager.attachController(mHost, createFragmentContainer(), this);
        mState = ATTACHED;
        mCalled = false;
        onAttach(mHost.getContext());
        if (!mCalled) { //校验调用supper
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onAttach()");
        }
        mFragmentManager.dispatchOnAttachFragment(this);
        mChildFragmentManager.dispatchAttach();
    }
    
    public void onAttach(@NonNull Context context) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onAttach(hostActivity);
        }
    } 
```


onCreateView
androidx/fragment/app/FragmentActivity.java
```
   public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(parent, name, context, attrs);
        if (v == null) {
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }
  
    final View dispatchFragmentsOnCreateView(@Nullable View parent, @NonNull String name,
            @NonNull Context context, @NonNull AttributeSet attrs) {
        return mFragments.onCreateView(parent, name, context, attrs);
    }  
```
androidx/fragment/app/FragmentController.java
```
  public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        return mHost.mFragmentManager.getLayoutInflaterFactory()
                .onCreateView(parent, name, context, attrs);
    }
```
androidx/fragment/app/FragmentLayoutInflaterFactory.java
```
 public View onCreateView(@Nullable final View parent, @NonNull String name,
            @NonNull Context context, @NonNull AttributeSet attrs) {
       ....

        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a =  context.obtainStyledAttributes(attrs, R.styleable.Fragment);
        if (fname == null) {
            fname = a.getString(R.styleable.Fragment_android_name);
        }
        int id = a.getResourceId(R.styleable.Fragment_android_id, View.NO_ID);
        String tag = a.getString(R.styleable.Fragment_android_tag);
        a.recycle();
         ....
        fragment.mContainer = (ViewGroup) parent;

        // The <fragment> tag is the one case where we:
        // 1) Move the Fragment to CREATED even if the FragmentManager isn't yet CREATED
        fragmentStateManager.moveToExpectedState();
        // 2) Create the Fragment's view despite not always moving to ACTIVITY_CREATED
        fragmentStateManager.ensureInflatedView();

        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname
                    + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        ...
        return fragment.mView;
    }
```
ensureInflatedView
androidx/fragment/app/FragmentStateManager.java
```
 void ensureInflatedView() {
        if (mFragment.mFromLayout && mFragment.mInLayout && !mFragment.mPerformedCreateView) {
           ...
            mFragment.performCreateView(mFragment.performGetLayoutInflater(
                    mFragment.mSavedFragmentState), null, mFragment.mSavedFragmentState);
            if (mFragment.mView != null) {
                mFragment.mView.setSaveFromParentEnabled(false);
                mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
                if (mFragment.mHidden) mFragment.mView.setVisibility(View.GONE);
                mFragment.performViewCreated();
                mDispatcher.dispatchOnFragmentViewCreated(
                        mFragment, mFragment.mView, mFragment.mSavedFragmentState, false);
                mFragment.mState = Fragment.VIEW_CREATED;
            }
        }
    }
```
androidx/fragment/app/Fragment.java
```
 void performCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mPerformedCreateView = true;
        mViewLifecycleOwner = new FragmentViewLifecycleOwner(this, getViewModelStore());
        mView = onCreateView(inflater, container, savedInstanceState);
        ...
    }
    
     //执行onCreateView
     public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (mContentLayoutId != 0) {
            return inflater.inflate(mContentLayoutId, container, false);
        }
        return null;
    }   
```



关于事务的回退栈
https://mp.weixin.qq.com/s/UApUiqNkAYW0muAuvQOdeg
涉及到回退栈的时候，是否放到事务中，按返回键会有很大的区别。三个操作分别命名为OP1、OP2、OP3。如果调用了FragmentTransaction.addToBackStack()，
「三个操作放到事务中时，」 按返回键时会执行与OP1、OP2、OP3相反的操作同时恢复到处理事务之前的状态
回退键监听
androidx/fragment/app/FragmentManager.java
```
 void attachController(@NonNull FragmentHostCallback<?> host,
            @NonNull FragmentContainer container, @Nullable final Fragment parent) {
   if (host instanceof OnBackPressedDispatcherOwner) {
            OnBackPressedDispatcherOwner dispatcherOwner = ((OnBackPressedDispatcherOwner) host);
            mOnBackPressedDispatcher = dispatcherOwner.getOnBackPressedDispatcher();
            LifecycleOwner owner = parent != null ? parent : dispatcherOwner;
            mOnBackPressedDispatcher.addCallback(owner, mOnBackPressedCallback);
        }          
 }
 
 private final OnBackPressedCallback mOnBackPressedCallback =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    FragmentManager.this.handleOnBackPressed();
                }
            };
 
  void handleOnBackPressed() {
        // First, execute any pending actions to make sure we're in an
        // up to date view of the world just in case anyone is queuing
        // up transactions that change the back stack then immediately
        // calling onBackPressed()
        execPendingActions(true); //先处理未执行的
        if (mOnBackPressedCallback.isEnabled()) {
            // We still have a back stack, so we can pop
            popBackStackImmediate(); //回退fragment的栈
        } else {
            // Sigh. Due to FragmentManager's asynchronicity, we can
            // get into cases where we *think* we can handle the back
            // button but because of frame perfect dispatch, we fell
            // on our face. Since our callback is disabled, we can
            // re-trigger the onBackPressed() to dispatch to the next
            // enabled callback
            mOnBackPressedDispatcher.onBackPressed(); //执行acitivy的回退
        }
    }   
    
    public boolean popBackStackImmediate() {
        return popBackStackImmediate(null, -1, 0);
    }
 
 private boolean popBackStackImmediate(@Nullable String name, int id, int flags) {
        execPendingActions(false);
        ensureExecReady(true);

        if (mPrimaryNav != null // We have a primary nav fragment
                && id < 0 // No valid id (since they're local)
                && name == null) { // no name to pop to (since they're local)
            final FragmentManager childManager = mPrimaryNav.getChildFragmentManager();
            if (childManager.popBackStackImmediate()) {
                // We did something, just not to this specific FragmentManager. Return true.
                return true;
            }
        }

        boolean executePop = popBackStackState(mTmpRecords, mTmpIsPop, name, id, flags);
        if (executePop) {
            mExecutingActions = true;
            try { //执行需要回退的record
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();
        return executePop;
    }
    
   boolean popBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @Nullable String name, int id, int flags) {
        int index = findBackStackIndex(name, id, (flags & POP_BACK_STACK_INCLUSIVE) != 0);
        if (index < 0) {
            return false;
        }
        //将需要回退的record添加到records
        for (int i = mBackStack.size() - 1; i >= index; i--) {
            records.add(mBackStack.remove(i));
            isRecordPop.add(true);
        }
        return true;
    }                 
```


状态销毁与恢复
androidx/fragment/app/FragmentManager.java
销毁保存
```
 void attachController(@NonNull FragmentHostCallback<?> host,
            @NonNull FragmentContainer container, @Nullable final Fragment parent) {
      if (mHost instanceof SavedStateRegistryOwner && parent == null) {
            SavedStateRegistry registry =
                    ((SavedStateRegistryOwner) mHost).getSavedStateRegistry();
            //SAVED_STATE_TAG的key为android:support:fragments        
            registry.registerSavedStateProvider(SAVED_STATE_TAG, () -> {
                  //销毁监听，保存状态
                        return saveAllStateInternal();
                    }
            );

            Bundle savedInstanceState = registry
                    .consumeRestoredStateForKey(SAVED_STATE_TAG);
               //恢复     
            if (savedInstanceState != null) {
                restoreSaveStateInternal(savedInstanceState);
            }
        }      
    }
    
 
  @NonNull
    Bundle saveAllStateInternal() {
        Bundle bundle = new Bundle();
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions(true);

        mStateSaved = true;
        mNonConfig.setIsStateSaved(true);

        // First save all active fragments.
        ArrayList<String> active = mFragmentStore.saveActiveFragments();

        // And grab all FragmentState objects
        ArrayList<FragmentState> savedState = mFragmentStore.getAllSavedState();
        if (savedState.isEmpty()) {
           ...
        } else {
            // Build list of currently added fragments.
            ArrayList<String> added = mFragmentStore.saveAddedFragments();

            // Now save back stack.
            BackStackRecordState[] backStack = null;
            if (mBackStack != null) {
                int size = mBackStack.size();
                if (size > 0) {
                    backStack = new BackStackRecordState[size];
                    for (int i = 0; i < size; i++) {
                        backStack[i] = new BackStackRecordState(mBackStack.get(i));
                        ...
                    }
                }
            }

            FragmentManagerState fms = new FragmentManagerState();
            fms.mActive = active;
            fms.mAdded = added;
            fms.mBackStack = backStack;
            fms.mBackStackIndex = mBackStackIndex.get();
            if (mPrimaryNav != null) {
                fms.mPrimaryNavActiveWho = mPrimaryNav.mWho;
            }
            fms.mBackStackStateKeys.addAll(mBackStackStates.keySet());
            fms.mBackStackStates.addAll(mBackStackStates.values());
            fms.mLaunchedFragments = new ArrayList<>(mLaunchedFragments);
            bundle.putParcelable(FRAGMENT_MANAGER_STATE_TAG, fms);

            for (String resultName : mResults.keySet()) {
                bundle.putBundle(RESULT_NAME_PREFIX + resultName, mResults.get(resultName));
            }

            for (FragmentState state : savedState) {
                Bundle fragmentBundle = new Bundle();
                fragmentBundle.putParcelable(FRAGMENT_STATE_TAG, state);
                bundle.putBundle(FRAGMENT_NAME_PREFIX + state.mWho, fragmentBundle);
            }
        }

        return bundle;
    }   
```
恢复
```
 void restoreSaveStateInternal(@Nullable Parcelable state) {
        // If there is no saved state at all, then there's nothing else to do
        if (state == null) return;
        Bundle bundle = (Bundle) state;

        for (String bundleKey : bundle.keySet()) {
            if (bundleKey.startsWith(RESULT_NAME_PREFIX)) {
                Bundle savedResult = bundle.getBundle(bundleKey);
                if (savedResult != null) {
                    savedResult.setClassLoader(mHost.getContext().getClassLoader());
                    String resultKey = bundleKey.substring(RESULT_NAME_PREFIX.length());
                    mResults.put(resultKey, savedResult);
                }
            }
        }

        // Restore the saved state of all fragments
        ArrayList<FragmentState> allFragmentStates = new ArrayList<>();
        for (String bundleKey : bundle.keySet()) {
            if (bundleKey.startsWith(FRAGMENT_NAME_PREFIX)) {
                Bundle savedFragmentBundle = bundle.getBundle(bundleKey);
                if (savedFragmentBundle != null) {
                    savedFragmentBundle.setClassLoader(mHost.getContext().getClassLoader());
                    allFragmentStates.add(savedFragmentBundle.getParcelable(FRAGMENT_STATE_TAG));
                }
            }
        }
        mFragmentStore.restoreSaveState(allFragmentStates);

        FragmentManagerState fms = bundle.getParcelable(FRAGMENT_MANAGER_STATE_TAG);
        if (fms == null) return;

        // Build the full list of active fragments, instantiating them from
        // their saved state.
        mFragmentStore.resetActiveFragments();
        for (String who : fms.mActive) {
            // Retrieve any saved state, clearing it out for future calls
            FragmentState fs = mFragmentStore.setSavedState(who, null);
            if (fs != null) {
                FragmentStateManager fragmentStateManager;
                 //FragmentManagerViewModel中获取fragment
                Fragment retainedFragment = mNonConfig.findRetainedFragmentByWho(fs.mWho);
                if (retainedFragment != null) {
                    ...
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, retainedFragment, fs);
                } else {
                    //fragment为空 进行创建
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, mHost.getContext().getClassLoader(),
                            getFragmentFactory(), fs);
                }
                Fragment f = fragmentStateManager.getFragment();
                f.mFragmentManager = this;
               ...
                fragmentStateManager.restoreState(mHost.getContext().getClassLoader());
                mFragmentStore.makeActive(fragmentStateManager);
                // Catch the FragmentStateManager up to our current state
                // In almost all cases, this is Fragment.INITIALIZING, but just in
                // case a FragmentController does something...unique, let's do this anyways.
                fragmentStateManager.setFragmentManagerState(mCurState);
            }
        }

        // Check to make sure there aren't any retained fragments that aren't in mActive
        // This can happen if a retained fragment is added after the state is saved
        for (Fragment f : mNonConfig.getRetainedFragments()) {
            if (!mFragmentStore.containsActiveFragment(f.mWho)) {
                ...
                mNonConfig.removeRetainedFragment(f);
                // We need to ensure that onDestroy and any other clean up is done
                // so move the Fragment up to CREATED, then mark it as being removed, then
                // destroy it without actually adding the Fragment to the FragmentStore
                f.mFragmentManager = this;
                FragmentStateManager fragmentStateManager = new FragmentStateManager(
                        mLifecycleCallbacksDispatcher, mFragmentStore, f);
                fragmentStateManager.setFragmentManagerState(Fragment.CREATED);
                fragmentStateManager.moveToExpectedState();
                f.mRemoving = true;
                fragmentStateManager.moveToExpectedState();
            }
        }

        // Build the list of currently added fragments.
        mFragmentStore.restoreAddedFragments(fms.mAdded);

        // Build the back stack.
        if (fms.mBackStack != null) {
            mBackStack = new ArrayList<>(fms.mBackStack.length);
            for (int i = 0; i < fms.mBackStack.length; i++) {
                BackStackRecord bse = fms.mBackStack[i].instantiate(this);
                ...
                mBackStack.add(bse);
            }
        } else {
            mBackStack = null;
        }
        mBackStackIndex.set(fms.mBackStackIndex);

        if (fms.mPrimaryNavActiveWho != null) {
            mPrimaryNav = findActiveFragment(fms.mPrimaryNavActiveWho);
            dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
        }

        ArrayList<String> savedBackStackStateKeys = fms.mBackStackStateKeys;
        if (savedBackStackStateKeys != null) {
            for (int i = 0; i < savedBackStackStateKeys.size(); i++) {
                mBackStackStates.put(savedBackStackStateKeys.get(i), fms.mBackStackStates.get(i));
            }
        }

        mLaunchedFragments = new ArrayDeque<>(fms.mLaunchedFragments);
    }
```
androidx/fragment/app/FragmentStateManager.java
fragment创建
```
 mDispatcher = dispatcher;
 mFragmentStore = fragmentStore;
 mFragment = fs.instantiate(fragmentFactory, classLoader);
```
androidx/fragment/app/FragmentState.java
```
Fragment instantiate(@NonNull FragmentFactory fragmentFactory,
            @NonNull ClassLoader classLoader) {
        //再次反射创建fragment    
        Fragment fragment = fragmentFactory.instantiate(classLoader, mClassName);
        if (mArguments != null) {
            mArguments.setClassLoader(classLoader);
        }
        fragment.setArguments(mArguments);
        ...
        fragment.mMaxState = Lifecycle.State.values()[mMaxLifecycleState];
        ....
        return fragment;
    }
```