
https://blog.csdn.net/lkr_lkr/article/details/93978270
```
 android.libraryVariants.all { variant ->
        if (variant.buildType.name == "release") {
            // 修改aar名称
            variant.outputs.all { output ->
                outputFileName = "${project.name}-V${android.defaultConfig.versionName}_${android.defaultConfig.versionCode}-${getGitHeadRefsSuffix}-${getFormatDate()}${isEmpty(variant.flavorName) ? "" : "-${variant.flavorName}"}-${variant.buildType.name}.aar"
            }
            // 复制aar到指定目录
            variant.assemble.doLast {
                variant.outputs.all { output ->
                    // 输出目录在：项目目录/build/outputs/aar/日期-gitCommit/
                    def outputPath = [project.rootDir.absolutePath, "build", "outputs", "aar", releaseTime() + "-" + getGitHeadRefsSuffix()].join(File.separator)
                    copy {
                        from output.outputFile
                        into outputPath
                    }
                    // 输入依赖语句
                    gradle.dependencieInfo.add("api(name: '${output.outputFile.name.replace('.aar', '')}', ext: 'aar')")
                    println("${gradle.dependencieInfo.join("\n")}")
                }
            }
        }
    }
```

```
android{
	    libraryVariants.all { variant ->
        if (variant.buildType.name == 'release') {
            variant.assemble.doLast {
                variant.outputs.each { output ->
                    def outputFile = output.outputFile
                    if (outputFile != null && outputFile.name.endsWith('release.aar')) {
                        def fileName = "${project.name}${variant.flavorName}_${android.defaultConfig.versionName}_${releaseTime()}"
                        def outputPath = "../output/aar"
                        copy {
                            from outputFile
                            into outputPath
                            rename { fileName + ".aar" }
                        }
                    }
                }
            }
        }
    }
}
```

For the latest version of Gradle 5+
```
defaultConfig {
    ...
    versionName "some-version-name-or-number"
    setProperty("archivesBaseName", "${archivesBaseName}-$versionName")
    ...
}
```

Using Gradle 6+ and AGP 4+
```
afterEvaluate {
    android.libraryVariants.all { variant ->
        variant.variantData.outputFactory.apkDataList.each { apkData ->
            if (apkData.outputFileName.endsWith('.aar')) {
                apkData.outputFileName = "${project.name}-${buildType.name}-anything-you-want.aar"
            }
        }
    }
}
```