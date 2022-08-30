

appcompat-1.2.0\res\values\values.xml
```
<style name="Theme.AppCompat" parent="Base.Theme.AppCompat"/>

 <style name="Base.Theme.AppCompat" parent="Base.V7.Theme.AppCompat">
    </style>
 
 <style name="Base.V7.Theme.AppCompat" parent="Platform.AppCompat">
        <item name="windowNoTitle">false</item>
        <item name="windowActionBar">true</item>
        <item name="windowActionBarOverlay">false</item>
 ...
 
 <style name="Platform.AppCompat" parent="android:Theme.Holo">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>

        <item name="android:buttonBarStyle">?attr/buttonBarStyle</item>
        <item name="android:buttonBarButtonStyle">?attr/buttonBarButtonStyle</item>
        <item name="android:borderlessButtonStyle">?attr/borderlessButtonStyle</item>
 ...                 
```
