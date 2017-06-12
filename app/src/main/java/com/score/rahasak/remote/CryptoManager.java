package com.score.rahasak.remote;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CryptoManager {

    private static final String TAG = SenzNotificationManager.class.getName();

    private static CryptoManager instance;

    private Cipher cipherEnc;
    private Cipher cipherDec;

    private CryptoManager() {
    }

    public static CryptoManager getInstance() {
        if (instance == null) {
            instance = new CryptoManager();
        }

        return instance;
    }

    public void initCiphers(SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        cipherEnc = Cipher.getInstance("AES");
        cipherEnc.init(Cipher.ENCRYPT_MODE, secretKey);

        cipherDec = Cipher.getInstance("AES");
        cipherDec.init(Cipher.DECRYPT_MODE, secretKey);
    }

    public byte[] encrypt(byte[] payload, int offset, int length) throws BadPaddingException, IllegalBlockSizeException {
        return cipherEnc.doFinal(payload, offset, length);
    }

    public byte[] decrypt(byte[] payload) throws BadPaddingException, IllegalBlockSizeException {
        return cipherDec.doFinal(payload);
    }
}
