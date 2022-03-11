https://mp.weixin.qq.com/s/sFM3OCorZ8HzV0R89cUAEw

APT注解  使用了javapoet的类库来生成.java文件   todo
Poet  [ˈpəʊɪt]  诗人
ARouter总共有三个自定义注解：Route，Autowired，Interceptor，对应有三个注解处理器RouteProcessor，AutowiredProcessor，
  InterceptorProcessor，这三个注解位于arouter-compiler模块
每个模块都配置了arouter-compiler，这样编译期可以收集到直接信息
```
// arouter-api要与arouter-compiler匹配使用，均使用最新版可以保证兼容
// 这里用api而不是implementation，方便其他组件依赖baseRouter后使用ARouter的功能
api 'com.alibaba:arouter-api:1.5.2'
// 我用的是kotlin所以下面用了kapt，如果是java替换成annotationProcessor即可
kapt  'com.alibaba:arouter-compiler:1.5.2'
```

以其中RouteProcessor为例，代码如下
```
@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_ROUTE, ANNOTATION_TYPE_AUTOWIRED})
public class RouteProcessor extends BaseProcessor {
    private Map<String, Set<RouteMeta>> groupMap = new HashMap<>(); // ModuleName and routeMeta.
    private Map<String, String> rootMap = new TreeMap<>();  // Map of root metas, used for generate class file in order.

    private TypeMirror iProvider = null;
    private Writer docWriter;       // Writer used for write doc
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //创建文档流 
        if (generateDoc) {
            try {
                docWriter = mFiler.createResource(
                        StandardLocation.SOURCE_OUTPUT,
                        //com.alibaba.android.arouter.docs
                        PACKAGE_OF_GENERATE_DOCS,
                        "arouter-map-of-" + moduleName + ".json"
                ).openWriter();
            } catch (IOException e) {
                logger.error("Create doc writer failed, because " + e.getMessage());
            }
        }
        //com.alibaba.android.arouter.facade.template.IProvider
        iProvider = elementUtils.getTypeElement(Consts.IPROVIDER).asType();
        logger.info(">>> RouteProcessor init. <<<");
    }  
    
      @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            //拿到Route的注解
            Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);
            try {
                logger.info(">>> Found routes, start... <<<");
                //开始解析
                this.parseRoutes(routeElements);

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    } 
    
     private void parseRoutes(Set<? extends Element> routeElements) throws IOException {
        if (CollectionUtils.isNotEmpty(routeElements)) {
            // prepare the type an so on.

            logger.info(">>> Found routes, size is " + routeElements.size() + " <<<");

            rootMap.clear();

            TypeMirror type_Activity = elementUtils.getTypeElement(ACTIVITY).asType();
            TypeMirror type_Service = elementUtils.getTypeElement(SERVICE).asType();
            TypeMirror fragmentTm = elementUtils.getTypeElement(FRAGMENT).asType();
            TypeMirror fragmentTmV4 = elementUtils.getTypeElement(Consts.FRAGMENT_V4).asType();

            // Interface of ARouter
            //com.alibaba.android.arouter.facade.template.IRouteGroup
            TypeElement type_IRouteGroup = elementUtils.getTypeElement(IROUTE_GROUP);
            //com.alibaba.android.arouter.facade.template.IProviderGroup
            TypeElement type_IProviderGroup = elementUtils.getTypeElement(IPROVIDER_GROUP);
            ClassName routeMetaCn = ClassName.get(RouteMeta.class);
            ClassName routeTypeCn = ClassName.get(RouteType.class);

            //创建Map<String, Class<? extends IRouteGroup>> 
            /*
               Build input type, format as :

               ```Map<String, Class<? extends IRouteGroup>>```
             */
            ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                    )
            );

            //创建Map<String, RouteMeta> 
            /*

              ```Map<String, RouteMeta>```
             */
            ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouteMeta.class)
            );
            //创建上面三个Map的句柄
            /*
              Build input param name.
             */
            ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();
            ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
            ParameterSpec providerParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();  // Ps. its param type same as groupParamSpec!

            //创建loadInto方法
            /*
              Build method : 'loadInto'
             */
            MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(rootParamSpec);
            //遍历路由注解
            //  Follow a sequence, find out metas of group first, generate java file, then statistics them as root.
            for (Element element : routeElements) {
                TypeMirror tm = element.asType();
                Route route = element.getAnnotation(Route.class);
                //路由信息
                RouteMeta routeMeta;

                // Activity or Fragment
                if (types.isSubtype(tm, type_Activity) || types.isSubtype(tm, fragmentTm) || types.isSubtype(tm, fragmentTmV4)) {
                    // Get all fields annotation by @Autowired
                    Map<String, Integer> paramsType = new HashMap<>();
                    Map<String, Autowired> injectConfig = new HashMap<>();
                    //拿到Autowired
                    injectParamCollector(element, paramsType, injectConfig);
                    //设置路由类型
                    if (types.isSubtype(tm, type_Activity)) {
                        // Activity
                        logger.info(">>> Found activity route: " + tm.toString() + " <<<");
                        routeMeta = new RouteMeta(route, element, RouteType.ACTIVITY, paramsType);
                    } else {
                        // Fragment
                        logger.info(">>> Found fragment route: " + tm.toString() + " <<<");
                        routeMeta = new RouteMeta(route, element, RouteType.parse(FRAGMENT), paramsType);
                    }

                    routeMeta.setInjectConfig(injectConfig);
                } else if (types.isSubtype(tm, iProvider)) {         // IProvider
                    logger.info(">>> Found provider route: " + tm.toString() + " <<<");
                    routeMeta = new RouteMeta(route, element, RouteType.PROVIDER, null);
                } else if (types.isSubtype(tm, type_Service)) {           // Service
                    logger.info(">>> Found service route: " + tm.toString() + " <<<");
                    routeMeta = new RouteMeta(route, element, RouteType.parse(SERVICE), null);
                } else {
                    throw new RuntimeException("The @Route is marked on unsupported class, look at [" + tm.toString() + "].");
                }
                //按照group排序 
                categories(routeMeta);
            }
            //provider重写的loadInto方法
            MethodSpec.Builder loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(providerParamSpec);

            Map<String, List<RouteDoc>> docSource = new HashMap<>();

            // Start generate java source, structure is divided into upper and lower levels, used for demand initialization.
            //遍历group
            for (Map.Entry<String, Set<RouteMeta>> entry : groupMap.entrySet()) {
                String groupName = entry.getKey();
                //group重写的loadInto方法
                MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(groupParamSpec);

                List<RouteDoc> routeDocList = new ArrayList<>();

                // Build group method body
                Set<RouteMeta> groupData = entry.getValue();
                //遍历路由信息
                for (RouteMeta routeMeta : groupData) {
                    RouteDoc routeDoc = extractDocInfo(routeMeta);

                    ClassName className = ClassName.get((TypeElement) routeMeta.getRawType());

                    switch (routeMeta.getType()) {
                        case PROVIDER:  // Need cache provider's super class
                            List<? extends TypeMirror> interfaces = ((TypeElement) routeMeta.getRawType()).getInterfaces();
                            for (TypeMirror tm : interfaces) {
                                routeDoc.addPrototype(tm.toString());

                                if (types.isSameType(tm, iProvider)) {   // Its implements iProvider interface himself.
                                    // This interface extend the IProvider, so it can be used for mark provider
                                    loadIntoMethodOfProviderBuilder.addStatement(
                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                            (routeMeta.getRawType()).toString(),
                                            routeMetaCn,
                                            routeTypeCn,
                                            className,
                                            routeMeta.getPath(),
                                            routeMeta.getGroup());
                                } else if (types.isSubtype(tm, iProvider)) {
                                    // This interface extend the IProvider, so it can be used for mark provider
                                    loadIntoMethodOfProviderBuilder.addStatement(
                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                            tm.toString(),    // So stupid, will duplicate only save class name.
                                            routeMetaCn,
                                            routeTypeCn,
                                            className,
                                            routeMeta.getPath(),
                                            routeMeta.getGroup());
                                }
                            }
                            break;
                        default:
                            break;
                    }

                    // Make map body for paramsType
                    StringBuilder mapBodyBuilder = new StringBuilder();
                    Map<String, Integer> paramsType = routeMeta.getParamsType();
                    Map<String, Autowired> injectConfigs = routeMeta.getInjectConfig();
                    if (MapUtils.isNotEmpty(paramsType)) {
                        List<RouteDoc.Param> paramList = new ArrayList<>();

                        for (Map.Entry<String, Integer> types : paramsType.entrySet()) {
                            mapBodyBuilder.append("put(\"").append(types.getKey()).append("\", ").append(types.getValue()).append("); ");

                            RouteDoc.Param param = new RouteDoc.Param();
                            Autowired injectConfig = injectConfigs.get(types.getKey());
                            param.setKey(types.getKey());
                            param.setType(TypeKind.values()[types.getValue()].name().toLowerCase());
                            param.setDescription(injectConfig.desc());
                            param.setRequired(injectConfig.required());

                            paramList.add(param);
                        }

                        routeDoc.setParams(paramList);
                    }
                    String mapBody = mapBodyBuilder.toString();

                    loadIntoMethodOfGroupBuilder.addStatement(
                            "atlas.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                            routeMeta.getPath(),
                            routeMetaCn,
                            routeTypeCn,
                            className,
                            routeMeta.getPath().toLowerCase(),
                            routeMeta.getGroup().toLowerCase());

                    routeDoc.setClassName(className.toString());
                    routeDocList.add(routeDoc);
                }

                // Generate groups
                String groupFileName = NAME_OF_GROUP + groupName;
                //写入文件com.alibaba.android.arouter.routes.ARouter$$Group$$..
                JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                        TypeSpec.classBuilder(groupFileName)
                                .addJavadoc(WARNING_TIPS)
                                .addSuperinterface(ClassName.get(type_IRouteGroup))
                                .addModifiers(PUBLIC)
                                .addMethod(loadIntoMethodOfGroupBuilder.build())
                                .build()
                ).build().writeTo(mFiler);

                logger.info(">>> Generated group: " + groupName + "<<<");
                rootMap.put(groupName, groupFileName);
                docSource.put(groupName, routeDocList);
            }

            if (MapUtils.isNotEmpty(rootMap)) {
                // Generate root meta by group name, it must be generated before root, then I can find out the class of group.
                for (Map.Entry<String, String> entry : rootMap.entrySet()) {
                    loadIntoMethodOfRootBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(PACKAGE_OF_GENERATE_FILE, entry.getValue()));
                }
            }

            // Output route doc
            if (generateDoc) {
                docWriter.append(JSON.toJSONString(docSource, SerializerFeature.PrettyFormat));
                docWriter.flush();
                docWriter.close();
            }

            // Write provider into disk
            String providerMapFileName = NAME_OF_PROVIDER + SEPARATOR + moduleName;
            //写入com.alibaba.android.arouter.routes.ARouter$$Providers$$..
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(providerMapFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IProviderGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfProviderBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated provider map, name is " + providerMapFileName + " <<<");

            // Write root meta into disk.
            String rootFileName = NAME_OF_ROOT + SEPARATOR + moduleName;
            //写入com.alibaba.android.arouter.routes.ARouter$$Root$$..
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(rootFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(elementUtils.getTypeElement(ITROUTE_ROOT)))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfRootBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated root, name is " + rootFileName + " <<<");
        }
    }
}
```
以ARouter的module-java测试模块为例
生成的添加路由Group文件
build/generated/ap_generated_sources/debug/out/com/alibaba/android/arouter/routes/ARouter$$Root$$modulejava.java
```
public class ARouter$$Root$$modulejava implements IRouteRoot {
  @Override
  public void loadInto(Map<String, Class<? extends IRouteGroup>> routes) {
    routes.put("m2", ARouter$$Group$$m2.class);
    routes.put("module", ARouter$$Group$$module.class);
    routes.put("test", ARouter$$Group$$test.class);
    routes.put("yourservicegroupname", ARouter$$Group$$yourservicegroupname.class);
  }
}
```
生成的添加到group("/test")的路由
/home/song/Downloads/ARouter-1.5.2/ARouter-1.5.2/module-java/build/generated/ap_generated_sources/debug/out/com/alibaba/android/arouter/routes/ARouter$$Group$$test.java
```
public class ARouter$$Group$$test implements IRouteGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> atlas) {
    atlas.put("/test/activity1", RouteMeta.build(RouteType.ACTIVITY, Test1Activity.class, "/test/activity1",/**Group**/ "test",/**paramsType，也就是注入的参数**/ new java.util.HashMap<String, Integer>(){{put("ser", 9); put("ch", 5); put("fl", 6); put("dou", 7); put("boy", 0); put("url", 8); put("pac", 10); put("obj", 11); put("name", 8); put("objList", 11); put("map", 11); put("age", 3); put("height", 3); }},/**Priority**/ -1,/**Extra**/ -2147483648));
    atlas.put("/test/activity2", RouteMeta.build(RouteType.ACTIVITY, Test2Activity.class, "/test/activity2", "test", new java.util.HashMap<String, Integer>(){{put("key1", 8); }}, -1, -2147483648));
    atlas.put("/test/activity3", RouteMeta.build(RouteType.ACTIVITY, Test3Activity.class, "/test/activity3", "test", new java.util.HashMap<String, Integer>(){{put("name", 8); put("boy", 0); put("age", 3); }}, -1, -2147483648));
    atlas.put("/test/activity4", RouteMeta.build(RouteType.ACTIVITY, Test4Activity.class, "/test/activity4", "test", null, -1, -2147483648));
    atlas.put("/test/fragment", RouteMeta.build(RouteType.FRAGMENT, BlankFragment.class, "/test/fragment", "test", new java.util.HashMap<String, Integer>(){{put("ser", 9); put("pac", 10); put("ch", 5); put("obj", 11); put("fl", 6); put("name", 8); put("dou", 7); put("boy", 0); put("objList", 11); put("map", 11); put("age", 3); put("height", 3); }}, -1, -2147483648));
    atlas.put("/test/webview", RouteMeta.build(RouteType.ACTIVITY, TestWebview.class, "/test/webview", "test", null, -1, -2147483648));
  }
}
```



