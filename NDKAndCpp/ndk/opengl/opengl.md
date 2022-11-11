open gl     Open Graphics Library
中文网
https://learnopengl-cn.github.io/

open gl es      OpenGL for Embedded Systems
OpenGL ES (OpenGL for Embedded Systems) 是 OpenGL三维图形 API 的子集，针对手机、PDA和游戏主机等嵌入式设备而设计。
  该API由Khronos集团定义推广，Khronos是一个图形软硬件行业协会，该协会主要关注图形和多媒体方面的开放标准。
OpenGL ES 是从 OpenGL 裁剪的定制而来的，去除了glBegin/glEnd，四边形（GL_QUADS）、多边形（GL_POLYGONS）等
  复杂图元等许多非绝对必要的特性

对于android开发者的好处
1 更加深入的了解 Android 的图像渲染架构
Surface
SurfaceTexture
SurfaceFlinger
2 用于相机开发
更加高效的实现滤镜, 贴纸等效果
共享 EGL 上下文实现视频录制
3 用于音视频开发
使用 OpenGL ES 实现视频流的渲染, 实现加水印等功能

android版本
https://developer.android.com/develop/ui/views/graphics/opengl/about-opengl
OpenGL ES 1.0 and 1.1 - This API specification is supported by Android 1.0 and higher.
OpenGL ES 2.0 - This API specification is supported by Android 2.2 (API level 8) and higher.
OpenGL ES 3.0 - This API specification is supported by Android 4.3 (API level 18) and higher.
OpenGL ES 3.1 - This API specification is supported by Android 5.0 (API level 21) and higher.


状态机
OpenGL自身是一个巨大的状态机(State Machine)：一系列的变量描述OpenGL此刻应当如何运行。OpenGL的状态通常被称为OpenGL上下文(Context)。
   我们通常使用如下途径去更改OpenGL状态：设置选项，操作缓冲。最后，我们使用当前OpenGL上下文来渲染。

假设当我们想告诉OpenGL去画线段而不是三角形的时候，我们通过改变一些上下文变量来改变OpenGL状态，从而告诉OpenGL如何去绘图。
一旦我们改变了OpenGL的状态为绘制线段，下一个绘制命令就会画出线段而不是三角形。

当使用OpenGL的时候，我们会遇到一些状态设置函数(State-changing Function)，这类函数将会改变上下文。以及状态使用函数(State-using Function)，
   这类函数会根据当前OpenGL的状态执行一些操作。只要你记住OpenGL本质上是个大状态机，就能更容易理解它的大部分特性。