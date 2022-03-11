
Explicit Representations  

 Curves

Bezier curves 
De Casteljau’s algorithm
 B-splines, etc.

Surfaces

Bezier surfaces 
Triangles & quads 
- Subdivision,simpliﬁcation, regularization


Many Explicit Representations in Graphics  几何的显式表达

triangle meshes 
Bezier surfaces 
subdivision surfaces 
NURBS 
point clouds


Point Cloud (Explicit) 点云

Easiest representation: list of points (x,y,z)
 Easily represent any kind of geometry 
 Useful for LARGE datasets (>>1 point/pixel)
 Often converted into polygon mesh     经常转为多边形面
 Difficult to draw in undersampled regions   点云量少，表达效果不好，通常用于扫描后的结果
 

Polygon Mesh (Explicit)  多边形面

Store vertices & polygons (often triangles or quads)   通常是三角形或四边形面
Easier to do processing / simulation, adaptive sampling 
More complicated data structures 
Perhaps most common representation in graphics   应用最广泛的


The Wavefront Object File (.obj) Format  obj文件

Commonly used in Graphics research 
Just a text file that specifies vertices, normals, texture coordinates and their connectivities
v 空间的八个点，代表立方体
vn 六个面的法线，自动生成的可能有冗余数据8个
vt 12个纹理坐标，最多24个  4*6 一个面四个点
f  face 三角形的连接情况  5/1/1  1/2/1 4/3/1   三角形 5 1 4  纹理 1 2 3  法线 1 1 1


Curves
Camera Paths

Bézier Curves  贝塞尔曲线
Defining Cubic Bézier Curve With Tangents  用一系列的控制点去定义曲线
 出发方向 bob1  结束方向b1b2

Evaluating Bézier Curves (de Casteljau Algorithm)  

  Consider three points (quadratic Bezier) b0 b1 b2  二次贝塞尔 quadratic 二次的 quadra 四面体 quadra kill 大杀四方
  Insert a point using linear interpolation b01 t   做一次线性插值
  Insert on both edges  b01 b11          
  Repeat recursively  b02                又一次
  Run the same algorithm for every t in [0,1]
  
  Cubic Bézier Curve – de Casteljau  三次贝塞尔
  Four input points in total   四个点  
  Same recursive linear interpolations 
  
  
Bézier Curve – Algebraic Formula  代数公式

  de Casteljau algorithm gives a pyramid of coefficients   给出系数的金字塔
  Every rightward arrow is multiplication by t, 向右箭头乘t
  Every leftward arrow by (1–t)
  
  Example: quadratic Bézier curve from three points
  b01(t) = (1 − t)b 0 + tb 1
  b11(t) = (1 − t)b 1 + tb 2
  b02(t) = (1 − t)b01 + tb11
  b02(t) = (1 - t)方 b0 + 2t(1 - t)b1 + t方b2
  
  
  Bézier Curve – General Algebraic Formula
  
  Bernstein form of a Bézier curve of order n:  n+1个控制点
  伯恩斯坦多项式
  
  Example: assume n = 3 and we are in R 3 i.e. we could have control points in 3D such as:
  
  b 0 = (0, 2, 3), b 1 = (2, 3, 5), b 2 = (6, 7, 9), b 3 = (3, 4, 5)
  
  These points define a Bezier curve in 3D that is a cubic polynomial in t:
  
  b n (t) = b 0 (1 - t)三次方 + b 1 3t(1 - t) 平方 + b 2 3t方 (1 - t) + b 3 t三次方
  
  
Properties of Bézier Curves

Interpolates endpoints 
 • For cubic Bézier: b(0) = b 0 ; b(1) = b 3  必须过起点和终点

Tangent to end segments 
• Cubic case: b0' (0) = 3(b 1 - b 0 ); b0'(1) = 3(b 3 - b 2 ) 开始位置的切线=3倍坐标相减 展现了切线的方向

Affine transformation property  放射变换 其他变换不是，例如投影
  • Transform curve by transforming control points  变换曲线通过变换控制点，变换后的控制点重新画曲线即可

