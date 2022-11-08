https://juejin.cn/post/6915258663342964744
compileSdkVersion
compileSdkVersion 告诉 Gradle 用哪个 Android SDK 版本编译你的应用。它纯粹只是在编译的时候使用。当你修改了 compileSdkVersion 的时候，
可能会出现新的编译警告、编译错误等（你真的应该修复这些警告，他们的出现一定是有原因的）。需要强调的是修改 compileSdkVersion 不会改变运行时的行为，
compileSdkVersion 并不会打包进APK里只是在编译时使用
targetSdkVersion
targetSdkVersion 是 Android 系统提供前向兼容的主要手段。这是什么意思呢？随着 Android 系统的升级，某个 API 或者模块的行为可能会发生改变，
但是为了保证APK 的行为还是和以前一致。只要 APK 的 targetSdkVersion 不变，即使这个 APK 安装在新 Android 系统上，其行为还是保持老的系统上的行为，
这样就保证了系统对老应用的前向兼容性。




build配置参数
```
1
productFlavors {
        production {
            applicationId = "com.myapp.app"
            resValue "string", "authority", "com.facebook.app.FacebookContentProvider5435651423234"
        }
        development {
            applicationId = "com.myapp.development"
            resValue "string", "authority", "com.facebook.app.FacebookContentProvider2134564533421"
        }
        qa {
            applicationId = "com.myapp.qa"
            resValue "string", "authority", "com.facebook.app.FacebookContentProvider29831237981287319"
        }
}
2
 manifestPlaceholders = [ activityLabel:"defaultName"]
```
manifest使用
```
1
<provider
    android:name="com.facebook.FacebookContentProvider"
    android:authorities="@string/authority"
    android:exported="true" />
2
<activity android:name=".MainActivity" android:label="${activityLabel}" >
```


签名相关
```
 signingConfigs {
        debug {
            keyAlias '11'
            keyPassword '22'
            storeFile file('ebug.jks')
            storePassword 'a333'
            enableV2Signing true //指定签名版本
            enableV1Signing true
        }
    }
    buildTypes {
        debug {
            signingConfig signingConfigs.debug
            debuggable true
            minifyEnabled false
        }
    }
```

