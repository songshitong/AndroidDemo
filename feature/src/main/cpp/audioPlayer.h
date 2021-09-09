//
// Created by ISS Mac on 2019-07-10.
//
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

}
#ifndef ANDROIDDEMO_AUDIOPLAYER_H
#define ANDROIDDEMO_AUDIOPLAYER_H


class audioPlayer {
public:


};

void getAudioPcm(void **pcm,size_t *pcmSize,AVFormatContext *formatContext,AVPacket *audioPacket,AVCodecContext *audioCodecContext,int audio_stream_index,SwrContext *swrContext,uint8_t *audio_outBuffer,int out_channel_nb,AVSampleFormat outAudioSampleFmt);
void  initContext(char *path,JNIEnv *env,jobject surface);
#endif //ANDROIDDEMO_AUDIOPLAYER_H
