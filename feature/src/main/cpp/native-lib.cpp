#include <jni.h>
#include <string>
#include <android/log.h>
//在cmakelists.txt中引入系统库libandroid.so(android) 可能会用到其他系统头文件inflateEnd(libz.so) libOpenSLES
#include <android/native_window_jni.h>
#include <unistd.h>
#include "nativelog.h"
#include "audioPlayer.h"
#include "native-lib.h"
#include "crash_monitor.h"
#include <cmath>
#include <ctime>
#include <cstdio>
#include <cstdarg>

extern "C" {
#include <libfaac/faac.h>
}
#define  MAX_AUDIO_FRAME_SIZE 48000*4


extern "C"
JNIEXPORT jint JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_run__I_3Ljava_lang_String_2(JNIEnv *env,
                                                                                  jclass type,
                                                                                  jint cmdLen,
                                                                                  jobjectArray cmd) {
    __android_log_print(ANDROID_LOG_INFO, "FFmpegCmd", "run start");
//  ffmpeg的函数都是以av开头
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_version_info %s", av_version_info());
    return cmdLen;
}

NativeLog *nativeLog = nullptr;
CrashMonitor monitor;

void selfLog(const char* fmt,...){
    va_list  args;
    va_start(args, fmt);
    char buffer[1024];
    vsnprintf(buffer,sizeof(buffer),fmt,args);
    va_end(args);
    __android_log_write(ANDROID_LOG_INFO, TAG, buffer);
    std::string result;
    result.append(buffer);
    result.append("\n");
    if(nativeLog){
        nativeLog->writeBuffer(const_cast<char *>(result.c_str()));
    }
}

void fmtLog(JNIEnv *env, jstring time, jint level, jstring process_id, jstring process_name,
            jstring thread_id,
            jstring thread_name, jstring method_name, jstring method_param, const std::string &log);

const char *getStringProperty(JNIEnv *env, jclass clazz, jobject obj, const char *propertyName) {
    jmethodID methodId = env->GetMethodID(clazz, propertyName, "()Ljava/lang/String;");
    auto jResult = (jstring) env->CallObjectMethod(obj, methodId);
    const char *result_str = env->GetStringUTFChars(jResult, nullptr);
    env->DeleteLocalRef(jResult);
    return result_str;
}

int getIntProperty(JNIEnv *env, jclass clazz, jobject obj, const char *propertyName) {
    jmethodID methodId = env->GetMethodID(clazz, propertyName, "()I");
    int jResult = env->CallIntMethod(obj, methodId);
    return jResult;
}

