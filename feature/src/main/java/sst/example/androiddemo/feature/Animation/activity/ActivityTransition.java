package sst.example.androiddemo.feature.Animation.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.ChangeClipBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import sst.example.androiddemo.feature.R;

public class ActivityTransition extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        https://blog.csdn.net/u010126792/article/details/85786790
//        利用overridePendingTransition实现转场动画可以在android2.0开始使用，google在Android5.0提出了MD的设计风格，
//        并扩充了Activity的转场动画，利用ActivityOptions和ActivityOptionsCompat可以实现新风格的转场动画。
//        ActivityOptionsCompat是ActivityOptions的兼容包，虽然是兼容包但内部有的动画风格在5.0以下的版本依然不支持，
//        只不过坚持在低版本使用ActivityOptionsCompat的动画不会出错
//         ActivityOptions 提供的静态方法
//        makeCustomAnimation(Context context, int enterResId, int exitResId)
//            用户自定义动画，指定进入和退出动画，api16开始支持
//            参数说明：
//            enterResId:Activity进入动画资源id
//            exitResId：Activity退出动画资源id
//        makeClipRevealAnimation (View source, int startX, int startY, int width, int height)
//            从一个view的剪切区域放大然后打开新的Activity，Api23开始支持，
//            参数说明：
//            startX,startY:区域起点，利用source作为原点
//            width，height：区域宽高
//        makeScaleUpAnimation(View source, int startX, int startY, int width, int height)
//            放大一个view，然后显示新的Activity
//            参数说明：
//            view：放大的view
//            startX，startY：从哪里开始缩放，以source为原点
//            width，height：新的activity从多大开始放大，如果是0,0则表示从最小开始
//        makeThumbnailScaleUpAnimation(View source, Bitmap thumbnail, int startX, int startY)
//            放大一张图片，然后打开activity
//            参数说明:
//            source：参考原点
//            thumbnail:要放大的图片
//            startX,startY:从哪里开始放大，以source为坐标原点
//        makeSceneTransitionAnimation(Activity activity, View sharedElement, String sharedElementName)
//        makeSceneTransitionAnimation(Activity activity, Pair…<View, String> sharedElements)
//             共享元素，可以有一个共享元素，也可以有多个  设置共享的view和transitionName


//        https://blog.csdn.net/u010126792/article/details/85794815
//        Trasition 内部保存了所有关于动画的信息，当场景变换时动画将在目标对象上执行。Transition基于两个概念：
//        场景（scenes）和变换（transitions），场景是UI当前状态，变换则定义了在不同场景之间动画变化的过程。
//
//        Transition分为三种类型（android5.0中使用）。
//        进入动画：Activity中的所有视图进入屏幕的动画。
//        退出动画：Activity中的所有视图退出屏幕的动画。
//        共享元素动画：利用共享的元素实现Activity的跳转动画。
//        进入动画和退出动画合称Content Transition（内容变换动画），所以Transition分为内容变换动画和共享元素动画。
//
//        当一个场景改变的时候，transition主要负责：
//           捕捉每个View在开始场景和结束场景时的状态，根据两个场景（开始和结束）之间的区别创建一个Animator，
//           所以Transition内部利用了属性动画。

//        如何在Activity中使用Transition
//            启用activity过度 Window.FEATURE_ACTIVITY_TRANSITIONS
//          启用窗口内容过渡
//          代码： getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//          xml:<item name="android:windowContentTransitions">true</item>
//        Fragment中使用Transition
//        和activity中使用大体相同，不同点如下：
//        Fragment在其FragmentTransaction执行added, removed, attached, detached, shown, ，hidden时触发动画。
//        在Fragment commit之前，共享元素需要通过调用addSharedElement(View, String) 方法来成为FragmentTransaction的一部分。

