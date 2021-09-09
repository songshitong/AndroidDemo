Lecture 18:

Advanced Topics in Rendering



Today’s lecture

- Advanced (?) light transport and materials

- A lot, but extremely high-level. Mostly FYI.  讲思想



Advanced Light Transport
Unbiased light transport methods  无偏
 - Bidirectional path tracing (BDPT)
 - Metropolis light transport (MLT)

Biased light transport methods    有偏
  - Photon mapping   光子映射
  - Vertex connection and merging (VCM)
Instant radiosity (VPL / many light methods)  实时辐射度


Biased vs. Unbiased Monte Carlo Estimators

An unbiased Monte Carlo technique does not have any systematic error  无偏的蒙特卡洛

  The expected value of an unbiased estimator will always be the correct value,  期望都是对的
  no matter how many samples are used

Otherwise, biased  期望的值和最后结果不一样

  //特殊情况 一致的
  One special case, the expected value converges to the correct value as infinite #samples are used — consistent

We’ll look again at this page after introducing Photon Mapping



Bidirectional Path Tracing (BDPT)  双向路径追踪

Recall: a path connects the camera and the light BDPT

- Traces sub-paths from both the camera and the light  半路径

- Connects the end points from both sub-paths


Bidirectional Path Tracing (BDPT)

Suitable if the light transport is complex on the light’s side   光传播复杂  大部分光源都是环境光，光路复杂
Difficult to implement & quite slow  难度高，渲染慢




Metropolis Light Transport (MLT)

A Markov Chain Monte Carlo (MCMC) application  马尔科夫连
  - Jumping from the current sample to the next with some PDF  根据当前的样本生成靠近的下一个样本   统计学概念
Very good at locally exploring difficult light paths  局部路径
Key idea
  - Locally perturb an existing path to get a new path  根据一条路径，加入微小的扰动，生成新的



Metropolis Light Transport (MLT) — Pros
Works great with difficult light paths  复杂光路  光从半开的门传进来    游泳池底部的光斑SDS path
 Also unbiased



Metropolis Light Transport (MLT) — Cons

Difficult to estimate the convergence rate 分析收敛速度
 Does not guarantee equal convergence rate per pixel
 So, usually produces “dirty” results    都是做的局部，像素没有联系，结果看起来脏  墙缝脏
  Therefore, usually not used to render animations  不能渲染动画  动画的上一帧和下一帧抖得厉害




 Photon Mapping 光子映射 适合caustics场景  焦聚的情况
 A biased approach & A two-stage method
 Very good at handling Specular-Diffuse-Specular (SDS) paths and generating caustics


 Photon Mapping — Approach (variations apply) 其中一种

 Stage 1 — photon tracing  打到diffuse时光在停在那里
  - Emitting photons from the light source, bouncing them around, then recording photons on diffuse surfaces
 Stage 2 — photon collection (final gathering)
  - Shoot sub-paths from the camera, bouncing them around, until they hit diffuse surfaces

 Calculation — local density estimation 计算-局部密度估计

 - Idea: areas with more photons should be brighter  光子多的更亮

 - For each shading point, find the nearest N photons. Take the surface area they over 取N个光子的面积，然后计算密度
    nearest neibor问题  给定一个点，找周围最近的几个点



 Photon Mapping

 Why biased? 为什么是有偏的

 Local Density estimation dN / dA != ΔN / ΔA   计算公式只接近
 But in the sense of limit
  - More photons emitted ->  很多光子
  - the same N photons covers a smaller ΔA ->
  - ΔA is closer to dA So, biased but consistent!  有偏但一致的


 Photon Mapping

 An easier understanding bias in rendering

 - Biased == blurry  有偏 就是相比正确结果模糊

 - Consistent == not blurry with infinite #samples  样本足够多就收敛到不模糊的

 Why not do a “const range” search for density estimation?
   只取一个光子的固定区域的密度   结果是有偏的   ΔA不会缩小



 Vertex Connection and Merging  VCM

 A combination of BDPT and Photon Mapping Key idea

 - Let’s not waste the sub-paths in BDPT if their end points cannot be connected but can be merged

 - Use photon mapping to handle the merging of nearby “photons”