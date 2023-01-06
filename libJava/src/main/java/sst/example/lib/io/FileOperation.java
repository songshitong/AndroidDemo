package sst.example.lib.io;


import java.io.File;
import java.io.IOException;

public class FileOperation {
    public static void main(String[] args) throws IOException {
        File file = new File("");
        //创建文件
        file.createNewFile();
    }
}