//        Content Transition
//        content transition决定了非共享view元素在activity和fragment切换期间是如何进入或者退出场景的
//        代码设置   启动activity要设置ActivityOptionsCompat.makeSceneTransitionAnimation！！！
//        设置Content Transition的函数：
//        getWindow().setEnterTransition() - ActivityA 跳转到 ActivityB，ActivityB中的View进入场景的transition。  在B中设置
//        getWindow().setExitTransition() - ActivityA 跳转到 ActivityB，ActivityA中的View退出场景的transition。  在A中设置
//        getWindow().setReturnTransition() - 从ActivityB 返回Activity A时，ActivityB中的View退出场景的transition。 在B中设置
//        getWindow().setReenterTransition() - 从ActivityB 返回Activity A时，ActivityA中的View进入场景的transition 在A中设置
//        xml设置
//        <item name="android:windowEnterTransition"></item>
//        <item name="android:windowExitTransition"></item>
//        <item name="android:windowReturnTransition"></item>
//        <item name="android:windowReenterTransition"></item>

//        系统提供的动画效果（用于content Transition）  这几个都是Transition的子类
//        explode(分解)：从屏幕中间进或出，移动视图。
//           代码:TransitionInflater.from(this).inflateTransition(android.R.transition.explode)或者newExplode()
//             xml设置:
//           <?xml version="1.0" encoding="utf-8"?>
//            <transitionSet xmlns:android="http://schemas.android.com/apk/res/android">
//            <explode
//                android:duration="500"
//                android:interpolator="@android:interpolator/accelerate_decelerate"/>
//            </transitionSet>
//
//       slide(滑动)：从屏幕边缘进或出，移动视图。
//           <slide xmlns:android="http://schemas.android.com/apk/res/android" android:slideEdge="bottom"/>
//        fade(淡出)：通过改变屏幕上的视图的不透明度，达到添加或移除视图
//            <fade xmlns:android="http://schemas.android.com/apk/res/android"/>

//         代码实现示例  super.onCreate前设置
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        Explode explode = new Explode();
        explode.setDuration(1000);
        getWindow().setEnterTransition(explode);
        getWindow().setExitTransition(explode);
        Transition transition1 = TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right);
        getWindow().setReenterTransition(transition1);
        Transition transition2 = TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right);
        getWindow().setReturnTransition(transition2);


        //共享元素动画
        //        共享元素动画  只有共享元素也要开启transition
