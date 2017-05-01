#include <jni.h>
/* Header for class OpusDecoder */

#ifndef _Included_com_score_rahasak_utils_OpusDecoder
#define _Included_com_score_rahasak_utils_OpusDecoder
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_OpusDecoder_nativeInitDecoder
  (JNIEnv *, jobject, jint, jint, jint);

JNIEXPORT jint JNICALL Java_com_score_rahasak_utils_OpusDecoder_nativeDecodeBytes
  (JNIEnv *, jobject, jbyteArray, jshortArray);

JNIEXPORT jboolean JNICALL Java_com_score_rahasak_utils_OpusDecoder_nativeReleaseDecoder
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
