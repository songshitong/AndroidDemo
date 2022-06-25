

AppBarLayout配合其他布局做效果，本身没有内容，例如MaterialToolbar负责显示内容
https://github.com/material-components/material-components-android/blob/master/docs/components/TopAppBar.md
```
 <com.google.android.material.appbar.AppBarLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">
            <com.google.android.material.appbar.MaterialToolbar
                android:id="@+id/topAppBar"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                app:title="@string/audio_record"
                app:titleTextColor="@color/black"
                app:titleTextAppearance="@style/AppBarText"
                app:titleCentered="true"
                app:navigationIcon="@drawable/icon_arrow_back" />
        </com.google.android.material.appbar.AppBarLayout>
```



https://juejin.cn/post/6844903975024590861
结构
TabLayout继承自 HorizontalScrollView
{TabView TabView TabView(taview内容取自TabItem) }

tabLayout动态设置item文字
tabLayout.getTabAt(binding.audioRecordTabSelect.getTabCount()-1).setText(Html.fromHtml(getFailText()));
