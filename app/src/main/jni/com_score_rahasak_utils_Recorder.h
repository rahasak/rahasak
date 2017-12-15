#include <jni.h>

#ifndef _Included_com_score_rahasak_utils_Recorder
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_score_rahasak_utils_Recorder_nativeStart
  (JNIEnv *env, jobject obj);

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_Recorder_nativeStartRecord
  (JNIEnv *env, jobject obj, jshortArray in);

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_Recorder_nativeStartPlay
  (JNIEnv *env, jobject obj, jshortArray out);

#ifdef __cplusplus
}
#endif
#endif
