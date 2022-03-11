http://liuwangshu.cn/framework/pms/5-packageparser.html

前言
在本系列的前面文章中，我介绍了PackageInstaller的初始化和安装APK过程、PMS处理APK的安装和PMS的创建过程，这些文章中经常会涉及到一个类，
那就是PackageParser，它用来在APK的安装过程中解析APK，那么APK是如何被解析的呢？这篇文章会给你答案。

1.引入PackageParser
Android世界中有很多包，比如应用程序的APK，Android运行环境的JAR包（比如framework.jar）和组成Android系统的各种动态库so等等，
由于包的种类和数量繁多，就需要进行包管理，但是包管理需要在内存中进行，而这些包都是以静态文件的形式存在的，
就需要一个工具类将这些包转换为内存中的数据结构，这个工具就是包解析器PackageParser。

在Android包管理机制（三）PMS处理APK的安装这篇文章中，我们知道安装APK时需要调用PMS的installPackageLI方法：
frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
```
private void installPackageLI(InstallArgs args, PackageInstalledInfo res) {
    ...
    PackageParser pp = new PackageParser();//1
    pp.setSeparateProcesses(mSeparateProcesses);
    pp.setDisplayMetrics(mMetrics);
    pp.setCallback(mPackageParserCallback);
    Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "parsePackage");
    final PackageParser.Package pkg;
    try {
        pkg = pp.parsePackage(tmpPackageFile, parseFlags);//2
    }
    ...
 }   
```
可以看到安装APK时，需要先在注释1处创建PackageParser，然后在注释2处调用PackageParser的parsePackage方法来解析APK。


2.PackageParser解析APK
Android5.0引入了Split APK机制，这是为了解决65536上限以及APK安装包越来越大等问题。Split APK机制可以将一个APK，拆分成多个独立APK。
在引入了Split APK机制后，APK有两种分类：
1 Single APK：安装文件为一个完整的APK，即base APK。Android称其为Monolithic。
2 Mutiple APK：安装文件在一个文件目录中，其内部有多个被拆分的APK，这些APK由一个 base APK和一个或多个split APK组成。Android称其为Cluster。
了解了APK，我们接着学习PackageParser解析APK，查看PackageParser的parsePackage方法：
frameworks/base/core/java/android/content/pm/PackageParser.java
```
public Package parsePackage(File packageFile, int flags, boolean useCaches)
           throws PackageParserException {
       Package parsed = useCaches ? getCachedResult(packageFile, flags) : null;
       if (parsed != null) {
           return parsed;
       }
       if (packageFile.isDirectory()) {//1
           parsed = parseClusterPackage(packageFile, flags);
       } else {
           parsed = parseMonolithicPackage(packageFile, flags);
       }
       cacheResult(packageFile, flags, parsed);

       return parsed;
   }
```
注释1处，如果要解析的packageFile是一个目录，说明是Mutiple APK，就需要调用parseClusterPackage方法来解析，
  如果是Single APK则调用parseMonolithicPackage方法来解析。这里以复杂的parseClusterPackage方法为例，了解了这个方法，
  parseMonolithicPackage方法自然也看的懂。
android_pms_PackageParser解析APK_1.png

frameworks/base/core/java/android/content/pm/PackageParser.java
```
private Package parseClusterPackage(File packageDir, int flags) throws PackageParserException {
        final PackageLite lite = parseClusterPackageLite(packageDir, 0);//1
       if (mOnlyCoreApps && !lite.coreApp) {//2
           throw new PackageParserException(INSTALL_PARSE_FAILED_MANIFEST_MALFORMED,
                   "Not a coreApp: " + packageDir);
       }
       ...
       try {
           final AssetManager assets = assetLoader.getBaseAssetManager();
           final File baseApk = new File(lite.baseCodePath);
           final Package pkg = parseBaseApk(baseApk, assets, flags);//3
           if (pkg == null) {
               throw new PackageParserException(INSTALL_PARSE_FAILED_NOT_APK,
                       "Failed to parse base APK: " + baseApk);
           }
           if (!ArrayUtils.isEmpty(lite.splitNames)) {
               final int num = lite.splitNames.length;//4
               pkg.splitNames = lite.splitNames;
               pkg.splitCodePaths = lite.splitCodePaths;
               pkg.splitRevisionCodes = lite.splitRevisionCodes;
               pkg.splitFlags = new int[num];
               pkg.splitPrivateFlags = new int[num];
               pkg.applicationInfo.splitNames = pkg.splitNames;
               pkg.applicationInfo.splitDependencies = splitDependencies;
               for (int i = 0; i < num; i++) {
                   final AssetManager splitAssets = assetLoader.getSplitAssetManager(i);
                   parseSplitApk(pkg, i, splitAssets, flags);//5
               }
           }
           pkg.setCodePath(packageDir.getAbsolutePath());
           pkg.setUse32bitAbi(lite.use32bitAbi);
           return pkg;
       } finally {
           IoUtils.closeQuietly(assetLoader);
       }
   }
```
注释1处调用parseClusterPackageLite方法用于轻量级解析目录文件，之所以要轻量级解析是因为解析APK是一个复杂耗时的操作，
  这里的逻辑并不需要APK所有的信息。parseClusterPackageLite方法内部会通过parseApkLite方法解析每个Mutiple APK，
  得到每个Mutiple APK对应的ApkLite（轻量级APK信息），然后再将这些ApkLite封装为一个PackageLite（轻量级包信息）并返回。
