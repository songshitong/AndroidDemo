
https://www.yangchaofan.cn/2021/05/android-fat-aar-useage/

fataar用途
将多个module的aar进行合并，对外提供一个aar


fat-aar合并主要过程为：
合并Manifest
合并jar
合并res资源
合并R文件（最关键的一步）
合并assets
合并libs
合并jni
合并Proguard

版本1.3.8

https://github.com/kezong/fat-aar-android/tree/513d8d1c6619e80f68f9456af01652f5a395332a
master/source/src/main/groovy/com/kezong/fataar/FatAarPlugin.groovy
```

 void apply(Project project) {
        this.project = project
        checkAndroidPlugin()
        //处理fatUtils
        FatUtils.attach(project)
        //配置各种路径 例如ReBundleDirectory  /outputs/${RE_BUNDLE_FOLDER}/${variant.name}
        DirectoryManager.attach(project)
        //创建外部可配置的参数  fataar {transformR  transitive 等}
        project.extensions.create(FatAarExtension.NAME, FatAarExtension)
        createConfigurations()
        //R类的转换
        registerTransform()
        //执行处理
        project.afterEvaluate {
            doAfterEvaluate()
        }
    }
    
    

private void createConfigurations() {
        创建embed
        //CONFIG_NAME = embed
        Configuration embedConf = project.configurations.create(CONFIG_NAME)
        createConfiguration(embedConf)
        FatUtils.logInfo("Creating configuration embed")

        project.android.buildTypes.all { buildType ->
            String configName = buildType.name + CONFIG_SUFFIX
            Configuration configuration = project.configurations.create(configName)
            createConfiguration(configuration)
            FatUtils.logInfo("Creating configuration " + configName)
        }

        project.android.productFlavors.all { flavor ->
            String configName = flavor.name + CONFIG_SUFFIX
            Configuration configuration = project.configurations.create(configName)
            createConfiguration(configuration)
            FatUtils.logInfo("Creating configuration " + configName)
            project.android.buildTypes.all { buildType ->
                String variantName = flavor.name + buildType.name.capitalize()
                String variantConfigName = variantName + CONFIG_SUFFIX
                Configuration variantConfiguration = project.configurations.create(variantConfigName)
                createConfiguration(variantConfiguration)
                FatUtils.logInfo("Creating configuration " + variantConfigName)
            }
        }
    }
    
 
     private registerTransform() {
        transform = new RClassesTransform(project)
        // register in project.afterEvaluate is invalid.
        project.android.registerTransform(transform)
    } 
    
    
    
     private void doAfterEvaluate() {
        embedConfigurations.each {
            if (project.fataar.transitive) {
                it.transitive = true
            }
        }

        project.android.libraryVariants.all { variant ->
            Collection<ResolvedArtifact> artifacts = new ArrayList()
            Collection<ResolvedDependency> firstLevelDependencies = new ArrayList<>()
            embedConfigurations.each { configuration ->
                if (configuration.name == CONFIG_NAME
                        || configuration.name == variant.getBuildType().name + CONFIG_SUFFIX
                        || configuration.name == variant.getFlavorName() + CONFIG_SUFFIX
                        || configuration.name == variant.name + CONFIG_SUFFIX) {
                    Collection<ResolvedArtifact> resolvedArtifacts = resolveArtifacts(configuration)
                    artifacts.addAll(resolvedArtifacts)
                    artifacts.addAll(dealUnResolveArtifacts(configuration, variant as LibraryVariant, resolvedArtifacts))
                    firstLevelDependencies.addAll(configuration.resolvedConfiguration.firstLevelModuleDependencies)
                }
            }

            if (!artifacts.isEmpty()) {
                //开始处理
                def processor = new VariantProcessor(project, variant)
                processor.processVariant(artifacts, firstLevelDependencies, transform)
            }
        }
    }  
```

source/src/main/groovy/com/kezong/fataar/VariantProcessor.groovy
```
void processVariant(Collection<ResolvedArtifact> artifacts,
                        Collection<ResolvableDependency> dependencies,
                        RClassesTransform transform) {
        String taskPath = 'pre' + mVariant.name.capitalize() + 'Build'
        TaskProvider prepareTask = mProject.tasks.named(taskPath)
       ...
        TaskProvider bundleTask = VersionAdapter.getBundleTaskProvider(mProject, mVariant.name)
        preEmbed(artifacts, dependencies, prepareTask)
        processArtifacts(artifacts, prepareTask, bundleTask)
        //处理class和jar
        processClassesAndJars(bundleTask)
       ...
        processManifest()
        processResources()
        processAssets()
        processJniLibs()
        processConsumerProguard()
        processGenerateProguard()
        processDataBinding(bundleTask)
        processRClasses(transform, bundleTask)
    }
```


