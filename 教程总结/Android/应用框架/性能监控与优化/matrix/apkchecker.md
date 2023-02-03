
matrix-apk-canary模块
Matrix-ApkChecker 作为Matrix系统的一部分，是针对android安装包的分析检测工具，根据一系列设定好的规则检测apk是否存在特定的问题，
并输出较为详细的检测结果报告，用于分析排查问题以及版本追踪。Matrix-ApkChecker以一个jar包的形式提供使用
使用 jar和apk位于同一个目录
java -jar matrix-apk-canary-2.0.8.jar --apk ./aa.apk -manifest -checkRe sProguard -findNonAlphaPng  -fileSize --order desc -countMethod --group class  -countClass
后面可以跟配置文件 config matrix-apk-canary\src\apk-checker-config.json
生成aa.html  示例：
```
taskType	1
taskDescription	Unzip the apk file to dest path.
total-size	142906657
entries	
entry-name	META-INF/com/android/build/gradle/app-metadata.properties
entry-size	51
entry-name	assets/dexopt/baseline.prof
entry-size	4362
entry-name	assets/dexopt/baseline.profm
entry-size	346
```

Matrix-ApkChecker 当前主要包含以下功能：

功能名	   作用	                  描述
UnzipTask	做一些前置的准备工作	解压文件，反混淆类名、反混淆资源，统计包中各个文件的大小
ManifestAnalyzeTask	读取manifest的信息	从AndroidManifest.xml中读取apk的全局信息，如包名、最小sdk、目标sdk、版本号
ShowFileSizeTask	按文件大小排序列出apk中包含的文件	列出超过一定大小的文件，可按文件后缀过滤，并且按文件大小排序
MethodCountTask	统计方法数	统计dex包含的方法数，并支持将输出结果按照类名(class)或者包名(package)来分组
CountRTask	统计apk中包含的R类以及R类中的field count	编译之后，代码中对资源的引用都会优化成int常量，除了R.styleable之外，其他的R类其实都可以删除
CountClassTask	统计类的数量	按照包名统计dex中类的数量
ResProguardCheckTask	检查是否经过了资源混淆(AndResGuard)	检查apk是否经过了资源混淆，推荐使用资源混淆来进一步减小apk的大小
FindNonAlphaPngTask	搜索不含alpha通道的png文件	对于不含alpha通道的png文件，可以转成jpg格式来减少文件的大小
MultiLibCheckTask	检查是否包含多个ABI版本的动态库	so文件的大小可能会在apk文件大小中占很大的比例，可以考虑在apk中只包含一个ABI版本的动态库
MultiSTLCheckTask	检查是否有多个动态库静态链接了STL	如果有多个动态库都依赖了STL，应该采用动态链接的方式而非多个动态库都去静态链接STL
UncompressedFileTask	搜索未经压缩的文件类型	某个文件类型的所有文件都没有经过压缩，可以考虑是否需要压缩
DuplicatedFileTask	搜索冗余的文件	对于两个内容完全相同的文件，应该去冗余
UnusedResourceTask	搜索apk中包含的无用资源	apk中未经使用到的资源，应该予以删除
UnusedAssetsTask	搜索apk中包含的无用assets文件	apk中未经使用的assets文件，应该予以删除
UnStrippedSoCheckTask	搜索apk中未经裁剪的动态库文件	动态库经过裁剪之后，文件大小通常会减小很多


各个Task的实现原理
各个具体的Task都是继承至ApkTask，该类有两个主要的方法init方法和call方法。前者主要做一些变量初始化工作，后者是真正进行检测的位置。
call方法将会在线程池中进行执行
com/tencent/matrix/apk/model/task/ApkTask.java
```
public abstract class ApkTask implements Callable<TaskResult> {
...
    public void init() throws TaskInitException {
        if (config == null) {
            throw new TaskInitException(TAG + "---jobConfig can not be null!");
        }

        if (params == null) {
            throw new TaskInitException(TAG + "---params can not be null!");
        }
    }

    ...
    @Override
    public abstract TaskResult call() throws TaskExecuteException;
}
```

UnzipTask
输入的Apk文件首先会经过UnzipTask处理，解压到指定目录，在这一步还会做一些全局的准备工作，包括反混淆类名（读取mapping.txt）、
反混淆资源(读取resMapping.txt)、统计文件大小等。

