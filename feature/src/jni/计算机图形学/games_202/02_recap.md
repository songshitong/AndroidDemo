
open gl

  Is a set of APIs that call the GPU pipeline from CPU  在cpu中调用GPU
  
    Therefore, language does not matter!
    Cross platform Alternatives (DirectX, Vulkan, etc.)  directx面向对象

  Cons

  Fragmented: lots of different versions C style, not easy to use Cannot debug (?)
 
  Understanding
  1-to-1 mapping to our software rasterizer in GAMES101



  important analogy:oil painting  画油画
    A. Place objects/models
    B. Set position of an easel
    C. Attach a canvas to the easel  画架
    D. Paint to the canvas
    E. (Attach other canvases to the easel and continue painting)
    F. (Use previous paintings for reference)
    
    
  A. Place objects/models
    Model speciﬁcation 
    Model transformation
  User speciﬁes an object’s vertices, normals, texture coords and send them to GPU as a   
  Vertex buﬀer object (VBO)
   Very similar to .obj ﬁles
  
  Use OpenGL functions to obtain matrices
    e.g., glTranslate, glMultMatrix, etc.
    No need to write anything on your own  
    
  
  B. Set up an easel
    View transformation Create / use a framebuffer 视图变换  framebuffer代表使用哪一个画架
    Set camera (the viewing transformation matrix) by simply calling, 
     e.g., gluPerspective(fovy可视角度，aspect长宽比,zNear近平面,zFar远平面)  
     
     

  C. Attach a canvas to the easel
   Analogy of oil painting: 
    E. you can also paint multiple pictures using the same easel  使用一个framebuffer绘制多个纹理
       一个画架对应多个画布
   One rendering pass in OpenGL
    A framebuffer is speciﬁed to use
    Specify one or more textures as output (shading, depth, etc.)
    Render (fragment shader speciﬁes the content on each texture)   
    不推荐framebuffer直接绘制到屏幕 上一帧没绘制完，第二帧开始了，此时画面撕裂
    1.垂直同步  绘制完第一帧再绘制第二帧
    2.双重缓冲，三重缓冲  将绘制的帧放到缓冲区
    
  
  D. Paint to the canvas  画油画最重要的部分
    i.e., how to perform shading
    This is when vertex / fragment shaders will be used
  For each vertex in parallel 
    OpenGL calls user-speciﬁed vertex shader: 顶点着色器
    Transform vertex (ModelView, Projection), other ops
  For each primitive, OpenGL rasterizes  openGL做的事情
    Generates a fragment for each pixel the fragment covers 
    
 
  
  For each fragment in parallel
   OpenGL calls user-speciﬁed fragment shader: 片段着色器   fragment理解为像素就行
   Shading and lighting calculations 
   OpenGL handles z-buffer depth test unless overwritten  深度测试可以openGl做，也可以自己做
  
  This is the “Real” action that we care about the most: 自己需要关心的，不用关心opengl,着色语言glsl重要
  user-deﬁned vertex, fragment shaders
    Other operations are mostly encapsulated 
    Even in the form of GUI-s  其他引擎，带有gui 
    
    
 
  Summary: in each pass
  
  Specify objects, camera, MVP, etc.
  Specify framebuffer and input/output textures
  Specify vertex / fragment shaders 
  (When you have everything speciﬁed on the GPU) Render! 老师不同意状态机的说法，可能劝退新人，告诉GPU所有需要需要确定的事情
  openGL 有立即模式，cpu说画一个三角形，GPU马上画出来  但应该减少cpu和GPU的通信
  
  Now what’s left?
  F. Multiple passes!   多次渲染
  (Use your own previous paintings for reference)     shadow map 
  
  
  
  OpenGL Shading Language (GLSL)
  Shading Languages   描述着色器是如何工作的  最终执行为GPU的汇编指令，需要做编译
  Vertex / Fragment shading described by small program
  Written in language similar to C but with restrictions
  Long history. Cook’s paper on Shade Trees, Renderman for oﬄine rendering 
     - In ancient times: assembly on GPUs！     以前要写汇编
     - Stanford Real-Time Shading Language, work at SGI
     - Still long ago: Cg from NVIDIA   以前的着色语言cg
     - HLSL in DirectX (vertex + pixel)   high level shading language
      - GLSL in OpenGL (vertex + fragment)
      
      
  Shader Setup
  Initializing (shader itself discussed later)
    Create shader (Vertex and Fragment)  写shader
    Compile shader  编译为汇编
    Attach shader to program  program 集合了所有的shader
    Link program  校验顶点着色器和片段着色器能否对的上之类的
    Use program
  Shader source is just sequence of strings shader的源文件
   Similar steps to compile a normal program  
   
   
  Shader Initialization Code (FYI)  for your interest
  
  GLuint initshaders (GLenum type, const char *filename) {
  // Using GLSL shaders, OpenGL book, page 679 
  GLuint shader = glCreateShader(type) ;  创建空的shader
  GLint compiled ; 
  string str = textFileRead (filename) ; 读文件
  GLchar * cstr = new GLchar[str.size()+1] ;
  const GLchar * cstr2 = cstr ; // Weirdness to get a const char 
  strcpy(cstr,str.c_str()) ; 复制到GLchar
  glShaderSource (shader, 1, &cstr2, NULL) ; 执行源代码
  glCompileShader (shader) ;  编译
  glGetShaderiv (shader, GL_COMPILE_STATUS, &compiled) ; 查询是否编译成功
  if (!compiled) {      
    shadererrors (shader) ;
    throw 3 ; 
    } 
    return shader ;
  }   
  
  
  
  
  Linking Shader Program (FYI)
  
  GLuint initprogram (GLuint vertexshader, GLuint fragmentshader) {
  
  GLuint program = glCreateProgram() ; 创建program
  GLint linked ; 
  glAttachShader(program, vertexshader) ;  创建两个shader
  glAttachShader(program, fragmentshader) ; 
  glLinkProgram(program) ;   链接
  glGetProgramiv(program, GL_LINK_STATUS, &linked) ; 查询link状态
   if (linked) glUseProgram(program) ;
    else {
     programerrors(program) ;
     throw 4 ;
    } 
    return program ;
  }
  
  
  Vertex Shader 顶点着色器  
    1.顶点的变换(需要知道顶点的位置)   不包括for循环，是for内的东西
    2.告诉片段着色器里面用到的需要被插值的量  varying
   vbo 告诉的，直接用就行
   attribute 顶点的属性，只存在顶点着色器
   vec3 代表x,y,z  aVertexPosition 顶点位置  代表这个顶点的位置
   aNormalPosition 法线位置
   aTextureCoord 纹理坐标
   vec4(aVertexPosition,1.0)齐次坐标
   
   varying vFragPos  varying vNormal  phong shader需要做的插值
   uniform 全局变量 直接从CPU扔到GPU来一系列全局变量 顶点着色器和片段着色器都可以用
   uniform uModelViewMatrix 物体固定的矩阵，不发生形变transform   uniform uProjectionMatrix 相机固定好的矩阵
   gl_Position 顶点的位置经过变换后的坐标
   highp high position 计算的精度
   
   
   
   
  fragment shader 
    sampler2d 纹理  可以查询这个纹理的属性 uSampler
    gl_FragColor openGl给的变量，告诉openGL要显示的颜色是什么
   
   
   
  Debugging Shaders   debug难，shader编译完运行在GPU，debug需要CPU和GPU的通信，需要专门的工具，GPU没有print，cout

  Years ago: NVIDIA Nsight with Visual Studio
    Needed multiple GPUs for debugging GLSL   需要多个GPU
    Had to run in software simulation mode in HLSL  hlsl需要软件模拟模式去跑
  
  Now
   Nsight Graphics (cross platform, NVIDIA GPUs only)  只支持NVIDIA的GPU
   RenderDoc (cross platform, no limitations on GPUs)  支持Android
   Unfortunately I don’t know if they can be used for WebGL  老师不知道
   android 使用 GAPID
   
  Personal advice 老师建议  将值以颜色显示出来  GPU的shader只能显示这些
   Print it out!
   But how?
   Show values as colors! 假设值的范围在0-200 将值除以200.0，变为0-1，传给gl_FragColor，显示后用颜色取色器取值换算
   openGL只要错误就是黑色的，谁的面子都不给
   
   
 The Rendering Equation
 Most important equation in rendering
  Describing light transport   描述光传播
  
 In real-time rendering (RTR)
  Visibility is often explicitly considered  通常显示的考虑visibility
  BRDF is often considered together with the cosine term  BRDF通常和cosine一起，在实时渲染中
  
  
 Environment Lighting
 Representing incident lighting from all directions
  Usually represented as a cube map or a sphere map (texture) 立方体map或球形map
  We’ll introduce a new representation in this course 八面体
  
 全局光照 包括直接光照和间接光照
 Direct illumination 直接光照    
 one-bounce illumination 间接光照 一次弹射光照  
 two-bounce global illumination  
 直接光照和一次弹射光照 区别明显    一次和两次区别不明显，所以实时渲染更关注一次弹射光照