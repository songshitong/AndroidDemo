https://blog.csdn.net/liuyu973971883/article/details/107917079

一、什么是Phaser？
Phaser又称“阶段器”，用来解决控制多个线程分阶段共同完成任务的情景问题。它与CountDownLatch和CyclicBarrier类似，
都是等待一组线程完成工作后再执行下一步，协调线程的工作。但在CountDownLatch和CyclicBarrier中我们都不可以动态的配置parties，
而Phaser可以动态注册需要协调的线程，相比CountDownLatch和CyclicBarrier就会变得更加灵活。

Phaser中也是通过计数器来控制。在Phaser中计数器叫做parties， 我们可以通过Phaser的构造函数或者register()方法来注册。

一个需要4个阶段完成的大任务，每个阶段需要3个小任务，针对这些小任务，我们分别起3个线程来执行这些小任务
  此时phase为4,parties为3   一共是3*4=12个小任务，使用arrive需要12次


二、Phaser的常用方法
注册
int register() //每有一个任务注册一次   返回到达的阶段  //注册一次表示phaser维护的线程个数，等同于parties
int bulkRegister(int parties)//一次注册多个

关闭
void forceTermination() //强制关闭当前phaser

等待相关
int arriveAndAwaitAdvance() //到达并等待其他线程到达
int arriveAndDeregister()  //到达并注销该parties，这个方法不会使线程阻塞
int arrive()  //到达，但不会使线程阻塞
int awaitAdvance(int phase) //等待前行，可阻塞也可不阻塞，判断条件为传入的phase是否为当前phaser的phase。如果相等则阻塞，反之不进行阻塞
//该方法与awaitAdvance类似，唯一不一样的就是它可以进行打断
int awaitAdvanceInterruptibly(int phase)   
int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit)

状态查询
int getRegisteredParties()//查询注册了多少个
int getArrivedParties() //查询到达了多少个
int getPhase()  //当前属于第几个phase
boolean isTerminated() //当前是否关闭


案例1
动态注册多个phase，然后等待完成
```
public class PhaserExample {
    private static Random random = new Random(System.currentTimeMillis());
    public static void main(String[] args) {
        Phaser phaser = new Phaser();
        //创建5个任务
        for (int i=0;i<5;i++){
            new Task(phaser).start();
        }
        //动态注册
        phaser.register();
        //等待其他线程完成工作
        phaser.arriveAndAwaitAdvance();
        System.out.println("All of worker finished the task");
    }

    private static class Task extends Thread{
        private Phaser phaser;

        public Task(Phaser phaser) {
            this.phaser = phaser;
            //动态注册任务
            this.phaser.register();
        }

        @Override
        public void run() {
            try {
                int time = random.nextInt(5);
                System.out.println("The thread ["+getName()+"] is working，time "+time);
                TimeUnit.SECONDS.sleep(random.nextInt(5));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("The thread ["+getName()+"] work finished");
            //等待其他线程完成工作
            phaser.arriveAndAwaitAdvance();
        }
    }
}
```
结果
```
The thread [Thread-0] is working，time 3
The thread [Thread-4] is working，time 1
The thread [Thread-3] is working，time 3
The thread [Thread-2] is working，time 3
The thread [Thread-1] is working，time 0
The thread [Thread-1] work finished
The thread [Thread-4] work finished
The thread [Thread-3] work finished
The thread [Thread-0] work finished
The thread [Thread-2] work finished
All of worker finished the task
```

