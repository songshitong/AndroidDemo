contentprovider是android四大组件之一的内容提供器，它主要的作用就是将程序的内部的数据和外部进行共享，为数据提供外部访问接口，被访问的数据主要以数据库的形式存在，而且还可以选择共享哪一部分的数据。
  这样一来，对于程序当中的隐私数据可以不共享，从而更加安全。contentprovider是android中一种跨程序共享数据的重要组件


使用系统的ContentProvider
系统的ContentProvider有很多，如通话记录，短信，通讯录等等，都需要和第三方的app进行共享数据。既然是使用系统的，那么contentprovider的具体实现就不需要我们担心了，
使用内容提供者的步骤如下
1获取ContentResolver实例
2确定Uri的内容，并解析为具体的Uri实例
3通过ContentResolver实例来调用相应的方法，传递相应的参数，但是第一个参数总是Uri，它制定了我们要操作的数据的具体地址

可以通过读取系统通讯录的联系人信息，显示在Listview中来实践这些知识。不要忘记在读取通讯录的时候，在清单文件中要加入相应的读取权限


自定义ContentProvider
查看具体内容 adb shell content query --uri  content://aa/bb/userId

重新实现ContentProvider之后，发现我们重写了6个重要的抽象方法
oncreate
query
update
insert
delete
gettype

oncreate方法应该是内容提供者创建的时候所执行的一个回调方法，负责数据库的创建和更新操作。这个方法在进程启动时执行。
gettype方法是获取我们通过参数传递进去的Uri的MIME类型，这个类型是什么，后面会有实例说明

内容提供者首先要做的一个事情就是将我们传递过来的Uri解析出来，确定其他程序到底想访问哪些数据。Uri的形式一般有两种：

1，以路径名为结尾，这种Uri请求的是整个表的数据，如: content://com.demo.androiddemo.provider/tabl1 标识我们要访问tabl1表中所有的数据
2，以id列值结尾，这种Uri请求的是该表中和其提供的列值相等的单条数据。 content://com.demo.androiddemo.provider/tabl1/1 标识我们要访问tabl1表中_id列值为1的数据。

如果是内容提供器的设计者，那么我们肯定知道这个程序的数据库是什么样的，每一张表，或者每一张表中的_id都应该有一个唯一的内容Uri。我们可以将传递进来的Uri和我们存好的Uri进行匹配，匹配到了之后，就说明数据源已经找到，便可以进行相应的增删改查操作