在UnzipTask#call方法中可以很清楚的看到整个流程。值得一提的是，在读取mapping.txt、resMapping.txt之后，会将混淆的map保存到全局的config中，
以备后用。此外，在解压的过程中，还会保存zip的每一项的大小以及混淆前后的文件名。
签名包mapping文件位置/app/build/outputs/mapping/pub/release/mapping.txt
可以使用<android-sdk>/tools/proguard/bin/proguardgui.sh进行反混淆

com/tencent/matrix/apk/model/task/UnzipTask.java
```
public class UnzipTask extends ApkTask {
    @Override
    public TaskResult call() throws TaskExecuteException {
       ...
        //大小
        ((TaskJsonResult) taskResult).add("total-size", inputFile.length());
            // 读取混淆结果文件并保存起来
            readMappingTxtFile(); //将混淆前后的文件保存到proguardClassMap
            config.setProguardClassMap(proguardClassMap);
            readResMappingTxtFile();//将混淆前后的保存到resguardMap
            config.setResguardMap(resguardMap);

           // 逐项解压文件，并对文件名尝试进行反混淆
            Enumeration entries = zipFile.entries();
            JsonArray jsonArray = new JsonArray();
            String outEntryName = "";
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                outEntryName = writeEntry(zipFile, entry); //拿到resguardMap中保存的混淆前名字
                if (!Util.isNullOrNil(outEntryName)) {
                    JsonObject fileItem = new JsonObject();
                    // 保存文件名-文件大小到json array中
                    fileItem.addProperty("entry-name", outEntryName);
                    fileItem.addProperty("entry-size", entry.getCompressedSize());
                    jsonArray.add(fileItem);
                    // 保存文件名、大小、反混淆前后的文件名到map
                    entrySizeMap.put(outEntryName, Pair.of(entry.getSize(), entry.getCompressedSize()));
                    entryNameMap.put(entry.getName(), outEntryName);
                }
            }
            // 保存map到全局的配置中
            config.setEntrySizeMap(entrySizeMap);
            config.setEntryNameMap(entryNameMap);
            // 添加json array到输出中
            ((TaskJsonResult) taskResult).add("entries", jsonArray);
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
            return taskResult;
          ...  
    }
}
```
上面在MMTaskJsonResult中将会针对部分类型的输出做进一步的格式化操作，所以导致从结果文件反推代码，有点对不上的问题




ManifestAnalyzeTask
用于读取AndroidManifest.xml中的信息，如：packageName、verisonCode、clientVersion等。
实现方法：利用ApkTool中的 AXmlResourceParser 来解析二进制的AndroidManifest.xml文件，并且可以反混淆出AndroidManifest.xml中引用的资源名称。
该Task的主要实现依托于另外一个ManifestParser类中。ManifestParser会使用apktool-lib-2.4.0.jar中的AXmlResourceParser类
来解析二进制的AndroidManifest.xml文件，此外Parser还通过解析apk中的resources.arsc以及apkchecker内置的android-framework.jar
中的resources.arsc这两个资源，完成xml中资源id至资源名的反混淆

com/tencent/matrix/apk/model/task/ManifestAnalyzeTask.java
```
@Override
    public TaskResult call() throws TaskExecuteException {
        try {
            ManifestParser manifestParser = null;
            if (!FileUtil.isLegalFile(arscFile)) {
                manifestParser = new ManifestParser(inputFile);
            } else {
                manifestParser = new ManifestParser(inputFile, arscFile);
            }
            TaskResult taskResult = TaskResultFactory.factory(getType(), TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            JsonObject jsonObject = manifestParser.parse();
            Log.d(TAG, jsonObject.toString());
            ((TaskJsonResult) taskResult).add("manifest", jsonObject);
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
            return taskResult;
        } catch (Exception e) {
            throw new TaskExecuteException(e.getMessage(), e);
        }
    }
```
com/tencent/matrix/apk/model/task/util/ManifestParser.java
```
 public JsonObject parse() throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(manifestFile);
            try {
                //AXmlResourceParser
                resourceParser.open(inputStream);
                int token = resourceParser.nextToken();
                while (token != XmlPullParser.END_DOCUMENT) {
                    token = resourceParser.next();
                    if (token == XmlPullParser.START_TAG) {
                        handleStartElement();
                    } else if (token == XmlPullParser.TEXT) {
                        handleElementContent();
                    } else if (token == XmlPullParser.END_TAG) {
                        handleEndElement();
                    }
                }
            }....
        //返回结果JsonObject
        return result;
    }
```
ManifestParser调用AXmlResourceParser解析AndroidManifest.xml的方式是PULL解析的方式，解析完毕之后返回给上层的就是manifest文件的json表示了。
然后ManifestAnalyzeTask将这个json作为返回值进行返回。
当然，MMTaskJsonResult也会该Task做了定制化的输出，摘除了包名、版本号等信息。

