package sst.example.lib.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;


public class FileUtil {


  //获取文件的创建时间  返回毫秒值
  public static long getFileCreateTime(String file) throws IOException {
    BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
    return attr.creationTime().toMillis();
  }


  //从InputStream读取字节码  https://stackoverflow.com/questions/1264709/convert-inputstream-to-byte-array-in-java
  public static byte[] readAllBytes(InputStream inputStream) throws IOException {
    final int bufLen = 4 * 0x400; // 4KB
    byte[] buf = new byte[bufLen];
    int readLen;
    IOException exception = null;

    try {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
          outputStream.write(buf, 0, readLen);

        return outputStream.toByteArray();
      }
    } catch (IOException e) {
      exception = e;
      throw e;
    } finally {
      if (exception == null) inputStream.close();
      else try {
        inputStream.close();
      } catch (IOException e) {
        exception.addSuppressed(e);
      }
    }
  }
}
