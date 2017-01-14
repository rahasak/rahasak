#include <jni.h>
/* Header for class OpusEncoder */

#ifndef _Included_com_score_rahasak_utils_OpusEncoder
#define _Included_com_score_rahasak_utils_OpusEncoder
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_OpusEncoder_nativeInitEncoder
  (JNIEnv *, jobject, jint, jint, jint);

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_OpusEncoder_nativeEncodeBytes
  (JNIEnv *, jobject, jshortArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif