
https://juejin.cn/post/7082320749977042980
OpenCV 使用 C/C++ 开发，同时也提供了 Python、Java、MATLAB等其他语言的接口，并且提供 Windows、Android（Android库下载）、ios等平台的接口。

最底层是基于硬件加速层（HAL）的各种硬件优化。
再上一层是OpenCV核心层，包括core、imgproc、calib3d、objdetect等。
再上一层是opencv_contrib，可以理解为OpenCV的扩展模块，里面包含一些最新的算法，不过API尚未稳定或未充分测试。
接下来是语言绑定和示例应用程序。
处于最上层的是 OpenCV 和操作系统的交互。


OpenCV核心层中包含的模块：
core 该模块包含 OpenCV 库的基础结构以及基本操作。
improc 图像处理模块包含基本的图像转换，包括滤波以及类似的卷积操作。
highgui在 OpenCV 3.0中，分割为 imcodecs、videoio 以及 highgui 三部分。  这个模块包含可以用来显示图像或者简单的输入的用户交互函数。
   这可以看作是一个非常轻量级的 Windows UI 工具包。
video 该模块包含读取和写视频流的函数。
calib3d 这个模块包括校准单个、双目以及多个相机的算法实现。
feature2d 这个模块包含用于检测、描述以及匹配特征点的算法。
objdectect 这个模块包含检测特定目标，比如人脸或者行人的算法。也可以训练检测器并用来检测其他物体。
ml机器学习模块本身是一个非常完备的模块，包含大量的机器学习算法实现并且这些算法都能和 OpenCV 的数据类型自然交互。
flann  Flann 的意思是“快速最邻近库”。这个库包含一些你也许不会直接使用的方法，但是其他模块中的函数会调用它在数据集中进行最邻近搜索。
GPU 在 OpenCV 中被分割为多个 cuda* 模块。  GPU 模块主要是函数在 CUDA GPU 上的优化实现，此外，还有一些仅用于 GPU 的功 能。
   其中一些函数能够返回很好的结果，但是需要足够好的计算资源，如果硬件没有GPU，则不会有什么提升。
photo 这是一个相当新的模块，包含计算摄影学的一些函数工具。
stitching 本模块是一个精巧的图像拼接流程实现。这是库中的新功能，但是就像 Photo 模块一样，这个领域未来预计有很大的增长。
nonfree 在 OpenCV 3.0 中，被移到 opencv_contrib/xfeatures2d。  OpenCV 包含一些受到专利保护的或者受到使用限制的（比如 SIFT 算法）算法。
  这些算法被隔离到它们自己的模块中，以表明你需要做一些特殊的工作，才可以在商业产品中使用它们。
contrib 在 OpenCV 3.0 中，融合进了 opencv_contrib。  这个模块包含一些新的、还没有被集成进 OpenCV 库的东西。
legacy在 OpenCV 3.0 中，被取消。  这个模块包含一些老的尚未被完全取消的东西。
ocl在OpenCV 3.0 中，被取消，取而代之的是 T-API。  这是一个较新的模块，可以认为它和 GPU 模块相似，它实现了开放并行编程的 Khronos OpenCL 标准。
  虽然现在模块的特性比 GPU 模块少很多，但 ocl 模块的目标是提供可以运行在任何 GPU 或者是其他可以搭载 Khronos 的并行设备。
  这与 GPU 模 块形成了鲜明的对比，后者使用 Nividia CUDA 工具包进行开发，因此只能在 Nividia GPU 设备上工作。

javaApi  https://blog.csdn.net/weixin_43418331/article/details/121795193
OpenCV Java Api会经常用到这几个类：
Mat类，主要用来定义Mat对象，切割Mat对象。常规的Bitmap位图在OpenCV中都要转换为Mat
Core类，主要用于Mat的运算，提供了很多运算功能的静态函数
ImgProc类，主要用于图像处理，也提供了很多处理功能的静态函数。
Utils类，主要用于Mat和Bitmap之间的转换



