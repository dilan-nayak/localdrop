package com.localdrop.service;

import com.localdrop.util.RSAUtil;
import org.springframework.stereotype.Service;

/**
 * 📄 EncryptionService.java
 *
 * 🔍 Purpose:
 * Acts as a service layer for managing RSA key exchange between devices.
 * This service fetches the local device's public key and stores public keys received from peers.
 *
 * 🧠 How it works:
 * - Uses utility methods from `RSAUtil` to handle actual key generation and saving
 * - The service itself focuses on exposing simple methods to the controller
 *
 * 🔧 Key Concepts:
 * - @Service: Marks this class as a Spring service component, making it injectable
 * - Public Key Sharing is part of **asymmetric encryption** (RSA-4096)
 * - LocalDrop uses this for securely encrypting AES session keys between devices
 *
 * 🔐 Key Exchange Flow:
 * 1. Device A sends its public key to Device B via `/api/keys/receive`
 * 2. Device B saves that key for future encrypted communication
 */
@Service
public class EncryptionService
{
    /**
     * 🔑 Fetches this device's public RSA key (base64-encoded)
     * @return Public key string to be shared with peers
     */

    public String getPublicKey() {
        return RSAUtil.getBase64PublicKey();
    }

    /**
     * 📥 Receives and stores a peer's public RSA key
     * @param key - the base64 string of the peer's public key
     */
    public void storePeerPublicKey(String key) {
        RSAUtil.savePeerKey(key);
    }

}
