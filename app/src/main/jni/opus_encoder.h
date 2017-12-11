int init_encoder(int samplingRate, int numberOfChannels, int frameSize);

int encode (short *in, int inSize, char *out, int outSize);

int release_encoder();


