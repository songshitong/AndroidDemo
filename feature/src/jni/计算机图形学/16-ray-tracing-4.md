Ray Tracing 4 

(Monte Carlo Path Tracing)  蒙特卡洛路径追踪



Monte Carlo Integration 蒙特卡洛积分  解决定积分    不定积分是啥

Why: we want to solve an integral, but it can be too difficult to solve analytically.
  我们想求解一个积分，但是很难解析地求解


What & How: estimate the integral of a function by averaging random samples of the function’s value
 黎曼积分  
  通过对函数值的随机样本求平均值来估计函数的积分   随机采样   estimate估计
  
  Let us define the Monte Carlo estimator for the definite integral of given function f(x)
  Random variable X i ⇠ p(x)  在值域随机取一个值  积分阈
  
  Example: Uniform Monte Carlo Estimator
   Uniform random variable 值域是一个常量  均匀采样
   
   
  Some notes:   
  • The more samples, the less variance.   样本越多，结果越准确
  • Sample on x, integrate on x. 
  
  
  
  Path Tracing 路径追踪   与光线追踪有什么不同呢？
  
  Motivation: Whitted-Style Ray Tracing
  
  Whitted-style ray tracing:
    Always perform specular reﬂections / refractions  光滑的反射
    Stop bouncing at diffuse surfaces  漫反射停止
  
  Are these simplifications reasonable? 这些真的对吗  合理吗
  
  High level: let’s progressively improve upon Whitted-Style Ray Tracing and lead to our path tracing algorithm!
  
  
  Whitted-Style Ray Tracing: Problem 1  问题一
  
  Where should the ray be reflected for glossy materials?
    Mirror reﬂection 镜面反射
    Glossy reﬂection 光泽反射  不那么光滑的反射  不那么光滑的不应改是镜面反射，whitted-style会认为
  
  Whitted-Style Ray Tracing: Problem 2   The Cornell box 康奈尔模型
    No reflections between diffuse materials?  漫反射和漫反射物体之间的光照弹射
    Path traced:
     direct illumination  天花板是黑的，左侧物体的左侧是黑的
    Path traced: 
     global illumination  天花板是亮的，这是实际的环境光  左侧物体的左侧是红的，结束来自红墙的光 
      color bleed就是全局光照的效果
      
      
  Whitted-Style Ray Tracing is Wrong
  But the rendering equation is correct   渲染方程是对的
  
  But it involves
   - Solving an integral over the hemisphere, and  解决半球积分
   - Recursive execution  递归定义 光一直在弹射
  How do you solve an integral numerically? 
  
  
  A Simple Monte Carlo Solution 解决第一个问题  使用蒙特卡洛解决
   场景 Suppose we want to render one pixel (point) in the following scene for direct illumination only
   
  Abuse the concept of Reflection Equation a little bit
  again, we assume all directions are pointing outwards 忽略发光，朝外
  
  Fancy as it is, it’s still just an integration over directions  只考虑直接光照
   So, of course we can solve it using Monte Carlo integration! 
   
  We want to compute the radiance at p towards the camera  计算p点的能量
  
  What’s our pdf?  1/2pi  球面是4pi 半球是2Pi 在半球立体角均匀采样然后积分就是2pi
  
  So, in general
   What does it mean?
   
   A correct shading algorithm for direct illumination! 对着色点的出射的能量 着色点四面八方光线对该点的贡献
   
   伪代码
   shade(p,wo)
    Randomly choose N directions wi~pdf  
    Lo = 0.0 
    For each wi
     Trace a ray r(p, wi) 在p沿着wi打一条光线
     If ray r hit the light
      Lo += (1 / N) * L_i * f_r * cosine / pdf(wi) 
    Return Lo
   
   
  Introducing Global Illumination 开始考虑间接光照
   One more step forward: what if a ray hits an object? 
   Q also reflects light to P! How much? The dir. illum. at Q!
   
  shade(p,wo)   加入了间接光照
      Randomly choose N directions wi~pdf  
      Lo = 0.0 
      For each wi
       Trace a ray r(p, wi) 在p沿着wi打一条光线
       If ray r hit the light
        Lo += (1 / N) * L_i * f_r * cosine / pdf(wi) 
       Else If ray r hit an object at q   光线在q点碰到物体
        lo += (1/n)*shade(q,-wi)*f_r*cosine / pdf(wi)  -wi入射方向的反向 作为p的入射方向   此时反射光线也可能是各个方向
      Return Lo
  
  Is it done? No.
  
  
  Path Tracing
  
  Problem 1: Explosion of #rays as #bounces go up: 光线弹射爆炸  指数爆炸
   Key observation: #rays will not explode iff N = ???????   指数N是多少不会爆炸 n=1
   在着色点只打出一条光线
   
  Path Tracing
  From now on, we always assume that only 1 ray is traced at each shading point: 
   shade(p,wo)   
         Randomly choose ONE directions wi~pdf   取一个方向
         Trace a ray r(p, wi) 在p沿着wi打一条光线
         If ray r hit the light
          return  L_i * f_r * cosine / pdf(wi)   1/n n=1 1/n=1
         Else If ray r hit an object at q   
          return shade(q,-wi)*f_r*cosine / pdf(wi) 
   此时算法仍然有问题 递归没有停止 shade(q,-wi)       
          
  This is path tracing! (FYI, Distributed Ray Tracing if N != 1)    n=1的蒙特卡洛积分就是路径追踪
  
  
  Ray Generation
  But this will be noisy! 噪点
  No problem, just trace more paths through each pixel and average their radiance!  一个像素接受更多的光线
  
  Very similar to ray casting in ray tracing 光线投射 类似蒙特卡洛方式的解决，用均匀采样解决
  
  ray_generation(camPos, pixel)
    Uniformly choose N sample positions within the pixel 
    pixel_radiance = 0.0 
    For each sample in the pixel
     Shoot a ray r(camPos, cam_to_sample)
     If ray r hit the scene at p
      pixel_radiance += 1 / N * shade(p, sample_to_cam) 
    Return pixel_radiance   
    
   问题1 解决了，使用路径追踪 但是递归没有
   Problem 2: The recursive algorithm will never stop!
    Dilemma: the light does not stop bouncing indeed! 
    Cutting #bounces == cutting energy!
    
   Solution: Russian Roulette (RR)（俄罗斯轮盘赌）解决方式  一定的概率停止追踪
     Russian Roulette is all about probability 
     With probability 0 < P < 1, you are fine 
     With probability 1 - P, otherwise 
     
   Example: two bullets,   Survival probability P = 4 / 6  生存概率  1-p的概率死亡
    6个格子，两颗子弹，4个空格子 
    
   Previously, we always shoot a ray at a shading point and get the shading result Lo
   
   Suppose we manually set a probability P (0 < P < 1) 
    With probability P, shoot a ray and return the shading result divided by P: Lo / P  一定的概率发射光线
    With probability 1-P, don’t shoot a ray and you’ll get 0  一定的概率不发射
   
   In this way, you can still expect to get Lo!: E = P * (Lo / P) + (1 - P) * 0 = Lo   期望仍为Lo 
   伪代码
   shade(p,wo)   
       Manually specify a probability P_RR
       Randomly select ksi in a uniform dist. in [0,1]  从0-1取随机数ksi
       If(ksi>P_RR) return 0.0;
       Randomly choose ONE directions wi~pdf   
       Trace a ray r(p, wi) 
       If ray r hit the light
         return  L_i * f_r * cosine / pdf(wi) / P_RR  结果/概率=期望
       Else If ray r hit an object at q   
         return shade(q,-wi)*f_r*cosine / pdf(wi) / P_RR
         
         
   Path Tracing
   Now we already have a correct version of path tracing! 
   But it’s not really efﬁcient.  不高效
     Low SPP (samples per pixel)  noisy results      一个像素打多少光线
     High SPP 光线多，结果正常
     
     
   Sampling the Light
   Understanding the reason of being inefficient  能否达到光源看运气，光源有大有小
   
   there will be 1 ray hitting the light. 
   So a lot of rays are “wasted” if we uniformly sample the hemisphere at the shading point. 
     均匀向四面八方采样，有大量的浪费
     
   蒙特卡洛的pdf 自己说了算   
   
   Sampling the Light (pure math) 优化：在光源采样
   Monte Carlo methods allows any sampling methods, so we can sample the light (therefore no rays are “wasted”)
   Assume uniformly sampling on the light:
   pdf = 1 / A (because ∫pdf dA = 1)
   But the rendering equation integrates on the solid angle: Lo = ∫Li fr cos dω.  渲染方程定义在立体角而不是光源面积
   Recall Monte Carlo Integration: Sample on x & integrate on x 
   Since we sample on the light, can we integrate on the light？ 将蒙特卡洛积分写成在光源积分就可
   
   
   Need to make the rendering equation as an integral of dA 
   Need the relationship between dω and dA 知道这两者的关系
   
   Easy! Recall the alternative def. of solid angle: 
     Projected area on the unit sphere
     光源转向着色点 dA cosA  此时dA的法线就是光线，da垂直于该方向 此时立体角=面积/距离
     
   Then we can rewrite the rendering equation as 此时渲染方程可重写
    对光源dA做积分
   Now an integration on the light! 
   Monte Carlo integration: “f(x)”: everything inside Pdf: 1 / A   光源的pdf
   
   
   Previously, we assume the light is “accidentally” shot by uniform hemisphere sampling
   Now we consider the radiance coming from two parts:
   1. light source (direct, no need to have RR) 这部分可以优化 不需要俄罗斯轮盘赌
   2. other reflectors (indirect, RR)
   
   伪代码
   shade(p, wo)
    # Contribution from the light source. 光源
    Uniformly sample the light at x’ (pdf_light = 1 / A) 
    L_dir = L_i * f_r * cos θ * cos θ’ / |x’ - p|^2 / pdf_light
    # Contribution from other reflectors. 反射
    L_indir = 0.0 
    Test Russian Roulette with probability P_RR 
    Uniformly sample the hemisphere toward wi (pdf_hemi = 1 / 2pi) 
    Trace a ray r(p, wi) 
    If ray r hit a non-emitting object at q
     L_indir = shade(q, -wi) * f_r * cos θ / pdf_hemi / P_RR
    Return L_dir + L_indir 
    
   
   One final thing: how do we know if the sample on the light is not blocked or not? 光源和物体之间是否被遮挡
    # Contribution from the light source.
    L_dir = 0.0 
    Uniformly sample the light at x’ (pdf_light = 1 / A) 
    Shoot a ray from p to x’ 
    If the ray is not blocked in the middle   查看光照是否碰到物体
     L_dir = …
   
   Now path tracing is ﬁnally done! 
   
   
   Some Side Notes
   
   • Path tracing (PT) is indeed difficult 路径追踪不容易
     - Consider it the most challenging in undergrad CS
     - Why: physics, probability, calculus, coding
     - Learning PT will help you understand deeper in these
   
   • Is it still “Introductory”? 是否能被称作入门
     - Not really, but it’s “modern" :) 属于现代化真正应用的知识
     - And so learning it will be rewarding also because … 路径追踪有用
     
   
   Is Path Tracing Correct? 路径追踪是正确的
   
   Yes, almost 100% correct, a.k.a. PHOTO-REALISTIC   照片级真实感
    左侧是照片，右侧是全局光照
    
   
   
   Ray tracing: Previous vs. Modern Concepts  对比
   
   • Previous - Ray tracing == Whitted-style ray tracing  以前ray tracing代指whitted-style ray tracing
   
   • Modern (my own definition) 现代光线传播的大集合
     - The general solution of light transport, including
     - (Unidirectional & bidirectional) path tracing
     - Photon mapping
     - Metropolis light transport
     - VCM / UPBP…   VCM
     
     
   Things we haven’t covered / won’t cover
   
   • Uniformly sampling the hemisphere  半球如何均匀采样
     - How? And in general, how to sample any function?
     (sampling) 作业提供了工具
   
   • Monte Carlo integration allows arbitrary pdfs 
      - What's the best choice? (importance sampling) 什么样的pdfs最好  重要采样理论 针对不同的形状使用不同的采样
   • Do random numbers matter? 随机数的质量  随机数的分布是否均匀  随机数是否扎堆
     - Yes! (low discrepancy sequences)   低差异化序列
     
     
   Things we haven’t covered / won’t cover
   
   • I can sample the hemisphere and the light 
      - Can I combine them? Yes! (multiple imp. sampling)  mis 不同的采样方法结合起来
   
   • The radiance of a pixel is the average of radiance on all paths passing through it 
     - Why? (pixel reconstruction filter)   像素重建滤波器 像素中心的光线权重是否要高一些？
   
   • Is the radiance of a pixel the color of a pixel?
     - No. (gamma correction, curves, color space) radiance转为颜色 需要 gamma校正  hdr  颜色空间
   
   • Asking again, is path tracing still “Introductory”? 仍然是入门级别
     - This time, yes. Fear the science, my friends.   敬畏科学 
       老师学rendering 10年，仍然感觉自己什么都不会  各种新技术突破
     