package com.civic.crowdcivics.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String uploadDir = "uploads";

    public String saveBase64Image(String base64Image, String prefix) throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Extract Base64 content (handling "data:image/jpeg;base64,..." prefix)
        String base64Content;
        String extension = ".jpg"; // Default extension

        if (base64Image.contains(",")) {
            String[] parts = base64Image.split(",");
            String header = parts[0];
            base64Content = parts[1];
            
            // Try to extract extension from header
            if (header.contains("image/png")) extension = ".png";
            else if (header.contains("image/gif")) extension = ".gif";
            else if (header.contains("image/webp")) extension = ".webp";
        } else {
            base64Content = base64Image;
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        
        // Generate unique filename
        String fileName = (prefix != null ? prefix + "_" : "") + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);

        // Save file
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(decodedBytes);
        }

        // Return relative URL for static resource handler
        return "/uploads/" + fileName;
    }
}