该部分的输出例子如下：搜索manifest可以找到
```
taskType	2
taskDescription	Read package info from the AndroidManifest.xml.
manifest	
android:versionCode	123
android:versionName	1.2.3
android:compileSdkVersion	32
android:compileSdkVersionCodename	12
```



ShowFileSizeTask
根据文件大小以及文件后缀名来过滤出超过指定大小的文件，并按照升序或降序排列结果。
实现方法：直接利用UnzipTask中统计的文件大小来过滤输出结果。
com/tencent/matrix/apk/model/task/ShowFileSizeTask.java
```
@Override
    public TaskResult call() throws TaskExecuteException {
        try {
            ...
            long startTime = System.currentTimeMillis();
             //从全局的config获取信息 由unzip设置 
            Map<String, Pair<Long, Long>> entrySizeMap = config.getEntrySizeMap();
            if (!entrySizeMap.isEmpty()) {                                                          //take advantage of the result of UnzipTask.
                for (Map.Entry<String, Pair<Long, Long>> entry : entrySizeMap.entrySet()) {
                    final String suffix = getSuffix(entry.getKey());
                    Pair<Long, Long> size = entry.getValue();
                    // 如果该项的大小超过了设定的阈值
                    if (size.getFirst() >= downLimit * ApkConstants.K1024) {
                        // 没有设置指定项 或者 指定项包含该项，则记录下来
                        if (filterSuffix.isEmpty() || filterSuffix.contains(suffix)) {
                            entryList.add(Pair.of(entry.getKey(), size.getFirst()));
                        } else {
                            Log.d(TAG, "file: %s, filter by suffix.", entry.getKey());
                        }
                    } else {
                        Log.d(TAG, "file:%s, size:%d B, downlimit:%d KB", entry.getKey(), size.getFirst(), downLimit);
                    }
                }
            }

            Collections.sort(entryList, new Comparator<Pair<String, Long>>() {
                @Override
                public int compare(Pair<String, Long> entry1, Pair<String, Long> entry2) {
                   ..//排序
                }
            });

           ...//结果输出
    }
```
html信息为Show files whose size exceed limit size in order



MethodCountTask
可以统计出各个Dex中的方法数，并按照类名或者包名来分组输出结果。
实现方法：利用google开源的 com.android.dexdeps 类库来读取dex文件，统计方法数。
com.android.dexdeps的google仓库可以看这里dexdeps。
https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/
在MethodCountTask中，最主要的方法就是将dex文件作为参数构造出DexData，然后调用DexData#getMethodRefs和DexData#getExternalReferences获取dex里面所有方法的引用以及所有外部类的引用。
最后按照方法所在的类的类名是否在外部类数组中，来将所有的方法划分为内部类方法以及外部类方法两大类。
这里的外部类的意思是：外部类没有被本dex里面的东西所引用。
com/tencent/matrix/apk/model/task/MethodCountTask.java
```
@Override
    public void init() throws TaskInitException {
        super.init();
        ...
        File[] files = inputFile.listFiles();
        try {
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(ApkConstants.DEX_FILE_SUFFIX)) {
                        //拿到所有的.dex
                        dexFileNameList.add(file.getName());
                        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                        dexFileList.add(randomAccessFile);
                    }
                }
            }
        } ...
    }

@Override
    public TaskResult call() throws TaskExecuteException {
        try {
            TaskResult taskResult = TaskResultFactory.factory(getType(), TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            JsonArray jsonArray = new JsonArray();
            for (int i = 0; i < dexFileList.size(); i++) {
                RandomAccessFile dexFile = dexFileList.get(i);
                //解析dex各种信息
                countDex(dexFile);
                dexFile.close();
                // 统计内部方法、外部方法的总数 存贮在map中value的个数
                int totalInternalMethods = sumOfValue(classInternalMethod);
                int totalExternalMethods = sumOfValue(classExternalMethod);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("dex-file", dexFileNameList.get(i));

                ...//排序，输出结果
    }
    

 private void countDex(RandomAccessFile dexFile) throws IOException {
        ...
        DexData dexData = new DexData(dexFile);
        //使用dexdeps加载dex
        dexData.load();
        // 获取dex中所有方法
        MethodRef[] methodRefs = dexData.getMethodRefs();
        // 获取dex中所有的外部引用类
        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        Map<String, String> proguardClassMap = config.getProguardClassMap();
        String className = null;
        // 先反混淆处理
        for (ClassRef classRef : externalClassRefs) {
            className = ApkUtil.getNormalClassName(classRef.getName());
            if (proguardClassMap.containsKey(className)) {
                className = proguardClassMap.get(className);
            }
            if (className.indexOf('.') == -1) {
                continue;
            }
            classExternalMethod.put(className, 0);
        }
        // 以方法的维度，将方法划分到内部、外部两个维度
        for (MethodRef methodRef : methodRefs) {
            className = ApkUtil.getNormalClassName(methodRef.getDeclClassName());
            if (proguardClassMap.containsKey(className)) {
                className = proguardClassMap.get(className);
            }
            if (!Util.isNullOrNil(className)) {
                if (className.indexOf('.') == -1) {
                    continue;
                }
                //保存在map   classExternalMethod，classInternalMethod
                if (classExternalMethod.containsKey(className)) {
                    classExternalMethod.put(className, classExternalMethod.get(className) + 1);
                } else if (classInternalMethod.containsKey(className)) {
                    classInternalMethod.put(className, classInternalMethod.get(className) + 1);
                } else {
                    classInternalMethod.put(className, 1);
                }
            }
        }

        //remove 0-method referenced class
        Iterator<String> iterator = classExternalMethod.keySet().iterator();
        while (iterator.hasNext()) {
            if (classExternalMethod.get(iterator.next()) == 0) {
                iterator.remove();
            }
        }
    }    
```
html输出描述Count methods in dex file, output results group by class name or package name



