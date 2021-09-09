Ray Tracing 1 

(Whitted-Style Ray Tracing) Whitted 怀特风格

Why Ray Tracing?

Rasterization couldn’t handle global eﬀects well   全局效果
 (Soft) shadows 
 And especially when the light bounces more than once
 
 soft shadow     Glossy reflection    Indirect Illumination  间接光照
 

Why Ray Tracing?
 Rasterization is fast, but quality is relatively low    光珊化快，但是是一种近似的方法，质量较低
 
 
Why Ray Tracing?

Ray tracing is accurate, but is very slow  光线追踪准确，但是非常慢

  Rasterization: real-time, ray tracing: ofﬂine
   ~10K CPU core hours to render one frame in production  一帧需要 10k cpu 每小时
   
 
Basic Ray-Tracing Algorithm


Light Rays

Three ideas about light rays

  1. Light travels in straight lines (though this is wrong)  光沿直线传播   光具有波动性，暂不考虑

  2. Light rays do not “collide” with each other if they cross (though this is still wrong)  光线和光线不会发生碰撞

  3. Light rays travel from the light sources to the eye (but the physics is invariant under path reversal - reciprocity).
    光从光源出发，进入人的眼睛   光线的可逆性，可以认为眼睛发出感知光线，到达光源  光路可逆性


  “And if you gaze long into an abyss, the abyss also gazes into you.” — Friedrich Wilhelm Nietzsche (translated) 
  
  
 
Ray Casting  光线投射

Appel 1968 - Ray casting

1. Generate an image by casting one ray per pixel    image 成像平面

2. Check for shadows by sending a ray to the light    


Ray Casting - Generating Eye Rays
  Pinhole Camera Model
  
  eye ray   (starts at eye and goes through pixel)    camera ray    打到一个物体
  closest scene  intersection point             只看最近的相交点
  
Ray Casting - Shading Pixels (Local Only) 判断会不会被照亮  
  shadow ray 到光源的虚线  存在物体，被遮挡
  perform shading calculation here to compute color of pixel (e.g. Blinn Phong model)  
  
 
Recursive (Whitted-Style) Ray Tracing  递归光线追踪，上面只进行了一次
 “An improved Illumination model for shaded display” T. Whitted, CACM 1980  
 
 
Recursive Ray Tracing
 1. 眼睛投射到物体  primary ray   
 2. Reﬂected ray (specular reﬂection)  物体反射  secondary rays
    Refracted rays (specular transmission)  物体折射  secondary rays
 3. shadow rays  反射点，折射点到光源的虚线   把所有点的颜色相加，为看到物体在成像面的着色点，挡住的阴影线不算
 
 
Ray-Surface Intersection  交点计算

Ray Equation  射线方程
Ray is defined by its origin and a direction vector    射线由其原点和方向矢量定义
 r(t)=o+td  0<=t<无穷   r point along ray   t time  o origin  d (normalized) direction
 

Ray Intersection With Sphere  和球相交

Ray: r(t) = o + t d, 0 <= t < 正无穷 
Sphere: p: (p-c)方-R方=0   球上点到球心的距离=半径

What is an intersection?
The intersection p must satisfy both ray equation and sphere equation

Solve for intersection:(o + t d - c)方- R 方 =0
  at方 + bt + c = 0, where
  a=d点乘d
  b=2(o-c)点乘d
  c=(o-c)点乘(o-c)-R方
  t=(-b+-(b方-4ac)开方)/2a
  
  
Ray Intersection With Implicit Surface
Ray: r(t) = o + t d, 0 <= t < 无穷

general implicit surface  p : f(p) = 0  
Substitute ray equation  f(o + t d) = 0
Solve for real, positive roots  结果是实数不是虚数  正的

Ray Intersection With Triangle Mesh  与三角形面的相交
Why?

• Rendering: visibility, shadows, lighting …

• Geometry: inside/outside test How to compute?  
  在三维物体内点一个点，发出射线，与物体交点是奇数,一定在物体内，交点为偶数一定物体外 物体不能有洞

Let’s break this down:

• Simple idea: just intersect ray with each triangle  与每个三角形求交，找到最近的那个

• Simple, but slow (acceleration?)         

• Note: can have 0, 1 intersections (ignoring multiple intersections)



Ray Intersection With Triangle 光线与三角形的交点

Triangle is in a plane

• Ray-plane intersection      判断光线与三角形所在的平面是否相交，

• Test if hit point is inside triangle   判断交点是否在三角形内
 
 Many ways to optimize…



Plane Equation 平面定义

