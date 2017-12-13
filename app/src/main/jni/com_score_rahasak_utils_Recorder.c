#include <stdio.h>
#include <jni.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <android/log.h>
#include <opensl/opensl_io.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"RAHASAK-JNI",__VA_ARGS__)

int SAMPLE_RATE;
int CHANNELS;
int PERIOD_TIME;
int FRAME_SIZE;
int BUFFER_SIZE;

#define BUFFERFRAMES 1024
#define VECSAMPS_MONO 160
#define SR 16000

OPENSL_STREAM* p;

short inbuf[VECSAMPS_MONO], outbuf[VECSAMPS_MONO];

JNIEXPORT void JNICALL Java_com_score_rahasak_utils_Recorder_nativeStart(JNIEnv *env, jobject obj) {
  p = android_OpenAudioDevice(SR,1,1,BUFFERFRAMES);
}

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_Recorder_nativeStartRecord(JNIEnv *env, jobject obj, jshortArray in) {
  // record
  int samp = android_AudioIn(p, inbuf, VECSAMPS_MONO);
  (*env)->SetShortArrayRegion(env, in, 0, VECSAMPS_MONO, inbuf);

  //LOG("--- inbuffer %d  \n",samp);

  return samp;
}

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_Recorder_nativeStartPlay(JNIEnv *env, jobject obj, jshortArray out) {
   // play
   jshort* signal = (*env)->GetShortArrayElements(env, out, 0);
   int samp = android_AudioOut(p, signal, VECSAMPS_MONO);

   (*env)->ReleaseShortArrayElements(env, out, signal, 0);

   //LOG("--- outbuffering %d  \n", samp);

   return samp;
}




