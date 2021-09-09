///需要信号处理 todo 信号处理学习 信号与系统
Antialiasing and Z-Buffering 反走样和深度缓冲

point-in-triangle test  像素中心对三角形可见性函数的采样

today
Antialiasing 
 sampling theory
 Antialiasing in practise
Visibility/occlusion
  Z-buffering
  
   
Recap: Testing in/out △ at pixels’ centers
Pixels are uniformly-colored squares
Compare: The Continuous Triangle Function
What’s Wrong With This Picture?
Jaggies!


Sampling is Ubiquitous in Computer Graphics
Rasterization = Sample 2D Positions
Photograph = Sample Image Sensor Plane 发生在不同的位置
Video = Sample Time    发生在不同的时间

Sampling Artifacts (Errors / Mistakes / Inaccuracies) in Computer Graphics 采样是广泛存在的，同样，采样的问题是广泛存在的

Artifacts 遗迹，伪影，瑕疵 特指Errors / Mistakes / Inaccuracies [ɪˈnækjəˌræsiz]不准确

常见的走样
Jaggies (Staircase Pattern)  楼梯样式
Moiré Patterns in Imaging    摩尔纹   Skip odd rows and columns

Wagon Wheel Illusion (False Motion)  车轮错觉（假动作）人眼在时间采样出现问题，采样跟不上运动的速度


Sampling Artifacts in Computer Graphics
Artifacts due to sampling - “Aliasing”
• Jaggies – sampling in space
• Moire – undersampling images
• Wagon wheel effect – sampling in time
• [Many more] …

artifacts 出现的本质，原因
Behind the Aliasing Artifacts 
• Signals are changing too fast (high frequency), but sampled too slowly

反走样的观点  采样之前做模糊或滤过
Antialiasing Idea: Blurring (Pre-Filtering) Before Sampling

Rasterization: Point Sampling in Space
Note jaggies in rasterized triangle where pixel values are pure red or white

 
Rasterization: Antialiased Sampling  先给三角形边缘做模糊或滤波，然后采样，结果就没有锯齿了
Pre-Filter (remove frequencies above Nyquist) 去除奈奎斯特以上的频率
  Nyquist 为防止信号混叠需要定义最小采样频率，称为奈奎斯特频率
Note antialiased edges in rasterized triangle where pixel values take intermediate values

Blurred Aliasing 
  Sample then filter, WRONG! 先采样然后模糊，不对  最后还是走样的
But why?
1. Why undersampling introduces aliasing?
2. Why pre-filtering then sampling can do antialiasing?
Let’s dig into fundamental reasons And look at how to implement antialiased rasterization

Frequency Domain 频域
Sines and Cosines 正弦，余弦

Frequencies cos 2pi f x   
  f 是频率  f=1 cos(2pi x)   f=2 cos(4pi x)
  f=1/T  t是周期,每个多长时间重复自己
Fourier Transform 傅里叶变换
  表示能将满足一定条件的某个函数表示成三角函数（正弦和/或余弦函数）或者它们的积分的线性组合
  用傅里叶变换描述其他的周期性函数，信号等，通过不断的级数展开，来拟合其他函数，项越多，越精确？
Represent a function as a weighted sum of sines and cosines
f(x)=A/2+2Acos(tw)/pi-2Acos(3tw)/3pi+2Acos(5tw)/5pi-2Acos(7tw)/7pi+...
  将频率从低到高分解  costw是低一点的频率+高一点cos3tw频率 A/2最高频
    对比每个小频率图和最后的组合图，可发现关系，频率高代表平滑曲线，频率低代表波动快的曲线  波动最快=直线？？

Fourier Transform Decomposes A Signal Into Frequencies
傅里叶级数展开与傅里叶变换紧密相连
将时域(空间域)变为频域

spatial domain  ==》Fourier transform ==》 frequency domain
               《== Inverse transform 《== 逆傅里叶变换
               
Higher Frequencies Need Faster Sampling  更高的频率需要更快的采样
Low-frequency signal: sampled adequately for reasonable reconstruction
  低频信号：充分采样，合理重构
High-frequency signal is insufﬁciently sampled: reconstruction incorrectly appears to be from a low frequency signal
  高频信号采样不足：重建错误地显示为来自低频信号
  
Undersampling Creates Frequency Aliases  欠采样会导致频率走样

High-frequency signal is insufficiently sampled: samples erroneously appear to be from a low-frequency signal
Two frequencies that are indistinguishable at a given sampling rate are called “aliases” 
走样的正规定义    
高频信号采样不足：样本错误地显示为来自低频信号
在给定的采样率下无法区分的两个频率称为“走样”   同样的采样无法区分低频和高频  高频的信息丢失了

Filtering = Getting rid of certain frequency contents
滤波     去掉某些频率内容

Visualizing Image Frequency Content  将图片经过傅里叶变换变为频域信息
  低频集中在原点，白色   高频在外部
  频谱横竖两条竖线，将不周期性重复的信号，认为重复性，图形在水平或数值方向上放了好多个

Filter Out Low Frequencies Only (Edges)  将低频信号过滤，留下高频的边缘信号  高频代表物体的边界  
  High-pass ﬁlter 高通滤波-》只通过高频
  边界：发生突变，变化很多-》高频
Filter Out High Frequencies (Blur)
  Low-pass ﬁlter 低通滤波器  只留低频信息-》变化缓慢-》模糊
  
