#include <jni.h>
#include <string>
#include <android/log.h>
//在cmakelists.txt中引入系统库libandroid.so(android) 可能会用到其他系统头文件inflateEnd(libz.so) libOpenSLES
#include <android/native_window_jni.h>
#include <unistd.h>
#include "nativelog.h"
#include "audioPlayer.h"
#include "crash_monitor.h"

extern "C" {
#include <libfaac/faac.h>
}
#define  MAX_AUDIO_FRAME_SIZE 48000*4



extern "C"
JNIEXPORT jint JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_run__I_3Ljava_lang_String_2(JNIEnv *env, jclass type, jint cmdLen,
                                                                                  jobjectArray cmd) {
    __android_log_write(ANDROID_LOG_INFO, "FFmpegCmd", "run start");
//  ffmpeg的函数都是以av开头
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_version_info %s", av_version_info());
    return cmdLen;
}

NativeLog* nativeLog = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nInitLog(JNIEnv *env, jclass clazz,
                                                               jstring log_path) {
    const char* nlogPath = env->GetStringUTFChars(log_path, nullptr);
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "native init logPath %s", nlogPath);
    nativeLog = new NativeLog(const_cast<char *>(nlogPath));
    env->ReleaseStringUTFChars(log_path,nlogPath);
}
CrashMonitor  monitor;
extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nInitCrashMonitor(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "call init ");
    monitor.init(NativeLog::onCrash);

}

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nLog(JNIEnv *env, jclass clazz, jstring jlog) {
    const char* nlog = env->GetStringUTFChars(jlog, nullptr);
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "native receive log %s", nlog);
    if(nativeLog){
        nativeLog->log(const_cast<char *>(nlog));
    }
    env->ReleaseStringUTFChars(jlog,nlog);
}

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nCloseLog(JNIEnv *env, jclass clazz) {
    if(nullptr != nativeLog){
        nativeLog->closeLog();
        delete nativeLog;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nStartPlay(JNIEnv *env, jclass type, jstring path_,
                                                                 jobject surface) {
    const char *path = env->GetStringUTFChars(path_, 0);
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "StartPlay %s", path);
    initContext(const_cast<char *>(path),env,surface);
    env->ReleaseStringUTFChars(path_, path);
}









void short2float(short* in, double *out, int len){
    register int i;
    for(i = 0; i < len; i++)
        out[i] = in[i] / 32767.0;
}

void addADTSheader(uint8_t *in, int packet_size, int channels) {
    int sampling_frequency_index = 11; //采样率下标
    int channel_configuration = channels; //声道数
    in[0] = 0xFF;
    in[1] = 0xF9;
    in[2] = 0x40 | (sampling_frequency_index << 2) | (channel_configuration >> 2);//0x6c;
    in[3] = (channel_configuration & 0x3) << 6;
    in[3] |= (packet_size & 0x1800) >> 11;
    in[4] = (packet_size & 0x1FF8) >> 3;
    in[5] = ((((unsigned char) packet_size) & 0x07) << 5) | (0xff >> 3);
    in[6] = 0xFC;
}

#define ADTS_HEADER_LENGTH 7

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegActivity_native_1replaceAudio(JNIEnv *env, jobject instance,
                                                                                jbyteArray pcmDatas_,jint len) {
    jbyte *pcmDatas = env->GetByteArrayElements(pcmDatas_, NULL);
    AVCodec *codec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    if (codec) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "找到AAC编码器  ");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "查到AAC编码器失败 ");
        return;
    }
    AVCodecContext *codecContext = avcodec_alloc_context3(codec);
    if (!codecContext) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "查到codecContext失败 ");
        return;
    }
    codecContext->codec_id = AV_CODEC_ID_AAC;
    codecContext->codec_type = AVMEDIA_TYPE_AUDIO;
    codecContext->bit_rate = 64000;
    codecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    codecContext->sample_rate = 44100;
    codecContext->channel_layout = AV_CH_LAYOUT_MONO;
    codecContext->channels = av_get_channel_layout_nb_channels(codecContext->channel_layout);

    int ret = avcodec_open2(codecContext, codec, NULL);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "avcodec_open2失败 %d",ret);
        return;
    }
    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_frame_alloc失败 ");

    }
    frame->nb_samples = codecContext->frame_size;
    frame->format = codecContext->sample_fmt;
    frame->channel_layout = codecContext->channel_layout;

    int bufferSize = av_samples_get_buffer_size(NULL, codecContext->channels, codecContext->frame_size,
                                                codecContext->sample_fmt, 0);
    uint8_t *encoderData = static_cast<uint8_t *>(av_malloc(bufferSize));

    avcodec_fill_audio_frame(frame, codecContext->channels, codecContext->sample_fmt, (const uint8_t *) encoderData,
                             bufferSize, 0);

//    ret = av_frame_make_writable(frame);
//    if(ret<0){
//        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_frame_make_writable失败,当前frame不可写 %d",ret);
//        return;
//    }
    //开始编码
    AVPacket pkt;
    av_init_packet(&pkt);
    short2float(reinterpret_cast<short *>(pcmDatas), reinterpret_cast<double *>(encoderData), len / 2);
    frame->data[0] = encoderData;
    frame->pts = 0;
    ret = avcodec_send_frame(codecContext, frame);
    if(ret<0){
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "avcodec_send_frame失败 %d",ret);
    }
    ret = avcodec_receive_packet(codecContext, &pkt);
    if(ret<0){
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR(EAGAIN) %d",AVERROR(EAGAIN));
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR_EOF %d",AVERROR_EOF);
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR(EINVAL) %d",AVERROR(EINVAL));

        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "avcodec_receive_packet失败 %d",ret);

    }
    while (ret>=0){
        int length = pkt.size + ADTS_HEADER_LENGTH;
        void *adts = malloc(ADTS_HEADER_LENGTH);
        //添加adts header 可以正常播放。
        addADTSheader((uint8_t *) adts, pkt.size + ADTS_HEADER_LENGTH,codecContext->channel_layout);
        uint8_t *out;
        memcpy(out, adts, ADTS_HEADER_LENGTH);
        free(adts);
        memcpy(out + ADTS_HEADER_LENGTH, pkt.data, pkt.size);

        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "输出转码 %d",out);
    }

    env->ReleaseByteArrayElements(pcmDatas_, pcmDatas, 0);
}




