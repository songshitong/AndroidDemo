


Window type can not be changed after the window is added.
代码位置
http://www.aospxref.com/android-12.0.0_r3/xref/frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java#2250
window的type添加后就不能改变了，一般发生在给window设置参数
正确参数的设置方式或者使用ViewGroup.LayoutParams
```
 WindowManager.LayoutParams lp = getWindow().getAttributes();
    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
    lp.height =  ViewGroup.LayoutParams.MATCH_PARENT;
```
错误
```
WindowManager.LayoutParams lp =  new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.MATCH_PARENT)

//原因 WindowManager.LayoutParams构造器与其他不同
public LayoutParams(int _type, int _flags) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
 ...
        }
```