3D Transforms  三维变换
Use homogeneous coordinates again:
3D point = (x, y, z, 1) T
3D vector = (x, y, z, 0) T
Use In general,(x, y, z,w) (w  != 0) transformations is the 3D point:
 (x/w,y/w,z/w)

Use 4×4 matrices for afﬁne transformations  先线性变换后平移
(x0)=[a b c tx][x]
(y0) [d e f ty][y]
(z0) [g h i tz][z]
(1)  [0 0 0 1] [1]
What’s the order?
Linear Transform first or Translation first? 先线性变换后平移

3D Transformations
Scale
 S(sx,sy,sz)=(sx,0,0,0)
             (0 ,sy,0,0)
             (0,0,0,sz)
             (0,0,0,1)

Translation
 T(tx,ty,tz)=(1,0,0,tx)
             (0,1,0,ty)
             (0,0,1,tz)
             (0,0,0,1)

Rotation around x-,y- or z-axis
Rx(A)=(1 0 0 0)           x轴不变(一行一列都是1 0 0 0)
      (0 cosA -sinA 0)    y轴变
      (0 sinA cosA 0)     z轴变
      (0 0     0   1)

Ry(A)=(cosA 0 sinA 0)     二行二列都是0 1 0 0    Rz叉乘Rx=Ry  所以Ry sin的正负与Rx Rz不一样
      (0    1  0   0)
      (-sinA 0 cosA 0)
      (0    0  0    1)

Rz(A) =(cosA -sinA 0  0)   三行三列都是0 0 1 0
       (sinA cosA  0 0)
       (0     0    1 0)
       (0     0    0 1)

3D Rotations
Compose any 3D rotation from R x , R y , R z ?   把任意一个旋转分解为x,y,z方向的旋转
Rxyz(A,B,C)= Rx(A)Ry(B)Rz(C)  a,b,c为欧拉角

So-called Euler angles

Often used in ﬂight simulators: roll, pitch, yaw   飞机滚动，上下抛，摇晃

Rodrigues’ Rotation Formula  罗德里德斯旋转公式
Rotation by angle α around axis n   绕旋转轴n旋转α 角度
R(n, α) = cos(α)I + (1 - cos(α))nn T + sin(α)(0 -nz ny)
                                             (nz 0  -nx)  矩阵N
                                             (-ny nx 0) 
                                             
How to prove this magic formula? 如何证明
Check out the supplementary material on the course website 课后补充材料

四元数  旋转与选择之间的插值  两个角度的平均数 本课先不学

Viewing (观测) transformation  观测变换
View (视图) / Camera transformation 
Projection (投影) transformation

 - Orthographic (正交) projection

 - Perspective (透视) projection


View / Camera Transformation  视图、相机变换
What is view transformation?
Think about how to take a photo    mvp变换
  Find a good place and arrange people (model transformation)   模型变换，把场景搭好
  Find a good “angle” to put the camera (view transformation)   视图变换，找个好的角度放相机
  Cheese! (projection transformation)                           喊茄子拍照，投影变换
                                           
How to perform view transformation?
Deﬁne the camera ﬁrst
  Position ~e                                    位置
  Look-at / gaze direction gˆ                    往哪看,注视的方向
  Up direction (assuming perp. to look-at) t^    向上方向，相机自身的旋转角度
  
Key observation 
  If the camera and all objects move together, the “photo” will be the same   相对运动，相机和物体同时运动，拍出的照片是一样的
  
How about that we always transform the camera to 此时把相机放在原点，y为向上方向，朝-z方向看，让物体绕相机变换，简化操作
 The origin, up at Y, look at -Z                  
 And transform the objects along with the camera  

Transform the camera by Mview 
  So it’s located at the origin, up at Y, look at -Z
  
Mview in math?
 Translates e to origin 
 Rotates g to -Z 
 Rotates t to Y 
 Rotates (g x t) To X 
 Difﬁcult to write!  
 
lets write Mview = Rview Tview
 Translate e to origin 
 Tview=[1 0 0 -xe]
       [0 1 0 -ye]
       [0 0 1 -ze]
       [0 0 0 1]
 Rotate g to -Z, t to Y, (g x t) To X
 Consider its inverse rotation: X to (g x t), Y to t, Z to -g   原始的旋转不好求，考虑先求逆旋转，x是单位向量
 x 是1，0，0，0  旋转到 g x t   旋转矩阵是 x(gxt) y(gxt) z(gxt) 0
 Rview-1=[x(gxt) xt x-g 0]    Rview=[x(gxt) y(gxt) z(gxt) 0]  旋转矩阵是正交矩阵，他的逆矩阵=他的转置 Rview-1的逆矩阵就是原始角度的旋转=Rview-1的转置
         [y(gxt)  yt y-g 0]         [xt     yt     zt     0]
         [z(gxt)   zt z-g 0]        [x-g    y-g    z-g    0]
         [0       0   0   1]        [0      0       0     1]
         
Summary
Transform objects together with the camera      同时移动相机和物体，直到相机在原点   世界坐标系-》相机坐标系
Until camera’s at the origin, up at Y, look at -Z         

Also known as ModelView Transformation
But why do we need this?
For projection transformation!

Projection transformation

- Orthographic projection 正交投影

- Perspective projection  透视投影


Projection in Computer Graphics

3D to 2D 
Orthographic projection   平行线仍然平行，用于工程制图，最本质不会带给人近大远小的现象，假设相机无限远
Perspective projection    人眼更倾向于透视投影，平行线不再平行，物体边缘的延长线会相交，会有近大远小的现象，假设相机在不远处

