
RadioButton
单选，只有一个按钮时，选中后不能取消了

CheckBox 可以多选，只有一个时选中后可以取消
```
<androidx.appcompat.widget.AppCompatCheckBox
            app:layout_constraintTop_toBottomOf="@id/login_btn_getSms"
            app:layout_constraintStart_toStartOf="@id/login_btn_getSms"
            android:button="@drawable/xg_selector_login_agreement"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>
```