CountRTask
可以统计R类以及R类的中的field数目
实现方法：同样是利用 com.android.dexdeps 类库来读取dex文件，找出R类以及field数目。
该类里面的操作也是依赖于dexdeps类库，原理类似于上面的统计方法
com/tencent/matrix/apk/model/task/CountRTask.java
```
 for (RandomAccessFile dexFile : dexFileList) {
                DexData dexData = new DexData(dexFile);
                dexData.load();
                dexFile.close();
                ClassRef[] defClassRefs = dexData.getInternalReferences();
                for (ClassRef classRef : defClassRefs) {
                    String className = ApkUtil.getNormalClassName(classRef.getName());
                    if (classProguardMap.containsKey(className)) {
                        className = classProguardMap.get(className);
                    }
                    String pureClassName = getOuterClassName(className);
                    //统计R文件
                    if (pureClassName.endsWith(".R") || "R".equals(pureClassName)) {
                        if (!classesMap.containsKey(pureClassName)) {
                            classesMap.put(pureClassName, classRef.getFieldArray().length);
                        } else {
                            classesMap.put(pureClassName, classesMap.get(pureClassName) + classRef.getFieldArray().length);
                        }
                    }
                }
            }
```
html描述 Count the R class




CountClassTask¶
统计类的数量。原理与上面的类似，也是依赖于dexdeps读取dex文件，输出时可以按照包名进行输出。
com/tencent/matrix/apk/model/task/CountClassTask.java
```
    for (int i = 0; i < dexFileList.size(); i++) {
                RandomAccessFile dexFile = dexFileList.get(i);
                DexData dexData = new DexData(dexFile);
                dexData.load();
                dexFile.close();
                ClassRef[] defClassRefs = dexData.getInternalReferences();
                Set<String> classNameSet = new HashSet<>();
                for (ClassRef classRef : defClassRefs) {
                    String className = ApkUtil.getNormalClassName(classRef.getName());
                    if (classProguardMap.containsKey(className)) {
                       //拿到反混淆的类名，存储到classNameSet
                        className = classProguardMap.get(className);
                    }
                    if (className.indexOf('.') == -1) {
                        continue;
                    }
                    classNameSet.add(className);
                }
```
html描述 Count classes in dex file, output results group by package name



ResProguardCheckTask
可以判断apk是否经过了资源混淆
实现方法：资源混淆之后的res文件夹会重命名成r，直接判断是否存在文件夹r即可判断是否经过了资源混淆。
这里的资源混淆指的是AndResGuard插件，非常好用，强烈推荐。
https://mp.weixin.qq.com/s/6YUJlGmhf1-Q-5KMvZ_8_Q

