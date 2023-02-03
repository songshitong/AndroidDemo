RemoveUnusedResourcesTask
RemoveUnusedResourcesTask的任务是在打包后以ZIP形式读取老包，按照ApkChecker在打包时检测出来的没有用到的资源列表
（该检测任务的代码在matrix/matrix-android/matrix-apk-canary/src/main/java/com/tencent/matrix/apk/model/task/UnusedResourcesTask.java，
里面的其他相关代码也非常具有参考价值）以及其他配置项，选择性的复制里面的项目到新包，然后签名等任务。
这个任务针对的是apk，我们在分析MatrixPlugin的代码时提到了其Task之间的依赖关系可以推理出这一点。

RemoveUnusedResourcesTask 与 shrinkResources 的区别？
shrinkResources对资源的自动移除，指的是将没有用到的资源替换为占位的非常小的资源，但是不会彻底从资源库中进行删除；此外，也没有处理resources.arsc文件。
至于为什么Google没有解决这两个问题，原因可以参考包体积优化——shrinkResources。
https://issuetracker.google.com/issues/37010152
而RemoveUnusedResourcesTask则会物理删除这些资源文件。

回到RemoveUnusedResourcesTask的具体实现，这是一个Task。Task执行时会从@TaskAction修饰的方法开始执行。
com/tencent/matrix/plugin/task/MatrixTasksManager.kt
```
  fun createMatrixTasks(android: AppExtension,
                          project: Project,
                          traceExtension: MatrixTraceExtension,
                          removeUnusedResourcesExtension: MatrixRemoveUnusedResExtension) {
        createMatrixTraceTask(android, project, traceExtension)
        createRemoveUnusedResourcesTask(android, project, removeUnusedResourcesExtension)
    }
    
    
```
com/tencent/matrix/plugin/task/RemoveUnusedResourcesTask.kt
```
 private fun createRemoveUnusedResourcesTask(
            android: AppExtension,
            project: Project,
            removeUnusedResourcesExtension: MatrixRemoveUnusedResExtension) {
        project.afterEvaluate {
            if (!removeUnusedResourcesExtension.enable) {
                return@afterEvaluate
            }
            android.applicationVariants.all { variant ->
                if (Util.isNullOrNil(removeUnusedResourcesExtension.variant) ||
                        variant.name.equals(removeUnusedResourcesExtension.variant, true)) {
                    ...
                    //注册RemoveUnusedResourcesTaskV2或者RemoveUnusedResourcesTask
                    val removeUnusedResourcesTaskProvider = if (removeUnusedResourcesExtension.v2) {
                        val action = RemoveUnusedResourcesTaskV2.CreationAction(
                                CreationConfig(variant, project), removeUnusedResourcesExtension
                        )
                        project.tasks.register(action.name, action.type, action)
                    } else {
                        val action = RemoveUnusedResourcesTask.CreationAction(
                                CreationConfig(variant, project), removeUnusedResourcesExtension
                        )
                        project.tasks.register(action.name, action.type, action)
                    }

                    variant.assembleProvider?.configure {
                        it.dependsOn(removeUnusedResourcesTaskProvider)
                    }

                    removeUnusedResourcesTaskProvider.configure {
                        it.dependsOn(variant.packageApplicationProvider)
                    }
                }
            }
        }
    }
```
com/tencent/matrix/plugin/task/RemoveUnusedResourcesTask.kt
```
 @TaskAction
    fun removeResources() {
        val symbolDirName = AgpCompat.getIntermediatesSymbolDirName()
        val signingConfig = AgpCompat.getSigningConfig(variant)
        //R.text位置 例如app\build\intermediates\runtime_symbol_list\debug\R.txt
        val rTxtPath = "${project.buildDir.absolutePath}/intermediates/${symbolDirName}/${variant.name}/R.txt"
        variant.outputs.forEach { output ->
            //apk位置
            val unsignedApkPath = output.outputFile.absolutePath
            val startTime = System.currentTimeMillis()
            removeUnusedResources(unsignedApkPath, rTxtPath, signingConfig)
            Log.i(TAG, "cost time %f s", (System.currentTimeMillis() - startTime) / 1000.0f)
        }
    }
```
removeResources方法的作用是获取到apk包的地址、拼凑出R.txt文件的地址、签名配置，最后调用removeUnusedResources方法开始移除资源。
这个方法比较长，我们分段看一下。