preEmbed
```
  private void preEmbed(Collection<ResolvedArtifact> artifacts,
                          Collection<ResolvedDependency> dependencies,
                          TaskProvider prepareTask) {
        TaskProvider embedTask = mProject.tasks.register("pre${mVariant.name.capitalize()}Embed") {
            doFirst {  //打印信息
                printEmbedArtifacts(artifacts, dependencies)
            }
        }

        prepareTask.configure {
            dependsOn embedTask
        }
    }

```

processArtifacts
```
 private void processArtifacts(Collection<ResolvedArtifact> artifacts, TaskProvider<Task> prepareTask, TaskProvider<Task> bundleTask) {
        if (artifacts == null) {
            return
        }
        for (final ResolvedArtifact artifact in artifacts) {
            if (FatAarPlugin.ARTIFACT_TYPE_JAR == artifact.type) {
                addJarFile(artifact.file) //将jar添加到mJarFiles
            } else if (FatAarPlugin.ARTIFACT_TYPE_AAR == artifact.type) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(mProject, artifact, mVariant.name)
                addAndroidArchiveLibrary(archiveLibrary) //将aar添加到mAndroidArchiveLibraries
                Set<Task> dependencies

                if (getTaskDependency(artifact) instanceof TaskDependency) {
                    dependencies = artifact.buildDependencies.getDependencies()
                } else {
                    CachingTaskDependencyResolveContext context = new CachingTaskDependencyResolveContext()
                    getTaskDependency(artifact).visitDependencies(context)
                    if (context.queue.size() == 0) {
                        dependencies = new HashSet<>()
                    } else {
                        dependencies = context.queue.getFirst().getDependencies()
                    }
                }
                final def zipFolder = archiveLibrary.getRootFolder()
                zipFolder.mkdirs()
                def group = artifact.getModuleVersion().id.group.capitalize()
                def name = artifact.name.capitalize()
                String taskName = "explode${group}${name}${mVariant.name.capitalize()}"
                Task explodeTask = mProject.tasks.create(taskName, Copy) {
                    from mProject.zipTree(artifact.file.absolutePath)
                    into zipFolder
                    //zip拷贝
                    doFirst {
                        // Delete previously extracted data.
                        zipFolder.deleteDir()
                    }
                }

                if (dependencies.size() == 0) {
                    explodeTask.dependsOn(prepareTask)
                } else {
                    explodeTask.dependsOn(dependencies.first())
                }
                Task javacTask = mVersionAdapter.getJavaCompileTask()
                javacTask.dependsOn(explodeTask)
                bundleTask.configure {
                    dependsOn(explodeTask)
                }
                mExplodeTasks.add(explodeTask) //将zip拷贝task添加到mExplodeTasks
            }
        }
    }
```

