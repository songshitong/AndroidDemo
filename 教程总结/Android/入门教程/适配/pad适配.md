

ro.build.characteristics=default(可以修改为平板模式与手机模式:tablet/phone,默认是手机模式,需配合density修改)
部分pad可能判断不出来，需要引入其他方法
获取值
需要引入layoutlib.jar
```
SystemProperties.get("ro.build.characteristics", "default")
```