package sst.example.androiddemo.feature.Animation.evaluator;

import static android.graphics.Color.BLUE;
import static android.graphics.Color.RED;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PointFEvaluator;
import android.animation.RectEvaluator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Point;
import android.os.Bundle;
import android.widget.TextView;

import sst.example.androiddemo.feature.R;

public class TypeEvaluatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type_evaluator);
        //估值器   Evaluator [ɪˈvæljuˌeɪtər] 评估者;评估器;评估人;评估员;直译器
//        https://juejin.cn/post/7000339881402564644
//        TypeEvaluator是一个接口，在开发中可以自定义该接口实例，利用ValueAnimator的setEvaluator(TypeEvaluator)
//         方法来控制动画的更新计算表达式。在日常开发中，不可能只是需要操纵单一数值的变化，如果需要同时操纵对象的多个属性，
//         如定义动画的x，y移动的坐标等，那就需要对TypeEvaluator有所了解了
//        系统已有的估值器：
//        IntEvaluator：针对整型属性
//        FloatEvaluator：
//        针对浮点型属性
//        ArgbEvaluator：针对Color属性
//                针对坐标的
//        PointFEvaluator  更改point坐标的
//        RectEvaluator    更改rect的

        TextView tv = findViewById(R.id.typeEvaluatorTV);
        ValueAnimator colorAnim = ObjectAnimator.ofInt(tv, "textColor", RED, BLUE);
        colorAnim.setDuration(4000);
        //测试ArgbEvaluator
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();

        tv.post(new Runnable() {
            @Override
            public void run() {
                int width = getWindow().getDecorView().getWidth();
                //测试MPointEvaluator
                Point point = new Point();
                ValueAnimator leftAnim = ObjectAnimator.ofObject(new MPointEvaluator(point),new Point(0,0),new Point(width/2,0));
                leftAnim.setDuration(2000);
                leftAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Point vPoint = (Point) animation.getAnimatedValue();
                        tv.setX(vPoint.x);
                    }
                });
                leftAnim.setRepeatCount(ValueAnimator.INFINITE);
                leftAnim.setRepeatMode(ValueAnimator.REVERSE);
                leftAnim.start();
            }
        });


    }

    //自定义Evaluator
    public class MPointEvaluator implements TypeEvaluator<Point> {
        Point point;

        //构造器传入point可以重用对象
        public MPointEvaluator(Point point) {
            this.point = point;
        }

        @Override
        public Point evaluate(float fraction, Point startValue, Point endValue) {
            point.x = (int) (startValue.x+(endValue.x-startValue.x)*fraction);
            point.y = (int) (startValue.y+(endValue.y-startValue.y)*fraction);
            return point;
        }
    }
}