拦截器注解处理InterceptorProcessor
```
@AutoService(Processor.class)
@SupportedAnnotationTypes(ANNOTATION_TYPE_INTECEPTOR)
public class InterceptorProcessor extends BaseProcessor {
    private Map<Integer, Element> interceptors = new TreeMap<>();
    private TypeMirror iInterceptor = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        iInterceptor = elementUtils.getTypeElement(Consts.IINTERCEPTOR).asType();

        logger.info(">>> InterceptorProcessor init. <<<");
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Interceptor.class);
            try {
                //开始解析Interceptor注解
                parseInterceptors(elements);
            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    /**
     * Parse tollgate.
     *
     * @param elements elements of tollgate.
     */
    private void parseInterceptors(Set<? extends Element> elements) throws IOException {
        if (CollectionUtils.isNotEmpty(elements)) {
            logger.info(">>> Found interceptors, size is " + elements.size() + " <<<");

            // Verify and cache, sort incidentally.
            //按优先级存储拦截器
            for (Element element : elements) {
                if (verify(element)) {  // Check the interceptor meta
                    logger.info("A interceptor verify over, its " + element.asType());
                    Interceptor interceptor = element.getAnnotation(Interceptor.class);

                    Element lastInterceptor = interceptors.get(interceptor.priority());
                    if (null != lastInterceptor) { // Added, throw exceptions
                        throw new IllegalArgumentException(
                                String.format(Locale.getDefault(), "More than one interceptors use same priority [%d], They are [%s] and [%s].",
                                        interceptor.priority(),
                                        lastInterceptor.getSimpleName(),
                                        element.getSimpleName())
                        );
                    }
                    //类型为Map<Integer, Element>  优先级,element
                    interceptors.put(interceptor.priority(), element);
                } else {
                    logger.error("A interceptor verify failed, its " + element.asType());
                }
            }

            // Interface of ARouter.
            //com.alibaba.android.arouter.facade.template.IInterceptor
            TypeElement type_ITollgate = elementUtils.getTypeElement(IINTERCEPTOR);
            //com.alibaba.android.arouter.facade.template.IInterceptorGroup
            TypeElement type_ITollgateGroup = elementUtils.getTypeElement(IINTERCEPTOR_GROUP);
            //创建Map<Integer, Class<? extends ITollgate>>
            /**
             *  Build input type, format as :
             *
             *  ```Map<Integer, Class<? extends ITollgate>>```
             */
            ParameterizedTypeName inputMapTypeOfTollgate = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Integer.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_ITollgate))
                    )
            );
             //创建句柄interceptors
            // Build input param name.
            ParameterSpec tollgateParamSpec = ParameterSpec.builder(inputMapTypeOfTollgate, "interceptors").build();

            // 重写方法loadInto
            // Build method : 'loadInto'
            MethodSpec.Builder loadIntoMethodOfTollgateBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(tollgateParamSpec);
            //方法体
            // Generate
            if (null != interceptors && interceptors.size() > 0) {
                // Build method body
                for (Map.Entry<Integer, Element> entry : interceptors.entrySet()) {
                    loadIntoMethodOfTollgateBuilder.addStatement("interceptors.put(" + entry.getKey() + ", $T.class)", ClassName.get((TypeElement) entry.getValue()));
                }
            }
            //写入com.alibaba.android.arouter.routes.ARouter$$Interceptors$$..
            // Write to disk(Write file even interceptors is empty.)
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(NAME_OF_INTERCEPTOR + SEPARATOR + moduleName)
                            .addModifiers(PUBLIC)
                            .addJavadoc(WARNING_TIPS)
                            .addMethod(loadIntoMethodOfTollgateBuilder.build())
                            .addSuperinterface(ClassName.get(type_ITollgateGroup))
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Interceptor group write over. <<<");
        }
    }
}
```
以ARouter的module-java测试模块为例
生成的添加拦截器文件
/build/generated/ap_generated_sources/debug/out/com/alibaba/android/arouter/routes/ARouter$$Interceptors$$modulejava.java
```
public class ARouter$$Interceptors$$modulejava implements IInterceptorGroup {
  @Override
  public void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptors) {
    interceptors.put(7, Test1Interceptor.class);
    interceptors.put(90, TestInterceptor90.class);
  }
}
```

//todo AutowiredProcessor  怎么实现activity的参数自动注入的？？