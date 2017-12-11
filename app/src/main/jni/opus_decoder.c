#include <com_score_rahasak_utils_OpusDecoder.h>
#include <string.h>
#include <android/log.h>
#include <opus/include/opus.h>
#include <stdio.h>

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"RAHASAK-JNI",__VA_ARGS__)

// Fields
char logMsg[255];
OpusDecoder *dec;

// Config
opus_int32 SAMPLING_RATE;
int CHANNELS;
int FRAME_SIZE;

int init_decoder(int samplingRate, int numberOfChannels, int frameSize)
{
	SAMPLING_RATE = samplingRate;
	FRAME_SIZE = frameSize;
	CHANNELS = numberOfChannels;

	int size;
	int error;

	size = opus_decoder_get_size(CHANNELS);
	dec = malloc(size);
	error = opus_decoder_init(dec, SAMPLING_RATE, CHANNELS);

	LOG("init decoder --- (%d) (%d) (%d) (%d)\n", SAMPLING_RATE, CHANNELS, FRAME_SIZE, error);

	return error;
}

int decode(char in[], int inSize, short out[], int outSize)
{
	int inputArraySize = inSize;
	int outputArraySize = outSize;

	opus_int16 *data = (opus_int16*)calloc(outputArraySize, sizeof(opus_int16));
	int decodedDataArraySize = opus_decode(dec, in, inputArraySize, data, FRAME_SIZE, 0);

	if (decodedDataArraySize >=0)
	{
		if (decodedDataArraySize <= outputArraySize)
		{
		    memcpy(data, out, decodedDataArraySize);
		}
		else
		{
			return -1;
		}
	}

	return decodedDataArraySize;
}

int release_decoder()
{
    free(dec);

    return 1;
}

