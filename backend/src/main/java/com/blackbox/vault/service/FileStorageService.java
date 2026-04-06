package com.blackbox.vault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(@Value("${file.upload-dir:/data/uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir);
    }

    public Path store(UUID projectId, String hash, String fileName, InputStream inputStream) throws IOException {
        Path projectDir = rootDir.resolve(projectId.toString());
        Files.createDirectories(projectDir);
        String storedName = hash + "_" + fileName;
        Path target = projectDir.resolve(storedName);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public Path resolve(String storagePath) {
        return Paths.get(storagePath);
    }
}