Orthographic Projection
A simple way of understanding 简单的方法
 Camera located at origin, looking at -Z, up at Y (looks familiar?) 
 Drop Z coordinate    把z坐标扔掉,物体挤在x,y平面得到的图
 Translate and scale the resulting rectangle to [-1, 1]2  把结果移到(-1，1)(-1,1)的矩形上，方便计算 长度为2的矩形
 
In general 正规的方法
给定一个任意的立方体，变为[-1,1]3的立方体
We want to map a cuboid [l, r] x [b, t] x [f, n] to the “canonical (正则、规范、标准)” cube [-1, 1]3 
x定义left,right  y定义bottom,top    z,near,far  z越小离相机越近，z的近比远小，数值上
opengl 左手系的好处在z的近比远小，但会造成x叉乘y不同于z

Slightly diﬀerent orders (to the “simple way”) 与简单的方法的不同
Center cuboid by translating  平移长方体
Scale into “canonical” cube   缩放至立方体

Transformation matrix?
Translate (center to origin) ﬁrst, then scale (length/width/height to 2) 立方体长度2，原来长度r-l
Mortho=[2/(r-l) 0 0 0] [1 0 0 -(l+r)/2]
       [0 2/(t-b) 0 0]  [0 1 0 -(b+t)/2]
       [0 0 2/(n-f) 0]  [0 0 1 -(f+n)/2]
       [0 0 0 1]        [0 0 0 1]

Caveat 警告
Looking at / along -Z is making near and far not intuitive (n > f)  近大远小
FYI: that’s why OpenGL (a Graphics API) uses left hand coords. 


Perspective Projection
Most common in Computer Graphics, art, visual system
Further objects are smaller    近大远小
Parallel lines not parallel; converge to single point  平行线不再平行，会相交于一点 converge相交

todo 其次坐标的含义

Before we move on  
 Recall: property of homogeneous coordinates   点乘上一个数，在三维中仍然是这个点
 (x, y, z, 1), (kx, ky, kz, k != 0), (xz, yz, z 2 , z != 0) all represent the same point (x, y, z) in 3D 
   e.g. (1, 0, 0, 1) and (2, 0, 0, 2) both represent (1, 0, 0)
 Simple, but useful     
 
 
How to do perspective projection
 First “squish” the frustum into a cuboid (n -> n, f -> f) (Mpersp->Mortho)  把锥体压成长方体，与正交相比，远的平面要大些
    1 近平面的四个点不变，该平面的任何一个点不会变
    2 挤压后远平面的f不会变，z轴的长度
    3 远平面的中心点不会发生变化
 Do orthographic projection (Mortho, already known!)   做正交投影
 投影做的事情就是把物体上的线，从一头投影到另一头，透视的近大远小是线条在做收缩，可以理解为将远平面角的四个点挤到x,y平面
 
 挤压怎么做
 In order to ﬁnd a transformation
 Recall the key idea: Find the relationship between transformed points (x’, y’, z’) and the original points (x, y, z)
 寻找原点(x,y,z)与变换后的点(x0,y0,z0)的关系
 n是近面到原点的距离，z是远面到原点的距离
 相机位置与z轴，近点，原点形成相似三角形   y0=(n/z)y  x0=(n/z)x
 In homogeneous coordinates
  (x)=> (n/z*x)    ==(nx)    乘以z
  (y)   (n/z*y)      (ny)
  (z)   (unknown)    (unknown)
  (1)   (1)          (z)
  
  So the “squish” (persp to ortho) projection does this
  M4x4(persp->ortho)(x)=(nx)
                    (y) (ny)
                    (z) (unknown)
                    (1) (1)
  Already good enough to ﬁgure out part of Mpersp->ortho   结果避免了使用x,y,z，使用常量n,节省计算机资源，计算变简单了
  Mpersp->ortho = (n 0 0 0)
                  (0 n 0 0) 
                  (? ? ? ?) why?
                  (0 0 0 1) 
                  
  How to ﬁgure out the third row of Mpersp->ortho
  Any information that we can use?  
  Observation: the third row is responsible for z’  
  Any point on the near plane will not change   近面上点不会变化，近面上z=n
  Any point’s z on the far plane will not change  远处平面上z不会发生变化
  
  Any point on the near plane will not change
  M4x4(persp->ortho)(x) = (nx)      =>使用n替换z (x) => (x)==(nx)
                    (y)   (ny)                 (y)     (y)  (ny)
                    (z)   (unknown)             (n)    (n)  (n*n)
                    (1)   (1)                   (1)    (1)  (n)
                    
  So the third row must be of the form (0 0 A B)     矩阵第三行计算    
  (0 0 A B)(x) = n*n   n2 has nothing  to do with x and y  n2与x,y没有关系，所以A,B前面是0，0
           (y)          
           (n)
           (1)
  0*x+0*y+An+B=n*n
  
  What do we have now?
  An + B = n 2
  
  
  Any point’s z on the far plane will not change
  f 是远平面上的一个点
  当f是远平面上的中心点
  (0)=>(0) == (0)
  (0)  (0)    (0)
  (f)  (f)    (ff)
  (1)  (1)    (f) 
  Af + B = f*f
  
  
  Solve for A and B
  
  An + B = n 2
  Af + B = f 2  => A=n+f B = -nf
  
  Mpersp->ortho = (n 0 0 0)
                  (0 n 0 0) 
                  (0 0 n+f -nf) 
                  (0 0 0 1) 
  
  
  Finally, every entry in Mpersp->ortho  is known!
  What’s next?
  - Do orthographic projection (Mortho ) to ﬁnish
  - Mpersp = Mortho Mpersp->ortho
  
  
  作业 近面和远面的点，在挤压过程中怎么变 变大，变小，不变？
  个人答案，z变小 取n和f的中点
           
           