AndResGuard插件在混淆资源时，有一个可选项keepRoot，可以选择是否将res目录混淆成r目录。
ResProguardCheckTask在检测时，会首先检查是否存在r目录。若存在，则说明资源混淆过了；否则会检查res目录下的目录是否是短目录名，若是则也说明是混淆过了的。
com/tencent/matrix/apk/model/task/ResProguardCheckTask.java
```
@Override
    public TaskResult call() throws TaskExecuteException {
        //r目录
        File resDir = new File(inputFile, ApkConstants.RESOURCE_DIR_PROGUARD_NAME);
        try {
            TaskResult taskResult = TaskResultFactory.factory(getType(), TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            //r目录存在
            if (resDir.exists() && resDir.isDirectory()) {
                Log.i(TAG, "find resource directory " + resDir.getAbsolutePath());
                ((TaskJsonResult) taskResult).add("hasResProguard", true);
            } else {
               //仍然为res目录
                resDir = new File(inputFile, ApkConstants.RESOURCE_DIR_NAME);
                if (resDir.exists() && resDir.isDirectory()) {
                    File[] dirs = resDir.listFiles();
                    boolean hasProguard = true;
                    for (File dir : dirs) {
                        //不符合[a-z_0-9]{1,3}的目录 1-3位的字母或数字
                        if (dir.isDirectory() && !fileNamePattern.matcher(dir.getName()).matches()) {
                            hasProguard = false;
                            Log.i(TAG, "directory " + dir.getName() + " has a non-proguard name!");
                            break;
                        }
                    }
                    //添加已经ResProguard的信息
                    ((TaskJsonResult) taskResult).add("hasResProguard", hasProguard);
                } else {
                    throw new TaskExecuteException(TAG + "---No resource directory found!");
                }
            }
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
            return taskResult;
        } catch (Exception e) {
            throw new TaskExecuteException(e.getMessage(), e);
        }
    }
```
html描述Check if the apk handled by resguard



FindNonAlphaPngTask
可以检测出apk中非透明的png文件
实现方法：通过 java.awt.BufferedImage 类读取png文件并判断是否有alpha通道。

非透明png可以转为jpg来减少文件大小。
FindNonAlphaPngTask的主要逻辑就是找到资源目录，遍历调用findNonAlphaPng。
com/tencent/matrix/apk/model/task/FindNonAlphaPngTask.java
```
 @Override
    public TaskResult call() throws TaskExecuteException {
        File resDir = new File(inputFile, ApkConstants.RESOURCE_DIR_PROGUARD_NAME);
        TaskResult taskResult = null;
        try {
            taskResult = TaskResultFactory.factory(getType(), TaskResultFactory.TASK_RESULT_TYPE_JSON, config);
            long startTime = System.currentTimeMillis();
            //匹配r目录或者res目录
            if (resDir.exists() && resDir.isDirectory()) {
                findNonAlphaPng(resDir);
            } else {
                resDir = new File(inputFile, ApkConstants.RESOURCE_DIR_NAME);
                if (resDir.exists() && resDir.isDirectory()) {
                    findNonAlphaPng(resDir);
                }
            }
            ...
    }
```
com/tencent/matrix/apk/model/task/FindNonAlphaPngTask.java
```
 private void findNonAlphaPng(File file) throws IOException {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File tempFile : files) {
                    findNonAlphaPng(tempFile);
                }
            } else if (file.isFile() && file.getName().endsWith(ApkConstants.PNG_FILE_SUFFIX) && !file.getName().endsWith(ApkConstants.NINE_PNG)) {
                //匹配png且非点9图  BufferedImage位于java.awt
                BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage != null && bufferedImage.getColorModel() != null && !bufferedImage.getColorModel().hasAlpha()) {
                    //png图片但是颜色空间没有透明通道
                    String filename = file.getAbsolutePath().substring(inputFile.getAbsolutePath().length() + 1);
                    if (entryNameMap.containsKey(filename)) {
                        filename = entryNameMap.get(filename);
                    }
                    long size = file.length();
                    if (entrySizeMap.containsKey(filename)) {
                        //反混淆
                        size = entrySizeMap.get(filename).getFirst();
                    }
                    //到达阈值保存
                    if (size >= downLimitSize * ApkConstants.K1024) {
                        nonAlphaPngList.add(Pair.of(filename, file.length()));
                    }
                }
            }
        }
    }
```
html描述Find out the non-alpha png-format files whose size exceed limit size in desc order




