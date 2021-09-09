Today
• Using AABBs to accelerate ray tracing
 - Uniform grids   均匀的格子
 - Spatial partitions  空间划分
 
Uniform Spatial Partitions (Grids)

Preprocess – Build Acceleration Grid
  1. Find bounding box
  2. Create grid
  3. Store each object in overlapping cells  找到和物体重叠的小格子 
  
Ray-Scene Intersection
  Step through grid in ray traversal order
  For each grid cell
    Test intersection with all objects stored at that cell  找到和光线相交的有物体的格子
    然后判断光线与物体是否相交，交点是多少
    光线与格子的交点  假设光线向右上传播，找到第一个格子，第二个肯定在第一个的右边或上边，实际可能不是这么做的
      如何光栅化一条线，可以用上面的算法
 
Grid Resolution?
  One cell 
   • No speedup
  Too many cells
   •Inefficiency due to extraneous grid traversal 
  Heuristic:  试出来的
   • #cells = C * #objs
   • C ≈ 27 in 3D      
   
Uniform Grids – When They Work Well
  Grids work well on large collections of objects that are distributed evenly in size and space
  
Uniform Grids – When They Fail
  “Teapot in a stadium” problem  物体分布不均匀，墙体存在镂空 
  

Spatial Partitions
Spatial Partitioning Examples  计算几何的研究   
  Oct-Tree  八叉树  横竖两刀分四块
  KD-Tree     横竖两刀分三块
  BSP-Tree  没有横平竖直切割，不好计算    
Note: you could have these in both 2D and 3D. In lecture we will illustrate principles in 2D. 

KD-Tree Pre-Processing
  Note: also subdivide   nodes 1 and 2, etc.蓝色和绿色都需要划分
  
Data Structure for KD-Trees  类似二分查找

Internal nodes store 
 • split axis: x-, y-, or z-axis
 • split position: coordinate of split plane along axis
 • children: pointers to child nodes   
 • No objects are stored in internal nodes 中间节点
Leaf nodes store   物体存在叶子节点
 • list of objects  
  
Traversing a KD-Tree
  判断光线与最大的盒子即根节点 是否有交点
  Internal node: split  判断与子节点是否相交
  Assume it’s leaf node: intersect all objects  叶子节点判断 光线与物体是否相交
  Internal node: split
  Leaf node: intersect all objects
  Internal node: split
  Leaf node: intersect all objects
  Intersection found
  
  KD tree问题 三角形与盒子求交，一个物体可能存在多个叶子节点
  
  
