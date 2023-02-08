


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


android.view.WindowManager$InvalidDisplayException: Unable to add window android.view.ViewRootImpl$W@9bf57b2 -- the specified window type -2 is not valid
addView需要指定type
```
WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
WindowManager.LayoutParams lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT);
 lp.type = WindowManager.LayoutParams.TYPE_APPLICATION; //部分类型需要权限申请
wm.addView(tv,lp);
```