int init_decoder(int samplingRate, int numberOfChannels, int frameSize);

int decode(char in[], int inSize, short out[], int outSize);

int release_decoder();