注释2处，mOnlyCoreApps用来指示PackageParser是否只解析“核心”应用，“核心”应用指的是AndroidManifest中属性coreApp值为true，
  只解析“核心”应用是为了创建一个极简的启动环境。mOnlyCoreApps在创建PMS时就一路传递过来，如果我们加密了设备，
  mOnlyCoreApps值就为true，具体的见Android包管理机制（四）PMS的创建过程这篇文章的第1小节。
  另外可以通过PackageParser的setOnlyCoreApps方法来设置mOnlyCoreApps的值。
lite.coreApp表示当前包是否包含“核心”应用，如果不满足注释2的条件就会抛出异常。
注释3处的parseBaseApk方法用于解析base APK，注释4处获取split APK的数量，根据这个数量在注释5处遍历调用parseSplitApk来解析每个split APK。
这里主要查看parseBaseApk方法，如下所示。
frameworks/base/core/java/android/content/pm/PackageParser.java
```
private Package parseBaseApk(File apkFile, AssetManager assets, int flags)
           throws PackageParserException {
       final String apkPath = apkFile.getAbsolutePath();
       String volumeUuid = null;
       if (apkPath.startsWith(MNT_EXPAND)) {
           final int end = apkPath.indexOf('/', MNT_EXPAND.length());
           volumeUuid = apkPath.substring(MNT_EXPAND.length(), end);//1
       }
       ...
       Resources res = null;
       XmlResourceParser parser = null;
       try {
           res = new Resources(assets, mMetrics, null);
           parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
           final String[] outError = new String[1];
           final Package pkg = parseBaseApk(apkPath, res, parser, flags, outError);//2
           if (pkg == null) {
               throw new PackageParserException(mParseError,
                       apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
           }
           pkg.setVolumeUuid(volumeUuid);//3
           pkg.setApplicationVolumeUuid(volumeUuid);//4
           pkg.setBaseCodePath(apkPath);
           pkg.setSignatures(null);
           return pkg;
       } catch (PackageParserException e) {
           throw e;
       }
       ...
   }
```
注释1处，如果APK的路径以/mnt/expand/开头，就截取该路径获取volumeUuid，注释3处用于以后标识这个解析后的Package，
注释4处的用于标识该App所在的存储卷UUID。
注释2处又调用了parseBaseApk的重载方法，可以看出当前的parseBaseApk方法主要是为了获取和设置volumeUuid。parseBaseApk的重载方法如下所示。
frameworks/base/core/java/android/content/pm/PackageParser.java
```
private Package parseBaseApk(String apkPath, Resources res, XmlResourceParser parser, int flags,
           String[] outError) throws XmlPullParserException, IOException {
       ...
       final Package pkg = new Package(pkgName);//1
       //从资源中提取自定义属性集com.android.internal.R.styleable.AndroidManifest得到TypedArray 
       TypedArray sa = res.obtainAttributes(parser,
               com.android.internal.R.styleable.AndroidManifest);//2
       //使用typedarray获取AndroidManifest中的versionCode赋值给Package的对应属性        
       pkg.mVersionCode = pkg.applicationInfo.versionCode = sa.getInteger(
               com.android.internal.R.styleable.AndroidManifest_versionCode, 0);
       pkg.baseRevisionCode = sa.getInteger(
               com.android.internal.R.styleable.AndroidManifest_revisionCode, 0);
       pkg.mVersionName = sa.getNonConfigurationString(
               com.android.internal.R.styleable.AndroidManifest_versionName, 0);
       if (pkg.mVersionName != null) {
           pkg.mVersionName = pkg.mVersionName.intern();
       }
       pkg.coreApp = parser.getAttributeBooleanValue(null, "coreApp", false);//3
       //获取资源后要回收
       sa.recycle();
       return parseBaseApkCommon(pkg, null, res, parser, flags, outError);
   }
```

注释1处创建了Package对象，注释2处从资源中提取自定义属性集 com.android.internal.R.styleable.AndroidManifest得到TypedArray ，
  这个属性集所在的源码位置为frameworks/base/core/res/res/values/attrs_manifest.xml。接着用TypedArray读取APK的AndroidManifest中的
  versionCode、revisionCode和versionName的值赋值给Package的对应的属性。
注释3处读取APK的AndroidManifest中的coreApp的值。
最后会调用parseBaseApkCommon方法，这个方法非常长，主要用来解析APK的AndroidManifest中的各个
标签，比如application、permission、uses-sdk、feature-group等等，其中四大组件的标签在application标签下，
  解析application标签的方法为parseBaseApplication。
frameworks/base/core/java/android/content/pm/PackageParser.java
```
  private boolean parseBaseApplication(Package owner, Resources res,
            XmlResourceParser parser, int flags, String[] outError)
        throws XmlPullParserException, IOException {
        ...
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("activity")) {//1
                Activity a = parseActivity(owner, res, parser, flags, outError, false,
                        owner.baseHardwareAccelerated);//2
                if (a == null) {
                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                    return false;
                }
                owner.activities.add(a);//3
            } else if (tagName.equals("receiver")) {
                Activity a = parseActivity(owner, res, parser, flags, outError, true, false);
                if (a == null) {
                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                    return false;
                }
                owner.receivers.add(a);
            } else if (tagName.equals("service")) {
                Service s = parseService(owner, res, parser, flags, outError);
                if (s == null) {
                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                    return false;
                }
                owner.services.add(s);
            } else if (tagName.equals("provider")) {
                Provider p = parseProvider(owner, res, parser, flags, outError);
                if (p == null) {
                    mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                    return false;
                }
                owner.providers.add(p);
             ...
            } 
        }
     ...
}
```
parseBaseApplication方法有近500行代码，这里只截取了解析四大组件相关的代码。注释1处如果标签名为activity，
 就调用注释2处的parseActivity方法解析activity标签并得到一个Activity对象（PackageParser的静态内部类），这个方法有300多行代码，
  解析一个activity标签就如此繁琐，activity标签只是Application中众多标签的一个，而Application只是AndroidManifest众多标签的一个，
  这让我们更加理解了为什么此前解析APK时要使用轻量级解析了。注释3处将解析得到的Activity对象保存在Package的列表activities中。
  其他的四大组件也是类似的逻辑。
PackageParser解析APK的代码逻辑非常庞大，基本了解本文所讲的就足够了，如果有兴趣可以自行看源码。
parseBaseApk方法主要的解析结构可以理解为以下简图。
android_pms_PackageParser解析APK_2.png


3.Package的数据结构
包被解析后，最终在内存是Package，Package是PackageParser的内部类，它的部分成员变量如下所示。
frameworks/base/core/java/android/content/pm/PackageParser.java
```
public final static class Package implements Parcelable {
    public String packageName;
    public String manifestPackageName;
    public String[] splitNames;
    public String volumeUuid;
    public String codePath;
    public String baseCodePath;
    ...
    public ApplicationInfo applicationInfo = new ApplicationInfo();
    public final ArrayList<Permission> permissions = new ArrayList<Permission>(0);
    public final ArrayList<PermissionGroup> permissionGroups = new ArrayList<PermissionGroup>(0);
    public final ArrayList<Activity> activities = new ArrayList<Activity>(0);//1
    public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
    public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
    public final ArrayList<Service> services = new ArrayList<Service>(0);
    public final ArrayList<Instrumentation> instrumentation = new ArrayList<Instrumentation>(0);
...
}
```
注释1处，activities列表中存储了类型为Activity的对象，需要注意的是这个Acticity并不是我们常用的那个Activity，
 而是PackageParser的静态内部类，Package中的其他列表也都是如此。Package的数据结构简图如下所示。
android_pms_PackageParser解析APK_3.png


从这个简图中可以发现Package的数据结构是如何设计的：
1 Package中存有许多组件，比如Acticity、Provider、Permission等等，它们都继承基类Component。
2 每个组件都包含一个info数据，比如Activity类中包含了成员变量ActivityInfo，这个ActivityInfo才是真正的Activity数据。
3 四大组件的标签内可能包含<intent-filter>来过滤Intent信息，因此需要IntentInfo来保存组件的intent信息，
  组件基类Component依赖于IntentInfo，IntentInfo有三个子类ActivityIntentInfo、ServiceIntentInfo和ProviderIntentInfo，
  不同组件依赖的IntentInfo会有所不同，比如Activity继承自Component<ActivityIntentInfo> ，Permission继承自Component<IntentInfo> 。
最终的解析的数据会封装到Package中，除此之外在解析过程中还有两个轻量级数据结构ApkLite和PackageLite，
   因为这两个数据和Package没有太大的关联就没有在上图中表示。