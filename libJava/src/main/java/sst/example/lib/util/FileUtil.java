package sst.example.lib.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;


public class FileUtil {


  //获取文件的创建时间  返回毫秒值
  public static long getFileCreateTime(String file) throws IOException {
    BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
    return attr.creationTime().toMillis();
  }
}
