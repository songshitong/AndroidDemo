
Applications of textures
 
 Introduction to geometry (2nd part of this course!)
   Examples of geometry Various representations of geometry

Application fro texture

Many, Many Uses for Texturing 纹理的作用

In modern GPUs, texture = memory + range query (filtering)

• General method to bring data to fragment calculations Many applications

• Environment lighting  环境光照

• Store microgeometry

• Procedural textures

• Solid modeling

• Volume rendering


Environment Map 环境贴图    使用环境贴图，物体表面出现环境投影   犹他茶壶

Environmental Lighting  环境光照
Environment map (left) used to render realistic lighting   用于渲染逼真的照明的环境图（左）

Spherical Environment Map  球形环境图
Spherical Map — Problem
  Prone to distortion (top and bottom parts)!  上下出现扭曲
  
 Cube Map  把环境光记录在立方体上而不是球  需要做一个对应关系
 A vector maps to cube point along that direction. The cube is textured with 6 square texture maps.
 Much less distortion! Need dir->face computation 
 

Textures can affect shading! 纹理会影响着色
 Textures doesn’t have to only represent colors
 What if  stores  the height / normal?   在法线的相对高度  凸出来，凹下去
 Bump/normal mapping      凹凸/法线贴图
 Fake the detailed geometry
 橘子 的凹凸，使用贴图，减少三角形表示
  Relative height to the underlying surface  黑的地方认为是0，白的认为是1 
  
  
Bump Mapping

Adding surface detail without adding more triangles

• Perturb surface normal per pixel (for shading computations only)   每个像素的扰动表面法线（仅用于shading计算）

• “Height shift” per texel defined by a texture   纹理定义的每个纹理像素的“高度偏移”

• How to modify normal vector?     


How to perturb the normal (in flatland)    flatland case 平原

• Original surface normal n(p) = (0, 1)

• Derivative at p is dp = c * [h(p+1) - h(p)]     导数，相邻两个点  差分方法得到切线 dp是该点的梯度，向右移动一个单位，向上移动多少
  c 是常数，代表凹凸影响大不大，做个缩放

• Perturbed normal is then n(p) = (-dp, 1).normalized()  法线垂直于切线，切线逆时针旋转90度就是法线，然后做归一化，法线是1


How to perturb the normal (in 3D)  在3维中

• Original surface normal n(p) = (0, 0, 1)

• Derivatives at p are   

- dp/du = c1 * [h(u+1) - h(u)]   纹理水平方向移动一个

- dp/dv = c2 * [h(v+1) - h(v)]    纹理竖直方向移动一个

• Perturbed normal is n = (-dp/du, -dp/dv, 1).normalized()

• Note that this is in local coordinate!

  More will be elaborated in FAQ of HW3
  转为世界坐标
  
 
Textures can affect shading!

Displacement mapping — a more advanced approach  置换贴图-一种更高级的方法  位移贴图

  Uses the same texture as in bumping mapping
  Actually moves the vertices  边缘和投影是不规则的，而凹凸贴图都是规则图形
   directx 动态纹理API  需要的时候更精细的贴图，不需要的时候使用粗糙的贴图
  
3D Procedural Noise + Solid Modeling   3D程序噪声+实体建模   定义3维的纹理  噪声算法

Provide Precomputed Shading 提供预先计算的阴影 人的褶皱更丰富，纹理提供阴影  一般用作不跟随环境光变化的地方，常暗的地方

3D Textures and Volume Rendering 3D纹理和体积渲染  



Introduction to geometry

Examples of geometry
 Various representations of geometry
 
Many Ways to Represent Geometry 几何表示
Implicit           隐含的      隐式几何
 algebraic surface 代数曲面
 level sets   水平集
  distance functions 距离函数
  Fractals
  Constructive solid geometry
  
Explicit       明确的          显式几何         
• point cloud  点云
• polygon mesh  多边形网格
• subdivision, NURBS  细分

Each choice best suited to a different task/type of geometry  每种选择最适合不同的任务/几何类型


