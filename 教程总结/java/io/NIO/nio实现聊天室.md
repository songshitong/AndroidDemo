https://www.cnblogs.com/qq-361807535/p/6670529.html

client代码
```
public class Client {
    public static void main(String[] args) throws Exception{
        ///建立非阻塞链接tcp
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(9898));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.configureBlocking(false);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String msg = scanner.nextLine();
            buffer.put(msg.getBytes());
            buffer.flip();
            ///写入数据
            channel.write(buffer);
            ///清空，方便下一次写入
            buffer.clear();
        }
    }
}
```

server代码
```
public class Server {
    public static void main(String[] args) throws Exception{
        ///配置非阻塞channel
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(9898));
        channel.configureBlocking(false);

        Selector selector = Selector.open();
        //注册accept事件
        channel.register(selector, SelectionKey.OP_ACCEPT);
        while (selector.select()>0){
          Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

          while (keys.hasNext()){
              SelectionKey key = keys.next();
              if(key.isAcceptable()){
                  //处理accept事件
                 SocketChannel socketChannel = channel.accept();
                 socketChannel.configureBlocking(false);
                 //注册read事件
                 socketChannel.register(selector,SelectionKey.OP_READ);
              }else if(key.isReadable()){
                  readMsg((SocketChannel)key.channel());
              }
              keys.remove();
          }

        }
    }

    private static void readMsg(SocketChannel channel) throws Exception{
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read =0;
        while ((read = channel.read(buffer))>0){
            buffer.flip();
            byte[] bytes = new byte[1024];
            ///读取数据到bytes数据
            buffer.get(bytes,0,read);
            System.out.println("receive client msg "+new String(bytes));
            buffer.clear();
        }
    }
}
```