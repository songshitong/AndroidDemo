
constraintlayout-2.1.4-sources.jar!\androidx\constraintlayout\motion\widget\MotionLayout.java

```
 public void setTransitionDuration(int milliseconds) {
       ...
        mScene.setDuration(milliseconds);
    }
    
  public void transitionToStart() {
        animateTo(0.0f);
    }  
 
  void animateTo(float position) {
        ...
        if (mScene == null) {
            return;
        }

        if (mTransitionLastPosition != mTransitionPosition && mTransitionInstantly) {
            // if we had a call from setProgress() but evaluate() didn't run,
            // the mTransitionLastPosition might not have been updated
            mTransitionLastPosition = mTransitionPosition;
        }

        if (mTransitionLastPosition == position) {
            return;
        }
        mTemporalInterpolator = false;
        float currentPosition = mTransitionLastPosition;
        mTransitionGoalPosition = position;
        mTransitionDuration = mScene.getDuration() / 1000f;
        setProgress(mTransitionGoalPosition);
        mInterpolator = null;
        mProgressInterpolator = mScene.getInterpolator();
        mTransitionInstantly = false;
        mAnimationStartTime = getNanoTime();
        mInTransition = true;
        mTransitionPosition = currentPosition;
        ...
        mTransitionLastPosition = currentPosition;
        //刷新
        invalidate();
    }    
```