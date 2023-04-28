
RadioButton,CheckBox都最终继承于Button
RadioGroup作为指示器示例
```
 <RadioGroup
          android:orientation="horizontal"
          android:layout_marginTop="10dp"
          android:layout_width="wrap_content"
          android:minHeight="1dp"
          android:layout_margin="0dp"
          android:layout_height="2dp">
        <androidx.appcompat.widget.AppCompatRadioButton
            android:background="@drawable/xg_select_indicator_rect"
            android:button="@null"
            android:layout_width="14dp"
            android:minHeight="1dp"
            android:minWidth="1dp"
            android:layout_height="match_parent"/>
      </RadioGroup>
```

一组按钮，一个选中，其他的非选中
RadioGroup  RadioButton
监听选中状态变化，然后改变样式
RadioButton.setOnCheckedChangeListener


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

button去除阴影
```
//1
android:stateListAnimator="@null"
//2
elevation=0
```

button去掉点击效果
```
1.
android:stateListAnimator="@null"
2.
style="@style/Widget.AppCompat.Button.Borderless"
3.
主题添加
<item name="buttonStyle">?android:attr/borderlessButtonStyle</item>
或者
<item name="buttonStyle">@style/Widget.AppCompat.Button.Borderless</item>-
```
