
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

Step1寻找
首先，根据定义的embedded属性找出需要合并的aar，并将aar解压到相应目录下


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