//        getWindow().setSharedElementEnterTransition();ActivityA 跳转到 ActivityB，ActivityB进入动画
//        getWindow().setSharedElementExitTransition();ActivityA 跳转到 ActivityB，ActivityA退出动画
//        getWindow().setSharedElementReenterTransition();从ActivityB 返回Activity A时，ActivityA进入场景的transition。
//        getWindow().setSharedElementReturnTransition();从ActivityB 返回Activity A时，ActivityB退出场景的transition
//        <item name="android:windowSharedElementEnterTransition"></item>
//        <item name="android:windowSharedElementExitTransition"></item>
//        <item name="android:windowSharedElementReenterTransition"></item>
//        <item name="android:windowSharedElementReturnTransition"></item>
//        系统提供的自定义动画
//        changeBounds:改变目标视图的布局边界，也就是改变view的宽高或者位置。
//          代码： ChangeBounds -捕获共享元素的layout bound，然后播放layout bound变化动画。ChangeBounds 是共享元素变换中用的最多的，
//            因为前后两个activity中共享元素的大小和位置一般都是不同的
//          xmL:
//            <transitionSet xmlns:android="http://schemas.android.com/apk/res/android">
//                <changeBounds
//                    android:duration="1000"
//                    android:interpolator="@android:interpolator/accelerate_decelerate" />
//            </transitionSet>
//        changeClipBounds:裁剪目标视图边界，view的裁剪区域边界。
//          代码：捕获共享元素clip bounds，然后播放clip bounds变化动画，view的裁剪区域边界
//        changeTransform:改变目标的缩放比例和旋转角度，对view进行缩放，旋转操作。
//          代码：ChangeTransform - 捕获共享元素的缩放（scale）与旋转（rotation）属性 ，然后播放缩放（scale）与旋转（rotation）属性变化动画
//        changeImageTransform:改变目标图片的大小和缩放比例，也就是改变图片的ScaleType。
//          代码：捕获共享元素（ImageView）的transform matrices 属性，然后播放ImageViewtransform matrices 属性变化动画。
//            与ChangeBounds相结合，这个变换可以让ImageView在动画中高效实现大小，形状或者ImageView.ScaleType 属性平滑过度
//        overlay：覆盖   默认为true，共享元素动画期间使用overlay，false使用the normal View hierarchy
//           getWindow().setSharedElementsUseOverlay(true)
//        arcMotion：圆弧效果
//             代码:  a=ArcMotion()  Transition.setPathMotion(a)
//           xmL:   <changeBounds>
//                <arcMotion android:minimumHorizontalAngle="15"
//                   android:minimumVerticalAngle="0"
//                   android:maximumAngle="90"/>
//              </changeBounds>
//
//        两个共享元素的要进行name标记
//        android:transitionName="shareElement"
//         启动activity要设置ActivityOptionsCompat.makeSceneTransitionAnimation的共享的view和transitionName
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(1000);
        ChangeClipBounds changeClipBounds = new ChangeClipBounds();
        changeClipBounds.setDuration(1000);
        ChangeImageTransform changeImageTransform = new ChangeImageTransform();
        changeImageTransform.setDuration(1000);
        ChangeTransform changeTransform = new ChangeTransform();
        changeTransform.setDuration(1000);
        getWindow().setSharedElementEnterTransition(changeBounds);
        getWindow().setSharedElementExitTransition(changeClipBounds);
        getWindow().setSharedElementReenterTransition(changeImageTransform);
        getWindow().setSharedElementReturnTransition(changeTransform);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transition);
        Button button = findViewById(R.id.activityTransition2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ActivityTransition.this, ActivityTransition2.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(ActivityTransition.this).toBundle());
            }
        });

        Button buttonShared = findViewById(R.id.activityTransition3);
        ImageView imageView = findViewById(R.id.sharedElementIV);
        buttonShared.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ActivityTransition.this, ActivityTransition3.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(ActivityTransition.this,imageView,"shareElement").toBundle());
            }
        });

//        https://blog.csdn.net/u010126792/article/details/86012113
//        Transition内部使用了属性动画实现，所以它可以认为是属性动画的封装。Transition两个核心概念为：场景（scenes）
//        和变换（transitions），场景是UI当前状态，变换则定义了在不同场景之间动画变化的过程。
//        所以Transition主要负责两个方面的事，一是保存开始和结束场景的两种状态，二是在两种状态之间创建动画。
//         由于场景记录了内部所有View的开始和结束状态，所以Transition动画更具连贯性。谁执行动画呢？TransitionManager负责执行动画的任务
//        Scene 场景
//        Scene 场景 场景过渡动画就是实现View从一种状态变化到另外一种状态，Scene就代表一个场景，它内部保存一个完整地视图结构，
//         从根ViewGroup到所有子view，还有它们的所有状态信息。所以Scene最终就一个设置了不同属性特征的ViewGroup。
//        Scene(ViewGroup sceneRoot)/Scene(ViewGroup sceneRoot, View layout)
//             利用Scene构造函数生成Scene时，需要sceneRoot,sceneRoot在动画开始时，会将sceneRoot中的所有子View都remove掉，
//             然后在sceneRoot中加载结束场景。通过代码new Scene(mSceneRoot, view)生成Scene，view必须是sceneRoot的直接子view，
//             或者view是没有parentview的，不然在addview的时候会报错。

//        Scene.getSceneForLayout(ViewGroup sceneRoot, int layoutId, Context context)
//          sceneRoot:根ViewGroup，内部包含多个场景viewGroup。
//          layoutId:view的布局文件资源id，代表一个场景。

