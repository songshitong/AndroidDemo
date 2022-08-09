
仓库的声明
https://stackoverflow.com/questions/69163511/build-was-configured-to-prefer-settings-repositories-over-project-repositories-b
setting.gradle
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