Object Partitions & Bounding Volume Hierarchy (BVH)  物体划分 图形学用的多的
  Bounding Volume Hierarchy (BVH)
   所有物体建立一个包围盒 root
   物体分为左右两堆，分别建立包围盒
   左侧一堆继续分为两堆，建立包围盒
   
  Summary: Building BVHs
  
  Find bounding box 
  Recursively split set of objects in two subsets 
  Recompute the bounding box of the subsets 
  Stop when necessary 
  Store objects in each leaf node  一个物体只存在一个叶子
  包围盒重叠越少越好
  
  
  Building BVHs
  
  How to subdivide a node?
  
  • Choose a dimension to split
  
  • Heuristic #1: Always choose the longest axis in node   沿着物体最长的轴划分
  
  • Heuristic #2: Split node at location of median object   取中间的物体，保证左右两边的相等
     这就是平衡二叉树，左右越相似，树的深度越小，遍历次数越少
     取一组无序数，第i大数，o(n)  快速选择  然后取中位数，这样不用排序了，排序复杂度要高
  
  Termination criteria?
  
  • Heuristic: stop when node contains few elements (e.g. 5)
  
  
  
  Data Structure for BVHs  数据结构
  
  Internal nodes store 中间节点
  
  • Bounding box
  
  • Children: pointers to child nodes
   
   Leaf nodes store
  
   • Bounding box
  
   • List of objects Nodes represent subset of primitives in scene
  
   • All objects in subtree
   
   
   BVH Traversal
   伪代码
   Intersect(Ray ray, BVH node) { 
     ///根节点是否相交
     if (ray misses node.bbox) return;
     ///叶子节点
     if (node is a leaf node)
      test intersection with all objs; 
      return closest intersection;
     ///中间节点 与子节点判断是否相交 
     hit1 = Intersect(ray, node.child1); 
     hit2 = Intersect(ray, node.child2);
     return the closer of hit1, hit2;
   }  
  
  
  Spatial vs Object Partitions  空间划分对比物体划分
  
  Spatial partition (e.g.KD-tree)
  
  • Partition space into non-overlapping regions
  
  • An object can be contained in multiple regions
  
  Object partition (e.g. BVH)
  
  • Partition set of objects into disjoint subsets
  
  • Bounding boxes for each set may overlap in space
  
  
  
  
  Basic radiometry (辐射度量学) 国内课程没有覆盖的
  
  - Advertisement: new topics from now on, scarcely covered in other graphics courses
  
  
  
  Radiometry — Motivation
  
  Observation
  
  • In assignment 3, we implement the Blinn-Phong model
  
  • Light intensity I is 10, for example
  
  • But 10 what? 
  
  Do you think Whitted style ray tracing gives you CORRECT results? 结果比较假
  
  All the answers can be found in radiometry   精确的光的度量，能量损耗
   • Also the basics of “Path Tracing”
   
   
 Radiometry
 
 Measurement system and units for illumination 
 Accurately measure the spatial properties of light  仍然假设光沿直线传播
  - New terms: Radiant flux, intensity, irradiance, radiance  定义光的属性
  Perform lighting calculations in a physically correct manner 在物理上定义光的准确属性
  ————————————————————————
  My personal way of learning things:
  - WHY, WHAT, then HOW    为什么要学   学的是什么   学习的一系列方法
   how最不重要，应为容易忘
   
   
 Radiant Energy and Flux (Power)
 
 Definition: Radiant energy is the energy of electromagnetic radiation.
  It is measured in units of joules, and denoted by the symbol:   能量
   Q[J = Joule]   单位焦耳
 
 ///单位时间能量  
 Definition: Radiant flux (power) is the energy emitted, reflected, transmitted or received, per unit time 
   @=dQ/dt [W=Watt] [lm=lumen]*   单位功率  流明
   
 Flux – #photons flowing through a sensor in unit time  单位时间体现了亮度 
 
 
 Important Light Measurements of Interest 
  Light Emitted From A Source     “Radiant Intensity”
  Light Falling On A Surface      “Irradiance”
  Light Traveling Along A Ray     “Radiance”
  
  
 Radiant Intensity
 
 单位立体角的能量
 Definition: The radiant (luminous) intensity is the power per unit solid angle (?) emitted by a point light source.
   solid angle 立体角
   i(w) = d@/dw
   [w/sr][lm/sr=cd=candela] 
   
 Angles and Solid Angles  角和立体角
   Angle: ratio of subtended arc length on circle to radius
    ✓=l/r     弧长/半径   圆放大缩小不影响角度
    Circle has 2⇡ radians
    
 Solid angle: ratio of subtended area on sphere to radius squared
   ⌦ = A/r 2     面积/半径平方
   Sphere has 4pi steradians   steradians单位
   
 
 Differential Solid Angles 单位立体角
  dA =(r d✓)(r sin ✓ d") =r 2 sin ✓ d✓ d"
  
  d! = = sin ✓ d✓ d#
  
 ! as a direction vector 欧米伽
   Will use ! to denote a direction vector (unit length)  方向向量
   
 Isotropic Point Source  点光源  各个方向均匀发射
  I= /4pi     
   
 
 Modern LED Light   LED灯
 
 Output: 815 lumens
 
 (11W LED replacement for 60W incandescent)  11w相当于60w的白炽灯
 
 Radiant intensity?
 
 Assume isotropic:
 
 Intensity = 815 lumens / 4pi sr = 65 candelas     
   