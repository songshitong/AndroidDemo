Materials and Appearances 材质和外观

What is Material in Computer Graphics?
Material == BRDF   渲染方程中brdf决定材质

Diffuse / Lambertian Material (BRDF)  漫反射
Light is equally reflected in each output direction  均匀的反射
 Suppose the incident lighting is uniform:  假设入射光的incident是均匀的
 能量守恒  假设物体不吸收光，进来多少就反射多少 此时brdf = 1/pi    
 反射率 albedo(color)  定义不同的颜色 漫反射的brdf  fr=p/pi
 
 
 Glossy material (BRDF)  抛光的材质
 
 
 Ideal reflective / refractive  material (BSDF * ) 玻璃或水的材质
  空气-水   
  空气-玻璃 颜色在玻璃球壳内，能量被吸收一部分
  
  
 Perfect Specular Reflection 镜面反射  
 w o + w i = 2*红色  红色=点积或投影
 方位角  方面绕着n在转
  ! o = (! i + ⇡)mod 2⇡ 入射角和反射角关系   立体角是反向的  2pi是循环
  
 Perfect Specular Reflection BRDF  镜面反射的brdf 不容易 这自己推到
 
 
 Specular Refraction 折射 
 In addition to reflecting off surface, light may be transmitted through surface.
 
 Light refracts when it enters a new medium. 
 海水 Caustics 翻译为焦散不好  castics永远因为聚焦而不是散射
   海水凹凸不平，网不同方向折射，海底一个点可能接受来自不同方向的折射光，类似放大镜把光聚到一起的
    对于渲染是灾难性的，path tracing很困难
    
 Snell’s Law 折射定律  斯涅耳定律
 
 Transmitted angle depends on index of refraction (IOR) for incident ray index of refraction (IOR) for exiting ray 
  折射率 真空1  水1.333 空气1.00029 玻璃1.5-1.6  钻石2.42 折射率高，折射出不同的波长，颜色
  
 Total internal reﬂection: 
 When light is moving from a more optically dense medium to a less optically dense medium   ni/nt>1 
  没有折射现象  入射介质的折射率大于折射介质的折射率 此时是全反射  入射空气 折射水
 Light incident on boundary from large enough angle will not exit medium.
  根号内部的东西小于0是没有意义的  
  
  Snell’s Window / Circle  从水底看 只能看到锥形的区域  大约97.2度
  球 球有对称性  出射点和入射点有对称性，可以保证折射光一定可以出来
   但是玻璃兔子有可能发生全反射，需要检验
   
  btdf t是折射   brdf r是反射  bsdf s是散射 包括btdf和brdf
  
  
  
  Fresnel Reflection / Term（菲涅⽿项）
  Reflectance depends on incident angle (and polarization of light) 
    This example: reflectance increases with grazing angle
    平视可以看到桌子对书的反射 和入射光有关系 入射光和法线的角度决定有多少能量被反射
    
    
  Fresnel Term (Dielectric,⌘=1.5)  只看红线unpolarized  polarization s和p代表极化现象，震动
    angle from normal 90 入射光和物体平行 完全被反射掉
     0  入射光和物体垂直 更多能量会被穿过去，反射能量会非常少
    公交车上，人看自己面前的玻璃可以看到窗外的物体，看别人的玻璃能看到车内的人和物
   
  Fresnel Term (Conductor)   导体  导体在任何角度 反射都很高 导体折射率为负       镜子-绝缘体
    镜子
    
  
  Fresnel Term — Formulae 菲涅⽿项计算方式
   Accurate: need to consider polarization  存在极化   跟入射光和法线夹角，入射介质，折射介质有关
   平常考虑不极化的情况 将极化的情况平均一下就好了  Reff=(Rs+Rp)/2
   
  Approximate: Schlick’s approximation 简化方法
  R(o)=R0+(1-R0)(1-coso)5次方  R0=(n1-n2/n1+n2)平方 
  
  
  Microfacet Material 微表面模型 微表面材质
   从远处看不到细节
   
   
  Microfacet Theory
  Rough surface  从远处看到的是材质，从近处看到的是几何
   • Macroscale: flat & rough  远处看是平的，粗糙的
   • Microscale: bumpy & specular  近处是凹凸不平，并且是镜面反射  组成远处的漫反射
  Individual elements of surface act like mirrors 每一个小面认为是微小的镜面
   • Known as Microfacets
   • Each microfacet has its own normal 
   
  Microfacet BRDF
   Key: the distribution of microfacets’ normals 研究微表面的法线
    Concentrated <==> glossy  微表面的法线沿着宏观表面的法线
    Spread <==> diffuse  微表面法线离中心分散
    
   What kind of microfacets reflect wi to wo? (hint: microfacets are mirrors)
    half vector 半程向量
    shadowingmasking term 几何项  微表面之间可能发生遮挡  自遮挡，自投影  光几乎是平着打到物体时 也叫grazing angle，掠射角度
    f(i,o)=F(i,h)G(i,o,h)D(h)/4(n,i)(n,o)
    Fresnel term 决定反射能量
    shadowingmasking 修正能量
    distribution of normals  法线分布是集中还是发散
    
   Autodesk Fusion 360 用这个软件渲染的摩托
   pbr
   
   
   Isotropic / Anisotropic Materials (BRDFs) 各向同性/各向异性
   Inside an elevator 电梯内部  金属从上到下被磨过了  形成从上到下的沟
    Key: directionality of underlying surface 方向性很弱
    Isotropic 各个方向分布均匀
    Anisotropic  电梯
    
   Anisotropic BRDFs 各向异性反映在BRDF上
   
   Reflection depends on azimuthal angle !  不满足在方位角上旋转
   
   f r (✓ i , " i ; ✓ r , " r ) = f r (✓ i , ✓ r , " r ! " i ) /
   
   Results from oriented microstructure of surface, e.g., brushed metal  
   
   
   Anisotropic BRDF: Brushed Metal 拉丝金属
    • How is the pan brushed? VRay renderer  商业渲染器
    
   Anisotropic BRDF: Nylon 尼龙  尼龙织在一起，更偏向与各向同性
   
   Anisotropic BRDF: Velvet 天鹅绒 通过擦床单，改变微表面性质
   
   
  Properties of BRDFs
   Non-negativity 永远是非负的  代表能量的分布
   Linearity  线性 可以分成很多块，然后加起来
   Reciprocity principle 可逆性
   Energy conservation 能量守恒  弹射会收敛，就是因为BRDF是能量守恒的
   Isotropic vs. anisotropic
     If isotropic 各项同性 只和相对的方位角有关  从四维表示为三维
     Then, from reciprocity, 根据可逆性  方位角不用考虑正负
     
     
  
  Measuring BRDFs 测量BRDF   实际测量的BRDF
  Avoid need to develop / derive models
     • Automatically includes all of the scattering effects present 
  Can accurately render with real-world materials  直接用测量数据，而不用物理模型计算
    • Useful for product design, special effects, ... 
  Theory vs. practice   理论对比实际
  
  
  Image-Based BRDF Measurement 基于想想
  Measuring BRDFs: gonioreflectometer 实际机器
    Spherical gantry at UCSD
    
  Measuring BRDFs
  
  General approach:
  foreach outgoing direction wo
    move light to illuminate surface with a thin beam from wo 
    for each incoming direction wi
       move sensor to be at direction wi from surface
       measure incident radiance 
       
       
  Improving efficiency:
  • Isotropic surfaces reduce dimensionality from 4D to 3D  各项同性可以降维
  • Reciprocity reduces # of measurements by half  可逆性 砍掉一半的测量
  • Clever optical systems    根据一部分猜出来剩下的
  
  
  Challenges in Measuring BRDFs  如何存储
  Accurate measurements at grazing angles 
  • Important due to Fresnel effects 
  Measuring with dense enough sampling to capture high frequency specularities 
  Retro-reflection 
  Spatially-varying reflectance, ...
  
  
  Representing Measured BRDFs
  
  Desirable qualities
  • Compact representation
  • Accurate representation of measured data
  • Efficient evaluation for arbitrary pairs of directions
  • Good distributions available for importance sampling
  
  
  BRDF的库 
  Tabular Representation
  
  Store regularly-spaced samples in
  
  (✓ i , ✓ o , |" i ! " o |)
  
  •
  
  Better: reparameterize angles to better match specularities
  
  Generally need to resample measured values to table
  
  Very high storage requirements
  
  
  MERL BRDF Database三菱电子实验室
   90 * 90 * 180 measurements 每个材质做的测量次数