获取unusedResources和ignoreRes，并在unusedResources中剔除需要忽略的资源。这样剩下的都是需要一个个删除的资源了
```
private fun removeUnusedResources(
            originalApk: String,
            rTxtFile: String,
            signingConfig: SigningConfig?) {
   ...         
   val inputFile = File(originalApk)
    for (res in ignoreRes) {
        ignoreResources.add(Util.globToRegexp(res))
    }

    val iterator = unusedResources.iterator()
    while (iterator.hasNext()) {
        val res = iterator.next()
        if (ignoreResource(res)) {
            iterator.remove()
            Log.i(TAG, "ignore unused resources %s", res)
        }
    }
    Log.i(TAG, "unused resources count:%d", unusedResources.size)
    ...                    
}
```

在apk目录下创建_shrinked后缀的apk空文件，作为处理后的apk。然后调用readResourceTxtFile方法读取r.txt文件并将里面的资源信息、
样式信息保存到各自的map中
R.txt格式例如：
int anim fade_in 0x7f010000
int animator view_anim_in 0x7f020000
int array exo_controls_playback_speeds 0x7f030000
int attr actionBarDivider 0x7f040000
int color design_dark_default_color_on_primary 0x7f060079
int dimen design_snackbar_elevation 0x7f070086
int drawable quit_bg 0x7f080163
int id ALT 0x7f0a0000
int layout test_toolbar 0x7f0d008f
int plurals content_description 0x7f0e0002
int raw keep_arcore 0x7f0f0000
int raw zxing_beep 0x7f0f0001
int string AUTH_REQUIRE_TITLE 0x7f100000
int style ExoStyledControls 0x7f110121
int[] styleable AppCompatTheme { 0x01010057, 0x010100ae, 0x7f040000, 0x7f040001, 0x7f040002, 0x7f040003, 0x7f040004, 0x7f040005, 0x7f040006, 0x7f040007, 0x7f040008, 0x7f040009, 0x7f04000a, 0x7f04000b, 0x7f04000c, 0x7f04000e, 0x7f04000f, 0x7f040010, 0x7f040011, 0x7f040012, 0x7f040013, 0x7f040014, 0x7f040015, 0x7f040016, 0x7f040017, 0x7f040018, 0x7f040019, 0x7f04001a, 0x7f04001b, 0x7f04001c, 0x7f04001d, 0x7f04001e, 0x7f04001f, 0x7f040020, 0x7f040024, 0x7f040027, 0x7f040028, 0x7f040029, 0x7f04002a, 0x7f04003a, 0x7f040066, 0x7f04007b, 0x7f04007c, 0x7f04007d, 0x7f04007e, 0x7f04007f, 0x7f040084, 0x7f040085, 0x7f040092, 0x7f04009c, 0x7f0400ce, 0x7f0400cf, 0x7f0400d0, 0x7f0400d2, 0x7f0400d3, 0x7f0400d4, 0x7f0400d5, 0x7f0400e6, 0x7f0400e8, 0x7f0400f2, 0x7f04010e, 0x7f04013a, 0x7f04013b, 0x7f04013c, 0x7f040140, 0x7f040145, 0x7f040156, 0x7f040157, 0x7f04015a, 0x7f04015b, 0x7f04015c, 0x7f0401f2, 0x7f040200, 0x7f040297, 0x7f040298, 0x7f040299, 0x7f04029a, 0x7f04029d, 0x7f04029e, 0x7f04029f, 0x7f0402a0, 0x7f0402a1, 0x7f0402a2, 0x7f0402a3, 0x7f0402a4, 0x7f0402a5, 0x7f04032f, 0x7f040330, 0x7f040331, 0x7f040347, 0x7f040349, 0x7f040354, 0x7f040356, 0x7f040357, 0x7f040358, 0x7f040385, 0x7f040386, 0x7f040387, 0x7f040388, 0x7f0403c7, 0x7f0403c8, 0x7f0403ef, 0x7f040428, 0x7f04042a, 0x7f04042b, 0x7f04042c, 0x7f04042e, 0x7f04042f, 0x7f040430, 0x7f040431, 0x7f040437, 0x7f040438, 0x7f040468, 0x7f040469, 0x7f04046b, 0x7f04046c, 0x7f040491, 0x7f04049a, 0x7f04049b, 0x7f04049c, 0x7f04049d, 0x7f04049e, 0x7f04049f, 0x7f0404a0, 0x7f0404a1, 0x7f0404a2, 0x7f0404a3 }
int styleable AppCompatTheme_android_windowIsFloating 0
int xml provider_paths 0x7f130001
```
...
val outputApk = inputFile.parentFile.absolutePath + "/" + inputFile.name.substring(0, inputFile.name.indexOf('.')) + "_shrinked.apk"
val outputFile = File(outputApk)
if (outputFile.exists()) {
    Log.w(TAG, "output apk file %s is already exists! It will be deleted anyway!", outputApk)
    outputFile.delete()
    outputFile.createNewFile()
}

val zipInputFile = ZipFile(inputFile)

zipOutputStream = ZipOutputStream(outputFile.outputStream())

val resourceMap = HashMap<String, Int>()
val styleableMap = HashMap<String, Array<Pair<String, Int>>>()
val resTxtFile = File(rTxtFile)
RemoveUnusedResourceHelper.readResourceTxtFile(resTxtFile, resourceMap, styleableMap)
...
```
readResourceTxtFile方法会解析R.txt文件，该文件中的数据格式可能有两种：
1 资源数据，每一行代表一个资源，这些数据保存到了resourceMap中。
key为资源名（R.dimen.vip_text_size_small），value为id值：
int dimen vip_text_size_small 0x7f070468
int drawable _50200_rd_attachment_item_save_selector 0x7f080006
int styleable ActionBar_titleTextStyle 28
回到主干上，在解析完R.txt文件并将解析结果保存到两个map后，就可以先将unusedResources对应的资源从resourceMap中进行移除。等待后面回写R.txt文件时，
unusedResources就不会出现在R.txt中了。同时，使用removeResources保存要删除的资源名与对应的id
```
 val removeResources = HashMap<String, Int>()
for (resName in unusedResources) {
    if (!ignoreResource(resName)) {
        val removed = resourceMap.remove(resName)
        if (removed != null) {
            removeResources[resName] = removed
        }
    }
}
```
下面开始真正的执行remove操作了。这里的思路是遍历APK这个ZIP文件的每一项：
1 如果该项是以res/开头的，说明是资源文件。根据ZipEntry的名称拼出对应的资源名，如果该资源名需要被删除，则不添加到output的APK包中；
  否则，如果不需要被删除，则添加到output的APK中。
