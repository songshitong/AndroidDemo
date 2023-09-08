
build.gradle.kts
https://developer.android.google.cn/studio/build/maven-publish-plugin
https://docs.gradle.org/current/userguide/publishing_customization.html#sec:publishing_custom_artifacts_to_maven
```
apply plugin: 'maven-publish' //插件

android{
   publishing {
        singleVariant("officialRelease") {
            // if you don't want sources/javadoc, remove these lines
            withSourcesJar()  //提供source文件
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