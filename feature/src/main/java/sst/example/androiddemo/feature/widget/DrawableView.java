package sst.example.androiddemo.feature.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.graphics.BitmapActivity;

public class DrawableView extends View {
    //    常用状态
    public static final int[] STATE_DEFAULT = new int[0];
    // View.PRESSED_ENABLED_STATE_SET
    public static final int[] STATE_PRESSED = new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled};
    public static final int[] STATE_SELECTED = new int[]{android.R.attr.state_selected};
    public static final int[] STATE_CHECKED = new int[]{android.R.attr.state_checked};
    public static final int[] STATE_UNCHECKED = new int[]{-android.R.attr.state_checked};
    public static final int[] STATE_DISABLED = new int[]{-android.R.attr.state_enabled};
    //    View.FOCUSED_STATE_SET
    public static final int[] STATE_FOCUSED = new int[]{android.R.attr.state_focused};
    //    View.EMPTY_STATE_SET;
    public static final int[] STATE_EMPTY = new int[]{};
    BitmapDrawable bd;
    //不建议使用，代码生成的.9图系统编译不会进行优化
    NinePatchDrawable np;
    ShapeDrawable sd = new ShapeDrawable();

    public DrawableView(Context context) {
        this(context, null);
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Bitmap bitmap = BitmapActivity.getBitmap(context, R.mipmap.ic_launcher);
        bd = new BitmapDrawable(context.getResources(), bitmap);
        bd.setTintMode(PorterDuff.Mode.ADD);
        bd.setAlpha(100);
        bd.setAntiAlias(true);
        bd.setDither(true);
        bd.setGravity(Gravity.LEFT);
        bd.setTint(Color.RED);//设置着色    Android21(5.0)后加入，可以给图片上色
        bd.setTintMode(PorterDuff.Mode.ADD);//设置着色模式
        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
//        xml 定制查看：
//        R.drawable.bitmap_drawable;
        setBackground(bd);


//        ShapeDrawable对于Xml的shape标签，
//        xml的根标签是shape对应的是 GradientDrawable
        Shape shape = new RectShape();
        shape.resize(500, 500);
        //子类有 PathShape    OvalShape  ArcShape  RoundRectShape
        sd.setShape(shape);
        sd.setDither(true);
        sd.setAlpha(100);
        //修改样式
        sd.getPaint().setStrokeWidth(20);

//        GradientDrawable使用shape作为根节点创建xml文件
//        源码中将shape标签编译为GradientDrawable
        setBackground(context, R.drawable.shape_drawable);


//        一个LayerDrawable是一个可以管理一组drawable对象的drawable。在LayerDrawable的drawable资源按照列表的顺序绘制，列表的最后一个drawable绘制在最上层
        //对应xml标签layer-list
        setBackground(context, R.drawable.layer_drawable);
        LayerDrawable ld = (LayerDrawable) getBackground();
        ld.setAlpha(200);
        ld.setDither(true);
        //获取layer-list的item 可以通过ID获取
        GradientDrawable gd = (GradientDrawable) ld.findDrawableByLayerId(R.id.layer_drawable_item1);
//        GradientDrawable 表示一个渐变区域，可以实现线性渐变、发散渐变和平铺渐变效果
//       其对应的标签为<shape>   一般都是配置shapeDrawable来使用，为其实现渐变颜色
//        GradientDrawable还可以设置边框，圆角，边框宽度
        gd.setCornerRadius(50);
        gd.setStroke(2, Color.RED, 10f, 5f);


//        StateListDrawable对于xml的<selector>标签
        setBackground(context, R.drawable.statelist_drawable);
        StateListDrawable sld = new StateListDrawable();
        sld.addState(STATE_DISABLED, ContextCompat.getDrawable(context, R.drawable.ic_launcher));

//        ColorStateList
//       1 xml定义 res/color/color_statelist.xml  跟标签是selector
//       2 代码定义 state数组和颜色数组
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
        };

        int[] colors = new int[]{
                Color.BLACK,
                Color.RED,
                Color.GREEN,
                Color.BLUE
        };
        ColorStateList cs = new ColorStateList(states, colors);
        cs = ContextCompat.getColorStateList(context, R.color.color_statelist);
        TextView tv = new TextView(context);
        tv.setTextColor(cs);

        //对图片着色
        Drawable wrapDrawable = DrawableCompat.wrap(sld);
        //兼容21以下的drawable
        DrawableCompat.setTintList(wrapDrawable, cs);
        setBackground(sld);


//        LevelListDrawable对应于<level-list>标签，也表示一个Drawable的集合，但集合中的每个Drawable都一个等级。根据不同等级，LevelListDrawable会切换到相应的Drawable
//       xml配置   R.drawable.levelist_drawable
        LevelListDrawable lld = new LevelListDrawable();
        lld.addLevel(1, 1, ContextCompat.getDrawable(context, R.drawable.levelist_drawable));
        lld.addLevel(0, 0, ContextCompat.getDrawable(context, R.drawable.dog));
        setBackground(lld);
        ImageView iv = new ImageView(context);
        iv.setBackground(lld);
        //切换level
        iv.setImageLevel(1);


//       TransitionDrawable用于实现两个Drawable之间的淡入淡出的效果，它对应的是<transition>标签
        setBackground(ContextCompat.getDrawable(context, R.drawable.transition_drawable));
        TransitionDrawable td = (TransitionDrawable) getBackground();
        td.setCrossFadeEnabled(true);
        td.startTransition(2000);

