Perspective Projection
What’s near plane’s l, r, b, t then?

If explicitly speciﬁed, good   f,n

如何定义视锥 frustum
 从摄像机出发，看向某个区域，看到的就是近的平面，给近的平面定义一个宽度和高度，也可以定义一个宽高比 aspect ratio=width/height
 视角ﬁeld-of-view 看到的角度的范围 
  fovY 垂直可视角度

Sometimes people prefer:

vertical ﬁeld-of-view (fovY) and aspect ratio (assume symmetry i.e. l = -r, b = -t)

How to convert from fovY and aspect to l, r, b, t? 转换
tan(fovY/2)=t/|n|  n的绝对值
aspect = r/t

What’s after MVP?
Canonical cube to ? 规范立方体

Canonical Cube to Screen   [kəˈnɒnɪkl]

What is a screen?
 An array of pixels    一个二维数组，每个元素是像素
 Size of the array: resolution  数组的大小是分辨率
 A typical kind of raster display  光栅成像设备
 
 Raster == screen in German  在德语中代表屏幕
 Rasterize == drawing onto the screen 光栅化，把东西画在屏幕上
 Pixel (FYI, short for “picture element”)  图片元素的缩写
  For now: A pixel is a little square with uniform color 一个带有颜色的小方块，最小单位，实际比这个复杂
  Color is a mixture of (red, green, blue)
  
Deﬁning the screen space  定义屏幕的空间，在屏幕定义一个坐标系，左下角为原点，向左是x,向上是y
Slightly different from the “tiger book”  
Pixels’ indices are   in the form of (x, y), where   both x and y are integers
Pixels’ indices are from   (0, 0) to (width - 1, height - 1)
Pixel (x, y) is centered at  (x + 0.5, y + 0.5)  像素中心
The screen covers range   (0, 0) to (width, height)

将前面定义的(-1,1)立方体转换到屏幕空间
Irrelevant to z  先不管z
Transform in xy plane: [-1, 1] 2 to [0, width] x [0, height]  
Viewport transform matrix:  视口变换  先缩放，后平移
Mviewport = (width/2 0 0 width/2)
            (0  height/2 0 height/2)
            (0   0       1 0)
            (0   0       0 1)
            
Next: Rasterizing Triangles into Pixels  光栅化本质将物体的多边形打散到屏幕空间的像素，每个像素的值是什么，将多边形真正画到屏幕上

Drawing Machines
CNC Sharpie Drawing Machine  在物体上画画
Laser Cutters  激光切割
Different Raster Displays 屏幕
Oscilloscope  示波器
Oscilloscope Art  示波器作画
Cathode Ray Tube  阴极射线管 crt屏幕
Television - Raster Display CRT   Raster Scan  隔行扫描，图像扫描到屏幕上，只画一半就可以
  第一帧只画奇数行，下一帧只画偶数行，减少工作量
  隔行扫描可以用于视频压缩，视频一帧记录奇数行，下一帧只记录偶数行
    缺点，对于高速运动的物体，体育比赛等，会造成画面撕裂
Frame Buffer: Memory for a Raster Display  显卡缓存，显存
 DAC=Digital to Analog Convertors
Flat Panel Displays  平板显示设备
  Low-Res LCD Display 计算器，低分辨率lcd设备
  手机 高分辨率设备 lcd,oled..
  视网膜屏幕 屏幕分辨率高到超出人眼视网膜的分辨率
  
LCD (Liquid Crystal Display) Pixel 液晶显示器  利用液晶的原理控制一个像素显示什么
 液晶会通过自己不同的排布影响光的极化，也就是光的偏振方向
 Principle: block or transmit light by twisting polarization
 Illumination from backlight (e.g. fluorescent or LED)
 Intermediate intensity levels by partial twist 
 
LED Array Display   Light emitting diode array 发光二极管
Electrophoretic (Electronic Ink) Display 电子墨水屏
  通电控制黑色或白色在上还是在下，从而控制显色
  优点：看起来自然
  缺点：刷新率很低，颜色反转需要时间，屏幕翻页需要时间
  
  
  
Rasterization: Drawing to Raster Displays  
Polygon Meshes 多边形网格
Triangle Meshes 三角形网格

Triangles - Fundamental Shape Primitives  三角形在图形学中得到广泛应用
Why triangles? 为什么使用三角形
• Most basic polygon  最基础的多边形
• Break up other polygons 任何其他多边形都可以拆成三角形
Unique properties
• Guaranteed to be planar 给定三个点，一定是平面的
• Well-defined interior  三角形内外的定义是清晰的(凸多边形和凹多边形的内外定义麻烦)
• Well-defined method for interpolating values at vertices over triangle (barycentric interpolation)
  三角形上顶点插值的定义良好的方法（重心插值）
  给定三角形的三个点，里面任一点与三个点的关系都可以定义出来
  
What Pixel Values Approximate a Triangle? 判断像素的中心点与三角形的位置，来决定像素亮不亮(内部肯定量，三角形的边缘如何判断)
Input: position of triangle vertices projected on screen
Output: set of pixel values approximating triangle 

A Simple Approach: Sampling  采样
Sampling a Function
Evaluating a function at a point is sampling. 采样是 给定一个函数f(x)，她是连续的，在不同的地方问，这个函数的值是多少
We can discretize a function by sampling.

for (int x = 0; x < xmax; ++x) output[x] = f(x);

Sampling is a core idea in graphics.  采样在图形学是个核心概念
We sample time (1D), area (2D), direction (2D), volume (3D)  时间，空间，方向，表面反射

Rasterization As 2D Sampling
 Sample If Each Pixel Center Is Inside Triangle 采样像素的中心是否在三角形内部
 Define Binary Function: inside(tri, x, y)
 
 inside(t, x, y)={
  1 Point (x, y) in triangle t
  0 otherwise
 } 
 
 for (int x = 0; x < xmax; ++x)
  for (int y = 0; y < ymax; ++y) 
    image[x][y] = inside(tri, x + 0.5, y + 0.5);
    
 Inside? Recall: Three Cross Products!  叉乘
 
 Edge Cases (Literally) 点在变上的情况
 Is this sample point covered by triangle 1, triangle 2, or both?  两个三角形组成长方形，点落在对角线
   要么不做处理，要么特殊处理  本课不做处理
   opengl 中认为点落在上边和左边是在内部，右边和下边不再内部  
 
 加速方法  
 Checking All Pixels on the Screen?
   Use a Bounding Box! 三角形的包围盒即可  轴向包围盒AABB axis-aligned bounding box
     min(p1x,p2x,p3x) max(p1x,p2x,p3x)       
     min(p1y,p2y,p3y) max(p1y,p2y,p3y)
     
 Incremental Triangle Traversal (Faster?)  三角形每一行都找最左和最右，相当于每一行都有一个包围盒
   suitable for thin and rotated triangles  窄长和旋转的三角形
 其他方法还有。。。。
 
 实际设备的光栅化
 Rasterization on Real Displays   
   iphone 6s 一个像素分为红绿蓝三个竖条
   galaxy s5 bayer pattern  bayer排列 红绿蓝不是一个方块  绿色的点要多，人眼对绿色更敏感。有的相机也用了这个原理，绿色的感官元件要多一点，人眼看着更舒服
  Real LCD Screen Pixels (Closeup)
  Notice R,G,B pixel geometry! But in this class, we will assume a colored square full-color pixel
  
 Aside: What About Other Display Methods?
   Color print: observe half-tone pattern  彩色打印机  减色系统，加的颜色越多得到的效果越黑
   
 Assume Display Pixels Emit Square of Light
  LCD pixel on laptop
  LCD pixels do not actually emit light in a square of uniform color, but this approximation suffices for our current discussion      
  本课程认为像素是一个内部均匀的小方块
  
  So, If We Send the Display the Sampled Signal
   Jaggies 锯齿状  实际显示效果出现锯齿状走样
   Aliasing (Jaggies) 走样
  
  下一节  反走样，抗锯齿 