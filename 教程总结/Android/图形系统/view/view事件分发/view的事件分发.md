http://liuwangshu.cn/application/view/5-dispatchingevents.html
android 8.0

1.处理点击事件的方法
View的层级
我们知道View的结构是树形的结构，View可以放在ViewGroup中，这个ViewGroup也可以放到另一个ViewGroup中，这样层层的嵌套就组成了View的层级。

什么是点击事件分发
当我们点击屏幕，就产生了触摸事件，这个事件被封装成了一个类：MotionEvent。而当这个MotionEvent产生后，那么系统就会将这个MotionEvent传递给View的层级，
MotionEvent在View的层级传递的过程就是点击事件分发。

点击事件分发的重要方法
点击事件有三个重要的方法它们分别是：  
dispatchTouchEvent(MotionEvent ev)：用来进行事件的分发  返回值boolean true事件已经处理
onInterceptTouchEvent(MotionEvent ev)：用来进行事件的拦截，在dispatchTouchEvent()中调用，需要注意的是View没有提供该方法
onTouchEvent(MotionEvent ev)：用来处理点击事件，在dispatchTouchEvent()方法中进行调用

为了了解这三个方法的关系，我们先来看看ViewGroup的dispatchTouchEvent()方法的部分源码：
```
@Override
   public boolean dispatchTouchEvent(MotionEvent ev) {
      ...省略
           if (actionMasked == MotionEvent.ACTION_DOWN
                   || mFirstTouchTarget != null) {
               final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;
               if (!disallowIntercept) {
                   intercepted = onInterceptTouchEvent(ev);
                   ev.setAction(action); // restore action in case it was changed
               } else {
                   intercepted = false;
               }
           } else {
               // There are no touch targets and this action is not an initial down
               // so this view group continues to intercept touches.
               intercepted = true;
           }

          ...省略
       return handled;
   }

    //更新FLAG_DISALLOW_INTERCEPT，请求父类不拦截事件  dispatchTouchEvent根据flag进行判断是否拦截
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        if (disallowIntercept == ((mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0)) {
            // We're already in this state, assume our ancestors are too
            return;
        }

        if (disallowIntercept) {
            mGroupFlags |= FLAG_DISALLOW_INTERCEPT;
        } else {
            mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
        }

        // Pass it up to our parent
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }
```
很明显在dispatchTouchEvent()方法中调用了onInterceptTouchEvent()方法来判断是否拦截事件，来看看onInterceptTouchEvent()方法：
```
  public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.isFromSource(InputDevice.SOURCE_MOUSE)
                && ev.getAction() == MotionEvent.ACTION_DOWN
                && ev.isButtonPressed(MotionEvent.BUTTON_PRIMARY)
                && isOnScrollbarThumb(ev.getX(), ev.getY())) {
            return true;
        }
        return false;
    }
```
onInterceptTouchEvent()方法默认返回false，不进行拦截，接着来看看dispatchTouchEvent()方法剩余的部分源码：
```
public boolean dispatchTouchEvent(MotionEvent ev) {
 final View[] children = mChildren;
    //遍历child
    for (int i = childrenCount - 1; i >= 0; i--) {
        final int childIndex = getAndVerifyPreorderedIndex(
                childrenCount, i, customOrder);
        //获取child        
        final View child = getAndVerifyPreorderedView(
                preorderedList, children, childIndex);

        // If there is a view that has accessibility focus we want it
        // to get the event first and if not handled we will perform a
        // normal dispatch. We may do a double iteration but this is
        // safer given the timeframe.
        if (childWithAccessibilityFocus != null) {
            if (childWithAccessibilityFocus != child) {
                continue;
            }
            childWithAccessibilityFocus = null;
            i = childrenCount - 1;
        }
        //todo isTransformedTouchPointInView 坐标转换后的事件是否在child里面
        if (!canViewReceivePointerEvents(child)
                || !isTransformedTouchPointInView(x, y, child, null)) {
            ev.setTargetAccessibilityFocus(false);
            continue;
        }
        //todo TouchTarget相关
        newTouchTarget = getTouchTarget(child);
        //child已经接收到touch事件了
        if (newTouchTarget != null) {
            // Child is already receiving touch within its bounds.
            // Give it the new pointer in addition to the ones it is handling.
            newTouchTarget.pointerIdBits |= idBitsToAssign;
            break;
        }

        resetCancelNextUpFlag(child);
        //将转换后的事件分发给子view或子ViewGroup
        if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {
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
            newTouchTarget = addTouchTarget(child, idBitsToAssign);
            alreadyDispatchedToNewTouchTarget = true;
            break;
        }

        // The accessibility focus didn't handle the event, so clear
        // the flag and do a normal dispatch to all children.
        ev.setTargetAccessibilityFocus(false);
    }
    if (preorderedList != null) preorderedList.clear();
  }
... 
}
protected boolean isTransformedTouchPointInView(float x, float y, View child,
            PointF outLocalPoint) {
        final float[] point = getTempPoint();
        point[0] = x;
        point[1] = y;
        //坐标转换
        transformPointToViewLocal(point, child);
        //判断坐标是否在view里面
        final boolean isInView = child.pointInView(point[0], point[1]);
        if (isInView && outLocalPoint != null) {
            //将新的值放在outLocalPoint
            outLocalPoint.set(point[0], point[1]);
        }
        return isInView;
    }
    
final boolean pointInView(float localX, float localY) {
        return pointInView(localX, localY, 0);
    }
public boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((mRight - mLeft) + slop) &&
                localY < ((mBottom - mTop) + slop);
    }    
```
我们看到了for循环，首先遍历ViewGroup的子元素，判断子元素是否能够接收到点击事件，如果子元素能够接收到则交由子元素来处理。
接下来看看dispatchTransformedTouchEvent()方法中实现了什么：
```
private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel,
           View child, int desiredPointerIdBits) {
       final boolean handled;

       // Canceling motions is a special case.  We don't need to perform any transformations
       // or filtering.  The important part is the action, not the contents.
       final int oldAction = event.getAction();
       if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
           event.setAction(MotionEvent.ACTION_CANCEL);
           if (child == null) {
               handled = dispatchTouchEvent(event);
           } else {
               handled = child.dispatchTouchEvent(event);
           }
           event.setAction(oldAction);
           return handled;
       }
 ...省略      
}       
```
如果有子View则调用子View的dispatchTouchEvent(event)方法。如果ViewGroup没有子View则调用super.dispatchTouchEvent(event)，
 ViewGroup是继承View的，我们再来看看View的dispatchTouchEvent(event)：
