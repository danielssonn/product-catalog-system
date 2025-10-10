package com.bank.product.gateway.controller;

import com.bank.product.gateway.model.Channel;
import com.bank.product.gateway.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller for Host-to-Host file processing channel
 * Handles file upload, transformation, and async processing
 */
@Slf4j
@RestController
@RequestMapping("/channel/host-to-host/files")
@RequiredArgsConstructor
public class FileProcessingController {

    private final FileProcessingService fileProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Map<String, Object>> uploadFile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-File-Format", defaultValue = "CSV") String fileFormat,
            @RequestHeader(value = "X-Callback-URL", required = false) String callbackUrl,
            @RequestPart("file") FilePart filePart) {
        
        log.info("File upload: tenantId={}, userId={}, filename={}, format={}", 
            tenantId, userId, filePart.filename(), fileFormat);
        
        return fileProcessingService.processFile(tenantId, userId, filePart, fileFormat, callbackUrl)
            .map(fileId -> Map.of(
                "fileId", fileId,
                "status", "PROCESSING",
                "message", "File uploaded successfully and processing started",
                "callbackConfigured", callbackUrl != null
            ));
    }

    @GetMapping("/{fileId}/status")
    public Mono<Map<String, Object>> getFileStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String fileId) {
        
        log.info("File status check: tenantId={}, fileId={}", tenantId, fileId);
        
        return fileProcessingService.getFileStatus(tenantId, fileId);
    }

    @GetMapping("/{fileId}/results")
    public Mono<Map<String, Object>> getFileResults(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String fileId) {
        
        log.info("File results: tenantId={}, fileId={}", tenantId, fileId);
        
        return fileProcessingService.getFileResults(tenantId, fileId);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteFile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String fileId) {
        
        log.info("File delete: tenantId={}, fileId={}", tenantId, fileId);
        
        return fileProcessingService.deleteFile(tenantId, fileId);
    }
}
