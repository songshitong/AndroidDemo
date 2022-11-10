
https://stackoverflow.com/questions/60878599/error-building-android-library-direct-local-aar-file-dependencies-are-not-supp
Direct local .aar file dependencies are not supported when building an AAR
build.gradle file
configurations.maybeCreate("default")
artifacts.add("default", file('spotify-app-remote-release-0.7.1.aar'))
settings.gradle
include ':spotify-app-remote'
build.gradle
api project(':spotify-app-remote')