package sst.example.lib.socket.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
	public static void main(String[] args) throws IOException {
		Socket sock = new Socket("localhost", 6666); // 连接指定服务器和端口
		///TCP存在连接的概念，面向流，所以使用文件流
		try (InputStream input = sock.getInputStream()) {
			try (OutputStream output = sock.getOutputStream()) {
				handle(input, output);
			}
		}
		sock.close();
		System.out.println("disconnected.");
	}

	//为什么写入网络数据时，要调用flush()方法。//	如果不调用flush()，我们很可能会发现，客户端和服务器都收不到数据，这并不是Java标准库的设计问题，而是我们以流的形式写入数据的时候，
//	并不是一写入就立刻发送到网络，而是先写入内存缓冲区，直到缓冲区满了以后，才会一次性真正发送到网络，这样设计的目的是为了提高传输效率。
//	如果缓冲区的数据很少，而我们又想强制把这些数据发送到网络，就必须调用flush()强制把缓冲区数据发送出去。
	private static void handle(InputStream input, OutputStream output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		Scanner scanner = new Scanner(System.in);
		System.out.println("[server] " + reader.readLine());
		for (;;) {
			System.out.print(">>> "); // 打印提示
			String s = scanner.nextLine(); // 读取一行输入
			writer.write(s);
			writer.newLine();
			writer.flush();
			String resp = reader.readLine();
			System.out.println("<<< " + resp);
			if (resp.equals("bye")) {
				break;
			}
		}
	}
}
