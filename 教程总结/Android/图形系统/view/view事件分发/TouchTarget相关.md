https://blog.csdn.net/dehang0/article/details/104317611
TouchTarget的设计
一次完整的触摸事件包括DOWN，MOVE，UP/CANCEL   这几个阶段大概率是同一个view触发的，所以查找路径是一样的
由于触摸事件的发生频率是很高的，且布局的嵌套层次也可能很深，如果每次在下发事件时都进行全量遍历的话不利于提升绘制效率。
为了提高事件的下发效率并减少对象的重复创建，ViewGroup 中声明了一个 TouchTarget 类型的全局变量，即 mFirstTouchTarget

TouchTarget主要包含触摸的view以及pointer ids
成员pointerIdBits用于存储多点触摸的这些触摸点的ID。pointerIdBits为int型，有32bit位，每一bit位可以表示一个触摸点ID，
最多可存储32个触摸点ID。
pointerIdBits是如何做到在bit位上存储ID呢？假设触摸点ID取值为x（x的范围可从0～31），存储时先将1左移x位，
然后pointerIdBits与之执行|=操作，从而设置到pointerIdBits的对应bit位上。
pointerIdBits的存在意义是记录TouchTarget接收的触摸点ID，在这个TouchTarget上可能只落下一个触摸点，也可能同时落下多个。
当所有触摸点都离开时，pointerIdBits就已被清0，那么TouchTarget自身也将被从mFirstTouchTarget中移除。

frameworks/base/core/java/android/view/ViewGroup.java
```
 private static final class TouchTarget {
        private static final int MAX_RECYCLED = 32;
        private static final Object sRecycleLock = new Object[0];
        //缓存的链表，减少对象创建
        private static TouchTarget sRecycleBin;
        //缓存大小
        private static int sRecycledCount;

        public static final int ALL_POINTER_IDS = -1; // all ones

        // The touched child view.
        public View child;

        // The combined bit mask of pointer ids for all pointers captured by the target.
        public int pointerIdBits;

        //链表存储多点触摸  链表保存多个TouchTarget，是因为存在多点触摸情况下，需要将事件拆分后派发给不同的child
        public TouchTarget next;

        public static TouchTarget obtain(@NonNull View child, int pointerIdBits) {
            ...
            final TouchTarget target;
            synchronized (sRecycleLock) {
                if (sRecycleBin == null) {
                    target = new TouchTarget();
                } else {
                    target = sRecycleBin;
                    sRecycleBin = target.next;
                     sRecycledCount--;
                    target.next = null;
                }
            }
            target.child = child;
            target.pointerIdBits = pointerIdBits;
            return target;
        }

        public void recycle() {
            ...
            synchronized (sRecycleLock) {
                //最大缓存数 MAX_RECYCLED = 32
                if (sRecycledCount < MAX_RECYCLED) {
                    next = sRecycleBin;
                    sRecycleBin = this;
                    sRecycledCount += 1;
                } else {
                    next = null;
                }
                child = null;
            }
        }
    }
```

