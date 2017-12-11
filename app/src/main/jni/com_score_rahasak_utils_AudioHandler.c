#include <stdio.h>
#include <jni.h>
#include <sys/socket.h>
#include <android/log.h>
#include <opensl/opensl_io.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"AudioDemo-JNI",__VA_ARGS__)

int SAMPLE_RATE;
int CHANNELS;
int PERIOD_TIME;
int FRAME_SIZE;
int BUFFER_SIZE;
char *TEST_CAPTURE_FILE_PATH;

static volatile int g_loop_exit = 0;
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeInit(JNIEnv *env, jobject obj, jint sampleRate, jint numberOfChannels, jstring recordFile)
{
    LOG("init ...! \n");
    SAMPLE_RATE = sampleRate;
    CHANNELS = numberOfChannels;
    PERIOD_TIME = 20;
	FRAME_SIZE = SAMPLE_RATE*PERIOD_TIME/1000;
	BUFFER_SIZE = FRAME_SIZE*CHANNELS;
	TEST_CAPTURE_FILE_PATH = (*env)->GetStringUTFChars(env, recordFile, 0);
}

/*
 * Class:     com_score_rahasak_utils_AudioHandler
 * Method:    nativeStartCapture
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStartCapture(JNIEnv *env, jobject obj)
{
    LOG("start capturing! \n");
    FILE * fp = fopen(TEST_CAPTURE_FILE_PATH, "wb");
    if( fp == NULL ) {
        LOG("cannot open file (%s)\n", TEST_CAPTURE_FILE_PATH);
        return -1;
    }

    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLE_RATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        fclose(fp);
        LOG("failed to open audio device ! \n");
        return JNI_FALSE;
    }

    int samples;
    short buffer[BUFFER_SIZE];
    g_loop_exit = 0;
    while (!g_loop_exit) {
        samples = android_AudioIn(stream, buffer, BUFFER_SIZE);
        if (samples < 0) {
            LOG("android_AudioIn failed !\n");
            break;
        }
        if (fwrite((unsigned char *)buffer, samples*sizeof(short), 1, fp) != 1) {
            LOG("failed to save captured data !\n ");
            break;
        }
        LOG("capture %d samples !\n", samples);
    }

    android_CloseAudioDevice(stream);
    fclose(fp);

    LOG("nativeStartCapture completed !");

    return JNI_TRUE;
}

/*
 * Class:     com_score_rahasak_utils_AudioHandler
 * Method:    nativeStopCapture
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStopCapture(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}

/*
 * Class:     com_score_rahasak_utils_AudioHandler
 * Method:    nativeStartPlayback
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStartPlayback(JNIEnv *env, jobject obj)
{
    FILE * fp = fopen(TEST_CAPTURE_FILE_PATH, "rb");
    if( fp == NULL ) {
        LOG("cannot open file (%s) !\n",TEST_CAPTURE_FILE_PATH);
        return -1;
    }

    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLE_RATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        fclose(fp);
        LOG("failed to open audio device ! \n");
        return JNI_FALSE;
    }

    int samples;
    short buffer[BUFFER_SIZE];
    g_loop_exit = 0;
    while (!g_loop_exit && !feof(fp)) {
        if (fread((unsigned char *)buffer, BUFFER_SIZE*2, 1, fp) != 1) {
            LOG("failed to read data \n ");
            break;
        }
        samples = android_AudioOut(stream, buffer, BUFFER_SIZE);
        if (samples < 0) {
            LOG("android_AudioOut failed !\n");
        }
        LOG("playback %d samples !\n", samples);
    }

    android_CloseAudioDevice(stream);
    fclose(fp);

    LOG("nativeStartPlayback completed !");

    return JNI_TRUE;
}

/*
 * Class:     com_score_rahasak_utils_AudioHandler
 * Method:    nativeStopPlayback
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStopPlayback(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}