图像在OpenCv的表示
https://juejin.cn/post/7083054545294589988
数字设备看到的是一个矩阵，该矩阵包含了所有像素点的值。最终在计算机世界里所有图像都可以简化为数值矩以及矩阵信息
OpenCV定义了Mat类作为基本图像容器，此外Mat还可以只单纯地表示一个矩阵。Mat由两个数据部分组成：矩阵头（包含矩阵尺寸，存储方法，存储地址等信息）
和一个指向存储所有像素值的矩阵（根据所选存储方法的不同矩阵可以是不同的维数）的指针。矩阵头的尺寸是常数值，但矩阵本身的尺寸会依图像的不同而不同。
例如，一个RGB的图片，其Mat对象的矩阵就是一个分别存储R、G、B通道值的三维矩阵。
在视觉算法中经常需要传递图片、拷贝图片等操作，每次都拷贝矩阵开销较大，因此OpenCV采用了引用计数机制，让每个Mat对象有自己的信息头，
但共享同一个矩阵，拷贝构造函数则只拷贝信息头和矩阵指针，而不拷贝矩阵。


Mat对象
```
/**
* @param rows行;对应bitmap的高
* @param clos列;对应bitmap的宽
* @param 颜色空间&数据类型CvType
* @param 矩阵的数据
**/
Mat(int rows, int cols, int type, void* data, size_t step=AUTO_STEP);

//bitmap创建Mat
void *pixels = 0;
AndroidBitmapInfo info;
// 获取bitmap的信息
AndroidBitmap_getInfo(env, bitmap, &info);
// 获取bitmap的像素值
AndroidBitmap_lockPixels(env, bitmap, &pixels);
cv::Mat rgbMat(info.height, info.width, CV_8UC4, pixels);
```
CvType含义
```
// CvType含义：[每个颜色所占位数][是否带符号][基本数据类型][每个颜色的通道数]
CV_[The number of bits per item][Signed or Unsigned][Type Prefix]C[The channel number]
// 例如CV_8UC4表示：每个颜色占8位，用unsigned char表示，每个颜色有4个通道(R、G、B、A)
cv::Mat rgbMat(info.height, info.width, CV_8UC4, pixels);
```



颜色空间
颜色空间是指对一个给定的颜色，如何组合颜色元素以对其编码。常见的有：
RGB：用红色Red、绿色Green和蓝色Blue作为基本色，是最常见的，这是因为人眼采用相似的工作机制，它也被显示设备所采用。
RGBA：在RGB的基础上加入了透明度Alpha。
YCrCb：在JPEG图像格式中广泛使用。
YUV：用于Android相机的颜色空间，"Y"表示明亮度（Luminance或Luma，也就是灰度值）；而"U"和"V"表示色度（Chrominance或Chroma），
  作用是描述影像色彩及饱和度，用于指定像素的颜色。


Bitmap转mat
```
1
Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.bg_color);
Mat mat = new Mat();
Utils.bitmapToMat(bitmap, mat);
2
Mat mat = Utils.loadResource(this,R.mipmap.bg_color);
```
mat转Bitmap
```
Bitmap newBitmap = Bitmap.createBitmap(mat.width(),mat.height(), 		Bitmap.Config.ARGB_8888);
Utils.matToBitmap(mat,newBitmap);
```
mat的数学计算
bitwise_not(Mat src,Mat dst)	非
bitwise_and(Mat src1,Mat src2,Mat dst)	与
bitwise_or(Mat src1,Mat src2,Mat dst)	或
bitwise_xor(Mat src1,Mat src2,Mat dst)	异或
add(Mat src1,Mat src2,Mat dst)	矩阵加法
subtract(Mat src1,Mat src2,Mat dst)	矩阵减法
multiplf(Mat src1,Mat src2,Mat dst)	矩阵乘法
divide(Mat src1,Mat src2,Mat dst)	矩阵除法