MultiLibCheckTask
可以判断apk中是否有针对多个ABI的so
实现方法：直接判断lib文件夹下是否包含多个目录。
so文件的大小可能会在apk文件大小中占很大的比例，可以考虑在apk中只包含一个ABI版本的动态库。
MultiLibCheckTask会检查lib目录下有多少个子目录，一个子目录就单标一个ABI版本的动态库。若不超过一个子目录，则表示检测通过
com/tencent/matrix/apk/model/task/MultiLibCheckTask.java
```
 @Override
    public TaskResult call() throws TaskExecuteException {
        try {
            TaskResult taskResult = TaskResultFactory.factory(getType(), TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            JsonArray jsonArray = new JsonArray();
            //拿到lib下所有目录
            if (libDir.exists() && libDir.isDirectory()) {
                File[] dirs = libDir.listFiles();
                for (File dir : dirs) {
                    if (dir.isDirectory()) {
                        jsonArray.add(dir.getName());
                    }
                }
            }
            ((TaskJsonResult) taskResult).add("lib-dirs", jsonArray);
            //校验目录个数是否为1
            if (jsonArray.size() > 1) {
                ((TaskJsonResult) taskResult).add("multi-lib", true);
            } else {
                ((TaskJsonResult) taskResult).add("multi-lib", false);
            }
        ...
    }
```
html描述Check if there are more than one library dir in the 'lib'.



MultiSTLCheckTask
可以检测apk中的so是否静态链接STL   //todo
实现方法：通过nm工具来读取so的符号表，如果出现 std:: 即表示so静态链接了STL。
如果有多个动态库都依赖了STL，应该采用动态链接的方式而非多个动态库都去静态链接STL。
在config文件中我们配置了--toolnm参数，将其指向了sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-nm。
在检测时通过ProcessBuilder来执行命令行命令，然后读取输出，判断输出中是否含有std::字符串。
相当于在Terminal中执行这样的代码：
<nm_path> -D -C <so_path> | grep "T std::"
com/tencent/matrix/apk/model/task/MultiSTLCheckTask.java
```
 @Override
    public TaskResult call() throws TaskExecuteException {
        try {
            TaskResult taskResult = TaskResultFactory.factory(getType(), TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            List<File> libFiles = new ArrayList<>();
            JsonArray jsonArray = new JsonArray();
            //拿到so文件
            if (libDir.exists() && libDir.isDirectory()) {
                File[] dirs = libDir.listFiles();
                for (File dir : dirs) {
                    if (dir.isDirectory()) {
                        File[] libs = dir.listFiles();
                        for (File libFile : libs) {
                            if (libFile.isFile() && libFile.getName().endsWith(ApkConstants.DYNAMIC_LIB_FILE_SUFFIX)) {
                                libFiles.add(libFile);
                            }
                        }
                    }
                }
            }
            //检测stl连接 使用arm-linux-androideabi-nm -D -C *.so 检测
            for (File libFile : libFiles) {
                if (isStlLinked(libFile)) {
                    Log.i(TAG, "lib: %s has stl link", libFile.getName());
                    jsonArray.add(libFile.getName());
                }
            }
            //连接多个
            ((TaskJsonResult) taskResult).add("stl-lib", jsonArray);
            if (jsonArray.size() > 1) {
                ((TaskJsonResult) taskResult).add("multi-stl", true);
            } else {
                ((TaskJsonResult) taskResult).add("multi-stl", false);
            }
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
            return taskResult;
        } catch (Exception e) {
            throw new TaskExecuteException(e.getMessage(), e);
        }
    }
    
 
 private boolean isStlLinked(File libFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(toolnmPath, "-D", "-C", libFile.getAbsolutePath());
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            String[] columns = line.split(" ");
            Log.d(TAG, "%s", line);
            // 检测 T std:: 字符串样式，若出现，表示静态链接了stl
            if (columns.length >= 3 && columns[1].equals("T") && columns[2].startsWith("std::")) {
                return true;
            }
            line = reader.readLine();
        }
        reader.close();
        process.waitFor();
        return false;
    }   
```
html描述Check if there are more than one shared library statically linked the STL.



