Why Transformation?

Viewing: (3D to 2D) projection  从三维世界(人眼)到二维世界(屏幕)的投影

scale
 x1=sx  y1=sy   s=scale num
 [x1]=[s 0][x]   s0为缩放矩阵
 [y1] [0 s][y]

 Scale (Non-Uniform) 两个s可以不等，

Reﬂection Matrix 反射操作,对称操作
  x1=-x  y1=-y
  [x1] = [-1 0][x]
  [y1]   [0 -1][y]

Shear Matrix  切变
 Hints:
 Horizontal shift is 0 at y=0
 Horizontal shift is a at y=1
 Vertical shift is always 0

 [x1]=[1 a][x]
 [y1] [0 1][y]


 Rotate(about the origin (0, 0), CCW by default) 旋转 默认原点0，0 逆时针
 [cosA -sinA]
 [sinA cosA]

 变化矩阵是对比变化前后的坐标得来的,取几个特殊点
 旋转前(1,0) 旋转后(cosA,sinA)
 [cosA]=[a b][1]
 [sinA] [c d][0]
 cosA=a*1+b*0=a
 sinA=c*1+d*0=c

 (0,1)->(-sinA,cosA)

 旋转-A角度
 R(-A)=[cosA sinA] =R(A)T 旋转A的转置
       [-sinA cosA]

 R(-A)=R(A)T=R(A)-1 (by difinition) R(A)是正交矩阵 矩阵的转置和逆矩阵相等
 旋转矩阵是正交矩阵
 求一个旋转的逆（往相反的方向旋转相同的角度）
   写出一个正向的旋转，求他的转置=他的逆矩阵
 旋转不好求，求方向角的矩阵，然后转置  



线性变化共同点 一个矩阵乘以另一个矩阵
Linear Transforms = Matrices(of the same dimension)
x0 = a x + b y
y0 = c x + d y

[x0]=[a b][x]
[y0] [c d][y]

x 0 = M x

Homogeneous coordinates 齐次坐标
Translation
x 0 = x + t x
y 0 = y + t y
此时不能写为矩阵相乘
只能写为
[x0]=[a b][x]+[tx]
[y0] [c d][y] [ty]
abcd 为单位矩阵
[1 0]
[0 1]
(So, translation is NOT linear transform!)
But we don’t want translation to be a special case
Is there a uniﬁed way to represent all transformations?  (and what’s the cost?)

trade off 取舍 no free launch 没有免费的午餐，只能权衡一方面

Solution: Homogenous Coordinates
Add a third coordinate (w-coordinate)  增加一个维度
2D point = (x, y, 1) T         todo 为什么点增加1
2D vector = (x, y, 0) T       向量平移不变性，平移后方向不变，所以是0

Matrix representation of translations
(x0)=(1 0 tx)(x) =(x+tx)
(y0) (0 1 ty)(y)  (y+ty)
(z0) (0 0 1) (1)  (1)

Valid operation if w-coordinate of result is 1 or 0  增加0或1的意义
• vector + vector = vector
• point – point = vector  (x1-x2) (y1-y2)
• point + vector = point  点延一个向量平移，仍是一个点
• point + point = ?? 代表两个点的中点  (验证，两个点相加 w=2，w变为1 (x1/w+x2/w)/2)
(x) is the 2d point (x/w),w != 0     变化就是x,y,w都除以w
(y)                 (y/w)
(w)                 (1)


Afﬁne Transformations  仿射变换
Afﬁne map = linear map + translation  先线性变换后平移
(x0)=(a b)(x)+(tx)
(y0) (c d)(y) (ty)

Using homogenous coordinates:  等价的先线性变换后平移
(x0)=(a b tx)(x)
(y0) (c d ty)(y)
(1)  (0 0 1) (1)

2D Transformations
Scale
  S(sx,sy)=(sx 0 0)
           (0  sy 0)
           (0  0  1)
Rotation
  R(A) = (cosA -sinA 0)
         (sinA cosA 0)
         (0     0   1)

Translation
  T(tx,ty)=(1 0 tx)
           (0 1 ty)
           (0 0 1)


Inverse Transform  逆变换，矩阵中代表逆矩阵
 a经过变换为b,b返回a的变换为逆变换


Composing Transforms 组合变换
1复杂的变换可以通过简单的变换得到
2变换的顺序是非常重要的 （矩阵乘的顺序不一样，结果也不一样）

Note that matrices are applied right to left:  矩阵的顺序是从右往左
对一个点先旋转45度，然后平移1的写法
T(1,0)R(45)[x] = [1 0 1][cos45 -sin45 0][x]=[cos45 -sin45 1][x] 根据其次坐标矩阵的特性(先线性后平移)，可以整合为一个矩阵
           [y]   [0 1 0][sin45  cos45 0][y] [sin45 cos45  0][y]
           [1]   [0 0 1][0       0    1][1] [0     0      1][1]

推广 存在一系列的变换
Sequence of afﬁne transforms A 1 , A 2 , A 3 , ...
  Compose by matrix multiplication
  Very important for performance!
  An (. . . A2 (A1 (x))) = An · · · A2 · A1 ·[x]
                                             [y]
                                             [1]
   Pre-multiply n matrices to obtain a single matrix representing combined transform
   根据矩阵的结合律可以先把变换矩阵相乘得到一个简单的矩阵，仍然是一个3乘3的矩阵，把变换统一了，很厉害

Decomposing Complex Transforms 分解
围绕图片的左下角C旋转A, c可能不在原点
Matrix representation? 从右往左，平移到原点(-c)，旋转A，平移到初始位置(c)
T(c) · R(A) · T(-c)

