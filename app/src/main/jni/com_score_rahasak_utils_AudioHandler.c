#include <stdio.h>
#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <android/log.h>
#include <opensl/opensl_io.h>
#include <opus_encoder.h>
#include <opus_decoder.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"RAHASAK-JNI",__VA_ARGS__)

int SAMPLE_RATE;
int CHANNELS;
int PERIOD_TIME;
int FRAME_SIZE;
int BUFFER_SIZE;

int sendSoc, recvSoc;
struct sockaddr_in serverAddr;
socklen_t addr_size;

static volatile int g_loop_exit = 0;
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeInit(JNIEnv *env, jobject obj, jint sampleRate, jint numberOfChannels, jstring oSenz, jstring nSenz)
{
    SAMPLE_RATE = 16000;
    CHANNELS = numberOfChannels;
	FRAME_SIZE = 160;
	BUFFER_SIZE = FRAME_SIZE * CHANNELS;

	// init opus encoder/decoder
    init_encoder(SAMPLE_RATE, CHANNELS, FRAME_SIZE);
    init_decoder(SAMPLE_RATE, CHANNELS, FRAME_SIZE);

    // configure server socket address struct
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(9090);
    serverAddr.sin_addr.s_addr = inet_addr("52.77.242.96");
    memset(serverAddr.sin_zero, '\0', sizeof serverAddr.sin_zero);

    // initialize size variable to be used later on
    addr_size = sizeof serverAddr;

    // send socket
    // send start stream message
    sendSoc = socket(PF_INET, SOCK_DGRAM, 0);
    char *oStream = (*env)->GetStringUTFChars(env, oSenz, 0);
    sendto(sendSoc, oStream, sizeof(oStream), 0, (struct sockaddr *)&serverAddr, addr_size);

    //recv socket
    // send nStream message
    recvSoc = socket(PF_INET, SOCK_DGRAM, 0);
    char *nStream = (*env)->GetStringUTFChars(env, nSenz, 0);
    sendto(recvSoc, nStream, sizeof(nStream), 0, (struct sockaddr *)&serverAddr, addr_size);
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStartCapture(JNIEnv *env, jobject obj)
{
    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLE_RATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        LOG("failed to open audio device ! \n");
        return JNI_FALSE;
    }

    int samples, enc;
    short buffer[BUFFER_SIZE];
    char opusBuf[32];
    g_loop_exit = 0;
    while (!g_loop_exit) {
        samples = android_AudioIn(stream, buffer, BUFFER_SIZE);
        if (samples < 0) {
            LOG("android_AudioIn failed !\n");
            break;
        }

        // opus encode
        // send stream
        enc = encode(buffer, 160, opusBuf, 32);
        sendto(sendSoc, opusBuf, enc, 0, (struct sockaddr *)&serverAddr, addr_size);

        LOG("--- capture %d samples %d encoded !\n", samples, enc);
    }

    android_CloseAudioDevice(stream);
    close(sendSoc);

    LOG("nativeStartCapture completed !");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStopCapture(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStartPlayback(JNIEnv *env, jobject obj)
{
    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLE_RATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        LOG("failed to open audio device ! \n");
        return JNI_FALSE;
    }

    int recvBytes, samples, dec;
    short buffer[BUFFER_SIZE];
    char opusBuf[32];
    g_loop_exit = 0;
    while (!g_loop_exit) {
        // read from socket
        recvBytes = recvfrom(recvSoc, opusBuf, 32, 0, NULL, NULL);
        LOG("received --- %d !\n", recvBytes);

        // decode
        dec = decode(opusBuf, recvBytes, buffer, 160);
        samples = android_AudioOut(stream, buffer, dec);
        if (samples < 0) {
            LOG("android_AudioOut failed !\n");
        }

        LOG("playback %d samples !\n", samples);
    }

    android_CloseAudioDevice(stream);

    LOG("nativeStartPlayback completed !");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_AudioHandler_nativeStopPlayback(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}