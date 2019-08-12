//
// Created by ISS Mac on 2019-07-10.
//
#include <string>
#include <android/log.h>
//在cmakelists.txt中引入系统库libandroid.so(android) 可能会用到其他系统头文件inflateEnd(libz.so) libOpenSLES
#include <android/native_window_jni.h>
#include "audioPlayer.h"
#include <android/log.h>
#include <unistd.h>
extern "C" {
//解码
#include "libavcodec/avcodec.h"
//封装格式
#include "libavformat/avformat.h"
#include "libavfilter/avfilter.h"
//缩放
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/dict.h>
//重采样
#include "libswresample/swresample.h"

// opensl es
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

}




AVPacket *audioPacket;
SwrContext *swrContext;
AVSampleFormat audioSampleFmt;
AVCodecContext *audioCodecContext;
uint8_t *audio_outBuffer;
AVSampleFormat outAudioSampleFmt;
AVFormatContext *formatContext;
int audio_stream_index;
int out_channel_nb;
int count;


size_t buffersize = 0;
void *buffer;
//TODO 在多个文件引入相同的变量定义
void getAudioPcm(void **pcm,size_t *pcmSize){
    int ret;
    if (av_read_frame(formatContext, audioPacket) >= 0) {
        avcodec_send_packet(audioCodecContext, audioPacket);
        //解压缩数据
        AVFrame *audioFrame = av_frame_alloc();
        //将packet数据解压缩至frame
        ret = avcodec_receive_frame(audioCodecContext, audioFrame);
        if (ret == AVERROR(EAGAIN)) {
            __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "当前输出不可用，请发送下一帧  %d", ret);
//            continue;
            return;
        } else if (ret < 0) {
            //读取完成/解析失败 没有调用avcodec_open2报错-22
            __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "合法错误 avcodec_receive_frame  %d", ret);
//            break;
            return;

        } else if (ret == AVERROR_EOF) {
            __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "没有更多frame avcodec_receive_frame  %d", ret);
//            break;
            return;

        }
        if (audioPacket->stream_index != audio_stream_index) {
            __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "当前不是音频流 %d", audioPacket->stream_index);
            //当前不是音频流
//            continue;
            return;

        }
        count++;
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "音频帧数 count %d", count);

        //frame不可以直接输出喇叭
        //mp3 转为统一采样率  44100 48800
        //frame --->统一格式
        swr_convert(swrContext, &audio_outBuffer, 2 * 44100, (const uint8_t **) (audioFrame->data),
                    audioFrame->nb_samples);//nb_samples 一个通道中可用的输入样本数

        //缓冲区大小 当前帧的实际大小
        int out_buffer_size = av_samples_get_buffer_size(NULL, out_channel_nb, audioFrame->nb_samples,
                                                         outAudioSampleFmt, 1);
        //写入文件 1写入最小值 文件1字节，像素4字节
//        fwrite(audio_outBuffer,1,out_buffer_size,fc_pcm);

        *pcm = audio_outBuffer;
        *pcmSize = static_cast<size_t>(out_buffer_size);
    }
}

int outCount=0;
//回调的函数
void getQueueCallBack(SLAndroidSimpleBufferQueueItf slBufferQueueItf, void *context) {
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "getQueueCallBack start ======》");
    buffersize = 0;
    getAudioPcm(&buffer, &buffersize);
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "getQueueCallBack after getAudioPcm buffersize %d",buffersize);

    outCount++;
    if (buffer != NULL && buffersize != 0) {
        //将得到的数据加入到队列中
        SLresult ret = (*slBufferQueueItf)->Enqueue(slBufferQueueItf, buffer, buffersize);
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "getQueueCallBack 加入到队列  ret %d", ret);

    }
}

SLObjectItf engineObject = NULL;//用SLObjectItf声明引擎接口对象
SLEngineItf engineEngine = NULL;//声明具体的引擎对象实例

//创建引擎
void createEngine() {
    SLresult ret;
    slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);//创建引擎
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);//实现engineObject接口对象
    ret = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);//通过引擎调用接口初始化SLEngineItf
    if (ret == SL_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "createEngine GetInterface 成功 ret %d", ret);

    } else {
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "createEngine GetInterface 失败 ret %d", ret);

    }
}

