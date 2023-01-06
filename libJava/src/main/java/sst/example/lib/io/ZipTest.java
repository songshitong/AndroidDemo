package sst.example.lib.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTest {
  //https://www.cnblogs.com/0616--ataozhijia/p/5022028.html
  public static void main(String[] args) {
    String helloPath = "D:\\workspace\\ForTest\\";
    zipSingleFile(helloPath+"hello.txt",helloPath);
  }

  //压缩单个文件
  public static void zipSingleFile(String filepath ,String zippath) {
    try {
      File file = new File(filepath);
      File zipFile = new File(zippath);
      InputStream input = new FileInputStream(file);
      ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
      zipOut.putNextEntry(new ZipEntry(file.getName()));
      int temp = 0;
      while((temp = input.read()) != -1){
        zipOut.write(temp);
      }
      input.close();
      zipOut.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
