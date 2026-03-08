package com.localdrop.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 📄 EncryptionUtil.java
 *
 * 🔍 Purpose:
 * This utility class handles AES (Advanced Encryption Standard) encryption and decryption.
 * It provides methods to:
 * - Generate a random AES-256 key
 * - Encrypt data using AES
 * - Decrypt data using AES
 *git
 * 🧠 Why AES?
 * - AES is a symmetric encryption algorithm: the same key is used for both encryption and decryption
 * - It's extremely fast and secure for encrypting large amounts of data (like files)
 * - In this app, we use AES to encrypt the actual file contents
 *
 * 🔧 Java Crypto Classes Used:
 * - `KeyGenerator`: generates secure AES keys
 * - `SecretKeySpec`: wraps raw key bytes into a usable key object
 * - `Cipher`: the actual encryption/decryption engine provided by Java
 */
public class EncryptionUtil
{
    /**
     * 🔐 Generates a 256-bit AES secret key
     * @return byte array representing the secret key
     * @throws Exception if the algorithm is not supported
     */
    public static byte[] generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * 📦 Encrypts data using AES with the provided key
     * @param data the plain (unencrypted) bytes
     * @param key the AES key bytes
     * @return encrypted byte array
     * @throws Exception if encryption fails
     */
    public static byte[] encryptWithAES(byte[] data, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    /**
     * 🔓 Decrypts AES-encrypted data using the same key
     * @param data the encrypted byte array
     * @param key the same AES key used for encryption
     * @return decrypted (original) byte array
     * @throws Exception if decryption fails
     */
    public static byte[] decryptWithAES(byte[] data, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

}