“Implicit” Representations of Geometry 没有点的具体坐标
Based on classifying points   基于分类点
 • Points satisfy some specified relationship
  E.g. sphere: all points in 3D, where x 2 +y 2 +z 2 = 1  球里面任何一个点 坐标的平方和=1  统一，x方+y方+z方-1=0
  推广：
  More generally, f(x,y,z) = 0
  红色代表1，蓝色代表0  满足=0则代表在蓝色区域内
  

Implicit Surface – Sampling Can Be Hard  隐式的坏处

f(x, y, z) = (2 -(x方 + y方)开放 )方 + z方 - 1

What points lie on f(x,y,z) = 0?     哪些点满足f函数，就是这个函数代表的形状是什么


Implicit Surface – Inside/Outside Tests Easy  有点

f(x, y, z) = x 2 + y 2 + z 2 - 1

Is (3/4, 1/2, 1/4) inside?

Just plug it in: f(x,y,z) = –1/8 < 0 Yes, inside.   =0物体上 <0物体内  >0物体外



“Explicit” Representations of Geometry

All points are given directly or via parameter mapping  给出所有的点/参数映射    马鞍面
 f:R方-》R三次方 (u,v)->(x,y,z)
 
 
Explicit Surface – Sampling Is Easy

f(u, v) = ((2 + cos u) cos v, (2 + cos u) sin v, sin u)  对应的x,y,z

What points lie on this surface?   找出所有的点
Just plug in (u,v) values!        


Explicit Surface – Inside/Outside Test Hard

f(u, v) = (cos u sin v, sin u sin v, cos v)

Is (3/4, 1/2, 1/4) inside?


No “Best” Representation – Geometry is Hard!

“I hate meshes.
 I cannot believe how hard this is.
 Geometry is hard.”
— David Baraff
Senior Research Scientist Pixar Animation Studios   皮克斯动画工作室资深研究科学家

Best Representation Depends on the Task!


Algebraic Surfaces (Implicit)
 Surface is zero set of a polynomial in x, y, z
 x,y,z平方和=1 球形
 (r-(x,y平方和)开方)平方+z方=R方  圆环体
 (x方+9*y方/4+z方-1)三次方=x方z三次方+9y方z三次方/80  心形体
 
 More complex shapes?  奶牛的形状函数
 
 Constructive Solid Geometry (Implicit)  csg  构造实体几何
 
 Combine implicit geometry via Boolean operations  对基本形状做布尔运算   交集，并集，减集
 
 
Distance Functions (Implicit)

Instead of Booleans, gradually blend surfaces together using

Distance functions:

giving minimum distance (could be signed distance) from anywhere to object  空间的点到一个物体的最小距离


Distance Functions (Implicit)

An Example: Blending (linear interp.) a moving boundary    示例：混合（线性插值）移动边界
 SDF(A) df距离函数 s 正负   对于A 阴影右侧线为0 右侧是正+ 左侧是负- 代表远离还是接近
 blend(SDF(A),SDF(B))相当于blend两个物体的边界
 
 
Blending Distance Functions (Implicit)
 Can blend any two distance functions d1, d2: 
 
Scene of Pure Distance Functions 距离函数的应用场景
  表示圆滑的过渡
  

Level Set Methods (Also implicit) 水平集

Closed-form equations are hard to describe complex shapes  闭合形式的方程很难描述复杂的形状
 Alternative: store a grid of values approximating function   备选方案：存储值近似函数的网格
 
 Surface is found where interpolated values equal zero  找到内插值等于零的曲面
  Provides much more explicit control over shape (like a texture) 提供对形状（如纹理）的更显式控制 
  
 生成类似于等高线的结果
 
Level Sets from Medical Data (CT, MRI, etc.) 医疗

Level sets encode, e.g., constant tissue density 
  
  
Level Sets in Physical Simulation  模拟

Level set encodes distance to air-liquid boundary  


Fractals (Implicit) 分形

Exhibit self-similarity, detail at all scales  自相似，部分和整体相似，计算机是递归    容易引起走样，变化的频率快
 “Language” for describing natural phenomena Hard to control shape!
  
  
Implicit Representations - Pros & Cons

Pros:

• compact description (e.g., a function)

• certain queries easy (inside object, distance to surface)

• good for ray-to-surface intersection (more later)

• for simple shapes, exact description / no sampling error

• easy to handle changes in topology (e.g., fluid) Cons:

• difficult to model complex shapes  