看一下TouchTarget的应用
```
 public boolean dispatchTouchEvent(MotionEvent ev) {
         ...
        boolean handled = false;
        if (onFilterTouchEventForSecurity(ev)) {
            final int action = ev.getAction();
            final int actionMasked = action & MotionEvent.ACTION_MASK;

            // Handle an initial down.
            if (actionMasked == MotionEvent.ACTION_DOWN) {
               // ACTION_DOWN表示一次全新的事件序列开始，那么清除旧的
                // TouchTarget（正常情况下TouchTarget在上一轮事件序列结束时会清空，若此时仍存在，
                   // 则需要先给这些TouchTarget派发ACTION_CANCEL事件，然后再清除），重置触摸滚动等相关的状态和标识位。
                cancelAndClearTouchTargets(ev);
                resetTouchState();
            }

            // Check for interception.
            final boolean intercepted;
            //....onInterceptTouchEvent的逻辑

            // 标记是否派发ACTION_CANCEL事件
            final boolean canceled = resetCancelNextUpFlag(this)
                    || actionMasked == MotionEvent.ACTION_CANCEL;

            // split标记是否需要进行事件拆分
            final boolean split = (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) != 0;
            //newTouchTarget用于保存新的派发目标
            TouchTarget newTouchTarget = null;
            boolean alreadyDispatchedToNewTouchTarget = false;
            // 只有当非cancele且不拦截的情况才进行目标查找，否则直接跳到执行派发步骤。如果是
            // 因为被拦截，那么还没有派发目标，则会由ViewGroup自己处理事件。
            if (!canceled && !intercepted) {
                ...
                   // 当ev为ACTION_DOWN或ACTION_POINTER_DOWN时，表示对于当前ViewGroup
                  // 来说有一个新的事件序列开始，那么需要进行目标查找。（不考虑悬浮手势操作） 
                if (actionMasked == MotionEvent.ACTION_DOWN
                        || (split && actionMasked == MotionEvent.ACTION_POINTER_DOWN)
                        || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                    final int actionIndex = ev.getActionIndex(); // always 0 for down
                    // 通过触摸点索引取得触摸点ID，然后左移x位（x=ID值）
                    final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex)
                            : TouchTarget.ALL_POINTER_IDS;

                    // 遍历mFirstTouchTarget链表，进行清理。若有TouchTarget设置了此触摸点ID，
                    // 则将其移除该ID，若移除后的TouchTarget已经没有触摸点ID了，那么接着移除
                    // 这个TouchTarget。
                    removePointersFromTouchTargets(idBitsToAssign);

                    final int childrenCount = mChildrenCount;
                    if (newTouchTarget == null && childrenCount != 0) {
                        final float x = ev.getX(actionIndex);
                        final float y = ev.getY(actionIndex);
                        // Find a child that can receive the event.
                        // Scan children from front to back.
                        final ArrayList<View> preorderedList = buildTouchDispatchChildList();
                        final boolean customOrder = preorderedList == null
                                && isChildrenDrawingOrderEnabled();
                        final View[] children = mChildren;
                        // 逆序遍历子view，即先查询上面的
                        for (int i = childrenCount - 1; i >= 0; i--) {
                            final int childIndex = getAndVerifyPreorderedIndex(
                                    childrenCount, i, customOrder);
                            final View child = getAndVerifyPreorderedView(
                                    preorderedList, children, childIndex);
                            ...
                            //判断该child能否接收触摸事件和点击位置是否命中child范围内。                        
                            if (!canViewReceivePointerEvents(child)
                                    || !isTransformedTouchPointInView(x, y, child, null)) {
                                ev.setTargetAccessibilityFocus(false);
                                continue;
                            }
                            // 遍历mFirstTouchTarget链表，查找该child对应的TouchTarget。
                           // 如果之前已经有触摸点落于该child中且消费了事件，这次新的触摸点也落于该child中，
                           // 那么就会找到之前保存的TouchTarget
                            newTouchTarget = getTouchTarget(child);
                            if (newTouchTarget != null) {
                                // 派发目标已经存在，只要给TouchTarget的触摸点ID集合添加新的
                                // ID即可，然后退出子view遍历。
                                //先前存在TouchTarget的情况下不执行dispatchTransformedTouchEvent，是因为需要对当次事件进行事件拆分，
                                //对ACTION_POINTER_DOWN类型进行转化，所以留到后面执行派发阶段，再统一处理
                                newTouchTarget.pointerIdBits |= idBitsToAssign;
                                break;
                            }

                            resetCancelNextUpFlag(child);
                            //将事件派发给child
                            if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {
                                //child消费了该事件
                                // Child wants to receive touch within its bounds.
                                mLastTouchDownTime = ev.getDownTime();
                                if (preorderedList != null) {
                                    // childIndex points into presorted list, find original index
                                    for (int j = 0; j < childrenCount; j++) {
                                        if (children[childIndex] == mChildren[j]) {
                                            mLastTouchDownIndex = j;
                                            break;
                                        }
                                    }
                                } else {
                                    mLastTouchDownIndex = childIndex;
                                }
                                mLastTouchDownX = ev.getX();
                                mLastTouchDownY = ev.getY();
                                // 为该child创建TouchTarget，添加到mFirstTouchTarget链表的头部，
                                // 并将其设置为新的头节点
                                newTouchTarget = addTouchTarget(child, idBitsToAssign);
                                // 标记已经派发过事件
                                alreadyDispatchedToNewTouchTarget = true;
                                break;
                            }
                            ...                           
                        }
                        if (preorderedList != null) preorderedList.clear();
                    }
                    // 子view遍历完毕   
                    // 检查是否找到派发目标
                    if (newTouchTarget == null && mFirstTouchTarget != null) {
                        // 若没有找到派发目标（没有命中child或命中的child不消费），但是存在
                       // 旧的TouchTarget，那么将该事件派发给最开始添加的那个TouchTarget，
                       // 多点触摸情况下有可能这个事件是它想要的。
                        newTouchTarget = mFirstTouchTarget;
                        while (newTouchTarget.next != null) {
                            newTouchTarget = newTouchTarget.next;
                        }
                        newTouchTarget.pointerIdBits |= idBitsToAssign;
                    }
                }
            }
            //执行派发  派发到TouchTarget
            // Dispatch to touch targets.
            if (mFirstTouchTarget == null) {
                 //若mFirstTouchTarget链表为空，说明没有派发目标，那么交由ViewGroup自己处理  第三个参数child为null
                handled = dispatchTransformedTouchEvent(ev, canceled, null,
                        TouchTarget.ALL_POINTER_IDS);
            } else {
                // Dispatch to touch targets, excluding the new touch target if we already
                // dispatched to it.  Cancel touch targets if necessary.
                TouchTarget predecessor = null;
                TouchTarget target = mFirstTouchTarget;
                // 遍历链表
                while (target != null) {
                    final TouchTarget next = target.next;
                    if (alreadyDispatchedToNewTouchTarget && target == newTouchTarget) {
                       // 若已经对newTouchTarget派发过事件，则标记消费该事件。
                        handled = true;
                    } else {
                        final boolean cancelChild = resetCancelNextUpFlag(target.child)
                                || intercepted;
                        //派发事件给child        
                        if (dispatchTransformedTouchEvent(ev, cancelChild,
                                target.child, target.pointerIdBits)) {
                            handled = true;
                        }
                        / 若取消该child，则从链表中移除对应的TouchTarget，并将
                        // TouchTarget回收进对象缓存池。
                        if (cancelChild) {
                            if (predecessor == null) {
                                mFirstTouchTarget = next;
                            } else {
                                predecessor.next = next;
                            }
                            target.recycle();
                            target = next;
                            continue;
                        }
                    }
                    predecessor = target;
                    target = next;
                }
            }

            // Update list of touch targets for pointer up or cancel, if needed.
            if (canceled
                    || actionMasked == MotionEvent.ACTION_UP
                    || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
             // 若是取消事件或事件序列结束，则清空TouchTarget链表，重置其他状态和标记位。       
                resetTouchState();
            } else if (split && actionMasked == MotionEvent.ACTION_POINTER_UP) {
               // 若是某个触摸点的事件子序列结束，则从所有TouchTarget中移除该触摸点ID。
              // 若有TouchTarget移除ID后，ID为空，则再移除这个TouchTarget。
                final int actionIndex = ev.getActionIndex();
                final int idBitsToRemove = 1 << ev.getPointerId(actionIndex);
                removePointersFromTouchTargets(idBitsToRemove);
            }
        }

        if (!handled && mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onUnhandledEvent(ev, 1);
        }
        return handled;
    }
    
 private void cancelAndClearTouchTargets(MotionEvent event) {
        if (mFirstTouchTarget != null) {
            boolean syntheticEvent = false;
            if (event == null) {
                //构建CANCEL事件
                final long now = SystemClock.uptimeMillis();
                event = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                syntheticEvent = true;
            }
            //派发CANCEL
            for (TouchTarget target = mFirstTouchTarget; target != null; target = target.next) {
                resetCancelNextUpFlag(target.child);
                dispatchTransformedTouchEvent(event, true, target.child, target.pointerIdBits);
            }
            //清除touchTargets
            clearTouchTargets();
            //motionEvent回收
            if (syntheticEvent) {
                event.recycle();
            }
        }
    }    
    
     private void clearTouchTargets() {
        TouchTarget target = mFirstTouchTarget;
        //回收target
        if (target != null) {
            do {
                TouchTarget next = target.next;
                target.recycle();
                target = next;
            } while (target != null);
            //mFirstTouchTarget清空
            mFirstTouchTarget = null;
        }
    }
    
private void removePointersFromTouchTargets(int pointerIdBits) {
        TouchTarget predecessor = null;
        TouchTarget target = mFirstTouchTarget;
        while (target != null) {
            final TouchTarget next = target.next;
            if ((target.pointerIdBits & pointerIdBits) != 0) {
                target.pointerIdBits &= ~pointerIdBits;
                if (target.pointerIdBits == 0) {
                    if (predecessor == null) {
                        mFirstTouchTarget = next;
                    } else {
                        predecessor.next = next;
                    }
                    target.recycle();
                    target = next;
                    continue;
                }
            }
            predecessor = target;
            target = next;
        }
    }  
   
   //child对应的TouchTarget 
   private TouchTarget getTouchTarget(@NonNull View child) {
        for (TouchTarget target = mFirstTouchTarget; target != null; target = target.next) {
            if (target.child == child) {
                return target;
            }
        }
        return null;
    } 
   
    //将child的TouchTarget置为头节点，更新为mFirstTouchTarget
    private TouchTarget addTouchTarget(@NonNull View child, int pointerIdBits) {
        final TouchTarget target = TouchTarget.obtain(child, pointerIdBits);
        target.next = mFirstTouchTarget;
        mFirstTouchTarget = target;
        return target;
    } 
    
  private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel,
            View child, int desiredPointerIdBits) {
        final boolean handled;

        // Canceling motions is a special case.  We don't need to perform any transformations
        // or filtering.  The important part is the action, not the contents.
        final int oldAction = event.getAction();
        if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
            //向下派发CANCEL事件
            event.setAction(MotionEvent.ACTION_CANCEL);
            //child为null调用super.dispatchTouchEvent()也就是自己的dispatchTouchEvent
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                handled = child.dispatchTouchEvent(event);
            }
            event.setAction(oldAction);
            return handled;
        }

        // Calculate the number of pointers to deliver.
        final int oldPointerIdBits = event.getPointerIdBits();
        final int newPointerIdBits = oldPointerIdBits & desiredPointerIdBits;

        // If for some reason we ended up in an inconsistent state where it looks like we
        // might produce a motion event with no pointers in it, then drop the event.
        if (newPointerIdBits == 0) {
            return false;
        }

        final MotionEvent transformedEvent;
        if (newPointerIdBits == oldPointerIdBits) {
            if (child == null || child.hasIdentityMatrix()) {
                if (child == null) {
                    handled = super.dispatchTouchEvent(event);
                } else {
                    final float offsetX = mScrollX - child.mLeft;
                    final float offsetY = mScrollY - child.mTop;
                    event.offsetLocation(offsetX, offsetY);

                    handled = child.dispatchTouchEvent(event);

                    event.offsetLocation(-offsetX, -offsetY);
                }
                return handled;
            }
            transformedEvent = MotionEvent.obtain(event);
        } else {
            //对事件拆分
            transformedEvent = event.split(newPointerIdBits);
        }

        // Perform any necessary transformations and dispatch.
        if (child == null) {
            handled = super.dispatchTouchEvent(transformedEvent);
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            transformedEvent.offsetLocation(offsetX, offsetY);
            if (! child.hasIdentityMatrix()) {
                transformedEvent.transform(child.getInverseMatrix());
            }

            handled = child.dispatchTouchEvent(transformedEvent);
        }

        // Done.
        transformedEvent.recycle();
        return handled;
    }            
```
在派发事件前，会先判断若当次ev是ACTION_DOWN，则对当前ViewGroup来说，表示是一次全新的事件序列开始，那么需要保证清空旧的TouchTarget链表，
  以保证接下来mFirstTouchTarget可以正确保存派发目标。