UncompressedFileTask
可以检测出未经压缩的文件类型
实现方法：直接利用UnzipTask中统计的各个文件的压缩前和压缩后的大小，判断压缩前和压缩后大小是否相等。
UncompressedFileTask可以指定要检测的文件类型。检测过程依赖于UnzipTask在解压时保存的每一项的size以及compressedSize。
Zip存储时可以是STORED、DEFLATED两种方式，前面这种方式只是不压缩的存储，所以这种方式下size等于compressedSize。
//todo zip算法
UncompressedFileTask会先将所有文件以文件后缀名进行归类并计算总的size、compressedSize，然后再逐个比较两者值是否一致，不一致的会保存结果。
com/tencent/matrix/apk/model/task/UncompressedFileTask.java
```
 @Override
    public TaskResult call() throws TaskExecuteException {
        try {
            TaskResult taskResult = TaskResultFactory.factory(type, TaskResultFactory.TASK_RESULT_TYPE_JSON, config);
            if (taskResult == null) {
                return null;
            }
            long startTime = System.currentTimeMillis();
            JsonArray jsonArray = new JsonArray();
            Map<String, Pair<Long, Long>> entrySizeMap = config.getEntrySizeMap();
            if (!entrySizeMap.isEmpty()) {                                                          //take advantage of the result of UnzipTask.
                for (Map.Entry<String, Pair<Long, Long>> entry : entrySizeMap.entrySet()) {
                    final String suffix = getSuffix(entry.getKey());
                    Pair<Long, Long> size = entry.getValue();
                    //符合条件的文件
                    if (filterSuffix.isEmpty() || filterSuffix.contains(suffix)) {
                        if (!uncompressSizeMap.containsKey(suffix)) {
                            uncompressSizeMap.put(suffix, size.getFirst());
                        } else {
                            //对相同类型文件合并
                            uncompressSizeMap.put(suffix, uncompressSizeMap.get(suffix) + size.getFirst());
                        }
                        if (!compressSizeMap.containsKey(suffix)) {
                            compressSizeMap.put(suffix, size.getSecond());
                        } else {
                            compressSizeMap.put(suffix, compressSizeMap.get(suffix) + size.getSecond());
                        }
                    } else {
                        Log.d(TAG, "file: %s, filter by suffix.", entry.getKey());
                    }
                }
            }

            for (String suffix : uncompressSizeMap.keySet()) {
                //压缩前后文件大小一致
                if (uncompressSizeMap.get(suffix).equals(compressSizeMap.get(suffix))) {
                    JsonObject fileItem = new JsonObject();
                    fileItem.addProperty("suffix", suffix);
                    fileItem.addProperty("total-size", uncompressSizeMap.get(suffix));
                    jsonArray.add(fileItem);
                }
            }
            ((TaskJsonResult) taskResult).add("files", jsonArray);
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
            return taskResult;
        } ....
    }
```
html描述Show uncompressed file types


DuplicatedFileTask
可以检测出冗余的文件
实现方法：通过比较文件的MD5是否相等来判断文件内容是否相同。
冗余文件应该只保留一份。
com/tencent/matrix/apk/model/task/DuplicateFileTask.java
```
 @Override
    public TaskResult call() throws TaskExecuteException {
        TaskResult taskResult = null;
        try {
            taskResult = TaskResultFactory.factory(getType(), TaskResultFactory.TASK_RESULT_TYPE_JSON, config);
            long startTime = System.currentTimeMillis();
            JsonArray jsonArray = new JsonArray();

            computeMD5(inputFile);

            ...//排序
            for (Pair<String, Long> entry : fileSizeList) {
                //同一个md5有多个文件 
                if (md5Map.get(entry.getFirst()).size() > 1) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("md5", entry.getFirst());
                    jsonObject.addProperty("size", entry.getSecond());
                    JsonArray jsonFiles = new JsonArray();
                    for (String filename : md5Map.get(entry.getFirst())) {
                        jsonFiles.add(filename);
                    }
                    jsonObject.add("files", jsonFiles);
                    jsonArray.add(jsonObject);
                }
            }
            ((TaskJsonResult) taskResult).add("files", jsonArray);
            taskResult.setStartTime(startTime);
            taskResult.setEndTime(System.currentTimeMillis());
        } catch (Exception e) {
            throw new TaskExecuteException(e.getMessage(), e);
        }
        return taskResult;
    }
    
 
  private void computeMD5(File file) throws NoSuchAlgorithmException, IOException {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File resFile : files) {
                    computeMD5(resFile);
                }
            } else {
                //计算文件的md5
                MessageDigest msgDigest = MessageDigest.getInstance("MD5");
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[512];
                int readSize = 0;
                long totalRead = 0;
                while ((readSize = inputStream.read(buffer)) > 0) {
                    msgDigest.update(buffer, 0, readSize);
                    totalRead += readSize;
                }
                inputStream.close();
                if (totalRead > 0) {
                    final String md5 = Util.byteArrayToHex(msgDigest.digest());
                    String filename = file.getAbsolutePath().substring(inputFile.getAbsolutePath().length() + 1);
                    //获取混淆前名字
                    if (entryNameMap.containsKey(filename)) {
                        filename = entryNameMap.get(filename);
                    }
                    //存入Map<String, List<String>>中
                    if (!md5Map.containsKey(md5)) {
                        md5Map.put(md5, new ArrayList<String>());
                        if (entrySizeMap.containsKey(filename)) {
                            fileSizeList.add(Pair.of(md5, entrySizeMap.get(filename).getFirst()));
                        } else {
                            fileSizeList.add(Pair.of(md5, totalRead));
                        }
                    }
                    md5Map.get(md5).add(filename);
                }
            }
        }
    }   
```
html输出 Find out the duplicated files



