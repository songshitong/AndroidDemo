
https://blog.csdn.net/mynameishuangshuai/article/details/51673273
Intent可分为隐式（implicitly）和显式（explicitly）两种：
显式 Intent
即在构造Intent对象时就指定接收者，它一般用在知道目标组件名称的前提下
```
Intent intent = new Intent(MainActivit.this, NewActivity.class);
startActivity(intent );  
```
隐式 Intent
即Intent的发送者在构造Intent对象时，并不知道也不关心接收者是谁，有利于降低发送者和接收者之间的耦合，它一般用在没有明确指出目标组件名称的前提下，
一般是用于在不同应用程序之间
```
Intent intent = new Intent();
intent.setAction("com.wooyun.test");
startActivity(intent);
```


intentfilter
Intent的action、type、category这三个属性来进行匹配判断的
action : Declares the intent action accepted, in the name attribute
category: Declares the intent category accepted, in the name attribute


action的匹配规则
action是一个字符串
```
<intent-filter>
    <action android:name="android.intent.action.SEND"/>
    <action android:name="android.intent.action.SEND_TO"/>
</intent-filter>
```

data的匹配规则
如果Intent没有提供type，系统将从data中得到数据类型
data由两部分组成：mimeType和URI
MineType指的是媒体类型：例如image/jpeg，auto/mpeg4和video/*等，可以表示图片、文本、视频等不同的媒体格式
uri则由scheme、host、port、path | pathPattern | pathPrefix这4部分组成
```
<scheme>://<host>:<port>/[<path>|<pathPrefix>|<pathPattern >]
```
Intent的uri可通过setData方法设置，mimetype可通过setType方法设置。


category的匹配规则
category也是一个字符串
```
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```



于intent-filter匹配优先级
首先查看Intent的过滤器(intent-filter),按照以下优先关系查找：action->data->category