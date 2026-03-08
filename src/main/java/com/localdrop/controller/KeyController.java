package com.localdrop.controller;

import com.localdrop.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 📄 KeyController.java
 *
 * 🔍 Purpose:
 * This controller handles public key exchange between devices.
 * It is essential for enabling encrypted communication using RSA, by sharing public keys between peers.
 *
 * 🧠 How it works:
 * - Devices generate an RSA key pair on startup (private + public key)
 * - The public key is shared with peers via this controller
 * - Received keys are stored temporarily in memory for secure communication
 *
 * 🔧 Key Concepts:
 * - @RestController: Marks this as a Spring MVC REST controller
 * - @RequestMapping: Sets the base path for this controller as /api/keys
 * - @Autowired: Automatically injects the EncryptionService bean
 * - @RequestBody: Maps raw POST body into a Java String (the peer's public key)
 */
@RestController
@RequestMapping("/api/keys")
public class KeyController
{
    /**
     * 💡 This service holds the current device's key pair and manages peer keys
     */
    @Autowired
    private EncryptionService encryptionService;

    /**
     * 🔓 Get this device’s public RSA key
     * @return public key string (to be shared with other peers)
     */
    @GetMapping("/public")
    public String getPublicKey()
    {
        return encryptionService.getPublicKey();
    }

    /**
     * 🔑 Receive another device’s public RSA key
     * @param otherKey - the RSA public key sent by another peer
     * This is used to encrypt AES keys for sending files/messages to that peer
     */
    @PostMapping("/receive")
    public void receivePublicKey(@RequestBody String otherKey)
    {
        encryptionService.storePeerPublicKey(otherKey);
    }
}
