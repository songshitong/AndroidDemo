
https://blog.csdn.net/yingaizhu/article/details/118972148
FileProvider 是 ContentProvider 的一个特殊子类
定义 FileProvider
```
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.owen.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <!-- ...... -->
</provider>
```
android:authorities 属性为 FileProvider 生成内容 URI 的授权，授权字符串必须保证唯一（通常使用包名组装）；
设置 android:exported 属性为 false（FileProvider 不需要对外公开）；
设置 android:grantUriPermissions 属性值为 true，允许给文件授予临时访问权限


指定可用文件
FileProvider 只能为事先指定目录下的文件生成内容 URI
首先需要创建一个 XML 资源文件，存放在 res/xml 目录下，XML 文件以 <patchs> 为根节点，在根节点下必须一个或者多个表示存储空间和路径的节点

在此 XML 文件中，<path> 可包含以下类型的子节点：

<files-path>：表示在应用内部存储空间中 files/ 子目录，这个目录路径跟 Context.getFilesDir() 返回的一致。
<cache-path>：表示在应用内部存储空间中 cache/ 子目录，这个目录路径跟 Context.getCacheDir() 返回的一致。
<external-path>：表示在应用外部存储空间中的根目录，这个目录路径跟 Environment.getExternalStorageDirectory() 返回的一致。
<external-files-path>：表示在应用外部存储空间中 files/ 子目录，这个目录路径跟 Context.getExternalFilesDir(String)、Context.getExternalFilesDir(null) 返回的一致。
<external-cache-path>：表示在应用外部存储空间中 cache/ 子目录，这个目录路径跟 Context.getExternalCacheDir() 返回的一致。
<external-media-path>：表示在应用外部存储空间中媒体子目录，这个目录路径跟 Context.getExternalMediaDirs() 返回的一致（注意：这个目录只在 API 21+ 的设备上有效）。
在这些表示目录路径的子节点中，都包含以下两个属性：

name：内容 URI 路径片段。为了增强安全性，这个值用来隐藏文件子目录的详细路径信息，也就是在内容 URI 中，用这个属性值替代子目录的路径信息。
path：需要共享文件所在的子目录详细路径，这个值是真实存在的路径。必须注意的是，这个属性值必须是一个子目录，而不能特定的文件或者一系列文件。你可以通过文件名共享单个文件，但是不能使用通配符指定多个文件



在 FileProvider 中引用目录配置
在应用清单文件中的 <provider> 标签内部，使用 <meta-data> 子标签引用目录配置 XML 资源，其中 android:name 属性值必须是 android.support.FILE_PROVIDER_PATHS， android:resources 引用定义好的 XML 资源文件
```
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.owen.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```