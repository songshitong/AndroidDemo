
opencv 默认图片格式为BGRA


https://blog.csdn.net/weixin_43418331/article/details/121795193
什么是通道数？

通常表示每个点能存放多少个数，如RGB彩色图中的每个像素点有三个值，即三通道。常见的通道数有1、3、4，分别对应单通道、三通道、四通道，其中四通道中会有透明通道数据。

单通道——灰度图

三通道——RGB彩色图

四通道——到Alpha通道的RGB图

图片的深度表示每个值有多少位来存储，是一个精度问题，一般图片是8Bit（位），则深度是8


什么是类型

图片深度与类型密切相关。U表示无符号整数、S表示符号整数、F表示浮点数。

OpenCV中使用bitmaptToMat()加载的图片，图片类型为CV_8UC4，通道顺序为BGRA。其中CV表示计算机视觉、8表示深度、U表示无符号整数、4表示通道数。
这就解释了为什么Mat加载图片后颜色空间默认为BGR了！！
#define 	CV_8UC4   CV_MAKETYPE(CV_8U,4)     C4的c应该是channel
常见类型
单通道        双通道          三通道         四通道
CV_8UC1      CV_8UC2        CV_8UC3      	CV_8UC4
CV_8SC1      CV_8SC2        CV_8SC3          CV_8SC4
CV_16UC1     CV_16UC2       CV_16UC3          CV_16UC4
CV_16SC1     CV_16SC2       CV_16SC3         CV_16SC4
CV_32SC1     CV_32SC2       CV_32SC3         CV_32SC4
...

图片深度与java类型对应
图像深度         java数据类型
cv_8u=0         8位byte
cv_8s=1         8位byte
cv_16u=2        16位char
cv_16s=3        16位char
cv_32s=4        32位整型int
cv_32f=5        32位float
cv_64f=6        64位double


