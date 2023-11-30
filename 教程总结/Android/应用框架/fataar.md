
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