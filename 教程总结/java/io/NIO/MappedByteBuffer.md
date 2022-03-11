https://www.jianshu.com/p/f90866dcbffc
java io操作中通常采用BufferedReader，BufferedInputStream等带缓冲的IO类处理大文件，不过java nio中引入了一种基于MappedByteBuffer
操作大文件的方式，其读写性能极高

通过MappedByteBuffer读取文件

```csharp
File file = new File("D://data.txt");
        long len = file.length();
        byte[] ds = new byte[(int) len];

        try {
            MappedByteBuffer mappedByteBuffer = new RandomAccessFile(file, "r")
                    .getChannel()
                    .map(FileChannel.MapMode.READ_ONLY, 0, len);
            for (int offset = 0; offset < len; offset++) {
                byte b = mappedByteBuffer.get();
                ds[offset] = b;
            }

            Scanner scan = new Scanner(new ByteArrayInputStream(ds)).useDelimiter(" ");
            while (scan.hasNext()) {
                System.out.print(scan.next() + " ");
            }

        } catch (IOException e) {}
```
