shading(Illumination, Shading and   Graphics Pipeline)着色  光照，着色，图形管线

先采样后模糊在频谱上证明
采样后，频谱是叠加的，模糊过滤后仍然是叠加的，走样没有解决

last lectures
Rasterization
 Rasterizing one triangle  多个三角形怎么办，遮挡问题
 Sampling theory 
 Antialiasing

解决办法 
Visibility / occlusion
 Z-buffering  深度缓存
 
Painter’s Algorithm 画家算法   先画远处的物体，然后画近处的，使其覆盖远处的物体
Inspired by how painters paint Paint from back to front, overwrite in the framebuffer 

Requires sorting in depth (O(n log n) for n triangles) 需要深度排序（n个三角形的O（n logn））
Can have unresolvable depth order  p q r两两存在覆盖关系
可以具有不可解的深度顺序


实际的算法
Z-Buffer
This is the algorithm that eventually won. Idea:
• Store current min. z-value for each sample (pixel)  存储每个样本的当前最小z值（像素）
• Needs an additional buffer for depth values  需要额外的深度值缓冲区
- frame buffer stores color values    帧缓冲区存储颜色值
- depth buffer (z-buffer) stores depth  深度缓冲区（z-buffer）存储深度

IMPORTANT: For simplicity we suppose   为了简单起见，我们假设
z is always positive (smaller z -> closer, larger z -> further)  z始终为正（z越小->越近，z越大->越远）

Z-Buffer Example 深度图
z 越小，颜色越深(颜色值小)


Z-Buffer Algorithm 深度算法 无法处理透明物体，需要特殊处理
Initialize depth buffer to ∞  初始化认为深度无限大
During rasterization:
  for (each triangle T)
    for (each sample (x,y,z) in T) 
      if (z < zbuffer[x,y]) // closest sample so far   保存z深度最小的像素的颜色和深度
        framebuffer[x,y] = rgb; // update color 
        zbuffer[x,y] = z; // update depth 
        else ; // do nothing, this sample is occluded   这个样本被阻塞了
  
        
Z-Buffer Complexity
Complexity
• O(n) for n triangles (assuming constant coverage)  假设常数覆盖，一个三角形覆盖100个像素
• How is it possible to sort n triangles in linear time?
如何在线性时间内对n个三角形进行排序  算法课上，至少 需要nlogn的时间排序
 这里不是排序，只是记录深度最小值

Drawing triangles in different orders?
  深度算法与三角形的顺序无关，只要绘制最近的就可
  一般实现使用浮点型  浮点型判等很难，所以几乎不会有两个三角形深度一样
  对于深度一样的本课不做深入处理

Most important visibility algorithm 
  • Implemented in hardware for all GPUs    深度测试，确保深度是正确的

 msaa 每个像素取采样点，对每一个采样点做z-buffer
 
 
Shading
 Illumination 
 Shading Graphics Pipeline     
 
Shading: Deﬁnition
In Merriam-Webster Dictionary  韦氏词典
 shad·ing, [ˈʃeɪdɪŋ], noun 名词
 The darkening or coloring of an illustration or diagram with parallel lines or a block of color.
 用平行线或色块使插图或图表变暗或着色
 
In this course
 The process of applying a material to an object.  将材质应用于物体的过程
 
其他模型  phong reflect model  Blinn-Phong是对 phong model的改进 h的计算比反射方向R好算
A Simple Shading Model (Blinn-Phong Reflectance Model) 着色模型  Blinn-Phong反射模型
Perceptual Observations  感性观察
  存在相同点
  高光  Specular highlights  光滑的物体在镜面反射附近反射
  漫反射 Diffuse reflection   四面八方放射
        Ambient lighting    间接光照，环境光照  环境光打在物体的背面，没有直射光也可以看到


Shading is Local 着色是局部的
Compute light reflected toward camera at a specific shading point  着色点
 Inputs:  v,n,l都是单位向量
 • Viewer direction, v   观察方向
 • Surface normal, n     法线，垂直于平面
 • Light direction, l (for each of many lights)  光照方向
 • Surface parameters (color, shininess, …)  颜色，光泽
 
No shadows will be generated! (shading ≠ shadow) 不会生成阴影 
 shading只考虑点的着色，光照，不考虑其他物体，如阴影
 
Diffuse Reﬂection  漫反射
 Light is scattered uniformly in all directions    光向各个方向均匀散射
 Surface color is the same for all viewing directions 
 
But how much light (energy) is received?  认为光是一种能量，物体的明暗发生变化
  Lambert’s cosine law   兰伯特余弦定律
  Top face of cube receives a certain amount of light
  Top face of 60º rotated cube intercepts half the light
  In general, light per unit area is proportional to cos θ = l • n
  
Light Falloff
  太阳光，能量守恒
  每个球壳的能量相等   球的面积 4pi r*r
  一个单位距离的能量是I intensity  [ɪnˈtensəti] 强烈
  当距离为r时，能量是I/r2   4pi*I=4pi r*r* x    x=i/r*r
  
Lambertian (Diffuse) Shading
Shading independent of view direction 
 Ld = kd(I/r*r )max(0, n·l) 
 Ld               diffusely reﬂected light
 kd               diffuse coefﬁcient (color)  漫反射系数（颜色），shading point会吸收部分能量，将不吸收的颜色反射出去
 I/r*r            energy arrived at the shading point
 max(0, n · l)    energy received by the shading point  n点乘l  nlcosθ 
                  反射的cos不可能是负的，最大是0，负的是折射的情况
 漫反射与V没有关系，无论从那看都是一样的
 
 Produces diffuse appearance  产生漫反射外观
   光照正面量  与光照夹角为0    接收到全部能量
   侧面暗     与光照存在夹角    接收到能量少 从正面到侧面是量到暗的过程              