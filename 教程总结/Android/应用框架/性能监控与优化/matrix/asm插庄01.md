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
com/tencent/matrix/trace/MethodCollector.java
```
public void collect(Set<File> srcFolderList, Set<File> dependencyJarList) throws ExecutionException, InterruptedException {
        List<Future> futures = new LinkedList<>();

        for (File srcFile : srcFolderList) {
            ArrayList<File> classFileList = new ArrayList<>();
            if (srcFile.isDirectory()) {
                listClassFiles(classFileList, srcFile);
            } else {
                classFileList.add(srcFile);
            }
            //通过asm加载文件
            for (File classFile : classFileList) {
                futures.add(executor.submit(new CollectSrcTask(classFile)));
            }
        }

        for (File jarFile : dependencyJarList) {
            futures.add(executor.submit(new CollectJarTask(jarFile)));
        }

        for (Future future : futures) {
            future.get();
        }
        futures.clear();

        futures.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                saveIgnoreCollectedMethod(mappingCollector);
            }
        }));

        futures.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                saveCollectedMethod(mappingCollector);
            }
        }));

        for (Future future : futures) {
            future.get();
        }
        futures.clear();
    }
```

3 字节码的插入功能，由于操作了字节码，所以需要将操作后的文件写入到指定位置，功能上最为复杂。
```
 start = System.currentTimeMillis()
        val methodTracer = MethodTracer(executor, mappingCollector, config, methodCollector.collectedMethodMap, methodCollector.collectedClassExtendMap)
        val allInputs = ArrayList<File>().also {
            it.addAll(dirInputOutMap.keys)
            it.addAll(jarInputOutMap.keys)
        }
        val traceClassLoader = TraceClassLoader.getClassLoader(project, allInputs)
        methodTracer.trace(dirInputOutMap, jarInputOutMap, traceClassLoader, skipCheckClass)

        Log.i(TAG, "[doTransform] Step(3)[Trace]... cost:%sms", System.currentTimeMillis() - start)
```
com/tencent/matrix/trace/MethodTracer.java
```
    public void trace(Map<File, File> srcFolderList, Map<File, File> dependencyJarList, ClassLoader classLoader, boolean ignoreCheckClass) throws ExecutionException, InterruptedException {
        List<Future> futures = new LinkedList<>();
        traceMethodFromSrc(srcFolderList, futures, classLoader, ignoreCheckClass);
        traceMethodFromJar(dependencyJarList, futures, classLoader, ignoreCheckClass);
        for (Future future : futures) {
            future.get();
        }
        if (traceError) {
            throw new IllegalArgumentException("something wrong with trace, see detail log before");
        }
        futures.clear();
    }

    private void traceMethodFromSrc(Map<File, File> srcMap, List<Future> futures, final ClassLoader classLoader, final boolean skipCheckClass) {
        if (null != srcMap) {
            for (Map.Entry<File, File> entry : srcMap.entrySet()) {
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        innerTraceMethodFromSrc(entry.getKey(), entry.getValue(), classLoader, skipCheckClass);
                    }
                }));
            }
        }
    } 
    

 private void innerTraceMethodFromSrc(File input, File output, ClassLoader classLoader, boolean ignoreCheckClass) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }

        for (File classFile : classFileList) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                final String changedFileInputFullPath = classFile.getAbsolutePath();
                final File changedFileOutput = new File(changedFileInputFullPath.replace(input.getAbsolutePath(), output.getAbsolutePath()));

                if (changedFileOutput.getCanonicalPath().equals(classFile.getCanonicalPath())) {
                    throw new RuntimeException("Input file(" + classFile.getCanonicalPath() + ") should not be same with output!");
                }

                if (!changedFileOutput.exists()) {
                    changedFileOutput.getParentFile().mkdirs();
                }
                changedFileOutput.createNewFile();
                //过滤"R.class", "R$", "Manifest", "BuildConfig"
                if (MethodCollector.isNeedTraceFile(classFile.getName())) {
                    is = new FileInputStream(classFile);
                    ClassReader classReader = new ClassReader(is);
                    ClassWriter classWriter = new TraceClassWriter(ClassWriter.COMPUTE_FRAMES, classLoader);
                    ClassVisitor classVisitor = new TraceClassAdapter(AgpCompat.getAsmApi(), classWriter);
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    is.close();

                    byte[] data = classWriter.toByteArray();

                    if (!ignoreCheckClass) {
                        try {
                            ClassReader cr = new ClassReader(data);
                            ClassWriter cw = new ClassWriter(0);
                            ClassVisitor check = new CheckClassAdapter(cw);
                            cr.accept(check, ClassReader.EXPAND_FRAMES);
                        } catch (Throwable e) {
                            System.err.println("trace output ERROR : " + e.getMessage() + ", " + classFile);
                            traceError = true;
                        }
                    }
                    //写入output
                    if (output.isDirectory()) {
                        os = new FileOutputStream(changedFileOutput);
                    } else {
                        os = new FileOutputStream(output);
                    }
                    os.write(data);
                    os.close();
                } else { //过滤的文件也写入output
                    FileUtil.copyFileUsingStream(classFile, changedFileOutput);
                }
            } ....
        }
    }       
```
innerTraceMethodFromSrc方法会使用ASM操作输入目录中的不含R、Manifest、BuildConfig关键词的所有class文件，然后将操作结果写到指定的output；当然，
被过滤掉的文件也需要写到指定的output，只是不需要经过ASM操作而已
通过TraceClassAdapter进行asm操作

