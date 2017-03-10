package com.score.rahasak.utils;

import android.content.Context;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * RSR encryption
 */
public class CryptoUtils {

    public static final String PUBLIC_KEY = "PUBLIC_KEY";
    public static final String PRIVATE_KEY = "PRIVATE_KEY";

    // size of RSA keys
    private static final int RSA_KEY_SIZE = 1024;
    private static final int SESSION_KEY_SIZE = 128;

    public static KeyPair initKeys(Context context) throws NoSuchProviderException, NoSuchAlgorithmException {
        // generate keypair
        KeyPairGenerator keyPairGenerator;
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // save keys in shared preferences
        savePublicKey(context, keyPair);
        savePrivateKey(context, keyPair);

        return keyPair;
    }

    private static void savePublicKey(Context context, KeyPair keyPair) {
        // get public key from keypair
        byte[] keyContent = keyPair.getPublic().getEncoded();
        String publicKey = Base64.encodeToString(keyContent, Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

        // save public key in shared preference
        PreferenceUtils.saveRsaKey(context, publicKey, CryptoUtils.PUBLIC_KEY);
    }

    private static void savePrivateKey(Context context, KeyPair keyPair) {
        // get public key from keypair
        byte[] keyContent = keyPair.getPrivate().getEncoded();
        String privateKey = Base64.encodeToString(keyContent, Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

        // save private key in shared preference
        PreferenceUtils.saveRsaKey(context, privateKey, CryptoUtils.PRIVATE_KEY);
    }

    public static PublicKey getPublicKey(Context context) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        // get key string from shared preference
        String keyString = PreferenceUtils.getRsaKey(context, CryptoUtils.PUBLIC_KEY);

        // convert to string key public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    public static PublicKey getPublicKey(String keyString) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        // convert to string key public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    public static PrivateKey getPrivateKey(Context context) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // get key string from shared preference
        String keyString = PreferenceUtils.getRsaKey(context, CryptoUtils.PRIVATE_KEY);

        // convert to string key public key
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }

    public static String getDigitalSignature(String payload, PrivateKey privateKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes());

        // Base64 encoded string
        return Base64.encodeToString(signature.sign(), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");
    }

    public static boolean verifyDigitalSignature(String payload, String signedPayload, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(payload.getBytes());

        byte[] signedPayloadContent = Base64.decode(signedPayload, Base64.DEFAULT);

        return signature.verify(signedPayloadContent);
    }

    public static String encrypt(PublicKey publicKey, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] data = cipher.doFinal(payload.getBytes());
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static String decrypt(PrivateKey privateKey, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] data = Base64.decode(payload, Base64.DEFAULT);
        return new String(cipher.doFinal(data));
    }

    public static String getSessionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(SESSION_KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
    }

    public static SecretKey getSecretKey(String encodedKey) {
        byte[] key = Base64.decode(encodedKey, Base64.DEFAULT);
        return new SecretKeySpec(key, 0, key.length, "AES");
    }

    public static byte[] encrypt(SecretKey secretKey, byte[] payload, int offset, int lenght) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return cipher.doFinal(payload, offset, lenght);
    }

    public static byte[] decrypt(SecretKey secretKey, byte[] payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        return cipher.doFinal(payload);
    }

    public static String encryptCCM(SecretKey secretKey, String salt, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(salt.getBytes("UTF-8")));

        byte[] data = cipher.doFinal(payload.getBytes());
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static String decryptCCM(SecretKey secretKey, String salt, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(salt.getBytes("UTF-8")));

        byte[] data = Base64.decode(payload, Base64.DEFAULT);
        return new String(cipher.doFinal(data));
    }

    public static byte[] encryptCCM(SecretKey secretKey, byte[] salt, byte[] payload, int offset, int lenght) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(salt));

        return cipher.doFinal(payload, offset, lenght);
    }

    public static byte[] decryptCCM(SecretKey secretKey, byte[] salt, byte[] payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(salt));

        return cipher.doFinal(payload);
    }

    public static String encryptGCM(SecretKey secretKey, String salt, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(salt.getBytes("UTF-8")));

        byte[] data = cipher.doFinal(payload.getBytes());
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static String decryptGCM(SecretKey secretKey, String salt, String payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(salt.getBytes("UTF-8")));

        byte[] data = Base64.decode(payload, Base64.DEFAULT);
        return new String(cipher.doFinal(data));
    }

    public static byte[] encryptGCM(SecretKey secretKey, byte[] salt, byte[] payload, int offset, int lenght) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(salt));

        return cipher.doFinal(payload, offset, lenght);
    }

    public static byte[] decryptGCM(SecretKey secretKey, byte[] salt, byte[] payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(salt));

        return cipher.doFinal(payload);
    }

}
