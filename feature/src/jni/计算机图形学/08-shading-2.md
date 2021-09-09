Shading, Pipeline and Texture Mapping

Specular Term (Blinn-Phong)  镜面反射项（Blinn-Phong）
Intensity depends on view direction  强度取决于观察方向  
  • Bright near mirror reflection direction 高光接近于镜面反射方向
  R是镜面反射方向
  看见高光，物体表面类似镜子，高光方向在R附近，会有发散
  看见高光代表观察方向接近镜面反射方向
  
V close to mirror direction 《==》 half vector near normal  v靠近反射方向  h(半程向量)接近n(法线)
 • Measure “near” by dot product of unit vectors  
  h = bisector(v, l)=v+l/(||v + l||) (半程向量)   bisector平分线，二等分线
  光照和观察方向的角平分线方向=向量v+向量l  归一化取单位向量  todo 归一化
  
  Ls = ks(I/r 2方 )max(0, cos阿拉法) p方= ks(I/r 2方 ) max(0, n · h) p方
  Ls = specularly reﬂected light  高光反射光
  Ks = specular coefﬁcient   高光系数 颜色通常认为是白色
  阿拉法 h与法线的夹角
  cos阿拉法-》n · h   cos代表接近程度=n点乘h
  暂时不考虑系数的能量 也就是l点乘n
  cos 可以代表接近程度，但是容忍度太高了，45度时，cos的值仍然很大，会被认为是高光，会看到一个超级大的高光
  P
  Cosine Power Plots    余弦幂图
  Increasing p narrows the reflection lobe   增加p会使反射波瓣变窄  cos曲线变窄
    p越大，高光越集中，blinn-phong中p通常是100-200
    
    
Ambient Term 间接光

Shading that does not depend on anything  不依赖于任何东西的着色
• Add constant color to account for disregarded illumination and fill in black shadows 添加恒定颜色，以说明忽略的照明和填充黑色阴影
• This is approximate / fake!    这是近似的/假的！
  La = ka Ia
  La  reﬂected ambient light
  ka  ambient coefﬁcient
  精确的计算需要全局光照的知识
  
Blinn-Phong Reflection Model
 Ambient+Diffuse+Specular=Blinn-Phong Reﬂection  
 L = La + Ld + Ls
   = kaIa + kd(I/r 2 ) max(0, n · l) + ks(I/r 2 ) max(0, n · h) p
   
Shading Frequencies  点所在着色面的多少
  What caused the shading difference?
  光滑的表面会有一块一块的问题

Shade each triangle (ﬂat shading)  水平着色
Flat shading
  Triangle face is flat — one normal vector
  Not good for smooth surfaces
  
Shade each vertex (Gouraud shading)   对每个顶点着色（Gouraud着色）
 Gouraud shading               古劳德着色
 Interpolate colors from vertices across triangle   从三角形的顶点插值颜色
 Each vertex has a normal vector (how?)    每个顶点都有一个法向量（如何？）
 
Shade each pixel (Phong shading)
Phong shading
 Interpolate normal vectors across each triangle  三角形三个顶点法线，每个像素插值出一个法线方向，每个像素进行着色
 Compute full shading model at each pixel   计算每个像素的全着色模型  
 Not the Blinn-Phong Reﬂectance Model      不是Blinn-Phong反射模型
 
Shading Frequency: Face, Vertex or Pixel
 面不是很密集的物体使用 Phong shading
 很复杂的物体，面很多使用 ﬂat shading  计算量少
 

法线怎么计算 
Defining Per-Vertex Normal Vectors   定义逐顶点法向量
Best to get vertex normals from the underlying geometry     最好从基础几何体获取顶点法线
 e.g. consider a sphere                                      考虑一个球体
Otherwise have to infer vertex normals from triangle faces   否则必须从三角形面推断顶点法线
 Simple scheme: average surrounding face normals              简单方案：平均周围面法线
 Nv=Ni连续加/||Ni连续加||   ni是向量
 
Defining Per-Pixel Normal Vectors  定义逐像素的法线
Barycentric interpolation (introducing soon) of vertex normals  顶点法线的重心插值
  Don’t forget to normalize the interpolated directions         别忘了规范化插值方向，保留单位向量
  

Graphics (Real-time Rendering) Pipeline 渲染管线  在硬件显卡写好，有一部分是可编程的，例如着色，该部分为shader
  Application          input: vertices in 3D space
      Vertices positioned in screen space
  Vertex Processing    out Vertex Stream
     Model, View, Projection transforms
     Vertices positioned in screen space
     Shading   顶点着色，可以发生在顶点处理阶段
     Texture mapping (introducing soon) 纹理
  Triangle Processing   out Triangle Stream
      Triangles positioned in screen space    根据三角形顶点连成三角形
  Rasterization         out Fragment Stream
     Sampling triangle coverage
     Fragments (one per covered sample)
  Fragment Processing  Shaded Fragments
     Z-Buffer Visibility Tests 深度测试
     Shading  Phong-shading着色，发生在该阶段
     Texture mapping (introducing soon) 纹理
  Framebuffer Operations  
  Display    Output: image (pixels)  
  
  
  
