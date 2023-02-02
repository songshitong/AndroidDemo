https://blog.yorek.xyz/android/3rd-library/matrix-trace-plugin/

matrix-plugin插件有两个功能模块：
1 trace：给每个需要插桩的方法分配唯一的方法id，并在方法的进出口插入一段代码，为TraceCanary模块分析实际问题提供数据支撑。
2 removeUnusedResources：在合成apk之前移除apkchecker检测出来的没有用到的资源清单，可以自动化的减少最终包体积大小。

插件定义  模块：matrix-gradle-plugin
配置文件
resources/META-INF/gradle-plugins/com.tencent.matrix-plugin.properties
入口文件：
implementation-class=com.tencent.matrix.plugin.MatrixPlugin


MatrixPlugin¶
自定义的插件需要实现了Plugin接口，并在apply方法里面完成要做的事情。

在MatrixPlugin中干了两件事。
1 首先是在项目的配置阶段通过project.extensions.create(name, type)方法将插件的自定义配置项以对应的type创建并保存起来，
  之后可以通过project.name获取到对应的配置项。
com/tencent/matrix/plugin/extension/MatrixExtension.kt
kotlin/com/tencent/matrix/plugin/extension/MatrixRemoveUnusedResExtension.kt
2 其次在项目配置完毕的回调project.afterEvaluate（这个回调会在tasks执行之前进行执行）中，将要执行任务的插入到task链中并设置依赖关系。
  这样随着构建任务的一个个执行，会执行到我们的代码。
对MatrixPlugin的两个子功能模块来说，这一步实现的方式有一点区别。trace模块因为是对所有有效的方法进行插桩，需要在proguard等任务完成之后在执行，
而这个时序不太好通过依赖关系进行确定，因此选择了hook了class打包成dex的这一过程，最终达到了先插桩后打dex的目的。
而removeUnusedResources只需要在将所有资源打包成apk之前执行即可。这两个子模块将会分开讨论。
com/tencent/matrix/plugin/task/MatrixTasksManager.kt
```
 private fun createMatrixTraceTask(
            android: AppExtension,
            project: Project,
            traceExtension: MatrixTraceExtension) {
            // 注入trace模块
        MatrixTraceCompat().inject(android, project, traceExtension)
    }
    private fun createRemoveUnusedResourcesTask(
            android: AppExtension,
            project: Project,
            removeUnusedResourcesExtension: MatrixRemoveUnusedResExtension) {
         // 创建RemoveUnusedResourcesTask并设置依赖项
        project.afterEvaluate {

            if (!removeUnusedResourcesExtension.enable) {
                return@afterEvaluate
            }
            android.applicationVariants.all { variant ->
                if (Util.isNullOrNil(removeUnusedResourcesExtension.variant) ||
                        variant.name.equals(removeUnusedResourcesExtension.variant, true)) {
                    ...
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


MatrixTraceTransform
trace模块调用了MatrixTraceTransform#inject方法。
在该方法会注册MatrixTraceTransform
com/tencent/matrix/plugin/trace/MatrixTraceInjection.kt
```
 fun inject(appExtension: AppExtension,
               project: Project,
               extension: MatrixTraceExtension) {
        injectTransparentTransform(appExtension, project, extension)
        project.afterEvaluate {
            if (extension.isEnable) {
                doInjection(appExtension, project, extension)
            }
        }
    }

    private fun injectTransparentTransform(appExtension: AppExtension,
                                           project: Project,
                                           extension: MatrixTraceExtension) {
        transparentTransform = MatrixTraceTransform(project, extension)
        appExtension.registerTransform(transparentTransform!!)
    }   
```


自定义transform还有几个要素，即实现其getInputTypes、getOutputTypes、getScopes、getName、isIncremental以及最重要的transform方法。
换句话说，自定义transform需要指定什么范围的什么输入，经过怎么样的transform，最后输出什么

MatrixTraceTransform.transform
com/tencent/matrix/plugin/transform/MatrixTraceTransform.kt
```
 override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        if (transparent) {
            transparent(transformInvocation)
        } else {
            transforming(transformInvocation)
        }
    }
    
  private fun transforming(invocation: TransformInvocation) {
     ...
        MatrixTrace(
                ignoreMethodMapFilePath = config.ignoreMethodMapFilePath,
                methodMapFilePath = config.methodMapFilePath,
                baseMethodMapPath = config.baseMethodMapPath,
                blockListFilePath = config.blockListFilePath,
                mappingDir = config.mappingDir,
                project = project
        ).doTransform(
                classInputs = inputFiles,
                changedFiles = changedFiles,
                isIncremental = isIncremental,
                skipCheckClass = config.skipCheckClass,
                traceClassDirectoryOutput = outputDirectory,
                inputToOutput = inputToOutput,
                legacyReplaceChangedFile = null,
                legacyReplaceFile = null,
                uniqueOutputName = true
        )

        val cost = System.currentTimeMillis() - start
        Log.i(TAG, " Insert matrix trace instrumentations cost time: %sms.", cost)
    }   