frameworks/base/core/java/android/view/View.java
```
public boolean dispatchTouchEvent(MotionEvent event) {
      ...省略
       boolean result = false;
       if (onFilterTouchEventForSecurity(event)) {
           //noinspection SimplifiableIfStatement
           ListenerInfo li = mListenerInfo;
           //mOnTouchListener的类型是OnTouchListener 
           if (li != null && li.mOnTouchListener != null
                   && (mViewFlags & ENABLED_MASK) == ENABLED
                   && li.mOnTouchListener.onTouch(this, event)) {
               result = true;
           }

           if (!result && onTouchEvent(event)) {
               result = true;
           }
       }
    ...省略
       return result;
   }
```
我们看到如果OnTouchListener不为null并且onTouch()方法返回true，则表示事件被消费，就不会执行onTouchEvent(event)，
 否则就会执行onTouchEvent(event)。
  //先执行外部的OnTouchListener，回调返回false然后是onTouchEvent方法
再来看看onTouchEvent()方法的部分源码：
frameworks/base/core/java/android/view/View.java
```
public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final int viewFlags = mViewFlags;
        final int action = event.getAction();

        final boolean clickable = ((viewFlags & CLICKABLE) == CLICKABLE
                || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
                || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE;

        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            if (action == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {
                setPressed(false);
            }
            mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return clickable;
        }
        if (mTouchDelegate != null) {
            if (mTouchDelegate.onTouchEvent(event)) {
                return true;
            }
        }

        if (clickable || (viewFlags & TOOLTIP) == TOOLTIP) {
            switch (action) {
                case MotionEvent.ACTION_UP:
                    mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
                    if ((viewFlags & TOOLTIP) == TOOLTIP) {
                        handleTooltipUp();
                    }
                    if (!clickable) {
                        removeTapCallback();
                        removeLongPressCallback();
                        mInContextButtonPress = false;
                        mHasPerformedLongPress = false;
                        mIgnoreNextUpEvent = false;
                        break;
                    }
                    boolean prepressed = (mPrivateFlags & PFLAG_PREPRESSED) != 0;
                    if ((mPrivateFlags & PFLAG_PRESSED) != 0 || prepressed) {
                        // take focus if we don't have it already and we should in
                        // touch mode.
                        boolean focusTaken = false;
                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                            focusTaken = requestFocus();
                        }

                        if (prepressed) {
                            // The button is being released before we actually
                            // showed it as pressed.  Make it show the pressed
                            // state now (before scheduling the click) to ensure
                            // the user sees it.
                            setPressed(true, x, y);
                        }

                        if (!mHasPerformedLongPress && !mIgnoreNextUpEvent) {
                            // This is a tap, so remove the longpress check
                            removeLongPressCallback();

                            // Only perform take click actions if we were in the pressed state
                            if (!focusTaken) {
                                // Use a Runnable and post this rather than calling
                                // performClick directly. This lets other visual state
                                // of the view update before click actions start.
                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();
                                }
                                if (!post(mPerformClick)) {
                                    //touch事件触发click
                                    performClick();
                                }
                            }
                        }

                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = new UnsetPressedState();
                        }

                        if (prepressed) {
                            postDelayed(mUnsetPressedState,
                                    ViewConfiguration.getPressedStateDuration());
                        } else if (!post(mUnsetPressedState)) {
                            // If the post failed, unpress right now
                            mUnsetPressedState.run();
                        }

                        removeTapCallback();
                    }
                    mIgnoreNextUpEvent = false;
                    break;
      
            }
           //clickable为true则消费该事件
            return true;
        }

        return false;
    }
```

