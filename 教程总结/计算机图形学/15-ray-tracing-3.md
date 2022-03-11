Ray Tracing 3 

(Light Transport & Global Illumination)  光线传播和全局光照


Radiometry
Irradiance
  Definition: The irradiance is the power per unit area incident on a surface point. 
   单位面积接受的能量
  单位[w/m 2] [lm/m 2 = lux]
  
  
  Why Do We Have Seasons? 季节
   
   
  Radiance
  
  Radiance is the fundamental field quantity that describes the distribution of light in an environment
    光传播
  
  • Radiance is the quantity associated with a ray
  
  • Rendering is all about computing radiance  光路追踪
  
  Radiance
  
  Definition: The radiance (luminance) is the power emitted, reflected, transmitted or received by a surface, per unit solid angle, per projected unit area.
    单位立体角，单位面积上有多少能量 
    单位 [w/sr m 2] [cd/m 2 = lm/sr m 2 = nit]
    
  进一步的理解
  Radiance
  
  Definition: power per unit solid angle per projected unit area.
  
  Recall
  
  • Irradiance: power per projected unit area
  
  • Intensity: power per solid angle So
  
  • Radiance: Irradiance per solid angle
  
  • Radiance: Intensity per projected unit area  
  
  
  Incident Radiance
  
  Incident radiance is the irradiance per unit solid angle arriving at the surface. 面积接受到的
   i.e. it is the light arriving at the surface along a given ray (point on surface and incident direction).
   
  
  Exiting Radiance
  
  Exiting surface radiance is the intensity per unit projected area leaving the surface. 从面积离开的
   e.g. for an area light it is the light emitted along a given ray (point on surface and exit direction).
   
   
  Irradiance vs. Radiance
   
  Irradiance: total power received by area dA    dA收到的能量
  Radiance: power received by area dA from “direction” dω   dA从方向dw接受的能量
  Unit Hemisphere:H 2
  
  
  
  Bidirectional Reflectance Distribution Function (BRDF) 双向反射分布函数
  
  Reflection at a Point      
   Radiance from direction ω i turns into the power E that dA receives
   Then power E will become the radiance to any other direction ω o
   反射可以看成把光弹走，也可以看成物体吸收了一部分，剩下的发出去，后面的更符合brdf 
   brdf 定义了向某个方向反射的能量，体现了不同的材质 镜面，布料的漫反射
   
  
  BRDF
  
  The Bidirectional Reflectance Distribution Function (BRDF) represents how much light is 
   reflected into each outgoing direction dL r (! r ) from each incoming direction 
   
   
  Challenge: Recursive Equation   递归定义
  Reflected radiance depends on incoming radiance  入射的光可能来自光源也可能来自物体的反射光
   But incoming radiance depends on reflected radiance (at another point in the scene) 
   
   
  The Rendering Equation 渲染方程
  
  Re-write the reflection equation: 
   by adding an Emission term to make it general! 忽略物体会自己发光
   
  The Rendering Equation 将自己发光加上
  How to solve? Next lecture!  解方程
  Note: now, we assume that all  directions are pointing outwards!  假设所有方向朝外
  
  
  Understanding the rendering equation
  Reﬂection Equation
  
  Sum over all light sources 多个点光源 相加即可
  Replace sum with integral  面光源 积分面光源的立体角   面即是多个点光源组成的 面光源是点光源的大集合
  Surfaces (interreflection) Reflected Light UNKNOWN  来自其他物体的反射光不知道
  
  Rendering Equation (Kajiya 86) kajiya论文定义的渲染方程
  
  
  
  Rendering Equation as Integral Equation    Integral 不可缺少的
   BRDF 定义材质
  Is a Fredholm Integral Equation of second kind [extensively studied numerically] with canonical form
   数学上的简化方式
   
  Linear Operator Equation 更进一步的简写  算子
  L = E + KL
  Can be discretized to a simple matrix equation [or system of simultaneous linear equations] 
   (L, E are vectors, K is the light transport matrix)  
   用来解渲染方程
   
  Ray Tracing and extensions  解的过程
   General class numerical Monte Carlo methods
   Approximate set of all paths of light in scene 
   
  
  Ray Tracing   Shading in Rasterization 光栅化一般只有发光和直接光照，光线追踪是所有的
  L = E + KE + K 2 E + K 3 E +...   类似泰勒展开的形式   结果是全局光照
  E 发光   KE 直接光照  k方E 反射  k3方E 两次放射
   
  Four-bounce global illumination  上方的灯 亮了    光线进入物体内部然后出来，玻璃是双层的，经过两次弹射进入，经过两次弹射出来就发光了
  经过无限次弹射，结果会进行收敛到某一亮度  能量守恒的道理
   相机一直快门就会过爆，能量一直累积
   
   
  Probability Review 概率复习  
  Random Variables
  
  X
  
  X ⇠ p(x)
  
  random variable. Represents a distribution of potential values
  
  probability density function (PDF). Describes relative probability of a random process choosing value
  
  X ⇠ p(x)
  
  Example: uniform PDF: all values over a domain are equally likely
  
  e.g. A six-sided die  骰子
  
  X takes on values 1, 2, 3, 4, 5, 6
  
  p(1) = p(2) = p(3) = p(4) = p(5) = p(6)  每一个面都是1/6
  
  
  Expected Value of a Random Variable  期望值
  
  The average value that one obtains if repeatedly drawing samples from the random distribution.
  Die example:
   E[X] = (1 + 2 + 3 + 4 + 5 + 6)/6 = 3.5
   
   
  Continuous Case: Probability Distribution Function (PDF)  连续情况下的分布  概率密度函数
   A random variable X that can take any of a continuous set of values, 
   where the relative probability of a particular value is given by a continuous probability density function p(x). 
   Conditions on p(x): 所有概率相加为1
  
  Function of a Random Variable   x同时也是个函数的结果
  
  A function Y of a random variable X is also a random variable: 