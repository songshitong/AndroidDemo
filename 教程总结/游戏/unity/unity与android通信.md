
https://juejin.cn/post/6844903940866179086
unity调用android方法
调用android的对象及方法
```
AndroidJavaClass ajc=new AndroidJavaClass(“com.unity3d.player.UnityPlayer”);
AndroidJavaObject ajo=ajc.GetStatic(“currentActivity”);

jo.Call(method ,parameter );    //调用实例方法
jo.Get(method ,parameter );     //获取实例变量
jo.Set(method ,parameter );     //设置实例变量
jo.CallStatic(method ,parameter );//调用静态方法
jo.GetStatic (method ,parameter );//获取静态变量
jo.SetStatic (method ,parameter );//设置静态变量
```
示例：
```
 void OnGUI()
    {
        if (GUI.Button(new Rect(150, 150, 250, 135), "调用Android原生方法"))
        {
            AndroidJavaClass ac = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject ao = ac.CallStatic<AndroidJavaObject>("currentActivity"); //可以声明一个activity的静态变量
            AndroidJavaObject androidClass = new AndroidJavaObject("cn.nutown.android_project.MyAndroidClass");//我们Android的类名
            androidClass.Call("showMassege",ao,"hi!");//对应我们Android的方法需要的参数
        } 
}
```


android调用unity  对象，方法，参数
```
UnityPlayer.UnitySendMessage(“GameObject”, “unityMethod”, “hi~”);
```