processClassesAndJars
```
 private void processClassesAndJars(TaskProvider<Task> bundleTask) {
        boolean isMinifyEnabled = mVariant.getBuildType().isMinifyEnabled()

        TaskProvider syncLibTask = mProject.tasks.named(mVersionAdapter.getSyncLibJarsTaskPath())
        TaskProvider extractAnnotationsTask = mProject.tasks.named("extract${mVariant.name.capitalize()}Annotations")
        //合并class
        mMergeClassTask = handleClassesMergeTask(isMinifyEnabled)
        syncLibTask.configure {
            dependsOn(mMergeClassTask)
            inputs.files(mAndroidArchiveLibraries.stream().map { it.libsFolder }.collect())
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            inputs.files(mJarFiles).withPathSensitivity(PathSensitivity.RELATIVE)
        }
        extractAnnotationsTask.configure {
            mustRunAfter(mMergeClassTask)
        }

        if (!isMinifyEnabled) {
            //合并jar
            TaskProvider mergeJars = handleJarMergeTask(syncLibTask)
            bundleTask.configure {
                dependsOn(mergeJars)
            }
        }
    }


 private TaskProvider handleClassesMergeTask(final boolean isMinifyEnabled) {
        final TaskProvider task = mProject.tasks.register("mergeClasses" + mVariant.name.capitalize()) {
            dependsOn(mExplodeTasks)
            dependsOn(mVersionAdapter.getJavaCompileTask())
            try {
                // main lib maybe not use kotlin
                TaskProvider kotlinCompile = mProject.tasks.named("compile${mVariant.name.capitalize()}Kotlin")
                if (kotlinCompile != null) {
                    dependsOn(kotlinCompile)
                }
            } catch(Exception ignore) {

            }

            inputs.files(mAndroidArchiveLibraries.stream().map { it.classesJarFile }.collect())
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            if (isMinifyEnabled) {
                inputs.files(mAndroidArchiveLibraries.stream().map { it.localJars }.collect())
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                inputs.files(mJarFiles).withPathSensitivity(PathSensitivity.RELATIVE)
            }
            File outputDir = DirectoryManager.getMergeClassDirectory(mVariant)
            File javacDir = mVersionAdapter.getClassPathDirFiles().first()
            outputs.dir(outputDir)

            doFirst {
                // Extract relative paths and delete previous output.
                def pathsToDelete = new ArrayList<Path>()
                mProject.fileTree(outputDir).forEach {
                    pathsToDelete.add(Paths.get(outputDir.absolutePath).relativize(Paths.get(it.absolutePath)))
                }
                outputDir.deleteDir()
                // Delete output files from javac dir.
                pathsToDelete.forEach {
                    Files.deleteIfExists(Paths.get("$javacDir.absolutePath/${it.toString()}"))
                }
            }

            doLast {
                ExplodedHelper.processClassesJarInfoClasses(mProject, mAndroidArchiveLibraries, outputDir)
                if (isMinifyEnabled) { //拷贝到outputDir
                    ExplodedHelper.processLibsIntoClasses(mProject, mAndroidArchiveLibraries, mJarFiles, outputDir)
                }
                //拷贝文件
                mProject.copy {
                    from outputDir
                    into javacDir
                    exclude 'META-INF/'
                }

                mProject.copy {
                    from outputDir.absolutePath + "/META-INF"
                    into DirectoryManager.getKotlinMetaDirectory(mVariant)
                    include '*.kotlin_module'
                }
            }
        }
        return task
    }
    
 
  private TaskProvider handleJarMergeTask(final TaskProvider syncLibTask) {
        final TaskProvider task = mProject.tasks.register("mergeJars" + mVariant.name.capitalize()) {
            dependsOn(mExplodeTasks)
            dependsOn(mVersionAdapter.getJavaCompileTask())
            mustRunAfter(syncLibTask)

            inputs.files(mAndroidArchiveLibraries.stream().map { it.libsFolder }.collect())
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            inputs.files(mJarFiles).withPathSensitivity(PathSensitivity.RELATIVE)
            def outputDir = mVersionAdapter.getLibsDirFile() //添加到"${mProject.buildDir.path}/intermediates/aar_libs_directory/${mVariant.name}/libs"
            outputs.dir(outputDir)

            doFirst {
                //将mAndroidArchiveLibraries，mJarFiles拷贝到outputDir
                ExplodedHelper.processLibsIntoLibs(mProject, mAndroidArchiveLibraries, mJarFiles, outputDir)
            }
        }
        return task
    }
   
```

