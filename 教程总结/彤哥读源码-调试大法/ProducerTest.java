public class ProducerTest{
    ///缓冲队列
    private static final BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(100);

    ///假设有5个人往收点放快递,每个人放100个
    private void  producer(){
        for (int i = 0; i < 5; i++) {
            final int num =i;
            new Thread(()->{
                for (int j = 0; j < 100; j++) {
                    try {
                        ///put表示缓冲池满了，线程进行阻塞
                        //num*100+j 每个人生成的数据不一样
                        blockingQueue.put(num*100+j);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("producer ->"+num+" finished");
            },"producer ->"+i).start();
        }

    }

    ///假设有4个消费者
    private  void consumer()   {
        for (int i = 0; i < 4; i++) {
            final  int finalI = i;
            new Thread(()->{
                while (true){
                    try {
                        final Integer data =   blockingQueue.take();
                        System.out.println("consumer ->"+ finalI +" take data "+data);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            },"consumer ->"+i).start();

        }
    }
    public static void main(String[] args) throws IOException {
        ProducerTest test = new ProducerTest();
        ///启动生产者和消费者
        test.producer();
        test.consumer();
        //阻塞主线程，防止程序终止
        System.in.read();
    }
}