Convex hull property   凸包特性
  • Curve is within convex hull of control points   曲线在控制点形成的凸包内
  
  
BTW: What’s a Convex Hull
  围成点的最小凸多边形  类似于橡皮筋包围多个钉子
  
  
Piecewise Bézier Curves 逐段贝塞尔
 Higher-Order Bézier Curves?  10个控制点
 Very hard to control! Uncommon    
  
 Instead, chain many low-order Bézier curve 
 Piecewise cubic Bézier the most common technique  逐段定义，一般是4个点的三次贝塞尔，控制点可以当成控制杆取拉动
 Widely used (fonts, paths, Illustrator, Keynote, …) 
 
 Piecewise Bézier Curve – Continuity 连续性
 多段合并怎么得到平滑的曲线  拼接点的两个的控制杆可以连成直线，同一方向，并且控制点的距离一样  距离不一样，曲线的变化速度不一样
 数学上导数连续
 
 C0 continuity: a n = b 0  第一段的终点等于第二段的起点   c0连续
 C1 continuity: a n = b 0 =1/2*(a n-1 + b1 )  an等于b0等于前后两点的中点   c1连续  切线相等  可以认为一阶导数连续
 
 还有c2连续 也叫曲率连续  二级导数连续  
 描述了一个函数到底“平滑”到什么程度。
  C^0：连续（注意连续不一定可导，比如魏尔斯特拉斯函数）
  C^r：1到r阶导数都连续
  C^∞：任意阶导数都连续，这个叫“光滑”。
  作用是，如果你需要使用函数的r阶导数去做一些理论，你必须要先证明这个函数属于C^r，否则导数不存在的话你的理论也就没有基础了。
 
 作者：知乎用户
 链接：https://www.zhihu.com/question/281832199/answer/422540922
 来源：知乎
 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 
 
 Other types of splines  样条
 
  Spline  相当于对曲线的总结
  a continuous curve constructed so as to pass through a given set of points and have a certain number of continuous derivatives.
    一条连续曲线，构造为可以通过给定的一组点并具有一定数量的连续导数
  In short, a curve under control
  图片是早期画曲线的图
  
  
 
  
  B-splines     basis splines 基样条   相对于内塞尔的增强版
   Short for basis splines 
   Require more information than Bezier curves 
   Satisfy all important properties that Bézier curves have (i.e. superset)
   贝塞尔 会有动一个点，整个曲线都要改  分段贝塞尔没有
   b-splines 具有局部性，改动一个点，会有影响范围  b样条不用分段
   
 
Important Note

•

In this course


-

-

-

We do not cover B-splines and NURBS  NURBS非均匀有理b样条

We also do not cover operations on curves (e.g. increasing/decreasing orders, etc.)

To learn more / deeper, you are welcome to refer to Prof. 
Shi-Min Hu’s course: https://www.bilibili.com/video/av66548502?from=search&seid=65256805876131485   


Surfaces

Bezier surfaces 
Subdivision surfaces (triangles & quads)
  
Bézier Surfaces

Extend Bézier curves to surfaces
Evaluating Surface Position For Parameters (u,v)

Visualizing Bicubic Bézier Surface Patch 
  根据16个点，先得到统一方向的四条贝塞尔，然后做线性插值，最后这条线扫过的面积

For bi-cubic Bezier surface patch,

Input: 4x4 control points Output is 2D surface parameterized by (u,v) in [0,1]方   u,v代表两个方向时间


Method: Separable 1D de Casteljau Algorithm

Goal: Evaluate surface position corresponding to (u,v) (u,v)-separable application of de Casteljau algorithm

• Use de Casteljau to evaluate point u on each of the 4 Bezier curves in

u. This gives 4 control points for the “moving” Bezier curve

• Use 1D de Casteljau to evaluate point v on the “moving” curve



Mesh Operations: Geometry Processing 几何处理

 Mesh subdivision     细分
 Mesh simplification  简化  用更少的面存储
 Mesh regularization  正规化  让三角形不出现特别尖，特别长的现象