Shader Programs
 Program vertex and fragment processing stages  对顶点和碎片处理阶段编程
 Describe operation on a single vertex (or fragment)  描述对单个顶点（或片段）的操作
     vertex shader 顶点着色  对顶点着色
     frgment/piexl shader  片元着色器  对像素着色
  Example GLSL fragment shader program  GLSL片段着色器程序示例  gl shader language=>glsl
  
  Shader function executes once per fragment.  着色器函数对每个片段执行一次 不用for循环
  Outputs color of surface at the current fragment’s screen sample position   输出当前碎片屏幕样本位置的表面颜色 gl_FragColor
  This shader performs a texture lookup to obtain the surface’s material color at this point, then performs a diffuse lighting calculation.
    此明暗器执行纹理查找以获取此时曲面的材质颜色，然后执行漫反射照明计算
    
  uniform sampler2D myTexture; 
  uniform vec3 lightDir; 
  varying vec2 uv; 
  varying vec3 norm;
  
  void diffuseShader() {
   vec3 kd; 
   kd = texture2d(myTexture, uv);    拿到漫反射系数kd,与纹理有关
   kd *= clamp(dot(–lightDir, norm), 0.0, 1.0);   dot点乘  简单的漫反射 kd = kd*(l,n的夹角余弦) 
   gl_FragColor = vec4(kd, 1.0);   vec3增加一个1.0 变为vec4 四维向量
  }
  
Snail Shader Program  蜗牛编程 
http://shadertoy.com/view/ld3Gz2  800行实现一个动态的蜗牛  
shadertoy 在网页上执行的shader程序
Inigo Quilez, https://youtu.be/XuSnLbB1j6E 逐步视频

现代图形学目标 实时渲染复杂场景     Unreal Engine Kite Demo 虚幻引擎
Goal: Highly Complex 3D Scenes in Realtime
 •100’s of thousands to millions of triangles in a scene
 • Complex vertex and fragment shader computations
 • High resolution (2-4 megapixel + supersampling)
 • 30-60 frames per second (even higher for VR)
 

Graphics Pipeline Implementation: GPUs
 Specialized processors for executing graphics pipeline computations  用于执行图形管道计算的专用处理器
  图形管线的硬件实现
  Discrete GPU Card (NVIDIA GeForce Titan X)  离散GPU卡
  Integrated GPU: (Part of Intel CPU die)     集成GPU
  随着GPU发展，出现其他着色器 
   geometry shader 几何着色器 动态的定义三角形
   compute  shader  计算  通用的GPU计算
  
GPU: Heterogeneous, Multi-Core Procesor  异构多核处理器  很多的小核，远远大于cpu，但是单个的性能不高
 Modern GPUs offer ~2-4 Tera-FLOPs of performance for executing vertex and fragment shader programs
 现代GPU为执行顶点和片段着色器程序提供了2-4万亿次的性能。
 Tera-Op’s of ﬁxed-function compute capability over here   Tera Op的固定函数计算能力在这里
 更多了解高性能计算
 

纹理与shading关系
  纹理定义shading每个点的颜色
Texture Mapping 纹理映射
 Different Colors at Different Places?
   Ld = kd(I/r*r )max(0, n·l)  每个点都有自己的漫反射系数，展示不同的颜色
 纹理映射的根本思路，定义任何一个点的不同属性  
 
 Surfaces are 2D 物体表面是二维的    物体的表面可以像地球仪一样展开
 Surface lives in 3D world space   物体表面生活在三维世界空间中 
 Every 3D surface point also has a place where it goes in the 2D image (texture).  
   每个3D曲面点在2D图像（纹理）中也有一个位置。
   
 Texture Applied to Surface   纹理贴到表面
 Each triangle “copies” a piece of the texture image to the surface 
   纹理坐标与三角形在物体的坐标一一对应
   1.艺术家的创造，工作量繁重
   2.自动化过程 把空间中的三角形映射成纹理坐标  参数化，几何的研究方向
 
 Visualization of Texture Coordinates 
 Each triangle vertex is assigned a texture coordinate (u,v) 物体的每个三角形映射到纹理上
 纹理坐标系用uv表示，u在0-1，v也是，约定俗成
 
 
Textures can be used multiple times! 纹理可以使用多次   纹理设计的好，往上往下都可以无缝衔接
  example textures used / tiled   使用/平铺纹理示例
  tiled texture的生成需要算法 比如，wang-tiled  
  

答疑
 作业的帧率很低
  没问题，用的CPU模拟的GPU，慢很正常  