//        TransitionManager在场景变换时控制transitions的执行。通过TransitionManager可以添加场景和Transition变换
//        TransitionManager当场景变换时开启动画的方式：
//        beginDelayedTransition(ViewGroup sceneRoot, Transition transition)
//        beginDelayedTransition(ViewGroup sceneRoot)
//        场景变幻时传入场景的view根sceneRoot，和transition动画。如果不指定Transition，默认为AutoTransition。
//        go函数需要生成对应的Scene，beginDelayedTransiton则不需要，只需要填入sceneRoot和Transition就可以实现Transition动画。
//        这个函数里面没有Scene，那它何时执行动画呢，很简单当view的某些属性信息改变时，就会执行动画
//        执行TransitionManager.beginDelayedTransition后，系统会保存一个当前视图树状态的场景，修改view的属性信息，
//           在下一次绘制时，系统会自动对比之前保存的视图树，然后执行一步动画
//           重要提醒：如果想让beginDelayedTransition有效果，必须每次改变视图属性之后，重新调用beginDelayedTransition，
//           或者改变之前调用beginDelayedTransition，这样才能够保存当前view的状态，否则存储的属性没有改变，不会有动画效果


//        go(Scene scene, Transition transition)
//        go(Scene scene)
//        go的方式需要传入scene，scene由Scene利用view生成。如果不指定Transition，则默认为AutoTransition
//
//        Transition的addTarget（），removeTarget（），只对某些view做动画，或者不对某些view做动画。
//        如果调用了addTarget则只对调用了这个函数的View做动画，其他View直接完成最终状态，
//        如果调用了removeTarget则是对没有调用这个函数的其他view做动画。如果同时调用了两个函数，
//        则调用removeTarget会从调用了addTarget中的view查找，然后剔除

        //TransitionManager.go
        //对一个布局文件的两个view分别生成scene  也可以对多个布局文件生成Scene
        FrameLayout mContainer = findViewById(R.id.viewcontainer);
        ImageView imageView1 = mContainer.findViewById(R.id.imageview1);
        ImageView imageView2 = mContainer.findViewById(R.id.imageview2);
        Scene scene1 = new Scene(mContainer, imageView1);
        Scene scene2 = new Scene(mContainer, imageView2);

        final boolean[] togger = {false};
        mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (togger[0]){
                    //展示场景2
                    Transition transition = new Fade();
                    transition.setDuration(1000);
                    TransitionManager.go(scene2,transition);

                }else{
                    //展示场景1
                    Transition transition = new Fade();
                    transition.setDuration(1000);
                    TransitionManager.go(scene1,transition);
                }
                togger[0] = !togger[0];
            }
        });


        FrameLayout mContainer2 = findViewById(R.id.viewcontainer2);
        ImageView imageview3 = mContainer2.findViewById(R.id.imageview3);
        final boolean[] togger2 = {false};
        mContainer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transition transition = new ChangeBounds();
                transition.setDuration(1000);
                TransitionManager.beginDelayedTransition(mContainer2,transition);
                if (togger2[0]){
                    FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) imageview3.getLayoutParams();
                    layoutParams1.height =100;
                    layoutParams1.width =100;
                    imageview3.setLayoutParams(layoutParams1);
                }else{
                    FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) imageview3.getLayoutParams();
                    layoutParams2.height = 700;
                    layoutParams2.width = 700;
                    imageview3.setLayoutParams(layoutParams2);
                }
                togger2[0] = !togger2[0];

            }
        });
        Button btn = findViewById(R.id.ChangeImageTransformBtn);
        final boolean[] togger3 = {false};
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transition transition = new ChangeImageTransform();
                transition.setDuration(1000);
                TransitionManager.beginDelayedTransition(mContainer2,transition);
                if (togger3[0]){
                    imageview3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }else{
                    imageview3.setScaleType(ImageView.ScaleType.FIT_XY);
                }
                togger3[0] = !togger3[0];
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //结束设置  退出时实现进入的反转
        ActivityCompat.finishAfterTransition(this);
    }
}