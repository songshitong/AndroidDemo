# 全局替换注释  command+r  选择regex支持正则，输入#.*,选择replace all即可以删除注释
#制定cmake最小支持版本，可能使用了高版本命令
cmake_minimum_required(VERSION 3.4.1)

#内置常量 CMAKE_SOURCE_DIR  /Users/issmac/androidstudio-workspace/AndroidDemo/feature 即cmakelists.txt的父目录
#        CMAKE_CXX_FLAGS    -g -DANDROID -fdata-sections -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -fno-addrsig -march=armv7-a -mthumb -Wa,--noexecstack -Wformat -Werror=format-security
#        CMAKE_ANDROID_ARCH_ABI  armeabi-v7a 与build.gradle配置有关


# 添加一个库，根据native-lib.cpp源文件编译一个native-lib的动态库
add_library( 
        native-lib
        SHARED
        src/main/cpp/native-lib.cpp)

# 查找系统库，这里查找的是系统日志库，并赋值给变量log-lib
find_library( 
        log-lib
        log)

# 设置依赖的库，第一个参数必须为目标模块，不能更换顺序
target_link_libraries( 
        native-lib
        ${log-lib})


#可以将find_library 注释掉  下面的可以改为：  target_link_libraries(
#        native-lib
#        log)


#输出cmakelist 内置常量的值


#集成第三方

#1 设置头文件目录   头文件在inc目录和cmakelist文件同一级，都在src/main/cpp目录下
#在cmake_minimum_required之后
include_directories(${CMAKE_SOURCE_DIR}/inc)

#2 src/main/jinLibs 建立该文件夹，并拷贝lib库，带有CPU架构目录

#3 设置build.gradle的CPU架构  android->defaultConfig下面
   # Google cmake教程包括常用参数https://developer.android.com/ndk/guides/cmake
   externalNativeBuild{   #设置本地库的CPU架构
      cmake{
         cppFlags '-std=c++17' //传递给c++的参数 例如使用17的标准
         abiFilters "x86"
         version "3.4.1"   #制定cmake版本
# 也可以使用下面这种语法向变量传递参数：
# arguments "-D变量名=参数".
arguments "-DANDROID_ARM_NEON=TRUE",
# 使用下面这种语法向变量传递多个参数（参数之间使用空格隔开）：
# arguments "-D变量名=参数1 参数2"
"-DANDROID_CPP_FEATURES=rtti exceptions"
   #制定编译工具链 clang/gcc
   arguments "-DANDROID_TOOLCHAIN=clang"
   # NDK 所用的安卓平台的版本是多少arguments "-DANDROID_PLATFORM=android-21"
   }
   ndk{
     abiFilters "x86"    #指定第三方库的CPU架构
   }

#4 设置第三方so库的路径，加载时会在这个目录查找，CMAKE_CXX_FLAGS传给编译器的标志位，代表文件路径
                                                              #相对CMAKE_SOURCE_DIR的路径
 #在add_library 之后                                            路径是绝对路径 绝对路径和相对路径混合的报错 ${CMAKE_SOURCE_DIR}/.../.../jniLibs/${CMAKE_ANDROID_ARCH_ABI}
 set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

#5 设置链接

target_link_libraries(
        native-lib
        fmod
        fmodL
        ${log-lib})

#6 在native-lib中声明第三方库jni方法，native-lib是自动生成cpp目录下
  #引入头文件 .hpp是既有函数的声明还有函数的实现和定义
   #include <fmod.hpp>
   #include <android/log.h>  //引入Android log库的头文件
   #using namepace FMOD;
   #System *system;          //创建系统变量  System应该是fmod的类
   #System_Create(&system); //传入二级指针
   #unsigned int version;
   #system->getVersion(&version); //拿到当前SDK的版本
   #开头是两个_
   #__android_log_print(ANDROID_LOG_ERROR,"TEST","FMOD version %08x",version)