首先当次事件未cancel且未被拦截，然后必须是ACTION_DOWN或ACTION_POINTER_DOWN，即新的事件序列或子序列的开始，才会进行派发事件查找。

在查找过程中，会逆序遍历子view，先找到命中范围的child。若该child对应的TouchTarget已经在mFirstTouchTarget链表中，
  则意味着之前已经有触摸点落于该child且消费了事件，那么只需要给其添加触摸点ID，然后结束子view遍历；若没有找到对应的TouchTarget，
  说明对于该child是新的事件，那么通过dispatchTransformedTouchEvent方法，对其进行派发，若child消费事件，
  则创建TouchTarget添加至mFirstTouchTarget链表，并标记已经派发过事件

当遍历完子view，若没有找到派发目标，但是mFirstTouchTarget链表不为空，则把最早添加的那个TouchTarget当作查找到的目标。

对于ACTION_DOWN类型的事件来说，在派发目标查找阶段，就会进行一次事件派发

执行派发阶段，即是对TouchTarget链表进行派发。在前面查找派发目标过程中，会将TouchTarget保存在以mFirstTouchTarget作为头节点的链表中，
   因此，只需要遍历该链表进行派发即可

举例：
假设childA、childB都能响应事件：
当触摸点1落于childA时，产生事件ACTION_DOWN，ViewGroup会为childA生成一个TouchTarget，后续滑动事件将派发给它。
当触摸点2落于childA时，产生ACTION_POINTER_DOWN事件，此时可以复用TouchTarget，并给它添加触摸点2的ID。
当触摸点3落于childB时，产生ACTION_POINTER_DOWN事件，ViewGroup会再生成一个TouchTarget，此时ViewGroup中有两个TouchTarget，
  后续产生滑动事件，将根据触摸点信息对事件进行拆分，之后再将拆分事件派发给对应的child