SLObjectItf outputMixObject = NULL;//用SLObjectItf创建混音器接口对象
SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;////具体的混音器对象实例
SLEnvironmentalReverbSettings settings = SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;//默认情况
//创建混音器
void createMixVolume() {
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, 0, 0);//用引擎对象创建混音器接口对象
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);//实现混音器接口对象
    SLresult sLresult = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                                         &outputMixEnvironmentalReverb);//利用混音器实例对象接口初始化具体的混音器对象
    //设置
    if (SL_RESULT_SUCCESS == sLresult) {
        (*outputMixEnvironmentalReverb)->
                SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &settings);
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "createMixVolume GetInterface 成功 sLresult %d", sLresult);

    } else {
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "createMixVolume GetInterface 失败 sLresult %d", sLresult);

    }
}


SLObjectItf audioplayer = NULL;//用SLObjectItf声明播放器接口对象
SLPlayItf slPlayItf = NULL;//播放器接口
SLAndroidSimpleBufferQueueItf slBufferQueueItf = NULL;//缓冲区队列接口
void createPlayer(int rate, int channels) {
    SLresult ret;
    /*
     * typedef struct SLDataLocator_AndroidBufferQueue_ {
    SLuint32    locatorType;//缓冲区队列类型
    SLuint32    numBuffers;//buffer位数
} */

    SLDataLocator_AndroidBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    /**
    typedef struct SLDataFormat_PCM_ {
        SLuint32        formatType;  pcm
        SLuint32        numChannels;  通道数
        SLuint32        samplesPerSec;  采样率
        SLuint32        bitsPerSample;  采样位数
        SLuint32        containerSize;  包含位数
        SLuint32        channelMask;     立体声
        SLuint32        endianness;    end标志位
    } SLDataFormat_PCM;
     */
    SLuint32 channelMask;
    if (channels == 2) {
        channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    } else {
        channelMask = SL_SPEAKER_FRONT_CENTER;
    }
    SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM, static_cast<SLuint32>(channels), static_cast<SLuint32>(rate * 1000),
                            SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, channelMask,
                            SL_BYTEORDER_LITTLEENDIAN};

    /*
     * typedef struct SLDataSource_ {
            void *pLocator;//缓冲区队列
            void *pFormat;//数据样式,配置信息
        } SLDataSource;
     * */
    SLDataSource dataSource = {&android_queue, &pcm};


    SLDataLocator_OutputMix slDataLocator_outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};


    SLDataSink slDataSink = {&slDataLocator_outputMix, NULL};


    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE};

    /*
     * SLresult (*CreateAudioPlayer) (
        SLEngineItf self,
        SLObjectItf * pPlayer,
        SLDataSource *pAudioSrc,//数据设置
        SLDataSink *pAudioSnk,//关联混音器
        SLuint32 numInterfaces,
        const SLInterfaceID * pInterfaceIds,
        const SLboolean * pInterfaceRequired
    );
     * */
    ret = (*engineEngine)->CreateAudioPlayer(engineEngine, &audioplayer, &dataSource, &slDataSink, 3, ids, req);
    if (ret != SL_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "创建audioplayer失败 ret %d", ret);
        return;
    }else{
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "创建audioplayer成功 ret %d", ret);
    }
    (*audioplayer)->Realize(audioplayer, SL_BOOLEAN_FALSE);
    (*audioplayer)->GetInterface(audioplayer, SL_IID_PLAY, &slPlayItf);//初始化播放器
    //注册缓冲区,通过缓冲区里面 的数据进行播放
    (*audioplayer)->GetInterface(audioplayer, SL_IID_BUFFERQUEUE, &slBufferQueueItf);
    //设置回调接口
    ret = (*slBufferQueueItf)->RegisterCallback(slBufferQueueItf, getQueueCallBack, NULL);
    if(SL_RESULT_SUCCESS == ret){
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "注册回调，RegisterCallback 成功 ret %d", ret);
    }else{
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "注册回调 RegisterCallback 失败 ret %d", ret);

    }
//    (*audioplayer)->RegisterCallback(audioplayer, getQueueCallBack, NULL);
            //播放
    (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
//    //开始播放
    getQueueCallBack(slBufferQueueItf, NULL);

}


