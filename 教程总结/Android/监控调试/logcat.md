

https://fqzhanghao.github.io/post/android-zhong-yu-zhi-dao-log-xian-shi-bu-quan-de-yuan-yin-liao/
Android系统的单条日志打印长度是有限的,默认为4K
Logger.h
```
#define LOGGER_ENTRY_MAX_LEN        (4*1024)  
#define LOGGER_ENTRY_MAX_PAYLOAD    \\  
    (LOGGER_ENTRY_MAX_LEN - sizeof(struct logger_entry))
```
Logcat使用的liblog资源包也提到，使用Log打印的message有可能被log内核驱动缩短：
The message may have been truncated by the kernel log driver.
自己打印日志，需要对日志进行截断





https://blog.csdn.net/ChaoLi_Chen/article/details/102933916
抓取系统日志
Android4.1之后 认为应用读取系统的log是不安全的,所以要对apk进行系统签名才能读取系统log,如果不能进行系统签名,那么就通过相应的adb命令进行读取
```
kernel中的log
#会持续输出
adb shell cat /dev/kmsg
#不会持续输出
adb shell  dmesg
#会持续输出
adb  logcat -b kernel     //Default -b main,system,crash,kernel
 
 
cpu中的log 
#会持续输出
adb shell top -m 5
 
 
memory 中的log
#不会持续输出
adb shell dumpsys meminfo
 
 
以及system中的log
#会持续输出
adb logcat
```
持续输出的一直监听写入即可
非持续输出的，每隔几秒监听一次

shell 命令执行支持重定向   adb shell top -m 5 > my.txt
```
 ProcessBuilder pBuilder = new ProcessBuilder(new String[]{"sh", "-c", "adb shell top -m 5"});
 pBuilder.redirectErrorStream(true);
 pr = pBuilder.start();
```
不支持重定向  adb shell top -m 5 
```
Runtime run = Runtime.getRuntime();  
Process proc = run.exec("adb shell top -m 5 > my.txt"); 
```
需要权限
```
    <uses-permission android:name="android.permission.READ_LOGS"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.DUMP"/>
    <uses-permission android:name="android.permission.INTERNET"/>
```
adb shell pm grant com.dp.logcatapp android.permission.READ_LOGS  //shell设置权限
判断服务是否开启
```
 public boolean isServiceRunning(Context context, String ServiceName) {
        if (TextUtils.isEmpty(ServiceName)) {
            return false;
        }
        ActivityManager myManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService =
                (ArrayList<ActivityManager.RunningServiceInfo>)
                        myManager.getRunningServices(100);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
```


抓取 cpu的log cpu的log是持续输出的
```
            mProcess = Runtime.getRuntime().exec("top -m 5");
            InputStreamReader inputStreamReader = new InputStreamReader(mProcess.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            if ((line = bufferedReader.readLine()) != null) {
                mCpuInfoWriter.write(line);
                mCpuInfoWriter.write("\n");
                if (line.startsWith("User")) {
                    mCpuInfoWriter.write(mDateFormat.format(new Date()));
                    mCpuInfoWriter.write("\n");
                }
                mCpuInfoWriter.flush();
            }
```
抓取kenel实现 
```
            List<String> commandList = new ArrayList<String>();
            commandList.add("logcat");
            commandList.add("-b");
            commandList.add("kernel");
            process = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            mKenelInfoWriter.write(mDateFormat.format(new Date()));
            mKenelInfoWriter.write("\n");
            while ((line = bufferedReader.readLine()) != null) {
                mKenelInfoWriter.write(line);
                mKenelInfoWriter.write("\n");
                mKenelInfoWriter.flush();
            }
```
抓取 mainlog 
```
      try {
            process = Runtime.getRuntime().exec("logcat");
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            mMainInfoWriter.write(mDateFormat.format(new Date()));
            mMainInfoWriter.write("\n");
            while ((line = bufferedReader.readLine()) != null) {
                mMainInfoWriter.write(line);
                mMainInfoWriter.write("\n");
                mMainInfoWriter.flush();
            }
```
抓取Memorylog  因为 Memorydums信息不是一直等待,每次只有一次输出,因此五秒钟循环一次
```
while (true) {
                process = Runtime.getRuntime().exec("dumpsys meminfo");
                InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = null;
                mMemoryInfoWriter.write(mDateFormat.format(new Date()));
                mMemoryInfoWriter.write("\n");
                while ((line = bufferedReader.readLine()) != null) {
                    mMemoryInfoWriter.write(line);
                    mMemoryInfoWriter.write("\n");
                    mMemoryInfoWriter.flush();
                }
                Thread.sleep(5000);//
            }
```