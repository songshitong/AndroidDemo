


scaleType
matrix使用举例  https://www.jianshu.com/p/5070854ef591
例如我们想让一张图宽度与屏幕保持一致，高度等比放缩，并且顶部与ImageView顶部对齐。这种方式不能通过给定的默认方式做到
```
 public Point size = new Point();
 
 //获取屏幕的尺寸
 WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
 wm.getDefaultDisplay().getSize(size);

 //屏幕宽度
 int screenWidth = size.x;

 //获取图片的原始宽度
 BitmapFactory.Options options = new BitmapFactory.Options();
 options.inJustDecodeBounds = true;
 BitmapFactory.decodeResource(getResources(), R.drawable.letter_editor_background, options);
 int imageWidth = options.outWidth;
 
 float scaleRate = windowWidth * 1.0f / imageWidth;
 
 //设置matrix
 Matrix matrix = new Matrix();
 
 //设置放缩比例
 matrix.setScale(1, scaleRate); //宽度不变，高度缩放
 
 ivBackground.setScaleType(ImageView.ScaleType.MATRIX)//xml中设置也可以
 ivBackground.setImageMatrix(matrix);
```
FIT_XY  drawable拉伸
MATRIX  使用自定义的matrix
CENTER  矩阵平移到中间
CENTER_CROP 先缩放后平移
CENTER_INSIDE 缩放平移
```
private void configureBounds() {
       ...
        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
            mDrawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            mDrawable.setBounds(0, 0, dwidth, dheight);

            if (ScaleType.MATRIX == mScaleType) {
                // Use the specified matrix as-is.
                if (mMatrix.isIdentity()) {
                    mDrawMatrix = null;
                } else {
                    mDrawMatrix = mMatrix;
                }
            } else if (fits) {
                // The bitmap fits exactly, no transform needed.
                mDrawMatrix = null;
            } else if (ScaleType.CENTER == mScaleType) {
                // Center bitmap in view, no scaling.
                mDrawMatrix = mMatrix;
                mDrawMatrix.setTranslate(Math.round((vwidth - dwidth) * 0.5f),
                                         Math.round((vheight - dheight) * 0.5f));
            } else if (ScaleType.CENTER_CROP == mScaleType) {
                mDrawMatrix = mMatrix;

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight;
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(Math.round(dx), Math.round(dy));
            } else if (ScaleType.CENTER_INSIDE == mScaleType) {
                mDrawMatrix = mMatrix;
                float scale;
                float dx;
                float dy;

                if (dwidth <= vwidth && dheight <= vheight) {
                    scale = 1.0f;
                } else {
                    scale = Math.min((float) vwidth / (float) dwidth,
                            (float) vheight / (float) dheight);
                }

                dx = Math.round((vwidth - dwidth * scale) * 0.5f);
                dy = Math.round((vheight - dheight * scale) * 0.5f);

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(dx, dy);
            } else {
                // Generate the required transform.
                mTempSrc.set(0, 0, dwidth, dheight);
                mTempDst.set(0, 0, vwidth, vheight);

                mDrawMatrix = mMatrix;
                mDrawMatrix.setRectToRect(mTempSrc, mTempDst, scaleTypeToScaleToFit(mScaleType));
            }
        }
    }
```