案例二
一个任务分为多步，每步都可以多个线程运行，每步执行完成之后进入下一步
5个运动员参加running，bicycle，jump
```
private static Random random = new Random(System.currentTimeMillis());
    public static void main(String[] args) {
        //初始化5个parties  对应启动了5个线程任务
        Phaser phaser = new Phaser(5);
        for (int i=1;i<6;i++){
            new Athlete(phaser,i).start();
        }
    }
    //创建运动员类
    private static class Athlete extends Thread{
        private Phaser phaser;
        private int no;//运动员编号

        public Athlete(Phaser phaser,int no) {
            this.phaser = phaser;
            this.no = no;
        }

        @Override
        public void run() {
            try {
                System.out.println(getNo() +": 当前处于第："+phaser.getPhase()+"阶段");
                System.out.println(getNo() +": start running");
                TimeUnit.SECONDS.sleep(random.nextInt(5));
                System.out.println(getNo() +": end running");
                //等待其他运动员完成跑步
                phaser.arriveAndAwaitAdvance();

                System.out.println(getNo() +": 当前处于第："+phaser.getPhase()+"阶段");
                System.out.println(getNo() +": start bicycle");
                TimeUnit.SECONDS.sleep(random.nextInt(5));
                System.out.println(getNo() +": end bicycle");
                //等待其他运动员完成骑行
                phaser.arriveAndAwaitAdvance();

                System.out.println(getNo() +": 当前处于第："+phaser.getPhase()+"阶段");
                System.out.println(getNo() +": start long jump");
                TimeUnit.SECONDS.sleep(random.nextInt(5));
                System.out.println(getNo() +": end long jump");
                //等待其他运动员完成跳远
                phaser.arriveAndAwaitAdvance();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private String getNo() {
            return "运动员"+no;
        }
    }
```
结果：
```
运动员2: 当前处于第：0阶段
运动员2: start running
运动员5: 当前处于第：0阶段
运动员4: 当前处于第：0阶段
运动员4: start running
运动员3: 当前处于第：0阶段
运动员1: 当前处于第：0阶段
运动员1: start running
运动员3: start running
运动员5: start running
运动员4: end running
运动员1: end running
运动员3: end running
运动员5: end running
运动员2: end running
运动员2: 当前处于第：1阶段
运动员1: 当前处于第：1阶段
运动员4: 当前处于第：1阶段
运动员4: start bicycle
运动员5: 当前处于第：1阶段
运动员5: start bicycle
运动员3: 当前处于第：1阶段
运动员3: start bicycle
运动员1: start bicycle
运动员2: start bicycle
运动员1: end bicycle
运动员2: end bicycle
运动员3: end bicycle
运动员4: end bicycle
运动员5: end bicycle
运动员5: 当前处于第：2阶段
运动员2: 当前处于第：2阶段
运动员2: start long jump
运动员3: 当前处于第：2阶段
运动员3: start long jump
运动员1: 当前处于第：2阶段
运动员4: 当前处于第：2阶段
运动员1: start long jump
运动员5: start long jump
运动员4: start long jump
运动员1: end long jump
运动员4: end long jump
运动员2: end long jump
运动员3: end long jump
运动员5: end long jump
```

案例三
利用arrive只监听线程完成第一部分任务
```
 private static Random random = new Random(System.currentTimeMillis());
    public static void main(String[] args) throws InterruptedException {
        //初始化6个parties   主线程，子线程存在6个线程
        Phaser phaser = new Phaser(6);
        //创建5个任务
        IntStream.rangeClosed(1,5).forEach(i->new ArrayTask(i,phaser).start());
        //等待5个任务的第一部分完成
        phaser.arriveAndAwaitAdvance();
        System.out.println("all work finished");
    }

    private static class ArrayTask extends Thread{
        private Phaser phaser;

        public ArrayTask(int name,Phaser phaser) {
            super(String.valueOf(name));
            this.phaser = phaser;
        }

        @Override
        public void run() {
            try {
                //模拟第一部分工作
                int workTime = random.nextInt(3);
                System.out.println(getTName() +" start working, time:"+workTime);
                TimeUnit.SECONDS.sleep(workTime);
                System.out.println(getTName() +" end working");
                //该方法表示到达但不会使线程阻塞
                phaser.arrive();
                //模拟第二部分工作
                int otherTime = random.nextInt(3);
                System.out.println(getTName() +" start other thing, time:"+otherTime);
                TimeUnit.SECONDS.sleep(otherTime);
                System.out.println(getTName() +" end other thing");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private String getTName() {
            return "Thread-"+getName();
        }
    }
```
结果：
```
Thread-1 start working, time:2
Thread-3 start working, time:0
Thread-2 start working, time:0
Thread-5 start working, time:1
Thread-4 start working, time:0
Thread-4 end working
Thread-3 end working
Thread-2 end working
Thread-2 start other thing, time:1
Thread-3 start other thing, time:1
Thread-4 start other thing, time:2
Thread-5 end working
Thread-5 start other thing, time:0
Thread-5 end other thing
Thread-3 end other thing
Thread-2 end other thing
Thread-1 end working
Thread-1 start other thing, time:2
all work finished
Thread-4 end other thing
Thread-1 end other thing
```


案例四 phase可以自定义监听到达的阶段
```
class  MPhaser extends Phaser{
        
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            switch (phase){
                case 0:
                    //不同阶段处理，到达这个phase
                    break;
                default:
                    break;
            }
            return super.onAdvance(phase, registeredParties);
        }
    }
```