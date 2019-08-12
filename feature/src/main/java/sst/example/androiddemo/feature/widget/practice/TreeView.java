package sst.example.androiddemo.feature.widget.practice;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import sst.example.androiddemo.feature.util.MyUtils;

import java.util.Iterator;
import java.util.LinkedList;

public class TreeView extends View {
    SnapShot snapShot;
    Paint mPaint;
    LinkedList<Branch> growingBranches = new LinkedList<>();
    private boolean hasEnd;
    //缩放过大时，树枝的末端就是一个个圆点，看起来不好
    float scaleFactor = 1f;

    public TreeView(Context context) {
        this(context,null);
    }

    public TreeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TreeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //数据初始化
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        growingBranches.add(getBranch());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //使用snapShot的canvas绘制，将绘制内容保存在snapShot的bitmap中,下一次的绘制在当前的bitmap基础上绘制
        drawBranches();

        //canvas直接绘制能否保存上一次的效果
//        使用canvas.setBitmap 将内容保存在bitmap
        canvas.drawBitmap(snapShot.bitmap,0,0,mPaint);

        if(!hasEnd){
            invalidate();
        }
    }

    private void drawBranches() {
        LinkedList<Branch> tempBranch = null;
        if(!growingBranches.isEmpty()){
            snapShot.canvas.save();
            snapShot.canvas.translate(getWidth()/2-217*scaleFactor ,getHeight()-490*scaleFactor);
            Iterator<Branch> iterator =  growingBranches.iterator();
            while (iterator.hasNext()){
              Branch branch =   iterator.next();
              if(!branch.grow(snapShot.canvas,mPaint,scaleFactor)){
                  //单个树枝绘制完成,处理子树枝
                  iterator.remove();
                  if(branch.childBranches !=null){
                       if(tempBranch == null){
                           tempBranch = branch.childBranches;

                       }else {
                           tempBranch.addAll(branch.childBranches);
                       }
                  }
              }
            }
            snapShot.canvas.restore();
            if(tempBranch != null){
//                将子树枝加入growingBranches，下一次draw开始绘制
                growingBranches.addAll(tempBranch);
            }
            if(growingBranches.isEmpty()){
                //全部树枝绘制完成
                hasEnd = true;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        snapShot = new SnapShot(Bitmap.createBitmap(getWidth(),getHeight(), Bitmap.Config.ARGB_8888));
    }

    //快照，用来保存上一次的内容
    class  SnapShot{
     Canvas canvas;
     Bitmap bitmap;

        public SnapShot(Bitmap bitmap) {
            this.bitmap = bitmap;
            canvas = new Canvas(bitmap);
        }
    }

    //树干，需要用到贝塞尔曲线
    class Branch{
        public  static final int branchColor = 0XFF775533;

        //贝塞尔 起点，控制点，终点
        Point[] points = new Point[3];
        float radius;
        float maxLength;
        int currentLength;
        //将当前分为多少份  0-maxLength   maxLength越长，消耗的时间越多
        float part;
        float growX,growY;
        LinkedList<Branch> childBranches;
        public Branch(int[] data) {
          points[0]=new Point(data[2],data[3]);
            points[1]=new Point(data[4],data[5]);
            points[2]=new Point(data[6],data[7]);
            radius = data[8];
            maxLength = data[9];
            part=1/maxLength;

        }

        public void addChild(Branch branch) {
            if(childBranches ==null){
                childBranches = new LinkedList<>();
            }
            childBranches.add(branch);
        }

        /**
         *
         * @param canvas
         * @param mPaint
         * @param scaleFactor 缩放
         * @return
         */
        public boolean grow(Canvas canvas,Paint mPaint,float scaleFactor){
           if(currentLength<maxLength){
               //计算当前需要绘制的位置
               bazier(currentLength*part);//currentLength*part == currentLength/maxLength
               //绘制
               drawBranch(canvas,mPaint,scaleFactor);
               //树的半径逐渐减小 下一次是当前的0.97
               radius *=0.97f;
               currentLength++;
               return  true;
           }else
           {
               return  false;
           }
        }

        private void drawBranch(Canvas canvas, Paint mPaint, float scaleFactor) {
            mPaint.setColor(branchColor);
            mPaint.setAntiAlias(true);
            canvas.save();
            canvas.scale(scaleFactor,scaleFactor);
            //移动画布，而不是控制画圆的中心点
            canvas.translate(growX,growY);
            MyUtils.log("growX "+growX+" growY "+growY+" radius "+radius);
            //树干是由一个个圆拼成的
            if(radius<1){
                radius=1;
            }
            canvas.drawCircle(0,0,radius,mPaint);
            canvas.restore();
        }

        private void bazier(float t) {
//          b(t) = (1-t)*(1-t)*P0+2*t*(1-t)*P1+t*t*P2
            float c0 = (1-t)*(1-t);
            float c1 = 2*t*(1-t);
            float c2 = t*t;
            growX = c0*points[0].x+c1*points[1].x+c2*points[2].x;
            growY = c0*points[0].y+c1*points[1].y+c2*points[2].y;
        }
    }

    Branch getBranch(){
         //id parentId, 贝塞尔起点，控制点，中点，最大角度，长度
      int[][] datas = new int[][]{
              {0,-1,217,490,252,60,182,10,30,100},
              {1,0,222,310,137,227,22,210,13,100},
              {2,1,132,245,116,240,76,205,2,40},
              {3,0,232,255,282,166,362,155,12,100},
              {4,3,260,210,330,219,343,236,3,80},
              {5,0,217,91,219,58,216,27,3,40},
              {6,0,228,207,95,57,10,54,9,80},
              {7,6,109,96,65,63,53,15,2,40},
              {8,6,180,155,117,125,77,140,4,60},
              {9,0,228,167,290,62,360,31,6,100},
              {10,9,272,103,328,87,330,81,2,80}
      };
      int n = datas.length;
      Branch[] branches = new Branch[n];
      for (int i=0;i<n;i++){
          int id = datas[i][0];
          int parentId = datas[i][1];
          branches[i] = new Branch(datas[i]);
          if(parentId!=-1){
              //将当前branch加入到父节点
              branches[parentId].addChild(branches[i]);
          }
      }
      return branches[0];//返回要绘制的主干
    }
}