处理manifest
```
  private void processManifest() {
         //获取ManifestProcessorTask    mVariant.getOutputs().first().getProcessManifestProvider().get()
        ManifestProcessorTask processManifestTask = mVersionAdapter.getProcessManifest()

        //mnifest的输出目录
        File manifestOutput
        if (FatUtils.compareVersion(VersionAdapter.AGPVersion, "4.2.0-alpha07") >= 0) {
            manifestOutput = mProject.file("${mProject.buildDir.path}/intermediates/merged_manifest/${mVariant.name}/AndroidManifest.xml")
        } else if (FatUtils.compareVersion(VersionAdapter.AGPVersion, "3.3.0") >= 0) {
            manifestOutput = mProject.file("${mProject.buildDir.path}/intermediates/library_manifest/${mVariant.name}/AndroidManifest.xml")
        } else {
            manifestOutput = mProject.file(processManifestTask.getManifestOutputDirectory().absolutePath + "/AndroidManifest.xml")
        }

        final List<File> inputManifests = new ArrayList<>()
        for (archiveLibrary in mAndroidArchiveLibraries) {
            inputManifests.add(archiveLibrary.getManifest())
        }

        TaskProvider<LibraryManifestMerger> manifestsMergeTask = mProject.tasks.register("merge${mVariant.name.capitalize()}Manifest", LibraryManifestMerger) {
            setGradleVersion(mProject.getGradle().getGradleVersion())
            setGradlePluginVersion(VersionAdapter.AGPVersion)
            setMainManifestFile(manifestOutput)
            setSecondaryManifestFiles(inputManifests)
            setOutputFile(manifestOutput)
        }

        processManifestTask.dependsOn(mExplodeTasks)
        processManifestTask.inputs.files(inputManifests)
        processManifestTask.doLast {
            // 通过LibraryManifestMerger合并
            manifestsMergeTask.get().doTaskAction()
        }
    }
```
src/main/java/com/kezong/fataar/LibraryManifestMerger.java
```
 protected void doFullTaskAction() throws ManifestMerger2.MergeFailureException, IOException {
        ILogger iLogger = new LoggerWrapper(getLogger());
        //通过ManifestMerger2进行合并
        //https://android.googlesource.com/platform/tools/base/+/master/build-system/manifest-merger/src/main/java/com/android/manifmerger/ManifestMerger2.java
        ManifestMerger2.Invoker mergerInvoker = ManifestMerger2.
                newMerger(getMainManifestFile(), iLogger, ManifestMerger2.MergeType.LIBRARY);
        List<File> secondaryManifestFiles = getSecondaryManifestFiles();
        List<ManifestProvider> manifestProviders = new ArrayList<>();
        if (secondaryManifestFiles != null) {
            for (final File file : secondaryManifestFiles) {
                if (!file.exists()) {
                    continue;
                }
                //添加manifestProvider
                manifestProviders.add(new ManifestProvider() {
                    @Override
                    public File getManifest() {
                        return file.getAbsoluteFile();
                    }

                    @Override
                    public String getName() {
                        return file.getName();
                    }
                });
            }
        }
        mergerInvoker.addManifestProviders(manifestProviders);
        //执行合并
        MergingReport mergingReport = mergerInvoker.merge();
        if (mergingReport.getResult().isError()) {
            getLogger().error(mergingReport.getReportString());
            mergingReport.log(iLogger);
            throw new BuildException(mergingReport.getReportString());
        }
      
        //输出到目录路径
        // fix utf-8 problem in windows
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(getOutputFile()), "UTF-8")
        );
        writer.append(mergingReport
                .getMergedDocument(MergingReport.MergedManifestKind.MERGED));
        writer.flush();
        writer.close();
    }
```

处理resources
processResources()  合并R.txt res目录
```
 private void processResources() {
        String taskPath = "generate" + mVariant.name.capitalize() + "Resources"
        TaskProvider resourceGenTask = mProject.tasks.named(taskPath)
        ...
        resourceGenTask.configure {
            dependsOn(mExplodeTasks)

            mProject.android.sourceSets.each { DefaultAndroidSourceSet sourceSet ->
                if (sourceSet.name == mVariant.name) {
                    for (archiveLibrary in mAndroidArchiveLibraries) {
                        FatUtils.logInfo("Merge resource，Library res：${archiveLibrary.resFolder}")
                        //添加sourceset
                        sourceSet.res.srcDir(archiveLibrary.resFolder)
                    }
                }
            }
        }
    }
```


processAssets()
```
private void processAssets() {
        Task assetsTask = mVersionAdapter.getMergeAssets() //mVariant.getMergeAssetsProvider().get()
        ...
        assetsTask.dependsOn(mExplodeTasks)
        assetsTask.doFirst {
            mProject.android.sourceSets.each {
                if (it.name == mVariant.name) {
                    for (archiveLibrary in mAndroidArchiveLibraries) {
                        if (archiveLibrary.assetsFolder != null && archiveLibrary.assetsFolder.exists()) {
                            FatUtils.logInfo("Merge assets，Library assets folder：${archiveLibrary.assetsFolder}")
                            //添加sourceSets
                            it.assets.srcDir(archiveLibrary.assetsFolder)
                        }
                    }
                }
            }
        }
    }
```

