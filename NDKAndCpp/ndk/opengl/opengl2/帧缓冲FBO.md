https://sharrychoo.github.io/blog/opengl-es-2.0/frame-buffer

当数据通过渲染管线输出到屏幕时, 我们如何拿到输出的数据呢?

带着这个问题, 我们了解一下 FBO 的相关知识

什么是帧缓冲?
帧缓冲 (Framebuffer Object, 简称 FBO) 即用于存放渲染管线输出数据帧的缓冲
帧缓冲组成.jpg

一个帧缓冲由颜色附件, 深度附件和模板附件组成
纹理可以作为颜色附件和深度附件使用
渲染缓冲可以作为深度附件和模板附件使用
Android 自带的帧缓冲使用 Surface/SurfaceTexture 描述, 对应一个 NativeWindow



使用场景
在图像输出到屏幕之前, 需要对图像进行加工
滤镜
水印
不希望数据直接输出到屏幕, 希望能够拦截输出的数据
将数据发送到 MediaCodec 进行硬编


创建缓冲帧
```
// 创建 fbo
int[] fboIds = new int[1];
GLES20.glGenBuffers(1, fboIds, 0);
int fboId = fboIds[0];  //结果id
```

绑定附件
1. 纹理附件
   将纹理附件添加到缓冲帧上时, 所有渲染命令都会输出到纹理中, 所有的渲染结果都会存储为纹理图像

创建纹理对象
```
// 创建纹理
GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D,
        0,
        GLES20.GL_RGBA,
        // 这里传入画布的宽高
        width, height,
        0,
        GLES20.GL_RGBA,
        GLES20.GL_UNSIGNED_BYTE,
        null
);
```
添加纹理附件
```
// 为 fbo 添加附件
GLES20.glFramebufferTexture2D(
        GLES20.GL_FRAMEBUFFER,
        GLES20.GL_COLOR_ATTACHMENT0,  // 颜色附件
        GLES20.GL_TEXTURE_2D, //纹理的类型
        mTextureId,
        0  //Mipmap level, 一般传 0
);
```
attachment: 附件类型有如下三种
颜色附件: GL_COLOR_ATTACHMENT0
深度附件: GL_DEPTH_ATTACHMENT
模板附件: GL_STENCIL_ATTACHMENT


渲染缓冲附件
除了可以绑定纹理附件之外, OpenGL 还引入了渲染缓冲对象, 它以原生渲染格式存储数据, 不会进行针对特定纹理格式的转换
因为存储的是原生数据, 因此进行 glSwapBuffers, 进行数据交换时比纹理附件更加高效
创建渲染缓冲对象
```
int[] renderbuffers = new int[1];
GLES20.glGenRenderbuffers(1, renderbuffers, 0);
int renderbuffer = renderbuffers[0];
```
添加渲染缓冲附件
```
// 绑定到当前渲染缓冲对象
GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderbufferId);
// 初始化渲染数据存储
GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_ATTACHMENT, width, height);
// 将渲染缓冲添加到 FBO 上
GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderbufferId);
```


绘制到缓冲帧
```
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferId);
...... // 在此之间的代码将绘制到缓冲帧上面去
GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
```


通过对 FBO 的学习, 让我们简单的了解到了帧缓冲的使用方式, 以及使用场景
它的工作流程类似于一个 Hook, 相当于在输出到屏幕之前先将准备好的数据 Hook 到 FBO 上, 由它进行加工处理
FBO 的使用频率是非常高的, 我们可以通过 fbo 实现对纹理添加水印这样子类似的功能, 可以在之前的纹理绘制的基础上进行拓展