
使用 淡入淡出动画
```
Glide.with(this).load("").transition(BitmapTransitionOptions.withCrossFade()).into(view);
```

先看BitmapTransitionOptions的父类TransitionOptions
com/bumptech/glide/TransitionOptions.java
```
public abstract class TransitionOptions<
        CHILD extends TransitionOptions<CHILD, TranscodeType>, TranscodeType>
    implements Cloneable {
  private TransitionFactory<? super TranscodeType> transitionFactory = NoTransition.getFactory();

  public final CHILD dontTransition() {
    return transition(NoTransition.getFactory());
  }

  public final CHILD transition(int viewAnimationId) {
    return transition(new ViewAnimationFactory<>(viewAnimationId));
  }

  public final CHILD transition(@NonNull ViewPropertyTransition.Animator animator) {
    return transition(new ViewPropertyAnimationFactory<>(animator));
  }

  @NonNull
  public final CHILD transition(
      @NonNull TransitionFactory<? super TranscodeType> transitionFactory) {
    this.transitionFactory = Preconditions.checkNotNull(transitionFactory);
    return self();
  }

  final TransitionFactory<? super TranscodeType> getTransitionFactory() {
    return transitionFactory;
  }
}

com/bumptech/glide/request/transition/TransitionFactory.java 构建Transition
public interface TransitionFactory<R> {
  Transition<R> build(DataSource dataSource, boolean isFirstResource);
}

com/bumptech/glide/load/resource/bitmap/BitmapTransitionOptions.java
public final class BitmapTransitionOptions
    extends TransitionOptions<BitmapTransitionOptions, Bitmap> {
 
    public static BitmapTransitionOptions withCrossFade() {
      return new BitmapTransitionOptions().crossFade();
   }
   
   public BitmapTransitionOptions crossFade() {
    //通过DrawableCrossFadeFactory构建
    return crossFade(new DrawableCrossFadeFactory.Builder());
  }
  
  public BitmapTransitionOptions crossFade(@NonNull DrawableCrossFadeFactory.Builder builder) {
    return transitionUsing(builder.build());
  }
}  
```
com/bumptech/glide/request/transition/DrawableCrossFadeFactory.java
```
public class DrawableCrossFadeFactory implements TransitionFactory<Drawable> {
  @Override
  public Transition<Drawable> build(DataSource dataSource, boolean isFirstResource) {
    return dataSource == DataSource.MEMORY_CACHE
        ? NoTransition.<Drawable>get()
        : getResourceTransition();
  }

  private Transition<Drawable> getResourceTransition() {
    if (resourceTransition == null) {
      resourceTransition = new DrawableCrossFadeTransition(duration, isCrossFadeEnabled);
    }
    return resourceTransition;
  }
}
```
DrawableCrossFadeFactory构建的是DrawableCrossFadeTransition

```
public class DrawableCrossFadeTransition implements Transition<Drawable> {
    public boolean transition(Drawable current, ViewAdapter adapter) {
        Drawable previous = adapter.getCurrentDrawable();
        if (previous == null) {
          previous = new ColorDrawable(Color.TRANSPARENT);
        }
        //最终通过TransitionDrawable完成动画
        TransitionDrawable transitionDrawable =
            new TransitionDrawable(new Drawable[] {previous, current});
        transitionDrawable.setCrossFadeEnabled(isCrossFadeEnabled);
        transitionDrawable.startTransition(duration);
        adapter.setDrawable(transitionDrawable);
    return true;
  }
}
```


glide的使用
com/bumptech/glide/RequestBuilder.java   保存配置
```
  public RequestBuilder<TranscodeType> transition(
      @NonNull TransitionOptions<?, ? super TranscodeType> transitionOptions) {
    if (isAutoCloneEnabled()) {
      return clone().transition(transitionOptions);
    }
    this.transitionOptions = Preconditions.checkNotNull(transitionOptions);
    isDefaultTransitionOptionsSet = false;
    return selfOrThrowIfLocked();
  }
```
com/bumptech/glide/request/SingleRequest.java
```
private final TransitionFactory<? super R> animationFactory;
private void onResourceReady(
      Resource<R> resource, R result, DataSource dataSource, boolean isAlternateCacheKey) {
      ...
      if (!anyListenerHandledUpdatingTarget) {
        //使用TransitionFactory构建Transition
        Transition<? super R> animation = animationFactory.build(dataSource, isFirstResource);
        target.onResourceReady(result, animation);
      }
      ...
  }
```
com/bumptech/glide/request/target/ImageViewTarget.java
```
  @Override
  public void onResourceReady(@NonNull Z resource, @Nullable Transition<? super Z> transition) {
    //执行transition动画
    if (transition == null || !transition.transition(resource, this)) {
      setResourceInternal(resource);
    } else {
      maybeUpdateAnimatable(resource);
    }
  }
```