ACTION_CANCEL 事件的特殊设计
按照正常情况来说，每个事件序列应该是都只交由一个 View 或者 ViewGroup 进行消费的，可是还存在一种特殊情况，即 View 消费了 ACTION_DOWN 事件，
而后续的 ACTION_MOVE 和 ACTION_UP 事件被其上层容器 ViewGroup 拦截了，导致 View 接收不到后续事件。这会导致一些异常问题， 
例如，Button 在接收到 ACTION_DOWN 事件后 UI 后呈现按压状态，如果接收不到 ACTION_UP 这个结束事件的话可能就无法恢复 UI 状态了。

为了解决这个问题，Android 系统就通过 ACTION_CANCEL 事件来作为事件序列的另外一种结束消息
当存在上诉情况时，ViewGroup 就会通过 dispatchTransformedTouchEvent 方法构造一个 ACTION_CANCEL 事件并将之下发给 View，
 从而使得 View 即使没有接受到 ACTION_UP 事件也可以知道本次事件序列已经结束了
同时，ViewGroup 也会将 View 从 mFirstTouchTarget 中移除，这样后续事件也就不会再尝试向 View 下发了


//todo dispatchTransformedTouchEvent的event.spilt  查看motionEvent的原理
// TouchTarget的位运算 
//final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex): TouchTarget.ALL_POINTER_IDS;

//todo 滑动冲突是不是touchTarget的问题。。。。