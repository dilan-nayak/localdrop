package com.localdrop.service;

import com.localdrop.model.FileMetadata;
import com.localdrop.util.EncryptionUtil;
import com.localdrop.util.RSAUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 📄 FileService.java
 *
 * 🔍 Purpose:
 * Handles file upload, encryption, storage, decryption, download, and metadata listing.
 * Ensures secure communication using AES-256 and RSA-4096 encryption.
 *
 * 🧠 How it works:
 * - Files are encrypted with a new AES key per upload
 * - AES key is encrypted using the recipient's RSA public key
 * - Encrypted file (.data) and encrypted AES key (.key) are stored in /storage
 * - On download, AES key is decrypted using the local private key, and then the file is decrypted
 * - A message is broadcasted via WebSocket when a new file is uploaded (for peer sync)
 *
 * 🔐 Key Concepts:
 * - AES = fast, symmetric encryption for files
 * - RSA = secure key exchange (asymmetric encryption)
 * - MultipartFile = Spring's way to handle file uploads
 * - WebSocket = real-time message broadcasting to peers
 */
@Service
public class FileService
{
    private static final String SHARED_DIR = "storage/";
    private final ClipboardService clipboardService;
    /**
     * 💉 Constructor injection of ClipboardService (used to notify peers after file upload)
     */
    public FileService( ClipboardService clipboardService )
    {
        this.clipboardService = clipboardService;
    }

    /**
     * 📥 Handles the upload and secure storage of a file.
     *
     * @param file the uploaded file
     * @param targetPublicKey the RSA public key of the recipient (used to encrypt AES key)
     * @return upload result message
     */
    public String handleFileUpload( MultipartFile file, String targetPublicKey )
    {
        try {
            // 🔐 Step 1: Generate AES key and encrypt the file content
            byte[] aesKey = EncryptionUtil.generateAESKey();
            byte[] encryptedFile = EncryptionUtil.encryptWithAES( file.getBytes(), aesKey );
            // 🔐 Step 2: Encrypt AES key using peer's RSA public key
            byte[] encryptedKey = RSAUtil.encryptRSA( aesKey, targetPublicKey );

            // 💾 Step 3: Save both encrypted file and encrypted key
            String fileBase = file.getOriginalFilename();
            try ( FileOutputStream fos = new FileOutputStream( SHARED_DIR + fileBase + ".data" ) )
            {
                fos.write(encryptedFile);
            }
            try ( FileOutputStream fos = new FileOutputStream( SHARED_DIR + fileBase + ".key" ) )
            {
                fos.write(encryptedKey);
            }

            // 🔔 Step 4: Notify all peers to refresh their file list
            clipboardService.broadcastClipboard(null, new TextMessage("__REFRESH_FILES__"));


            return "File uploaded and encrypted successfully.";
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return "Upload failed.";
        }
    }

    /**
     * 📤 Decrypts a stored file and returns it as a downloadable resource.
     * Temp decrypted files are created and can be deleted later for security.
     *
     * @param filename the base name of the file (without extensions)
     * @return decrypted file as a Spring Resource
     */
    public Resource getDecryptedFile(String filename) {
        try {
            // 🗂 Read encrypted data and encrypted AES key
            byte[] encryptedFile = Files.readAllBytes( new File( SHARED_DIR + filename + ".data" ).toPath() );
            byte[] encryptedKey = Files.readAllBytes( new File( SHARED_DIR + filename + ".key" ).toPath() );

            // 🔐 Decrypt AES key, then decrypt file
            byte[] aesKey = RSAUtil.decryptRSA( encryptedKey );
            byte[] fileBytes = EncryptionUtil.decryptWithAES( encryptedFile, aesKey );

            // 💾 Write decrypted data to a temporary file
            File decrypted = new File( SHARED_DIR + "temp_" + filename );
            try ( FileOutputStream fos = new FileOutputStream( decrypted ) )
            {
                fos.write( fileBytes );
            }

            return new FileSystemResource( decrypted );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 📃 Lists all encrypted files currently stored in the system.
     * Only shows files with `.data` extension (ignores `.key` and temp files).
     *
     * @return list of file metadata (name, size, last modified)
     */
    public List< FileMetadata > listAllFiles()
    {
        File folder = new File( SHARED_DIR );
        File[] files = folder.listFiles( ( dir, name ) -> name.endsWith( ".data" ) );

        List< FileMetadata > list = new ArrayList<>();
        if ( files != null ) {
            for ( File file : files ) {
                FileMetadata meta = new FileMetadata();
                meta.setName( file.getName().replace( ".data", "" ) );
                meta.setSize( file.length() );
                meta.setUploadTime( file.lastModified() );
                list.add( meta );
            }
        }
        return list;
    }
}
