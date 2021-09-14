debug  圆形  运行程序
resume 三角  运行到下一个断点
stop   正方形  停止代码
view point   查看断点
toggle breakpoint 让断点失效

断点详情  suspend 挂起 可以选择全部线程，单个线程
           程序运行到断点然后停住就是挂起，一般配合 evaluate and log 使用，用来查看结果而不程序挂起
        condition 给断点增加条件
        remove once hit  控制断点行为，只触发一次
        disable until hitting the following breakpoint 前置断点
        
       

show execution point 跳到当前断点(比如此时查看其它代码，点击此处跳转到当前运行点)
step over  运行到下一行
step into  进入方法  一般是自己的方法
  一行代码有多个可以进入的，idea可以提示，设置断点时也可以选择给哪一部分打断点 
force step into  调试框架或jdk源码，强制进入   Force Step Into 会进入到任何方法中单步执行
step out   退出当前方法    下面还有断点，执行下面的断点
drop frame 
run to cursor  运行到光标处(没有打断点但是本次可以运行到光标处停止)
   for循环中分支可以使用
evaluate expression  对当前变量进行计算
Trace Current Stream Chain  java stream流操作的debug  可以查看filter，map，sort等各个阶段的结果

Memory View  内存查看面板

主面板 frames   代表栈帧，方法栈，可以查看源码的调用流程
  从main方法到test方法
  
  切换线程  frames下方切换主线程或者子线程
  右击栈帧可以进行操作  
    drop frame     丢掉当前栈帧，从上一帧执行   一帧一般代表一个方法
    force return
    export threads 导出线程信息
    copy  stack    复制栈执行的方法
    Throw an exception  可以直接在调试中抛出指定的异常   throw new NullPointerException()
    Force return    类似上面的手动抛异常，只不过是返回一个指定值罢了,void不用输入
  
variables 变量区  
  当前断点所在的上下文
  点击+ 可以增加观察变量，方便对比前后变化