processJniLibs
```
private void processJniLibs() {
        String taskPath = 'merge' + mVariant.name.capitalize() + 'JniLibFolders'
        TaskProvider mergeJniLibsTask = mProject.tasks.named(taskPath)
        if (mergeJniLibsTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }

        mergeJniLibsTask.configure {
            dependsOn(mExplodeTasks)

            doFirst {
                for (archiveLibrary in mAndroidArchiveLibraries) {
                    if (archiveLibrary.jniFolder != null && archiveLibrary.jniFolder.exists()) {
                        mProject.android.sourceSets.each {
                            //sourceSets添加jniLibs
                            if (it.name == mVariant.name) {
                                it.jniLibs.srcDir(archiveLibrary.jniFolder)
                            }
                        }
                    }
                }
            }
        }
    }
```

processConsumerProguard
```
private void processConsumerProguard() {
        String mergeTaskName = 'merge' + mVariant.name.capitalize() + 'ConsumerProguardFiles'
        TaskProvider mergeFileTask = mProject.tasks.named(mergeTaskName)
        ...
        mergeFileTask.configure {
            dependsOn(mExplodeTasks)
            doLast {
                try {
                    Collection<File> files = mAndroidArchiveLibraries.stream().map { it.proguardRules }.collect()
                    File of
                    if (outputFile instanceof File) {
                        of = outputFile
                    } else {
                        // RegularFileProperty.class
                        of = outputFile.get().asFile
                    }
                    //将所有文件内容append到outputFile
                    FatUtils.mergeFiles(files, of)
                } catch (Exception e) {
                    ...
                    e.printStackTrace()
                }
            }
        }
    }
```


processDataBinding 处理databinding
```
 private void processDataBinding(TaskProvider<Task> bundleTask) {
        bundleTask.configure {
            doLast {
                for (archiveLibrary in mAndroidArchiveLibraries) {
                    if (archiveLibrary.dataBindingFolder != null && archiveLibrary.dataBindingFolder.exists()) {
                        String filePath = "${DirectoryManager.getReBundleDirectory(mVariant).path}/${archiveLibrary.dataBindingFolder.name}"
                        new File(filePath).mkdirs()
                        mProject.copy { //data-binding文件拷贝到filpath
                            from archiveLibrary.dataBindingFolder
                            into filePath
                        }
                    }

                    if (archiveLibrary.dataBindingLogFolder != null && archiveLibrary.dataBindingLogFolder.exists()) {
                        String filePath = "${DirectoryManager.getReBundleDirectory(mVariant).path}/${archiveLibrary.dataBindingLogFolder.name}"
                        new File(filePath).mkdirs()
                        mProject.copy { //data-binding-base-class-log目录文件拷贝
                            from archiveLibrary.dataBindingLogFolder
                            into filePath
                        }
                    }
                }
            }
        }
    }
```