std::string convertJString(JNIEnv *env, jstring str) {
    char *charStr = const_cast<char *>(env->GetStringUTFChars(str, nullptr));
    std::string result(charStr, env->GetStringLength(str));
    env->ReleaseStringUTFChars(str, charStr);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_AFOLog_nInitLog(JNIEnv *env, jobject thiz,
                                                            jobject config) {
    jclass clazz = env->FindClass("sst/example/androiddemo/feature/ffmpeg/Configuration");

    char *fileDir = const_cast<char *>(getStringProperty(env, clazz, config, "getFileDir"));
    selfLog("native init fileDir %s",fileDir);

    int singleLogUnit = getIntProperty(env, clazz, config, "getSingleLogUnit");
    selfLog( "native init singleLogUnit %d",
                        singleLogUnit);

    char *logSpliterator = const_cast<char *>(getStringProperty(env, clazz, config,
                                                                "getLogSpliterator"));
    selfLog( "native init logSpliterator %s",
                        logSpliterator);

    char *strSplitter = const_cast<char *>(getStringProperty(env, clazz, config, "getStrSplitter"));
    selfLog("native init strSplitter %s", strSplitter);

    int cacheBuffer = getIntProperty(env, clazz, config, "getCacheBuffer");
    selfLog("native init cacheBuffer %d", cacheBuffer);

    int fileMaxLength = getIntProperty(env, clazz, config, "getFileMaxLength");
    selfLog( "native init fileMaxLength %d",
                        fileMaxLength);
    char *fileNamePrefix = const_cast<char *>(getStringProperty(env, clazz, config, "getFileNamePrefix"));
    selfLog( "native init fileNamePrefix %s",
             fileNamePrefix);

    char *extraInfo = const_cast<char *>(getStringProperty(env, clazz, config, "getExtraInfo"));
    selfLog( "native init extraInfo %s",
             extraInfo);

    int logLevel = getIntProperty(env, clazz, config, "getLogLevel");
    selfLog( "native init logLevel %d", logLevel);
    int maxStorage = getIntProperty(env, clazz, config, "getMaxStorage");
    selfLog( "native init logLevel %d", maxStorage);
    nativeLog = new NativeLog();
    nativeLog->strSplitter = strSplitter;
    nativeLog->cacheBuffer = cacheBuffer;
    nativeLog->logSpliterator = logSpliterator;
    nativeLog->singleLogUnit = singleLogUnit;
    nativeLog->fileMaxLength = fileMaxLength;
    nativeLog->fileNamePrefix = fileNamePrefix;
    nativeLog->extraInfo = extraInfo;
    nativeLog->maxStorage = maxStorage;
    nativeLog->init(fileDir);
    env->DeleteLocalRef(clazz);
}




extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_CrashMonitor_nInitCrashMonitor(JNIEnv *env,
                                                                           jobject thiz) {
    selfLog( "call init ");
    //回调给java层处理可能来不及 直接在native层处理
    monitor.init([](char* str){
        if(nativeLog){
            selfLog( "catch error:%s",str);
            nativeLog->flushCache(str);
        }
    });
}


std::string getLogPrefix(JNIEnv *env, jstring time, jint level,
                         jstring process_id, jstring process_name,
                         jstring thread_id, jstring thread_name,
                         jstring method_name, jstring method_param) {
    std::string s;
    s.append(convertJString(env, time));
    s.append(nativeLog->strSplitter);
    std::string levelStr;
    switch (level) {
        case 0:
            levelStr = "DEBUG";
            break;
        case 1:
            levelStr = "INFO";
            break;
        case 2:
            levelStr = "WARN";
            break;
        case 3:
            levelStr = "ERROR";
            break;
        default:
            selfLog( "level not found:%d", level);
            break;
    }
    s.append(levelStr);
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, process_id));
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, process_name));
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, thread_id));
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, thread_name));
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, method_name));
    s.append(nativeLog->strSplitter);
    s.append(convertJString(env, method_param));
    s.append(nativeLog->strSplitter);
    return s;
}


extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_AFOLog_nLog(JNIEnv *env, jobject thiz, jstring time,
                                                        jint level,
                                                        jstring process_id, jstring process_name,
                                                        jstring thread_id, jstring thread_name,
                                                        jstring method_name, jstring method_param,
                                                        jstring message) {
    std::string log = convertJString(env, message);
//    selfLog( "native receive log length:%d %s",
//                        log.length(), log.c_str());
    if (nullptr == nativeLog) {
        return;
    }
    //log格式拼接
    if (log.length() <= nativeLog->singleLogUnit) {
        fmtLog(env, time, level, process_id, process_name, thread_id, thread_name, method_name,
               method_param,
               log);
    } else {
        int count = ceil(log.length() / (double) nativeLog->singleLogUnit);
//        selfLog( "native receive log count:%d",
//                            count);

        for (int i = 0; i < count; i++) {
            int end = nativeLog->singleLogUnit;
            if ((i + 1) * nativeLog->singleLogUnit > log.length()) {
                end = log.length() - i * nativeLog->singleLogUnit;
            }
            std::string subStr = log.substr(i * nativeLog->singleLogUnit, end);
//            selfLog(
//                                "native receive log subStr.length:%dnd", subStr.length());
            fmtLog(env, time, level, process_id, process_name, thread_id, thread_name, method_name,
                   method_param,
                   subStr);
        }
    }
}

