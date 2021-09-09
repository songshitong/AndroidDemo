package sst.example.lib.socket.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
	public static void main(String[] args) throws IOException, InterruptedException {
		DatagramSocket ds = new DatagramSocket();
		ds.setSoTimeout(1000);
		//向指定服务器发送报文，并不是真正的连接
		ds.connect(InetAddress.getByName("localhost"), 6666); // 连接指定服务器和端口
		DatagramPacket packet = null;
		for (int i = 0; i < 5; i++) {
			// 发送:
			String cmd = new String[] { "date", "time", "datetime", "weather", "hello" }[i];
			byte[] data = cmd.getBytes();
			///udp没有流的概念，发送数据报文
			packet = new DatagramPacket(data, data.length);
			ds.send(packet);
			// 接收:
			byte[] buffer = new byte[1024];
			packet = new DatagramPacket(buffer, buffer.length);
			//阻塞方法
			ds.receive(packet);
			String resp = new String(packet.getData(), packet.getOffset(), packet.getLength());
			System.out.println(cmd + " >>> " + resp);
			Thread.sleep(1500);
		}
		ds.disconnect();
		System.out.println("disconnected.");
	}
}