Filter Out Low and High Frequencies  
  不是很明显的边界特征 去掉高频和低频
  圆圈扩大，保留更多高频，边界特征越明显，其他特征越不明显

Filtering = Convolution 卷积 (= Averaging 平均) 


Convolution=加权平均,为了取得附近的特征           
Point-wise local averaging in a “sliding window” “滑动窗口”中的逐点局部平均
窗口1   1   3    5  3 7 1 3 8 6 4
filter 1/4 1/2 1/4  向右移动filter
result     3    4
3=1*1/4+3*1/2+5*1/4
4=3*1/4+5*1/2+3*1/4

Convolution Theorem 卷积定理
Convolution in the spatial domain is equal to multiplication in the frequency domain, and vice versa
空间域中的卷积等于频域中的乘法，反之亦然  时域的乘积等于频域的卷积

Option 1:
• Filter by convolution in the spatial domain    空间域卷积滤波
 Option 2:
• Transform to frequency domain (Fourier transform)    变换到频域（傅里叶变换
• Multiply by Fourier transform of convolution kernel  卷积核的乘傅里叶变换
• Transform back to spatial domain (inverse Fourier)   转换回空间域

Box Filter 卷积核，滤波器 3*3的盒子
1/9*3*3的盒子   1/9是为了减少对结果影响

Box Function = “Low Pass” Filter  低通滤波器
Wider Filter Kernel = Lower Frequencies  宽滤盒=低频域

从频率方面看采样
Sampling = Repeating Frequency Contents  重复频率内容
原函数*冲击函数=采样结果
频谱上
原函数和冲击函数的卷积=采样结果  相当于重复原始函数频谱


Aliasing = Mixed Frequency Contents    混合频率内容  混叠
Dense sampling:   密集采样
Sparse sampling:  稀疏采样


Antialiasing

How Can We Reduce Aliasing Error?

Option 1: Increase sampling rate   增加采样率
• Essentially increasing the distance between replicas in the Fourier domain  实质上增加了Fourier域中副本之间的距离
• Higher resolution displays, sensors, framebuffers…   更高分辨率的显示器、传感器、帧缓冲区
• But: costly & may need very high resolution     成本高昂，可能需要非常高的分辨率   屏幕分辩高意味着采样率高，像素间隔小，频谱的堆叠距离大，不容易发生走样

Option 2: Antialiasing   反走样
• Making Fourier contents “narrower” before repeating   在重复之前使内容“更窄”
• i.e. Filtering out high frequencies before sampling   采样前过滤掉高频
  Antialiasing = Limiting, then repeating
    Filtering  Filter to avoid Aliasing When reducing Fs   对频谱过滤
    Then sparse sampling         采样后，不发生频谱的堆叠

实际操作    
A Practical Pre-Filter    一种实用的预滤波器
A 1 pixel-width box filter (low pass, blurring)    一个像素宽的低通滤波器

todo 高斯模糊
模糊 这种做法？？
 三角形边缘像素取周围9个的平均值，边缘发生模糊，颜色发生扩散

反走样的基本方法
Antialiasing By Averaging Values in Pixel Area
  Solution:
  • Convolve f(x,y) by a 1-pixel box-blur
    - Recall: convolving = filtering = averaging
  • Then sample at every pixel’s center  
  
  
Antialiasing by Computing Average Pixel Value
In rasterizing one triangle, 
the average value inside a pixel area of f(x,y) = inside(triangle,x,y) is equal to the area of the pixel covered by the triangle.  
 经过一个像素宽度的卷积，三角形边缘经过的像素点颜色均匀了
 
 
像素卷积比较困难，计算量大，有了下面的近似方法 
Antialiasing By Supersampling (MSAA)  超采样抗锯齿  ssaa  muliti sample anti aliasing 更多的采样点来解决走样，他是反走样的近似，严格来说没有解决反走样

Supersampling
Approximate the effect of the 1-pixel box filter by sampling multiple locations within a pixel and averaging their values:
通过对一个像素内的多个位置进行采样并取其值的平均值来近似1像素长方体滤波器的效果
 4x4 supersampling  2x2 supersampling
 
Point Sampling: One Sample Per Pixel
Supersampling: Step 1
 Take NxN samples in each pixel.  每个像素分为N*N
Supersampling: Step 2
 Average the NxN samples “inside” each pixel.  判断每个采样点中心是否在三角形内，取平均值 3/4,25%
Supersampling: Result
  采样，像素都一个颜色，像素中就是过三角形的，直接是采样结果
This is the corresponding signal emitted by the display  这是显示器发出的相应信号


msaa的缺点
Antialiasing Today
No free lunch! 
• What’s the cost of MSAA? 
  计算量增加 N*N的计算量
  工业界的做法，采样点不再像素中心，采样点被临近的像素复用
其他采样方式  
Milestones (personal idea)  
• FXAA (Fast Approximate AA)  快速近似抗锯齿  前期图形处理，找到锯齿边界，把锯齿去掉
• TAA (Temporal AA)  时间抗锯齿 复用上一帧的结果

超分辨率的含义
Super resolution / super sampling  超分辨率/超采样
• From low resolution to high resolution   从低分辨率到高分辨率
• Essentially still “not enough samples” problem    基本上仍然存在“样本不足”问题
• DLSS (Deep Learning Super Sampling)   深度学习超级采样  猜测分辨率不足的地方
