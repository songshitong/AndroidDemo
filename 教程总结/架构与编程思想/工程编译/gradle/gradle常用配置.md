


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