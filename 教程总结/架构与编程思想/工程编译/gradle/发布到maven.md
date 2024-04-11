
build.gradle.kts
https://developer.android.google.cn/studio/build/maven-publish-plugin
https://docs.gradle.org/current/userguide/publishing_customization.html#sec:publishing_custom_artifacts_to_maven
```
apply plugin: 'maven-publish' //插件

android{
   publishing {
        singleVariant("officialRelease") {
            // if you don't want sources/javadoc, remove these lines
            withSourcesJar()  //提供source文件，可以不加
            // withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("officialRelease") {
                groupId = "com.xx"
                artifactId = "xx"
                version = "1.0"
                from(components["officialRelease"])  //变体的名字
                
                //对pom进行的操作 
                pom.withXml {
                  Node pomNode = asNode()
                  pomNode.dependencies.'*'.findAll() {
                    //将所有的默认依赖移除
                    //it.parent().remove(it)
                  }
                }
            }
        }
        repositories {
            maven {
                credentials {
                    username = "admin" //nexus的用户名
                    password = "xxx"
                }
                isAllowInsecureProtocol=true
                url = uri("http:/xxxxx/repository/maven-releases/")
                name = "nexus" //仓库名称
            }
        }
    }
}
```

build.gradle形式
```
apply plugin: 'maven-publish'


afterEvaluate {
  publishing {
    repositories {
      //release仓库
      maven {
        //仓库的名字和地址
        name = "nexus"
        url = "xxx"
        allowInsecureProtocol = true
        // 仓库用户名密码
        credentials {
          username = xx
          password = xx
        }
      }
    }
    publications {
      // 创建名为 release的任务
      release(MavenPublication) {
        from components.release
        // 文件的groupId
        groupId = 'xxx'
        //文件的名字
        artifactId = "xxx"
        version = "xxx"
        //对pom进行的操作
        pom.withXml {
          Node pomNode = asNode()
          pomNode.dependencies.'*'.findAll() {
            //将所有的默认依赖移除
            //it.parent().remove(it)
          }
        }
      }
    }
  }
}
```

定制pom关系
1
变更依赖方式  compileOnly不会上传依赖关系，implementation会

2  对pom进行的操作  
```
        pom.withXml {
          println("it ${asString()}") //以string树的形式展示pom
          val dependencies = asNode().get("dependencies") as NodeList //
          val value =( dependencies.firstOrNull() as? Node)?.value()
          (value as NodeList).forEach {
             //每一个依赖
          }
        }
```



pom文件和module文件
https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html
gradle上传包后，会生成pom,module文件，都包含依赖相关信息
Gradle Module Metadata is a format used to serialize the Gradle component model. 
It is similar to Apache Maven™'s POM file or Apache Ivy™ ivy.xml files. The goal of metadata files is to provide 
to consumers a reasonable model of what is published on a repository.
module文件示例
https://github.com/gradle/gradle/blob/master/platforms/documentation/docs/src/docs/design/gradle-module-metadata-latest-specification.md
```
{
    "formatVersion": "1.0",
    "component": {
        "group": "my.group",
        "module": "mylib",
        "version": "1.2"
    },
    "createdBy": {
        "gradle": {
            "version": "4.3",
            "buildId": "abc123"
        }
    },
    "variants": [
        {
            "name": "api",
            "attributes": {
                "org.gradle.usage": "java-api",
                "org.gradle.category": "library",
                "org.gradle.libraryelements": "jar"
            },
            "files": [
                { 
                    "name": "mylib-api.jar", 
                    "url": "mylib-api-1.2.jar",
                    "size": "1453",
                    "sha1": "abc12345",
                    "md5": "abc12345"
                }
            ],
            "dependencies": [
                { 
                    "group": "some.group", 
                    "module": "other-lib", 
                    "version": { "requires": "3.4" },
                    "excludes": [
                        { "group": "*", "module": "excluded-lib" }
                    ],
                    "attributes": {
                       "buildType": "debug"
                    }
                }
            ]
        },
        ....
```