Plane is defined by normal vector and a point on plane   一条法线和一个点
  法线确定一个面，但是面可以沿着法线移动，在定义一个点，平面就固定下来了
  
  
Plane Equation  (if p  satisfies it, then p is on the plane ）
  
 p : (p-p0)点乘 N =0  (两个点的连线与法线垂直)     ax+by+cz+d=0  p用x,y,z表示 平面方程
 p: all points on plane
 p0: one point on plane 
 N :  normal vector
 
 交点既满足光线方程有满足平面方程
 Ray equation:
 r(t) = o + t d, 0 <= t < 正无穷
 
 
 solve for  intersection
 Set p = r(t)  and solve for t
 
 (p-p0)点乘 N = (o+td - p0)点乘N =0
 t=(p0-o)点乘N / d 点乘 N       check 0<=t< 正无穷   下一步 计算是否在三角形内
 
 
 更简单的算法
 Möller Trumbore Algorithm    mt 算法
 
 A faster approach, giving barycentric coordinate directly
 Derivation in the discussion section!
 解方程 克莱姆法则
 
 
 相交 加速的计算方法
 Accelerating Ray-Surface Intersection
 
 Ray Tracing – Performance Challenges
 
 Simple ray-scene intersection
 
 • Exhaustively test ray-intersection with every triangle
 
 • Find the closest hit (i.e. minimum t)
 
 Problem:
 
 • Naive algorithm = #pixels ⨉ # traingles (⨉ #bounces)
 
 • Very slow!
 
 For generality, we use the term objects instead of triangles later (but doesn’t necessarily mean entire objects)
  为了通用起见，我们稍后使用术语“对象”而不是三角形（但不一定表示整个对象）
  
 San Miguel Scene, 10.7M triangles   San Miguel场景，存在10.7M个三角形
 
 
 Bounding Volumes  包围盒    包围体积
 
 Bounding Volumes
 
 Quick way to avoid intersections: bound complex object with a simple volume
 
 • Object is fully contained in the volume
 
 • If it doesn’t hit the volume, it doesn’t hit the object  先判断与包围盒相交，然后判断与物体相交
 
 • So test BVol first, then test object if it hits
 
 
 Ray-Intersection With Box
 
 Understanding: box is the intersection of 3 pairs of slabs  3个不同对面的交集
 
 Specifically:
 
 We often use an Axis-Aligned Bounding Box (AABB)(轴对⻬包围盒)
  i.e. any side of the BB is along either x, y, or z axis    包围盒的每个轴与坐标系的x,y,z是平行的，不是任意旋转的
  
  
 
 Ray Intersection with Axis-Aligned Box
 
 2D example; 3D is the same! Compute intersections with slabs     先将维，降级思维的复杂度，然后升维
 
 and take intersection of tmin/tmax intervals   
 第一条 进入与出去后延长线的线段
 第二条  出发点与出点的线段      Note: t min < 0  这时 tmin 小于0
 求两个线段的交集，得到最终结果
 
 
 Ray Intersection with Axis-Aligned Box

 Recall: a box (3D) = three pairs of inﬁnitely large slabs  3维情况
  Key ideas

   The ray enters the box only when it enters all pairs of slabs  进入所有的对面
   The ray exits the box as long as it exits any pair of slabs    退出任意一个的对面
 
   For each pair, calculate the t min and t max (negative is ﬁne)
   For the 3D box, t enter = max{t min }, t exit = min{t max }   t enter 最后光线进入的时间   texit 最早离开的时间
    
   If t enter < t exit , we know the ray stays a while in the box  (so they must intersect!) 光线进入的时间小于出去的时间，相交
   (not done yet, see the next slide) 负的情况没有考虑
   
   
 
 Ray Intersection with Axis-Aligned Box
 
 However, ray is not a line  是一条射线
 
   Should check whether t is negative for physical correctness!
 
 What if texit < 0?      texit<0 t在光线背后的时刻

   The box is “behind” the ray — no intersection!
 
 What if t exit >= 0 and t enter < 0?
 
     The ray’s origin is inside the box — have intersection!
 
 In summary, ray and AABB intersect iff tenter < texit && texit >= 0   iff当且仅当
 
 
 Why Axis-Aligned ? 为什么使用轴对齐的盒子  光线和轴平行的面好求
 General  总的公式
 t=(p0-o)点乘N / d 点乘 N  
 
 Slabs perpendicular  to x-axis  平面垂直于x轴，只考虑光线在x轴上的传播   px0 p在x的分量
 t=(px0-ox)/ dx   只用考虑x方向
 