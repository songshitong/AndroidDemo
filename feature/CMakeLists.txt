
cmake_minimum_required(VERSION 3.4.1)
message("CMAKE_SOURCE_DIR ===  ${CMAKE_SOURCE_DIR}")
message("CMAKE_CXX_FLAGS ===  ${CMAKE_CXX_FLAGS}")

message("CMAKE_ANDROID_ARCH_ABI ===  ${CMAKE_ANDROID_ARCH_ABI}")

#添加头文件
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/micro-ecc-master)

file(GLOB SOURCE_FILES ${CMAKE_SOURCE_DIR}/src/main/cpp/micro-ecc-master/*.c)
source_group(micro-ecc-master FILES ${SOURCE_FILES})
message("SOURCE_FILES ===  ${SOURCE_FILES}")


add_library(
        native-lib
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/nativelog.cpp
        src/main/cpp/audioPlayer.cpp
        src/main/cpp/libfaac.a
        src/main/cpp/autobuffer.cc
        src/main/cpp/ptrbuffer.cc
        src/main/cpp/log_crypt.cc
        src/main/cpp/log_buffer.cc
        src/main/cpp/crash_monitor.cpp
        ${SOURCE_FILES}
)



 set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI}")




target_link_libraries( # Specifies the target library.
        native-lib
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        log
        android
        z
        OpenSLES
        )