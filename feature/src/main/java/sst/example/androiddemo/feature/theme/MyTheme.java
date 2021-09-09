package sst.example.androiddemo.feature.theme;

import android.content.Context;
import android.util.TypedValue;

class MyTheme {

   //获取主题属性 attr--attribute属性
   public int getThemeAttribute(Context context){
       TypedValue typedValue = new TypedValue();
       context.getTheme().resolveAttribute(android.R.attr.statusBarColor,typedValue,true);
       return  typedValue.data;
    }
}
