
进度条模式
progressDrawable  设置样式

不确定模式
//改变颜色
android:indeterminateTint="@color/ca_blue_1a66ff" 
android:indeterminateTintMode="src_in"
自定义旋转图片
indeterminateDrawable  xml中为rotate



SeekBar去除左右内边距
android:paddingStart=”0dp” android:paddingEnd=”0dp”
https://stackoverflow.com/questions/3333658/how-to-make-a-vertical-seekbar-in-android
SeekBar由横向变为竖直SeekBar
```
<SeekBar
android:id="@+id/seekBar1"
android:layout_width="match_parent"
android:layout_height="wrap_content"
android:rotation="270"/>
```