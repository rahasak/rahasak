#include <stdio.h>
#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <android/log.h>
#include <opensl/opensl_io.h>
#include <opus_encoder.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"RAHASAK-JNI",__VA_ARGS__)

int SAMPLE_RATE;
int CHANNELS;
int PERIOD_TIME;
int FRAME_SIZE;
int BUFFER_SIZE;

int sendSoc, addr_size;
struct sockaddr_in server_addr;

static volatile int g_loop_exit = 0;
JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Recorder_nativeInit(JNIEnv *env, jobject obj, jint sampleRate, jint numberOfChannels, jstring oSenz)
{
    SAMPLE_RATE = 16000;
    CHANNELS = numberOfChannels;
	FRAME_SIZE = 160;
	BUFFER_SIZE = FRAME_SIZE * CHANNELS;

	// init opus encoder
    init_encoder(SAMPLE_RATE, CHANNELS, FRAME_SIZE);

    // socket
    sendSoc = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);

    // configure server socket address struct
    memset((char *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(9090);
    inet_aton("10.2.2.1" , &server_addr.sin_addr);

    // initialize size variable to be used later on
    addr_size = sizeof(server_addr);

    // send oStream message
    char *oStream = (*env)->GetStringUTFChars(env, oSenz, 0);
    sendto(sendSoc, oStream, strlen(oStream), 0, (struct sockaddr *)&server_addr, addr_size);
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Recorder_nativeStartCapture(JNIEnv *env, jobject obj)
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
        sendto(sendSoc, opusBuf, enc, 0, (struct sockaddr *)&server_addr, addr_size);
        LOG("--- sent %d \n", enc);
    }

    android_CloseAudioDevice(stream);
    close(sendSoc);

    LOG("nativeStartCapture completed !");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_Recorder_nativeStopCapture(JNIEnv *env, jobject obj)
{
    g_loop_exit = 1;
    return JNI_TRUE;
}