UnusedResourceTask
可以检测出apk中未使用的资源，对于getIdentifier获取的资源可以加入白名单
实现方法：
1. 过读取R.txt获取apk中声明的所有资源得到declareResourceSet；
2. 通过读取smali文件中引用资源的指令（包括通过reference和直接通过资源id引用资源）得出class中引用的资源classRefResourceSet；
3. 通过ApkTool解析res目录下的xml文件、AndroidManifest.xml 以及 resource.arsc 得出资源之间的引用关系；
4. 根据上述几步得到的中间数据即可确定出apk中未使用到的资源。

1 readMappingTxtFile 读取mapping文件 使用rclassProguardMap保存代码引用格式的混淆前后的资源名
com.tencent.mm.R.l.aRW -> com.tencent.mm.R.string.fade_in_property_anim

2 readResourceTxtFile 读取R文件
使用resourceDefMap保存普通资源的资源值、资源名
0x7f010001 -> R.anim.anim
使用styleableMap保存styleable类型的资源值、资源名列表
R.styleable.AVLoadingIndicatorView -> [0x7f0401f3、0x7f0401fc]

3 将所有resourceDefMap中的资源名另存到unusedResSet中，这是待删除的资源池了，后面的操作过程中将会从里面移除用到的资源
4 decodeCode 反编译dex文件到smali文件，按照特定语法格式进行匹配，找出里面引用资源的指令（包括通过reference和直接通过资源id引用资源）。
遇到资源id时，通过resourceDefMap得到对应的资源名；遇到styleable资源名时，通过styleableMap得到资源值列表，然后通过resourceDefMap得到资源名；
  普通资源直接保存名字。处理过后的资源名都保存到resourceRefSet中。这些资源就是程序真正引用的资源了。
5 decodeResources 解析res目录下的xml文件、AndroidManifest.xml以及resources.arsc文件。
xml中的遇到的资源保存到fileResMap，这里面key是一个资源的名称R.layout.xx，value是所遇到的资源的名称R.color.xxx；
values目录下的xml遇到的资源以及AndroidManifest.xml中的资源保存到valuesReferences中，类似于R.color.xxx。
fileResMap的kv经过反混淆之后，保存到了nonValueReferences中。valuesReferences中的值经过反混淆之后，保存到了resourceRefSet中。
在之前，代码中所引用的资源也保存到了这里，因此，这个集合里面的资源都是有效的。
接着，将会遍历resourceRefSet中的资源a，如果nonValueReferences中包含了这个资源a，则从unusedResSet中将删除被这个资源所引用的资源集合b。
对b会继续这么递归下去。这个意思就是如果资源a是有效的，那么a所引用的资源集合b也是有效的，将有效的资源从全量资源池中进行移除，
那么全量资源池中剩下的就需要被删除了
到了这里后，resourceRefSet里面的值都是有效值；unusedResSet里面的值都是待移除的值；但是这里还没有处理白名单问题。
最后，处理白名单，遍历unusedResSet，如果是白名单里面的值，添加到resourceRefSet中；同时也会从nonValueReferences中寻找这个资源的子资源，
添加到resourceRefSet中。
6 最后调用unusedResSet.removeAll(resourceRefSet)，将白名单集合从unusedResSet中进行移除。上报unusedResSet集合即可。


第一步：读取mapping文件
com/tencent/matrix/apk/model/task/UnusedResourcesTask.java
```
```
