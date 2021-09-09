
Shading 3 (Texture Mapping cont.) 纹理映射
Shading 3
 Barycentric coordinates  重心插值
 Texture queries 
 Applications of textures
 
 
 Interpolation Across Triangles: Barycentric Coordinates(重⼼坐标)
 
 Why do we want to interpolate?
 
 • Specify values at vertices
 
 • Obtain smoothly varying values across triangles   得到平滑的值
 
 What do we want to interpolate?  插值什么内容
 
 • Texture coordinates, colors, normal vectors, …
 
 How do we interpolate?
 
 • Barycentric coordinates
 
 
 重心坐标的含义
 A coordinate system for triangles  (q,w,e)表示重心坐标 A,B,C是三角形三个顶点
 (x,y)= qA+wB+eC  
 Inside the triangle if all three coordinates are non-negative
 三角形的任意一个点(x,y)都可以用三个顶点坐标的线性组合表示，其中q+w+e=1，q,w,e非负在三角形，存在负的可能在三角形平面外
 
 What’s the barycentric coordinate of A?
 A的重心坐标(1,0,0) 1A+0B+0C=A
 
 Geometric viewpoint — proportional areas  任意一个点的重心坐标通过面积比得到
 q= Aa/(Aa+Bb+Cc)  w=Bb/(Aa+Bb+Cc) e=Cc(Aa+Bb+Cc)   Aa是A点对面的三角形(不相邻)
 
 What’s the barycentric coordinate of the centroid? 三角形重心的重心坐标   重心，把三角形分成等面积的三份
 (q,w,e) = (1/3,1/3,1/3)     (x, y) = 1/3A + 1/3B + 1/3C
 
 Barycentric Coordinates: Formulas       公式
 q=-(x - x B )(y C - y B ) + (y - y B )(x C - x B )/-(x A - x B )(y C - y B ) + (y A - y B )(x C - x B )
 w= -(x - x C )(y A - y C ) + (y - y C )(x A - x C )/-(x B - x C )(y A - y C ) + (y B - y C )(x A - x C )
 e=1-q-w
 
 
 Using Barycentric Coordinates

 Linearly interpolate values at vertices
 
 V = qV A + w V B + e V C
 V A , V B , V C can be positions, texture coordinates, color, normal, depth, material attributes…
 
 However, barycentric coordinates are not invariant under projection!   投影下不能保证重心坐标不变
 三角形对物体的投影会发生变化，投影三角形形状变化，重心坐标改变
 
 深度 三角形投影到屏幕中去，投影的中的重心坐标应该是投影前的三维坐标计算  投影回去使用逆变换
     三维空间中的值最好在三维空间做插值，然后对应到二维
     
 
 Applying Textures
 
 Simple Texture Mapping: Diffuse Color
 for each rasterized screen sample (x,y):    (x,y)Usually a pixel’s center
  (u,v) = evaluate texture coordinate at (x,y)    (x,y)Using barycentric coordinates!
  texcolor = texture.sample(u,v);
  set sample’s color to texcolor;    Usually the diﬀuse albedo Kd
                                      (recall the Blinn-Phong reﬂectance model)
                                      
 Texture Magnification  纹理放大
 (What if the texture is too small?)         
 
 Texture Magnification - Easy Case   出现马赛克的效果
 
 Generally don’t want this — insufficient texture resolution A pixel on a texture — a texel (纹理元素、纹素)
 Nearest  取近似值
 Bilinear 双线性  达到模糊效果
 Bicubic  双三次                            
  
 Bilinear Interpolation  双线性插值
 Want to sample texture value f(x,y) at red point
 
 Black points indicate texture sample locations
 
 Take 4 nearest sample locations, with texture values as labeled.   取临近4个做插值
   And fractional offsets, (s,t) as shown   分数偏移量（s，t）如图所示
 
 Linear interpolation (1D)线性插值
 lerp(x, v 0 , v 1 ) = v 0 + x(v 1 - v 0 )  
 
 Two helper lerps（horizontal） 水平方向的插值
 
 u0=lerp(s,u00,u10)
 u1=lerp(s,u10,u11)
 
 Final vertical lerp, to get result:  竖直方向插值
 f(x, y) = lerp(t, u 0 , u 1 )
 
 Bicubic 取临近16个做插值     双线性插值眼角部分仍然可以看到锯齿状，三次的就没有了
 
 
 
 Texture Magnification (hard case)
 
 (What if the texture is too large?)  纹理大了怎么样，引起更严重的问题
 
 Point Sampling Textures — Problem   远处摩尔纹Moire，近处锯齿Jaggies
 
 
 Screen Pixel “Footprint” in Texture 
 近处像素覆盖纹理较小  
 远处像素覆盖纹理较大，此时用像素的中心去做采样，其他地方的纹理被中心的纹理替代了
 
 Will Supersampling Do Antialiasing? 超采样能做到抗锯齿吗
 Yes! But costly!
 
 
 Antialiasing — Supersampling？
 
 Will supersampling work?
 
 • Yes, high quality, but costly
 
 • When highly minified, many texels in pixel footprint
 
 • Signal frequency too large in a pixel
 
 • Need even higher sampling frequency
 
 Let’s understand this problem in another way
 
 • What if we don’t sample?
 
 • Just need to get the average value within a range!  获得范围内的平均值
 
 
 Point Query vs. (Avg.) Range Query   算法 点查询问题  范围查询问题
 范围内平均值查询 范围内最大值查询  范围内最小值查询
 
 
 Different Pixels -> Different-Sized Footprints
 
 
 Mipmap 贴图
 Allowing (fast, approx., square) range queries  允许做范围查询(快，近似，正方形)
 
 Mipmap (L. Williams 83)
 “Mip” comes from the Latin “multum in parvo", meaning a multitude in a small space  拉丁语 很多小的东西合到一块
  通过一张图生成一系列图  原始纹理level0，第一层纹理level1  每增加一层，分辨率缩小到一半
  
 Mipmap可以提前生成
 “Mip hierarchy”
 level = D 
 What is the storage overhead of a mipmap?  存储量增加了多少  1/3
 1.计算等比数列
 2.原始层复制三份，放置在左上，右上，左下   右下放置(第二层左上，第二层右上，第二层左下 右下角放置(无限循环))  比例 3/1
   存储量是比例，每层都乘以三
   
 Computing Mipmap Level D
 屏幕的点(x,y)在纹理的展开坐标(u,v)
 Estimate texture footprint using texture coordinates of neighboring screen samples
 使用相邻屏幕样本的纹理坐标估计纹理足迹
   
 D = log2 L  
 L = max(  ((du/dx)的平方+(dv/dx)的平方)的开方 ， ((du/dy)的平方+(dv/dy)的平方)的开方  )
 纹理与周围纹理的距离，取最大值，以该值围绕纹理的的正方形(做了一个近似值，4个纹理点围城的面积)
 如果L是 1乘1 那么在原始mipmap找对应的像素
 如果L是 4乘4 那么L在mipmap第二层会变为一个像素,就能立刻得到想要的像素
 
 D rounded to nearest integer level  会出现图层不连贯的现象，没法表示第1.8层，此时做第一层和第二层的插值
 近处对应的mipmap的底层，可以看到很多细节，远处对应mipmap的高层，一个像素覆盖的区域大，只有mipmap到了高层，一个像素近似这个区域
 近处纹理多，展开后相近与原始像素，远处展开纹理图后，展开内容更多，一个像素占据更多的内容
 
 mipmap层插值 三线性插值
  在Mipmap Level D插值做双线性插值，在Mipmap Level D+1做双线性插值
  对结果进行线性插值  Linear interpolation based on continuous D value
  
  
 Mipmap Limitations
 Overblur Why?  远处糊掉  过分的模糊
 
 Anisotropic Filtering 各向异性过滤，部分解决mipmap三线性插值出现的问题   总共的开销是原来的三倍
   Better than Mipmap! 
   计算多少层  压缩了几次，随着x增加，逐渐收敛到原始的三倍   只要显存足够，x增大不会影响算力
  
  
 Irregular Pixel Footprint in Texture 纹理中不规则的像素足迹
 像素空间的规则形状映射到纹理空间经常是不规则图形  对于长条形，Mipmap是正方形包住长方形，求了一个更大区域的平均
 
 
 Anisotropic Filtering   每次对原始图形的一个方向缩放,不同方向表现不同
 
 Ripmaps and summed area tables
 
 • Can look up axis-aligned rectangular zones  可以查找与轴对齐的矩形区域
 
 • Diagonal footprints still a problem  对于斜着的纹理，仍然存在长条区域，包裹一个斜条纹理的现象
 
 EWA filtering    不规则形状，拆成很多圆形取覆盖   性能消耗较大
   • Use multiple lookups 
     Weighted average
     Mipmap hierarchy still helps
     Can handle irregular footprints 