
RadioButton,CheckBox都最终继承于Button

RadioButton
单选，只有一个按钮时，选中后不能取消了

//文字和图片不同位置  android:drawableBottom=""
CheckBox 可以多选，只有一个时选中后可以取消
```
<androidx.appcompat.widget.AppCompatCheckBox
            app:layout_constraintTop_toBottomOf="@id/login_btn_getSms"
            app:layout_constraintStart_toStartOf="@id/login_btn_getSms"
            android:button="@drawable/xg_selector_login_agreement"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>
```

wrap_content或者小尺寸不生效
一般主题有默认大小，小尺寸不生效
可以手动设置
```
android:minWidth=1dp

默认主题，前提对应主题没有重写minWidth属性
<item name="android:minHeight">1dp</item>
<item name="android:minWidth">1dp</item>
```