void  initContext( char *path){
    //底层操作绘制渲染通过ANativeWindow
//    ANativeWindow *nativeWindow =  ANativeWindow_fromSurface(env,surface);

    //ffmpeg 音频绘制   视频绘制
    //初始化网络
    avformat_network_init();
    //总上下文
    formatContext = avformat_alloc_context();
    //TODO c++ null  二级地址
    AVDictionary *options = NULL;
    //设置超时时间，单位微妙，现在是3秒，超过3秒打不开文件认为打开失败
    // 二级指针一般代表会改变地址，用寻址符*取到指针的指针
    av_dict_set(&options, "timeout", "30000000", 0);
    int ret = avformat_open_input(&formatContext, path, NULL, NULL);
    //TODO c++ if
    if (ret) { //打开失败
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "打开失败 avformat_open_input error ");
        return;
    }
    //找到视频流
    ret = avformat_find_stream_info(formatContext, NULL);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "找不到文件信息 avformat_find_stream_info error ");
        return;
    }
    //记录视频流索引
    int video_stream_index = -1;
    //记录视频流索引
    audio_stream_index = -1;
    for (int i = 0; i < formatContext->nb_streams; i++) {
        //视频流->视频信息->编码类型
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
            __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "视频流index %d", video_stream_index);
            continue;
        } else if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_index = i;
            __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "音频流index %d", audio_stream_index);
            continue;
        }
    }
//    __android_log_print(ANDROID_LOG_INFO,"FFmpegCmd","视频解码开始=======》");
//
//    //视频流解码参数
//    AVCodecParameters *codecParameters = formatContext->streams[video_stream_index]->codecpar;
//
//    //解码器 h264   codec_id编码类型
//    AVCodec *avCodec = avcodec_find_decoder(codecParameters->codec_id);
//
//    //解码器上下文  ffmpeg升级优化，context的名称改变，context3当前最新
//    AVCodecContext *avCodecContext =  avcodec_alloc_context3(avCodec);
//    //将解码器参数copy到解码器上下文
//    avcodec_parameters_to_context(avCodecContext,codecParameters);
//    //初始化AVCodecContext以使用给定的AVCodec
//    avcodec_open2(avCodecContext,avCodec,NULL);
//    //解码 压缩数据生成YUV数据（很大）  跟YUV有关的都封装在AvPacket
//    // 之前版本使用malloc 类似Java的new AVPacket,现在通过ffmpeg内部函数，方便维持队列
//    AVPacket *avPacket = av_packet_alloc();
//    //重视速度  fast_bilinear,point
//    //重视质量  cubic,spline,lanczos
//    //缩小
//    // 重视速度 fast_bilinear ,point
//    // 重视质量 gauss,fast_bilinear
//    // 重视锐度  cubic,spline,lanczos
//    //拿到视频转码上下文
//   SwsContext *swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,avCodecContext->pix_fmt,avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,SWS_BILINEAR,0,0,0);
//
//   //设置NativeWindow的buffer大小 buffer数据绘制到屏幕     WINDOW_FORMAT_RGBA_8888窗体显示类型
//    ANativeWindow_setBuffersGeometry(nativeWindow,avCodecContext->width,avCodecContext->height,WINDOW_FORMAT_RGBA_8888);
//    ANativeWindow_Buffer outBuffer;
//    if(ANativeWindow_lock(nativeWindow,&outBuffer,NULL)){
//        ANativeWindow_release(nativeWindow);
//        nativeWindow =0;
//        return;
//    }
//    //接受的数组
//    uint8_t *dst_data[4];
//    //从视频流中读取数据包（仍为压缩数据）
//    while (av_read_frame(formatContext,avPacket)>=0){
//       avcodec_send_packet(avCodecContext,avPacket);
//        AVFrame *avFrame = av_frame_alloc();
//
//        ret = avcodec_receive_frame(avCodecContext,avFrame);
//       if(ret == AVERROR(EAGAIN)){
//           //当前状态不可用，继续下一次输入
//           continue;
//       }else if(ret<0){
//           __android_log_print(ANDROID_LOG_INFO,"FFmpegCmd","合法错误 avcodec_receive_frame  ");
//           //读到视频末尾/解析失败
//           break;
//       }else if(ret == AVERROR_EOF ){
//                __android_log_print(ANDROID_LOG_INFO,"FFmpegCmd","没有更多frame avcodec_receive_frame  %d",ret);
//                break;
//            }
//       if(avPacket->stream_index!=video_stream_index){
//           //不是视频流
//           __android_log_print(ANDROID_LOG_INFO,"FFmpegCmd","当前不是音频流 %d",avPacket->stream_index);
//
//           continue;
//       }
//       //每一行首地址
//       int dst_linesize[4];
//       //确定容器大小    align 1左对齐
//       av_image_alloc(dst_data,dst_linesize,avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,1);
////        将YUV转为rgb
//       sws_scale(swsContext,avFrame->data,avFrame->linesize,0,avFrame->height,dst_data,dst_linesize);
//
//       //avframe yuv ----> iamge dst_data  ---> 渲染surfaceview