com/tencent/matrix/trace/MethodTracer.java
```
 private class TraceClassAdapter extends ClassVisitor {
  TraceClassAdapter(int i, ClassVisitor classVisitor) {
            //i asm版本
            super(i, classVisitor);
        }
        
  //遍历到类的header时被调用
       @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            this.superName = superName;
            //判断是否为"android/app/Activity";  "android/support/v7/app/AppCompatActivity";
            //"android/support/v4/app/FragmentActivity"; "androidx/appcompat/app/AppCompatActivity";
            this.isActivityOrSubClass = isActivityOrSubClass(className, collectedClassExtendMap);
            this.isNeedTrace = MethodCollector.isNeedTrace(configuration, className, mappingCollector);
            if ((access & Opcodes.ACC_ABSTRACT) > 0 || (access & Opcodes.ACC_INTERFACE) > 0) {
                //是否为抽象类或接口
                this.isABSClass = true;
            }
        } 
  //遍历到方法时被调用
   @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            if (!hasWindowFocusMethod) {
                //判断方法是否为onWindowFocusChanged
                hasWindowFocusMethod = MethodCollector.isWindowFocusChangeMethod(name, desc);
            }
            if (isABSClass) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            } else {
                MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                return new TraceMethodAdapter(api, methodVisitor, access, name, desc, this.className,
                        hasWindowFocusMethod, isActivityOrSubClass, isNeedTrace);
            }
        }

   //类遍历结束时被调用      
    @Override
        public void visitEnd() {
            if (!hasWindowFocusMethod && isActivityOrSubClass && isNeedTrace) {
                insertWindowFocusChangeMethod(cv, className, superName);
            }
            super.visitEnd();
        }     
 }
```
ASM的API设计使用了访问者模式，正如类名的后缀Visitor。此外，ASM每遍历到一个东西，都会调用Visitor里面的对应的visit方法。
我们在这里面可以使用Java代码做逻辑判断，判断到需要插入字节码的时候，使用ASM提供的API进行插入。

上面的代码意思就是访问到类时，判断是不是Activity及其子类，判断是否需要插桩，判断是否是抽象类。
访问到方法时，如果是抽象类，则不做处理。如果遇到了onWindowFocusChanged方法，则设置标志位；不管有没有遇到，都会交给TraceMethodAdapter进行后续处理。
最后，如果类没有遇到onWindowFocusChanged方法且是Activity或子类且需要插桩，则使用ASM API插入这么一段代码(手动插入onWindowFocusChanged方法)：
```
 private void insertWindowFocusChangeMethod(ClassVisitor cv, String classname, String superClassName) {
        //onWindowFocusChanged  ARGS:(Z)V
        // public void onWindowFocusChanged (boolean)
        MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC, TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD,
                TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS, null, null);
        // {
        methodVisitor.visitCode();
        // this
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        // boolean
        methodVisitor.visitVarInsn(Opcodes.ILOAD, 1);
        // super.onWindowFocusChanged(boolean)
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD,
                TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS, false);
        // com/tencent/matrix/trace/core/AppMethodBeat.at(this, boolean)        
        traceWindowFocusChangeMethod(methodVisitor, classname);
        // 返回语句
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
 }
 
     private void traceWindowFocusChangeMethod(MethodVisitor mv, String classname) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        //com/tencent/matrix/trace/core/AppMethodBeat
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "at", "(Landroid/app/Activity;Z)V", false);
    }
```
上面这段代码可能看着头疼，因为这涉及到了字节码的层面。不过也不用太担心，我们可以在AS上下载ASM Bytecode Viewer插件，先写好要插桩的代码，
然后使用此插件查看ASM的对应写法，可以增加效率。

最后，我们看看TraceMethodAdapter是如何处理方法的
```
 private class TraceMethodAdapter extends AdviceAdapter {
 
      @Override
        protected void onMethodEnter() {
            TraceMethod traceMethod = collectedMethodMap.get(methodName);
            if (traceMethod != null) {
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                // 插入 void com/tencent/matrix/trace/core/AppMethodBeat.i(int)
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "i", "(I)V", false);
                if (checkNeedTraceWindowFocusChangeMethod(traceMethod)) {
                   //如果需要插庄，插入com/tencent/matrix/trace/core/AppMethodBeat.at(this, boolean)
                    traceWindowFocusChangeMethod(mv, className);
                }
            }
        }
        
   @Override
        protected void onMethodExit(int opcode) {
            TraceMethod traceMethod = collectedMethodMap.get(methodName);
            // 插入 void com/tencent/matrix/trace/core/AppMethodBeat.o(int)
            if (traceMethod != null) {
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "o", "(I)V", false);
            }
        }      
 }
```
看完整个trace模块，我们会发现，其实插桩入门真的很简单。
transform的注入流程、src/jar包中class文件的读写、以及ASM的流程都可以套用。只是ClassVisitor需要自己写，而这部分的代码又可以参考ASM Bytecode Viewer插件