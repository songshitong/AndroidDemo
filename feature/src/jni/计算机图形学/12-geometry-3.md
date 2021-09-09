Mesh Operations: Geometry Processing

Mesh subdivision 
Mesh simplification 
Mesh regularization

Mesh Subdivision (upsampling) 增加三角形数量  
  Increase resolution
  
Mesh Simplification (downsampling)  网格简化
  Decrease resolution; try to preserve shape/appearance
  
Mesh Regularization (same #triangles)  正三角形
  Modify sample distribution to improve quality    
  
  
Loop Subdivision

Common subdivision rule for triangle meshes 
 First, create more triangles (vertices) 
 Second, tune their positions  调整他们的位置
 

Loop Subdivision  loop 细分 loop是个人   loop细分是三角形网格

 Split each triangle into four   连接三条边的中点
 Assign new vertex positions according to weights 
  - New / old vertices updated differently   区分新的顶点和旧的顶点，分别改变位置
  
 Loop Subdivision — Update
  Update to: 3/8 * (A + B) + 1/8 * (C + D)   白点位置收AB影响更多，受CD影响更少  理解为对周围几个点的平均，所以会更平滑
  非边界情况，新点被两个三角形共享，共享的两个点为A B 不共享的为C D
  
 For old vertices (e.g. degree 6 vertices here):
  Update to: (1 - n*u) * original_position +   u * neighbor_position_sum
  n : vertex degree    数据结构图论  顶点的度  顶点链接的边的数量
  v : 3/16 if n=3;3/(8n) otherwise    连接其他顶点最多，受其他点影响越大，可以由其他点推测出来，连接点越少，该点越相信自己
  
  
Catmull-Clark Subdivision (General Mesh)  对于一般情况，非三角形
  quad face 四边形面  Non-quad face 非四边形面
  Extraordinary vertex (奇异点)(degree != 4)  
  Each subdivision step:
    Add vertex in each face   每个面取中点
    Add midpoint on each edge  每条边取中点
    Connect all new vertices
    
  
  After one subdivision:
  
  How many extraordinary vertices?  4个 2个原来的，2个新增的     在非四边形内点一个点，与边相连，一定是奇异点 度=边数
  What are their degrees?          原来的仍然是5度，新的是3度
  How many non-quad faces?         0个非四边形面   经过一次细分，每个非四边形面有一个奇异点，每个非四边形消失了
  经过一次细分后，再做奇异点就不会增加了
  
  
FYI: Catmull-Clark Vertex Update Rules (Quad Mesh)  
  新的点更新
  Face point  v1 v2 v3 v4  中点f       f=(v1+v2+v3+v4)/4
  Edge point  v1 v2中点e  f1 f2 面的中点  e=(v1+v2+f1+f2)/4
  Vertex point  老的点更新
  v=(f 1 + f 2 + f 3 + f 4 +2(m 1 +m 2 +m 3 +m 4 )+4p)/16   m midpoint of edge   p old “vertex point”
  
  

Mesh Simplification

Goal: reduce number of mesh elements while maintaining the overall shape

Collapsing An Edge 边坍缩
 Suppose we simplify a mesh using edge collapsing    把一条边的两个点捏在一起，边就消失
 
 Quadric Error Metrics （⼆次误差度量）  
 How much geometric error is introduced by simpliﬁcation?
  Not a good idea to perform local averaging of vertices
  Quadric error: new vertex should minimize its sum of square distance (L2 distance) to previously related triangle planes!
  5个点-》3个点   新的点到原本几个面的距离平方和最小   由不知道这个点-》这个点与周围点的关系
  
Quadric Error of Edge Collapse
 How much does it cost to collapse an edge?
 Idea: compute edge midpoint, measure quadric error 
 
 Better idea: choose point that minimizes quadric error 对每条边进行坍缩，找到最小的二次误差
 More details: Garland & Heckbert 1997.   
 
 
 Simplification via Quadric Error
 
 Iteratively collapse edges Which edges? Assign score with quadric error metric*
 
 • approximate distance to surface as sum of distances to planes containing triangles
 
 • iteratively collapse edge with smallest score
 
 • greedy algorithm... great results!
 
 * (Garland & Heckbert 1997)
 
 数据结构 优先队列或堆    坍缩一条边后其他的边发生变化，他的二次误差需要重新计算
   瞬间取到最小值o(1),以最小代价动态更新其他值
   
 
 
Shadows  没有阴影感觉物体浮在空中，阴影用于展示物体接地
How to draw shadows using rasterization?
 Shadow mapping!                                                            
                       
Shadow mapping!  问题：走样    经典shadow mapping 
   只能处理点光源，特定阴影有明显边界，非0即1，要么在阴影里要么不在，这种阴影叫做硬阴影，阴影锐利，
    软阴影，边界模糊，光源有一定范围               
An Image-space Algorithm

  no knowledge of scene’s geometry during shadow computation  生成阴影时不需要知道场景信息
  must deal with aliasing artifacts

  Key idea:
   the points NOT in shadow must be seen both by the light and by the camera  
    一个点不在阴影里，则相机和光源都可以看到这个点               
    点在阴影里，光看不到         
   
    
Pass 1: Render from Light     从光源看向场景，记录任何点的深度
 Depth image from light source    
 
Pass 2A: Render from Eye      从相机看向场景，记录深度
 Standard image (with depth) from eye 
 
Pass 2B: Project to light     从相机看到的点投影到光源
 Project visible points in eye view back to light source 
 (Reprojected) depths match for light and eye. VISIBLE  黄色线，深度一致
 (Reprojected) depths from light and eye are not the same. BLOCKED!!  红色线，深度不一致
 
 shadow map 的问题 
   判浮点数的相等困难  引入误差精度 >=distance+误差精度
   一个像素可能覆盖很多点，存在偏差     
   map 分辨率   将shadow记录在map中，map的分辨率怎么确定，分辨率低引起走样问题  
   
Well known rendering technique  shadow map 虽然有问题，但是应用广泛
 - Basic shadowing technique for early animations (Toy Story, etc.) and in EVERY 3D video game
 
 
Problems with shadow maps
 Hard shadows (point lights only)
 Quality depends on shadow map resolution  (general problem with image-based techniques)  阴影质量，高，中，低，代表阴影分辨率
 Involves equality comparison of ﬂoating point depth values means issues of scale, bias, tolerance                    
 
 
Hard shadows vs. soft shadows
  软阴影 离物体根部越近阴影越硬，离物体越远阴影越软   
  阴影分为两种  umbra 本影，完全看不到光源          penumbra  半阴影 看到部分光源
  图片是日食   日全食 本影  日偏食 半影