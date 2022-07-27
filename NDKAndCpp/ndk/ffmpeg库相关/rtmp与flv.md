

rtmp 推流   视频格式flv

从网页中拿到flv地址   network->media->查找flv相关文件
https://hdllive-main.autohome.com.cn/athmlive-main/z1_athmlive-main_231451.flv


https://www.cnblogs.com/chyingp/p/flv-getting-started.html
FLV（Flash Video）是一种流媒体格式，因其体积小、协议相对简单，很快便流行开来，并得到广泛的支持。
常见的HTTP-FLV直播协议，就是使用HTTP流式传输通过FLV封装的音视频数据
概括地说，FLV 由 FLV header 跟 FLV file body 两部分组成，而 FLV file body 又由多个 FLV tag组成。

FLV = FLV header + FLV file body
FLV file body = PreviousTagSize0 + Tag1 + PreviousTagSize1 + Tag2 + ... + PreviousTagSizeN-1 + TagN

FLV tag又分为3种类型：
Video Tag：存放视频相关数据；
Audio Tag：存放音频相关数据；
Script Tag：存放音视频元数据；