package sst.example.lib.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TimeTest {
  public static final String PATH =
      "D:\\workspace\\AndroidDemo\\libJava\\src\\main\\java\\sst\\example\\lib\\io\\a.text";

  public static void main(String[] args) {
    System.out.println(Runtime.version());
    Map<Integer, Object> values = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      //生成随机字符
      values.put(i, UUID.randomUUID().toString().substring(0, 8));
    }
    TimeTest test = new TimeTest();
    long directTime = System.currentTimeMillis();
    test.writeCfg(values);
    System.out.println("不使用buffer的时间 " + (System.currentTimeMillis() - directTime));

    long bufferTime  = System.currentTimeMillis();
    test.writeCfgWithBuffer(values);
    System.out.println("使用buffer的时间 "+(System.currentTimeMillis() - bufferTime));
    //cpu: Intel(R) Core(TM) i5-10505 CPU @ 3.20GHz   3.19 GHz    磁盘nvme固态
    //java version:11.0.15+0-b2043.56-9505619
    //不使用buffer的时间 38
    //使用buffer的时间 2
  }

  private synchronized void writeCfg(Map<Integer, Object> values) {
    FileOutputStream fileOut = null;
    //ObjectOutputStream缓冲区大小
    //private final byte[] buf = new byte[1024];
    //private final byte[] hbuf = new byte[5];
    //private final char[] cbuf = new char[256];
    ObjectOutputStream objOut = null;
    try {
      fileOut = new FileOutputStream(PATH);
      objOut = new ObjectOutputStream(fileOut);
      objOut.writeObject(values);
      fileOut.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (null != fileOut) {
        try {
          fileOut.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (null != objOut) {
        try {
          objOut.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  private synchronized void writeCfgWithBuffer(Map<Integer, Object> values) {
    FileOutputStream fileOut = null;
    ObjectOutputStream objOut = null;
    BufferedOutputStream bufferOut = null;
    try {
      fileOut = new FileOutputStream(PATH);
      bufferOut = new BufferedOutputStream(fileOut,1024);
      objOut = new ObjectOutputStream(bufferOut);
      objOut.writeObject(values);
      fileOut.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if(null != bufferOut){
        try {
          bufferOut.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      if (null != fileOut) {
        try {
          fileOut.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (null != objOut) {
        try {
          objOut.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