//      InsetDrawable  此 XML 文件用于定义以指定距离插入其他可绘制对象的可绘制对象。当视图需要小于视图实际边界的背景可绘制对象时，此类可绘制对象很有用
        setBackground(context, R.drawable.inset_drawable);
        InsetDrawable id = (InsetDrawable) getBackground();


//        ScaleDrawable对应<scale>标签，主要基于当前的level，对指定的Drawable进行缩放操作
//        要将其显示出来的话，必须给ScaleDrawable设置一个大于0小于10000的等级（级别越大Drawable显示得越大，等级为10000时就没有缩放效果了），否则将无法正常显示。draw里面进行了判断
        setBackground(context, R.drawable.scale_drawable);
        ScaleDrawable scaleDrawable = (ScaleDrawable) getBackground();
        scaleDrawable.setLevel(1);

//        ClipDrawable是通过设置一个Drawable的当前显示比例来裁剪出另一张Drawable
//        图片不能是9图，因为这涉及到裁剪这张图片，如果设置为九图，裁剪的实际情况会与想要的效果不一样
//        ClipDrawable的level值范围在[0,10000]，level的值越大裁剪的内容越少，当level为10000时则完全显示，而0表示完全裁剪，不可见
        setBackground(context, R.drawable.clip_drawable);
        final ClipDrawable cd = (ClipDrawable) getBackground();
        cd.setLevel(level);
        //绘制徐徐展开的图片
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (level <= 10000) {
                    level += 200;
                    cd.setLevel(level);
                    handler.sendEmptyMessageDelayed(0, 50);
                }
            }
        };
        handler.sendEmptyMessage(0);


        //ColorDrawable 是最简单的Drawable，它实际上是代表了单色可绘制区域，它包装了一种固定的颜色，当ColorDrawable被绘制到画布的时候会使用颜色填充Paint，在画布上绘制一块单色的区域。 在xml文件中对应<color>标签，它只有一个android:color属性，通过它来决定ColorDrawable的颜色
        setBackground(context, R.drawable.color_drawable);
        ColorDrawable colorDrawable = (ColorDrawable) getBackground();
        colorDrawable.setAlpha(100);


//        AnimationDrawable   xml形式实现帧动画 frame-by-frame   会一次性把所有图片加载到内存中,可能会造成oom
        setBackground(context, R.drawable.animation_drawable);
        AnimationDrawable ad = (AnimationDrawable) getBackground();
        ad.start();

//       VectorDrawable使用 vector根标签     在21以下使用，gradle中配置vectorDrawables.useSupportLibrary = true      xml中使用 app:srcCompat
//        VectorDrawableCompat 兼容类
        setBackground(context, R.drawable.ic_android_black_24dp);
        VectorDrawable vectorDrawable = (VectorDrawable) getBackground();

//        AnimatedVectorDrawable vector动画  对应xml标签 animated-vector
//        AnimatedVectorDrawableCompat 兼容类
        setBackground(context, R.drawable.ainmated_vector_drawable);
        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) getBackground();
        avd.start();

        //使用animated-selector标签
        setBackground(context, R.drawable.animated_selector);


//       AdaptiveIconDrawable 使用标签 adaptive-icon
//        两张图层大小都必须为 108 x 108 dp。图层中心 72 x 72 dp 范围为可视范围。系统会保留四周外的 36dp 范围用于生成有趣的视觉效果（如视差效果和跳动）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AdaptiveIconDrawable aid = new AdaptiveIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background), ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground));
//            setBackground(aid);
        }


//       drawable        something that can be drawn  描述的是一些可以被画出来的东西
//        使用简单，比自定义View成本低
//        非图片类的Drawable所占空间小，能减小apk大小
//        自定义drawable
//        setBackground(new CustomDrawable());

//        使这个drawable变得状态不定。这个操作不能还原（变为不定后就不能变为原来的状态）。
//        一个状态不定的drawable可以保证它不与其他任何一个drawabe共享它的状态。这对于你需要更改从同一资源加载来的drawable的属性时非常有用。
//        默认情况下，所有的从同一资源（R.drawable.XXX）加载来的drawable实例都共享一个共用的状态，如果你更改一个实例的状态，其他所有的实例都会收到相同的通知
//        如果把这个drawable变为mutate drawable后，这个drawable就不会与其他drawable共享状态，这个mutate操作是不可逆转的
        Drawable drawable = null;
        if (drawable != null) {
            drawable.mutate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    int level = 0;
    Handler handler;

    private void setBackground(Context context, int statelist_drawable) {
        setBackground(ContextCompat.getDrawable(context, statelist_drawable));
    }


    class CustomDrawable extends Drawable {
        @Override
        protected boolean onLevelChange(int level) {
            //level changge
            return super.onLevelChange(level);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            //bounds change
            super.onBoundsChange(bounds);
        }

        @Override
        public boolean onLayoutDirectionChanged(int layoutDirection) {
//            layoutDirection 改变
            return super.onLayoutDirectionChanged(layoutDirection);
        }

        private Paint mPaint = new Paint();

        public CustomDrawable() {
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.FILL);
            //动画监听
            setCallback(new Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable who) {

                }

                @Override
                public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

                }

                @Override
                public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

                }
            });
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, 500, mPaint);

        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
