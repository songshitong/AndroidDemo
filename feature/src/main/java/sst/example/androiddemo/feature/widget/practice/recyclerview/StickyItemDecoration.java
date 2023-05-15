package sst.example.androiddemo.feature.widget.practice.recyclerview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import sst.example.androiddemo.feature.util.MyUtils;

import java.util.List;
import java.util.logging.Logger;

//创建recyclerview的吸顶效果
public class StickyItemDecoration extends RecyclerView.ItemDecoration {
    Paint mPaint;
    Paint textPaint;
    Rect rect;
    List<ItemBean> itemBeans;
    public StickyItemDecoration(List<ItemBean> itemBeans) {
        super();
        this.itemBeans = itemBeans;
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        rect = new Rect();

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(60);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);

    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        //1 给每一个item上方绘制条
        //2 同一组不进行绘制，前一个后一个group不同   与上一个view的group绘制，group不进行缓存，有循环会发生后面覆盖前面
        //3 滚动固定 item+固定条高度到recyclerview顶部高度小于0 ->item的top<固定条高度

        //4 出现问题，同一组要维持一个固定条 ---》前面可见position为0和后面可见组第一个才绘制固定条
        //获取可见view
        int visibleCount = parent.getChildCount();
        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        for (int i = 0; i < visibleCount; i++) {
            final  View itemView = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(itemView);
            int currentGroup = itemBeans.get(position).group;
            //与下一个比较
            int nextGroup = -1;//默认group不属于任何一组
            if(position<itemBeans.size()-1){
                nextGroup = itemBeans.get(position+1).group;
            }
            if(position == layoutManager.findFirstVisibleItemPosition() ||isGroupFirst(position,itemBeans)){
                MyUtils.log("text "+itemBeans.get(position).text+" nextGroup "+nextGroup+" currentGroup "+currentGroup);
                int itemTop = itemView.getTop();

                int rectTop = itemTop-100;
                int rectBottom = itemTop;
                if(itemTop <100){
                    rectTop =0;
                    rectBottom =100;
                    //第一组itemTop
                }

                //顶上去的操作
                if(nextGroup != currentGroup){
                    View nextView = layoutManager.findViewByPosition(position+1);
                    int nextRectTop = nextView.getTop();
                    if(nextRectTop<100*2 ){
                        //改变前一组  currentGroup
                        rectTop =nextRectTop-100*2;
                        rectBottom = nextRectTop-100;
                    }
                }


                rect.set(0,rectTop,parent.getWidth(),rectBottom);
                c.drawRect(rect,mPaint);
                //绘制文字
                c.drawText("group "+itemBeans.get(position).group,rect.left,rect.bottom-rect.height()/2,textPaint);
            }

        }

    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        //item的偏移
        int position = parent.getChildAdapterPosition(view);
        if (isGroupFirst(position,itemBeans)){
            outRect.top=100;
        }
    }

    //判断该position不是其group的第一个
    //在itemBeans中group第一个的text最后一个字符是1
    boolean isGroupFirst(int position,List<ItemBean> itemBeans){
        String text = itemBeans.get(position).text;
        return text.charAt(text.length()-1) == "0".toCharArray()[0];
    }

}