void fmtLog(JNIEnv *env, jstring time, jint level, jstring process_id, jstring process_name,
            jstring thread_id,
            jstring thread_name, jstring method_name, jstring method_param,
            const std::string &log) {
    std::string s;
    s.append(getLogPrefix(env, time, level, process_id, process_name, thread_id, thread_name,
                          method_name, method_param));
    s.append(log);
    if (nativeLog->logSpliterator) {
        s.append(nativeLog->logSpliterator);
    }
    nativeLog->log(const_cast<char *>(s.c_str()));
}


extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_AFOLog_nCloseLog(JNIEnv *env, jobject thiz) {
    if (nullptr != nativeLog) {
        nativeLog->closeLog();
        delete nativeLog;
        nativeLog = nullptr;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_AFOLog_flushCache(JNIEnv *env, jobject thiz) {
    if (nativeLog) {
        NativeLog::flushCache();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegCmd_nStartPlay(JNIEnv *env, jclass type,
                                                                 jstring path_,
                                                                 jobject surface) {
    const char *path = env->GetStringUTFChars(path_, 0);
    __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "StartPlay %s", path);
    initContext(const_cast<char *>(path), env, surface);
    env->ReleaseStringUTFChars(path_, path);
}


void short2float(short *in, double *out, int len) {
    register int i;
    for (i = 0; i < len; i++)
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
Java_sst_example_androiddemo_feature_ffmpeg_FFmpegActivity_native_1replaceAudio(JNIEnv *env,
                                                                                jobject instance,
                                                                                jbyteArray pcmDatas_,
                                                                                jint len) {
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
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "avcodec_open2失败 %d", ret);
        return;
    }
    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_frame_alloc失败 ");

    }
    frame->nb_samples = codecContext->frame_size;
    frame->format = codecContext->sample_fmt;
    frame->channel_layout = codecContext->channel_layout;

    int bufferSize = av_samples_get_buffer_size(NULL, codecContext->channels,
                                                codecContext->frame_size,
                                                codecContext->sample_fmt, 0);
    uint8_t *encoderData = static_cast<uint8_t *>(av_malloc(bufferSize));

    avcodec_fill_audio_frame(frame, codecContext->channels, codecContext->sample_fmt,
                             (const uint8_t *) encoderData,
                             bufferSize, 0);

//    ret = av_frame_make_writable(frame);
//    if(ret<0){
//        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd", "av_frame_make_writable失败,当前frame不可写 %d",ret);
//        return;
//    }
    //开始编码
    AVPacket pkt;
    av_init_packet(&pkt);
    short2float(reinterpret_cast<short *>(pcmDatas), reinterpret_cast<double *>(encoderData),
                len / 2);
    frame->data[0] = encoderData;
    frame->pts = 0;
    ret = avcodec_send_frame(codecContext, frame);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "avcodec_send_frame失败 %d", ret);
    }
    ret = avcodec_receive_packet(codecContext, &pkt);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR(EAGAIN) %d", AVERROR(EAGAIN));
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR_EOF %d", AVERROR_EOF);
        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "AVERROR(EINVAL) %d", AVERROR(EINVAL));

        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "avcodec_receive_packet失败 %d", ret);

    }
    while (ret >= 0) {
        int length = pkt.size + ADTS_HEADER_LENGTH;
        void *adts = malloc(ADTS_HEADER_LENGTH);
        //添加adts header 可以正常播放。
        addADTSheader((uint8_t *) adts, pkt.size + ADTS_HEADER_LENGTH,
                      codecContext->channel_layout);
        uint8_t *out;
        memcpy(out, adts, ADTS_HEADER_LENGTH);
        free(adts);
        memcpy(out + ADTS_HEADER_LENGTH, pkt.data, pkt.size);

        __android_log_print(ANDROID_LOG_ERROR, "FFmpegCmd ", "输出转码 %s", out);
    }

    env->ReleaseByteArrayElements(pcmDatas_, pcmDatas, 0);
}
