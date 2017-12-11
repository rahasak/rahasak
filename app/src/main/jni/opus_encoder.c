#include <opus_encoder.h>
#include <string.h>
#include <android/log.h>
#include <opus/include/opus.h>
#include <stdio.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"RAHASAK-JNI",__VA_ARGS__)

OpusEncoder *enc;

opus_int32 SAMPLING_RATE;
int CHANNELS;
int APPLICATION_TYPE = OPUS_APPLICATION_VOIP;
int FRAME_SIZE;
const int MAX_PAYLOAD_BYTES = 1500;


int init_encoder(int samplingRate, int numberOfChannels, int frameSize)
{
	FRAME_SIZE = frameSize;
	SAMPLING_RATE = 16000;
	CHANNELS = numberOfChannels;

	int error;
	int size;

	size = opus_encoder_get_size(1);
	enc = malloc(size);
	error = opus_encoder_init(enc, SAMPLING_RATE, CHANNELS, APPLICATION_TYPE);

	LOG("init encoder --- (%d) (%d) (%d) (%d)\n", SAMPLING_RATE, CHANNELS, FRAME_SIZE, error);

	return error;
}

int encode (short *in, int inSize, char *out, int outSize)
{
	int inputArraySize = inSize;
	int outputArraySize = outSize;

	unsigned char *data = (unsigned char*)calloc(MAX_PAYLOAD_BYTES,sizeof(unsigned char));
	int dataArraySize = opus_encode(enc, in, FRAME_SIZE, data, MAX_PAYLOAD_BYTES);

	if (dataArraySize >=0)
	{
		if (dataArraySize <= outputArraySize)
		{
		    memcpy(data, out, dataArraySize);
		}
		else
		{
			return -1;
		}
	}

	// (*env)->ReleaseShortArrayElements(env,in,audioSignal,JNI_ABORT);
	return dataArraySize;
}

int release_encoder()
{
    free(enc);

    return 1;
}