2 如果自定义配置中配置了需要签名，则META-INF/目录都忽略，不需要执行复制的操作。因为output的APK在后面的签名环节会生成这些内容。
3 如果需要删除resources.arsc中的没有用到的资源项。则会将输入的APK中的这个ZipEntry解压到本地，然后使用ArscReader读取并从中移除没有用到的资源项，
 操作完成后写回到resources_shrinked.arsc文件中，并将这个文件添加到output的APK中。这样就达到了删除resources.arsc中的没有用到的资源项的目的。
 当然，这一步的操作比较繁琐，需要对arsc文件了解非常深，这里限于篇幅不做过多讨论。 //todo
```
for (zipEntry in zipInputFile.entries()) {
            // 第一步，操作资源文件
                    if (zipEntry.name.startsWith("res/")) {
                        val resourceName = entryToResouceName(zipEntry.name)
                        if (!Util.isNullOrNil(resourceName)) {
                            if (removeResources.containsKey(resourceName)) {
                                Log.i(TAG, "remove unused resource %s", resourceName)
                                continue
                            } else {
                                RemoveUnusedResourceHelper.addZipEntry(zipOutputStream, zipEntry, zipInputFile);
                            }
                        } else {
                            RemoveUnusedResourceHelper.addZipEntry(zipOutputStream, zipEntry, zipInputFile);
                        }
                    } else {
                    // 第二步，META-INF签名文件
                        if (needSign && zipEntry.name.startsWith("META-INF/")) {
                            continue
                        } else {
                        // 第三步，处理resources.arsc文件
                            if (shrinkArsc && zipEntry.name.equals("resources.arsc", true) && unusedResources.size > 0) {
                                val srcArscFile = File(inputFile.parentFile.absolutePath + "/resources.arsc");
                                val destArscFile = File(inputFile.parentFile.absolutePath + "/resources_shrinked.arsc")
                                if (srcArscFile.exists()) {
                                    srcArscFile.delete()
                                    srcArscFile.createNewFile()
                                }
                                RemoveUnusedResourceHelper.unzipEntry(zipInputFile, zipEntry, srcArscFile)

                                val reader = ArscReader(srcArscFile.absolutePath)
                                val resTable = reader.readResourceTable()
                                for (resName in removeResources.keys) {
                                    ArscUtil.removeResource(resTable, removeResources[resName]!!, resName)
                                }
                                val writer = ArscWriter(destArscFile.absolutePath)
                                writer.writeResTable(resTable)
                                Log.i(TAG, "Shrink resources.arsc size %f KB", (srcArscFile.length() - destArscFile.length()) / 1024.0)
                                RemoveUnusedResourceHelper.addZipEntry(zipOutputStream, zipEntry, destArscFile)
                            } else {
                                RemoveUnusedResourceHelper.addZipEntry(zipOutputStream, zipEntry, zipInputFile);
                            }
                        }
                    }
                }
```

这样，我们得到了一个处理之后的APK文件，下面就是对其进行签名的操作了。签名完成之后，将老包备份为xxx_back.apk，新包重命名为老包的名称。
这样操作之后，不会影响该task之后的打包流程，对其他流程来说是没有任何感知的
```
 if (needSign) {
                    Log.i(TAG, "Sign apk...")
                    val processBuilder = ProcessBuilder()
                    //使用apksigner签名
                    processBuilder.command(apksigner, "sign", "-v",
                            "--ks", signingConfig!!.storeFile?.absolutePath,
                            "--ks-pass", "pass:" + signingConfig.storePassword,
                            "--key-pass", "pass:" + signingConfig.keyPassword,
                            "--ks-key-alias", signingConfig.keyAlias,
                            outputFile.absolutePath)
                    val process = processBuilder.start()
                    process.waitFor()
                    if (process.exitValue() != 0) {
                        throw GradleException(process.errorStream.bufferedReader().readLines()
                                .joinTo(StringBuilder(), "\n").toString())
                    }
                }
                val backApk = inputFile.parentFile.absolutePath + "/" + inputFile.name.substring(0, inputFile.name.indexOf('.')) + "_back.apk"
                inputFile.renameTo(File(backApk))
                outputFile.renameTo(File(originalApk))
```

最后，清理一下样式资源文件，并将留下来的资源、样式重新写回到R.txt中。在这一步中，一个样式资源只要有一个子项被用到，都不会被剔除。
```
 //modify R.txt to delete the removed resources
                if (removeResources.isNotEmpty()) {
                    val styleableItera = styleableMap.keys.iterator()
                    while (styleableItera.hasNext()) {
                        val styleable = styleableItera.next()
                        val attrs = styleableMap[styleable]
                        var j = 0
                        for (i in 0 until (attrs!!.size)) {
                            j = i
                            if (!removeResources.containsValue(attrs[i].right)) {
                                break
                            }
                        }
                        if (attrs.size > 0 && j == attrs.size) {
                            Log.i(TAG, "removed styleable $styleable")
                            styleableItera.remove()
                        }
                    }
                    val newResTxtFile = resTxtFile.parentFile.absolutePath + "/" + resTxtFile.name.substring(0, resTxtFile.name.indexOf('.')) + "_shrinked.txt"
                    //删除后写入R.txt
                    RemoveUnusedResourceHelper.shrinkResourceTxtFile(newResTxtFile, resourceMap, styleableMap)

                    //Other plugins such as "Tinker" may depend on the R.txt file, so we should not modify R.txt directly .
                    //new File(newResTxtFile).renameTo(resTxtFile);
                }
```
RemoveUnusedResourcesTask依赖的输入源ApkChecker在做包体积大小监控中的规则监控时，非常好用，可以帮助我们分析出具体的包大小增长的原因

unusedResources的来源