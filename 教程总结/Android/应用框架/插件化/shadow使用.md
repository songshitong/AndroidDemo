
存在的坑
1 so文件可能无法使用
https://github.com/Tencent/Shadow/issues/628
shadow替换activity为shadowActivity，导致jni的c/c++接口签名对应不上了
2 不支持系统app  https://github.com/Tencent/Shadow/issues/1195
  系统APP使用可能出问题
3 插件进程可能在后台被杀掉
可能需要增加前台服务保活  https://github.com/Tencent/Shadow/issues/1038


//todo 深度使用  尝试自己写写

参考
sample\source\sample-plugin\sample-app
```
apply plugin: 'com.tencent.shadow.plugin'
 dependencies {
        classpath 'com.tencent.shadow.core:runtime'
        classpath 'com.tencent.shadow.core:activity-container'
        classpath 'com.tencent.shadow.core:gradle-plugin'
        classpath "org.javassist:javassist:$javassist_version"
    }

shadow {
    transform {
//        useHostContext = ['abc']
    }

    packagePlugin {
        pluginTypes {
            debug {
                loaderApkConfig = new Tuple2('sample-loader-debug.apk', ':sample-loader:assembleDebug')
                runtimeApkConfig = new Tuple2('sample-runtime-debug.apk', ':sample-runtime:assembleDebug')
                pluginApks {
                    pluginApk1 {
                        businessName = 'sample-plugin-app'
                        partKey = 'sample-plugin-app'
                        buildTask = ':sample-app:assemblePluginDebug'
                        apkPath = 'projects/sample/source/sample-plugin/sample-app/build/outputs/apk/plugin/debug/sample-app-plugin-debug.apk'
                        hostWhiteList = ["com.tencent.shadow.sample.host.lib"]
                        dependsOn = ['sample-base']
                    }
                    ....
                }
            }

            release {
                loaderApkConfig = new Tuple2('sample-loader-release.apk', ':sample-loader:assembleRelease')
                runtimeApkConfig = new Tuple2('sample-runtime-release.apk', ':sample-runtime:assembleRelease')
                pluginApks {
                    pluginApk1 {
                        businessName = 'sample-plugin-app'
                        partKey = 'sample-plugin-app'
                        buildTask = ':sample-app:assemblePluginRelease'
                        apkPath = 'projects/sample/source/sample-plugin/sample-app/build/outputs/apk/plugin/release/sample-app-plugin-release.apk'
                        hostWhiteList = ["com.tencent.shadow.sample.host.lib"]
                        dependsOn = ['sample-base']
                    }
                    ...
                }
            }
        }

        loaderApkProjectPath = 'projects/sample/source/sample-plugin/sample-loader'
        runtimeApkProjectPath = 'projects/sample/source/sample-plugin/sample-runtime'

        archiveSuffix = System.getenv("PluginSuffix") ?: ""
        archivePrefix = 'plugin'
        destinationDir = "${getRootProject().getBuildDir()}"

        version = 4
        compactVersion = [1, 2, 3]
        uuidNickName = "1.1.5"
    }    
```

判断当前是否为插件
PluginChecker.isPluginMode() ? "当前环境：插件模式" : "当前环境：独立安装"