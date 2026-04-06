package com.blackbox.vault.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.vault.dto.FileVaultResponse;
import com.blackbox.vault.service.VaultService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @PostMapping("/api/projects/{projectId}/files")
    public ResponseEntity<ApiResponse<FileVaultResponse>> upload(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file) {
        FileVaultResponse response = vaultService.upload(projectId, user.getUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/projects/{projectId}/files")
    public ResponseEntity<ApiResponse<List<FileVaultResponse>>> getFiles(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<FileVaultResponse> response = vaultService.getFiles(projectId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/projects/{projectId}/files/{vaultId}")
    public ResponseEntity<ApiResponse<FileVaultResponse>> getFile(
            @PathVariable UUID projectId,
            @PathVariable UUID vaultId,
            @AuthenticationPrincipal CustomUserDetails user) {
        FileVaultResponse response = vaultService.getFile(projectId, vaultId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/projects/{projectId}/files/history")
    public ResponseEntity<ApiResponse<List<FileVaultResponse>>> getFileHistory(
            @PathVariable UUID projectId,
            @RequestParam String fileName,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<FileVaultResponse> response = vaultService.getFileHistory(projectId, fileName, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/files/{vaultId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID vaultId) {
        Path filePath = vaultService.getFilePath(vaultId);
        String fileName = vaultService.getFileName(vaultId);
        Resource resource = new FileSystemResource(filePath);

        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