//       //锁住buffer 防止其他线程同时写  inOutDirtyBounds对当前window加限制1
//       ANativeWindow_lock(nativeWindow,&outBuffer,NULL);
//        uint8_t *firstWindown = static_cast<uint8_t *>(outBuffer.bits);
//        //输入源（RGB）的
//        uint8_t *src_data = dst_data[0];
//        //拿到一行有多少个字节RGBA  stride是一行多少像素
//        int destStride = outBuffer.stride*4;
//        int src_linesize = dst_linesize[0];
//        //绘制  内存的拷贝 一行一行拷贝，从src_data拷贝到outBuffer,outBuffer固定大小，两个存在对齐
//        for (int i = 0; i < outBuffer.height; ++i) {
//            //注释查看memccpy
//            memcpy(firstWindown+i*destStride,src_data+i*src_linesize,destStride);
//        }
//
//       //解锁
//       ANativeWindow_unlockAndPost(nativeWindow);
//        usleep(1000 * 16);
//        av_frame_free(&avFrame);
//        //重置/擦拭avPacket
////        av_packet_unref(avPacket);
//    }
//

    if (audio_stream_index > -1) {
        __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "音频解码开始=======》");
        //音频解码
        AVCodecParameters *audioCodecParameters = formatContext->streams[audio_stream_index]->codecpar;
        AVCodec *audioCodec = avcodec_find_decoder(audioCodecParameters->codec_id);
        audioCodecContext = avcodec_alloc_context3(audioCodec);
        avcodec_parameters_to_context(audioCodecContext, audioCodecParameters);
        //使用audioCodec初始化audioCodecContext
        avcodec_open2(audioCodecContext, audioCodec, NULL);

        audioPacket = av_packet_alloc();
        //音频解码上下文
        swrContext = swr_alloc();
        //输入参数们
        //采样位数
        audioSampleFmt = audioCodecContext->sample_fmt;
        //采样频率
        int audioSampleRate = audioCodecContext->sample_rate;
        //输入声道布局
        uint64_t channelLayout = audioCodecContext->channel_layout;
        //输出参数 固定，输出源喇叭
        //输出采样格式  16位
        outAudioSampleFmt = AV_SAMPLE_FMT_S16;
        //输出采样
        int out_sample_rate = 44100;
        //输出声道布局  双声道（左声道、右声道）
        uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
        //设置转换器的输出和输入参数
        swr_alloc_set_opts(swrContext, out_ch_layout, outAudioSampleFmt, out_sample_rate, channelLayout, audioSampleFmt,
                           audioSampleRate, 0, NULL);
        //初始化转换器其他的默认参数
        swr_init(swrContext);

        // 定义缓冲区
        audio_outBuffer = static_cast<uint8_t *>(av_malloc(2 * out_sample_rate));//初始化大小通道数*采样率
        count = 0;
//    FILE *fc_pcm = fopen(path,"wb");//要写入的文件
        createEngine();
        createMixVolume();
        //计算声道数
        out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);
        createPlayer(audioSampleRate, out_channel_nb);

//    fclose(fc_pcm);
//        av_free(audio_outBuffer);
//        swr_free(&swrContext);

    }


//    ANativeWindow_release(nativeWindow);
//    av_freep(&dst_data[0]);
    //关闭解码器
//    avcodec_close(avCodecContext);
    //关闭视频文件
//    avformat_close_input(&formatContext);


    //TODO 第二次播放视频，重启APP error  阻塞时队列？
}