上面可以看到只要View的CLICKABLE和LONG_CLICKABLE一个为true，那么onTouchEvent就会返回true消耗这个事件。
CLICKABLE和LONG_CLICKABLE代表View可以被点击和长按点击，可以通过View的setClickable和setLongClickable方法来设置，
也可以通过View的setOnClickListenter和setOnLongClickListener来设置，他们会自动将View的设置为CLICKABLE和LONG_CLICKABLE。
接着在ACTION_UP事件会调用performClick()方法：
```
  public boolean performClick() {
        final boolean result;
        final ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            li.mOnClickListener.onClick(this);
            result = true;
        } else {
            result = false;
        }

        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);

        notifyEnterOrExitForAutoFillIfNeeded(true);

        return result;
    }
```
如果View设置了点击事件OnClickListener，那么它的onClick()方法就会被执行。



2.点击事件分发的传递规则
看到这里我们就可以知道点击事件分发的这三个重要方法的关系，用伪代码来简单表示就是：
```
public boolean dispatchTouchEvent(MotionEvent ev) {
  boolean result=false;
  if(onInterceptTouchEvent(ev)){
        result=onTouchEvent(ev);
   }else{
        result=child.dispatchTouchEvent(ev);
  }
return result;
```
点击事件由上而下的分发传递规则
当点击事件产生后会由DecorView传递给Activity来处理，再传递给Window再传递给顶层的ViewGroup，一般在事件传递中只考虑ViewGroup的onInterceptTouchEvent()方法，
  因为一般情况我们不会去重写dispatchTouchEvent()方法。
对于根ViewGroup，点击事件首先传递给它的dispatchTouchEvent()方法，如果该ViewGroup的onInterceptTouchEvent()方法返回true，
  则表示它要拦截这个事件，这个事件就会交给它的onTouchEvent()方法处理，如果onInterceptTouchEvent()方法返回false，
  则表示它不拦截这个事件，则交给它的子元素的dispatchTouchEvent()来处理，如此的反复下去。如果传递给最底层的View，View是没有子View的，
  就会调用View的dispatchTouchEvent()方法，一般情况下最终会调用View的onTouchEvent()方法。

举个现实的例子，就是我们的应用产生了重大的bug，这个bug首先会汇报给技术总监那：

技术总监（顶层ViewGroup)→技术经理（中层ViewGroup)→工程师（底层View)
技术总监不拦截，把bug分给了技术经理，技术经理不拦截把bug分给了工程师，工程师没有下属只有自己处理了。
事件由上而下传递返回值规则为：true，拦截，不继续向下传递；或者false，不拦截，继续向下传递。


点击事件由下而上的处理传递规则
点击事件传给最底层的View，如果他的onTouchEvent()方法返回true，则事件由最底层的View消耗并处理了，如果返回false则表示该View不做处理，
 则传递给父View的onTouchEvent()处理，如果父View的onTouchEvent()仍旧返回返回false，则继续传递给改父View的父View处理，如此的反复下去。

再返回我们现实的例子，工程师发现这个bug太难搞不定（onTouchEvent()返回false)，他只能交给上级技术经理处理，
   如果技术经理也搞不定（onTouchEvent()返回false)，
  那就把bug传给技术总监，技术总监一看bug很简单就解决了（onTouchEvent()返回true)。

事件由下而上传递返回值规则为：true，处理了，不继续向上传递；或者false，不处理，继续向上传递。


点击事件传递时的其他问题
上面源码我们看到：如果我们设置了OnTouchListener并且onTouch()方法返回true，则onTouchEvent()方法不会被调用，
  否则则会调用onTouchEvent()方法，可见OnTouchListener的优先级要比onTouchEvent()要高。在OnTouchEvent()方法中，
  如果当前设置了OnClickListener则会执行它的onClick()方法。
View的OnTouchEvent()方法默认都会返回true，除非它是不可点击的也就是CLICKABLE和LONG_CLICKABLE都为false。


结合ViewRootImpl事件转发.md 完整点击流程
DecorView.dispatchTouchEvent->Activity.dispatchTouchEvent  后面的流程没有消费事件调用onTouchEvent
->Window.superDispatchTouchEvent->DecorView.superDispatchTouchEvent
->ViewGroup.dispatchTouchEvent->ViewGroup.onInterceptTouchEvent
todo 流程存在分支，总结程流程图
```
public boolean dispatchTouchEvent(MotionEvent ev) {
  boolean result=false;
  if(onInterceptTouchEvent(ev)){
        result=onTouchEvent(ev);
   }else{
        result=child.dispatchTouchEvent(ev);
  }
return result;
```
view.performClick ->view.onClick
https://juejin.cn/post/6844904041487532045#heading-16






