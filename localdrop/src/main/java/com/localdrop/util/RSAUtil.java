package com.localdrop.util;

import java.io.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * 📄 RSAUtil.java
 *
 * 🔍 Purpose:
 * Utility class for managing RSA key pair generation, encryption, decryption, and peer key storage.
 * This class enables secure exchange of AES keys between devices using asymmetric encryption (RSA-4096).
 *
 * 🔐 Why RSA?
 * - RSA is an asymmetric encryption algorithm
 * - Each device has a public key (shared with others) and a private key (kept secret)
 * - RSA is used to encrypt the AES key during file transfer, ensuring only the intended device can decrypt it
 *
 * 🧠 How it works:
 * - Generates and stores a 4096-bit RSA key pair if not already saved
 * - Provides public key in base64 format for safe sharing
 * - Encrypts AES keys using the peer's public key
 * - Decrypts AES keys using our own private key
 */
public class RSAUtil
{
  // 🔐 Path where private key is stored
  private static final String PRIVATE_KEY_FILE = "config/private.key";
  private static final String CONFIG_DIR = "config";

  // Generates or loads the RSA key pair
  private static KeyPair keyPair = generateKeyPair ();

  /**
   * 🔑 Generates a new RSA key pair or loads it from disk (singleton-style)
   * @return the RSA key pair (public + private)
   */
  public static KeyPair generateKeyPair()
  {
    try
    {
      File configDir = new File ( CONFIG_DIR );
      if ( !configDir.exists () )
      {
        configDir.mkdirs ();
      }
      if ( new File ( PRIVATE_KEY_FILE ).exists () )
      {
        // 🔁 Load existing key pair from file
        ObjectInputStream ois = new ObjectInputStream ( new FileInputStream ( PRIVATE_KEY_FILE ) );
        keyPair = ( KeyPair ) ois.readObject ();
        ois.close ();
      }
      else
      {
        // 🆕 Generate a new key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance ( "RSA" );
        generator.initialize ( 4096 );
        keyPair = generator.generateKeyPair ();
        // 💾 Save the key pair to disk for persistence
        ObjectOutputStream oos = new ObjectOutputStream ( new FileOutputStream ( PRIVATE_KEY_FILE ) );
        oos.writeObject ( keyPair );
        oos.close ();
      }
    }
    catch ( Exception e )
    {
      e.printStackTrace ();
    }
    return keyPair;
  }

  /**
   * 📦 Encrypts a byte array (e.g., AES key) using a peer's public key
   * @param data the plaintext data to encrypt
   * @param base64PublicKey the peer's public key (in base64 format)
   * @return encrypted byte array
   */
  public static byte[] encryptRSA ( byte[] data, String base64PublicKey ) throws Exception
  {
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec ( Base64.getDecoder ().decode ( base64PublicKey ) );
    KeyFactory keyFactory = KeyFactory.getInstance ( "RSA" );
    PublicKey publicKey = keyFactory.generatePublic ( keySpec );
    Cipher cipher = Cipher.getInstance ( "RSA" );
    cipher.init ( Cipher.ENCRYPT_MODE, publicKey );
    return cipher.doFinal ( data );
  }

  /**
   * 🔓 Decrypts an RSA-encrypted byte array using this device’s private key
   * @param encryptedData the encrypted byte array (e.g., encrypted AES key)
   * @return decrypted data
   */
  public static byte[] decryptRSA ( byte[] encryptedData ) throws Exception
  {
    Cipher cipher = Cipher.getInstance ( "RSA" );
    cipher.init ( Cipher.DECRYPT_MODE, keyPair.getPrivate ( ));
    return cipher.doFinal ( encryptedData );
  }

  /**
   * 📤 Returns this device's public key as a Base64 string (for sharing with peers)
   * @return Base64-encoded public key
   */
  public static String getBase64PublicKey ()
  {
    return Base64.getEncoder ().encodeToString ( keyPair.getPublic ().getEncoded () );
  }

  /**
   * 💾 Saves a peer’s public key to disk (used for future encrypted communication)
   * @param key the peer's public RSA key (base64-encoded)
   */
  public static void savePeerKey ( String key )
  {
    File configDir = new File ( CONFIG_DIR );
    if ( !configDir.exists () )
    {
      configDir.mkdirs ();
    }
    try ( FileWriter writer = new FileWriter ("config/peer_public.key" ) )
    {
      writer.write ( key );
    }
    catch ( IOException e )
    {
      e.printStackTrace ();
    }
  }
}
