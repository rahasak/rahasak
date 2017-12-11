#include <stdio.h>
#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <android/log.h>
#include <opensl/opensl_io.h>
#include <opus_decoder.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG, "RAHASAK-JNI", __VA_ARGS__)

int SAMPLE_RATE;
int CHANNELS;
int PERIOD_TIME;
int FRAME_SIZE;
int BUFFER_SIZE;

int recvSoc, addr_size;
struct sockaddr_in server_addr;

static volatile int g_loop_exit = 0;
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Player_nativeInit(JNIEnv *env, jobject obj, jint sampleRate, jint numberOfChannels, jstring nSenz)
{
    SAMPLE_RATE = 16000;
    CHANNELS = numberOfChannels;
	FRAME_SIZE = 160;
	BUFFER_SIZE = FRAME_SIZE * CHANNELS;

	// init opus decoder
    init_decoder(SAMPLE_RATE, CHANNELS, FRAME_SIZE);

    // socket
    recvSoc = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);

    // configure server socket address struct
    memset((char *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(9090);
    inet_aton("10.2.2.1" , &server_addr.sin_addr);

    // initialize size variable to be used later on
    addr_size = sizeof(server_addr);

    // send nStream message
    char *nStream = (*env)->GetStringUTFChars(env, nSenz, 0);
    sendto(recvSoc, nStream, strlen(nStream), 0, (struct sockaddr *)&server_addr, addr_size);
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Player_nativeStartPlayback(JNIEnv *env, jobject obj)
{
    LOG("playing +++++++ ");
    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLE_RATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        LOG("failed to open audio device ! \n");
        return JNI_FALSE;
    }

    int recvBytes, samples, dec;
    short buffer[BUFFER_SIZE];
    char opusBuf[160];
    g_loop_exit = 0;
    while (!g_loop_exit) {
        // read from socket
        LOG("waiting +++++++ ");
        recvBytes = recvfrom(recvSoc, opusBuf, 160, 0, (struct sockaddr *) &server_addr, &addr_size);
        LOG("received --- %d \n", recvBytes);

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

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Player_nativeStopPlayback(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}