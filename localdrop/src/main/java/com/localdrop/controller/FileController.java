package com.localdrop.controller;

import com.localdrop.model.FileMetadata;
import com.localdrop.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 📄 FileController.java
 *
 * 🔍 Purpose:
 * This controller handles all HTTP requests related to file upload, listing, and download.
 * It acts as a bridge between frontend requests and backend business logic defined in FileService.
 *
 * 🧠 How it works:
 * - Accepts file uploads from clients and forwards them for encryption and storage
 * - Lists available encrypted files stored in the system
 * - Allows download of decrypted files via secure endpoints
 *
 * 🔧 Key Concepts:
 * - @RestController: Tells Spring this class handles REST API endpoints
 * - @RequestMapping: Base path prefix for all endpoints in this controller
 * - MultipartFile: Spring's way of handling file uploads in forms
 * - ResponseEntity: Used to return full HTTP responses including headers and status
 */

@RestController
@RequestMapping("/api/files")
public class FileController
{
    /**
     * 💡 Injecting FileService to delegate file handling tasks
     * @Autowired tells Spring to automatically inject the required bean
     */
    @Autowired
    private FileService fileService;

    /**
     * 📥 Endpoint to upload a file securely
     * @param file - the uploaded file
     * @param targetPublicKey - recipient's public RSA key used for AES key encryption
     * @return HTTP response with upload status message
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile( @RequestParam( "file" ) MultipartFile file,
                                             @RequestParam( "targetPublicKey" ) String targetPublicKey )
    {
        return ResponseEntity.ok(fileService.handleFileUpload(file, targetPublicKey));
    }

    /**
     * 📂 Endpoint to list all available encrypted files
     * @return List of FileMetadata objects containing file name, size, and timestamp
     */
    @GetMapping("/list")
    public List<FileMetadata> listFiles() {
        return fileService.listAllFiles();
    }

    /**
     * 📤 Endpoint to download a file (after decryption)
     * @param filename - file name without extension
     * @return HTTP response containing the decrypted file as a downloadable resource
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileService.getDecryptedFile(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