processRClasses处理R文件
```
 private void processRClasses(RClassesTransform transform, TaskProvider<Task> bundleTask) {
        TaskProvider reBundleTask = configureReBundleAarTask(bundleTask)
        //注册RClassesTransform
        TaskProvider transformTask = mProject.tasks.named("transformClassesWith${transform.name.capitalize()}For${mVariant.name.capitalize()}")
        transformTask.configure {
            it.dependsOn(mMergeClassTask)
        }
        if (mProject.fataar.transformR) {
            transformRClasses(transform, transformTask, bundleTask, reBundleTask)
        } else {
            generateRClasses(bundleTask, reBundleTask)
        }
    }
    
 
  private void transformRClasses(RClassesTransform transform, TaskProvider transformTask, TaskProvider bundleTask, TaskProvider reBundleTask) {
        transform.putTargetPackage(mVariant.name, mVariant.getApplicationId())
        transformTask.configure {
                    doFirst {
                        // library package name parsed by aar's AndroidManifest.xml
                        // so must put after explode tasks perform.
                        Collection libraryPackages = mAndroidArchiveLibraries
                                .stream()
                                .map { it.packageName }
                                .collect()
                        transform.putLibraryPackages(mVariant.name, libraryPackages);
                    }
                }
        bundleTask.configure {
            //执行rebundle
            finalizedBy(reBundleTask)
        }
    }  
    
    
     private TaskProvider configureReBundleAarTask(TaskProvider bundleTask) {
        File aarOutputFile
        File reBundleDir = DirectoryManager.getReBundleDirectory(mVariant)
        bundleTask.configure { it ->
            if (FatUtils.compareVersion(mProject.gradle.gradleVersion, "5.1") >= 0) {
                aarOutputFile = new File(it.getDestinationDirectory().getAsFile().get(), it.getArchiveFileName().get())
            } else {
                aarOutputFile = new File(it.destinationDir, it.archiveName)
            }

            doFirst {
                // Delete previously unzipped data.
                reBundleDir.deleteDir()  //删除之前的
            }

            doLast {
                mProject.copy {
                    from mProject.zipTree(aarOutputFile)
                    into reBundleDir
                }
                FatUtils.deleteEmptyDir(reBundleDir)
            }
        }

        String taskName = "reBundleAar${mVariant.name.capitalize()}"
        //zip是org.gradle.api.tasks.bundling.Zip
        TaskProvider task = mProject.getTasks().register(taskName, Zip.class) {
            it.from reBundleDir
            it.include "**"
            if (aarOutputFile == null) {
                aarOutputFile = mVersionAdapter.getOutputFile()
            }
            //设置输出目录 重新打包为aar
            if (FatUtils.compareVersion(mProject.gradle.gradleVersion, "5.1") >= 0) {
                it.getArchiveFileName().set(aarOutputFile.getName())
                it.getDestinationDirectory().set(aarOutputFile.getParentFile())
            } else {
                it.archiveName = aarOutputFile.getName()
                it.destinationDir = aarOutputFile.getParentFile()
            }

            doLast {
                FatUtils.logAnytime(" target: ${aarOutputFile.absolutePath} [${FatUtils.formatDataSize(aarOutputFile.size())}]")
            }
        }

        return task
    }
```
R transform的处理
src/main/java/com/kezong/fataar/RClassesTransform.java
R.java的路径 ./app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/R.jar   //版本可能路径不同
```
 private Map<String, String> buildTransformTable(String variantName) {
        String targetPackage = targetPackageMap.get(variantName);
        Collection<String> libraryPackages = libraryPackageMap.get(variantName);
        if (targetPackage == null || libraryPackages == null) {
            return null;
        }
        //构建不同类型的R
        final List<String> resourceTypes = Arrays.asList("anim", "animator", "array", "attr", "bool", "color", "dimen",
                "drawable", "font", "fraction", "id", "integer", "interpolator", "layout", "menu", "mipmap", "navigation",
                "plurals", "raw", "string", "style", "styleable", "transition", "xml");

        HashMap<String, String> map = new HashMap<>();
        for (String resource : resourceTypes) {
            String targetClass = targetPackageMap.get(variantName).replace(".", "/") + "/R$" + resource;
            for (String libraryPackage : libraryPackageMap.get(variantName)) {
                String fromClass = libraryPackage.replace(".", "/") + "/R$" + resource;
                map.put(fromClass, targetClass);
            }
        }

        return map;
    }
    
    
  @Override
    public void transform(TransformInvocation transformInvocation) throws InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        //构建需要处理的map
        Map<String, String> transformTable = buildTransformTable(transformInvocation.getContext().getVariantName());
        final boolean isIncremental = transformInvocation.isIncremental() && this.isIncremental();
        final TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();

        if (!isIncremental) {
            outputProvider.deleteAll();
        }

        final File outputDir = outputProvider.getContentLocation("classes", getOutputTypes(), getScopes(), Format.DIRECTORY);

        try {
            for (final TransformInput input : transformInvocation.getInputs()) {
                for (final DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    final File directoryFile = directoryInput.getFile();
                    final ClassPool classPool = new ClassPool();
                    //构建R.class
                    classPool.insertClassPath(directoryFile.getAbsolutePath());

                    for (final File originalClassFile : getChangedClassesList(directoryInput)) {
                        if (!originalClassFile.getPath().endsWith(".class")) {
                            continue; // ignore anything that is not class file
                        }

                        Future<?> submit = executor.submit(() -> {
                            try {
                                File relative = FilesKt.relativeTo(originalClassFile, directoryFile);
                                String className = filePathToClassname(relative);
                                final CtClass ctClass = classPool.get(className);
                                if (transformTable != null) {
                                    ClassFile classFile = ctClass.getClassFile();
                                    ConstPool constPool = classFile.getConstPool();
                                    //重写R.class的内容
                                    constPool.renameClass(transformTable);
                                }
                                //常量重新写入
                                ctClass.writeFile(outputDir.getAbsolutePath());
                            } catch (CannotCompileException | NotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                        });

                        futures.add(submit);
                    }
                }
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        //等待执行完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        futures.clear();
        ...
    }
```