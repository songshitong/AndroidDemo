
https://stackoverflow.com/questions/60878599/error-building-android-library-direct-local-aar-file-dependencies-are-not-supp
Direct local .aar file dependencies are not supported when building an AAR
1
build.gradle file
configurations.maybeCreate("default")
artifacts.add("default", file('spotify-app-remote-release-0.7.1.aar'))
settings.gradle
include ':spotify-app-remote'
build.gradle
api project(':spotify-app-remote')

2
新建lib_xx目录
添加build.gradle和对应的aar
```
configurations.maybeCreate("default")
artifacts.add("default", file('xx.aar'))
```
在setting.gradle中配置
include ':lib_xx'
使用对应的aar
api project(':lib_xx')
