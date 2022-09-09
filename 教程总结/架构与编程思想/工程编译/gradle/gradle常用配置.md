
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