```
com/tencent/matrix/plugin/trace/MatrixTrace.kt
```
 fun doTransform(classInputs: Collection<File>,
                    changedFiles: Map<File, Status>,
                    inputToOutput: Map<File, File>,
                    isIncremental: Boolean,
                    skipCheckClass: Boolean,
                    traceClassDirectoryOutput: File,
                    legacyReplaceChangedFile: ((File, Map<File, Status>) -> Object)?,
                    legacyReplaceFile: ((File, File) -> (Object))?,
                    uniqueOutputName: Boolean
    ) {
      ...
    }
```
这里matrix分为了三个小步骤：
1 混淆处理过程：将编译生成的mapping文件进行解析，保存到MappingCollector中；将内置黑名单以及配置的黑名单(blackListFile)
 利用MappingCollector进行混淆，利于后面在处理时直接进行匹配，这些黑名单（不需要进行插桩的类或者包）保存到blockSet中；
 解析配置的baseMethodMapFile文件，并利用MappingCollector进行混淆后，保存到collectedMethodMap文件中。
 最后收集所有目录和jar中的文件到dirInputOutMap和jarInputOutMap中，这个过程需要注意处理增量的情况。
```
 val futures = LinkedList<Future<*>>()
        val mappingCollector = MappingCollector()
        val methodId = AtomicInteger(0)
        val collectedMethodMap = ConcurrentHashMap<String, TraceMethod>()

        futures.add(executor.submit(ParseMappingTask(
                mappingCollector, collectedMethodMap, methodId, config)))

        val dirInputOutMap = ConcurrentHashMap<File, File>()
        val jarInputOutMap = ConcurrentHashMap<File, File>()

        for (file in classInputs) {
            if (file.isDirectory) {
                futures.add(executor.submit(CollectDirectoryInputTask(
                        directoryInput = file,
                        mapOfChangedFiles = changedFiles,
                        mapOfInputToOutput = inputToOutput,
                        isIncremental = isIncremental,
                        traceClassDirectoryOutput = traceClassDirectoryOutput,
                        legacyReplaceChangedFile = legacyReplaceChangedFile,
                        legacyReplaceFile = legacyReplaceFile,
                        // result
                        resultOfDirInputToOut = dirInputOutMap
                )))
            } else {
                val status = Status.CHANGED
                futures.add(executor.submit(CollectJarInputTask(
                        inputJar = file,
                        inputJarStatus = status,
                        inputToOutput = inputToOutput,
                        isIncremental = isIncremental,
                        traceClassFileOutput = traceClassDirectoryOutput,
                        legacyReplaceFile = legacyReplaceFile,
                        uniqueOutputName = uniqueOutputName,
                        // result
                        resultOfDirInputToOut = dirInputOutMap,
                        resultOfJarInputToOut = jarInputOutMap
                )))
            }
        }

        for (future in futures) {
            future.get()
        }
        futures.clear()

        Log.i(TAG, "[doTransform] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - start)
```
保存到
com/tencent/matrix/trace/Configuration.java
```
 public int parseBlockFile(MappingCollector processor) {
        //  "[package]\n"
        //            + "-keeppackage android/\n"
        //            + "-keeppackage com/tencent/matrix/\n";
        String blockStr = TraceBuildConstants.DEFAULT_BLOCK_TRACE
                + FileUtil.readFileAsString(blockListFilePath);
        String[] blockArray = blockStr.trim().replace("/", ".").replace("\r", "").split("\n");
        if (blockArray != null) {
            for (String block : blockArray) {
                if (block.length() == 0) {
                    continue;
                }
                if (block.startsWith("#")) {
                    continue;
                }
                if (block.startsWith("[")) {
                    continue;
                }
                if (block.startsWith("-keepclass ")) {
                    block = block.replace("-keepclass ", "");
                    blockSet.add(processor.proguardClassName(block, block));
                } else if (block.startsWith("-keeppackage ")) {
                    block = block.replace("-keeppackage ", "");
                    blockSet.add(processor.proguardPackageName(block, block));
                }
            }
        }
        return blockSet.size();
    }
```

2 遍历dirInputOutMap和jarInputOutMap中的所有class文件的所有非抽象方法，在方法结尾时判断该方法是不是空方法、是不是get/set方法、
是不是默认或匿名构造方法、以及是不是黑名单方法，这些方法属于被过滤掉的方法；而其他方法将会被插桩。这两种类型的方法会被记录下来，
分别保存在app/build/outputs/mapping/debug/ignoreMethodMapping.txt、app/build/outputs/mapping/debug/methodMapping.txt中。
这一步是ASM实现的，但是只有一些判断逻辑，只读入了文件，不涉及到字节码的插入以及生成文件的回写，代码位于MethodCollector中。
```
start = System.currentTimeMillis()
val methodCollector = MethodCollector(executor, mappingCollector, methodId, config, collectedMethodMap)
methodCollector.collect(dirInputOutMap.keys, jarInputOutMap.keys)
Log.i(TAG, "[doTransform] Step(2)[Collection]... cost:%sms", System.currentTimeMillis() - start)
```