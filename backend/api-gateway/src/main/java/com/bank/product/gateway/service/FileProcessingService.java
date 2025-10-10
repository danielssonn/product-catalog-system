package com.bank.product.gateway.service;

import com.bank.product.gateway.client.ProductServiceClient;
import com.bank.product.gateway.dto.ProductConfigurationRecord;
import com.bank.product.gateway.model.FileUpload;
import com.bank.product.gateway.parser.FileParser;
import com.bank.product.gateway.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for processing files from Host-to-Host channel
 * Handles CSV, JSON, and other file formats containing product configurations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingService {

    private final List<FileParser> fileParsers;
    private final FileUploadRepository fileUploadRepository;
    private final ProductServiceClient productServiceClient;

    /**
     * Process uploaded file asynchronously
     */
    public Mono<String> processFile(String tenantId, String userId, FilePart filePart,
                                     String fileFormat, String callbackUrl) {
        String fileId = UUID.randomUUID().toString();

        log.info("Processing file: fileId={}, tenantId={}, filename={}, format={}",
                fileId, tenantId, filePart.filename(), fileFormat);

        // Create file upload record
        FileUpload fileUpload = FileUpload.builder()
                .id(fileId)
                .fileName(fileId)
                .originalFileName(filePart.filename())
                .contentType(String.valueOf(filePart.headers().getContentType()))
                .tenantId(tenantId)
                .userId(userId)
                .status(FileUpload.FileProcessingStatus.UPLOADED)
                .callbackUrl(callbackUrl)
                .callbackSent(false)
                .uploadedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .errors(new ArrayList<>())
                .createdSolutionIds(new ArrayList<>())
                .build();

        // Determine file format
        fileUpload.setFileFormat(determineFileFormat(filePart.filename(), fileFormat));

        // Save initial record
        return fileUploadRepository.save(fileUpload)
                .flatMap(saved -> {
                    // Start async processing
                    processFileAsync(saved, filePart, tenantId, userId);
                    return Mono.just(saved.getId());
                });
    }

    /**
     * Process file asynchronously in the background
     */
    @Async
    protected void processFileAsync(FileUpload fileUpload, FilePart filePart, String tenantId, String userId) {
        log.info("Starting async processing for file: {}", fileUpload.getId());

        fileUpload.setStatus(FileUpload.FileProcessingStatus.VALIDATING);
        fileUpload.setProcessingStartedAt(LocalDateTime.now());
        fileUploadRepository.save(fileUpload).subscribe();

        try {
            // Find appropriate parser
            FileParser parser = findParser(fileUpload.getContentType(), fileUpload.getOriginalFileName());
            if (parser == null) {
                throw new IllegalArgumentException("No parser found for file type: " + fileUpload.getContentType());
            }

            // Convert FilePart to InputStream using piped streams
            PipedInputStream inputStream = new PipedInputStream();
            PipedOutputStream outputStream = new PipedOutputStream(inputStream);

            // Write FilePart content to piped output stream in separate thread
            new Thread(() -> {
                try {
                    filePart.content()
                            .doOnNext(dataBuffer -> {
                                try {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    outputStream.write(bytes);
                                } catch (IOException e) {
                                    log.error("Error writing to pipe", e);
                                }
                            })
                            .doFinally(signalType -> {
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    log.error("Error closing output stream", e);
                                }
                            })
                            .subscribe();
                } catch (Exception e) {
                    log.error("Error processing file part", e);
                }
            }).start();

            // Parse file
            Flux<ProductConfigurationRecord> records = parser.parse(inputStream);

            // Process records
            processRecords(fileUpload, records, tenantId, userId)
                    .doFinally(signalType -> log.info("File processing completed: {}", fileUpload.getId()))
                    .doOnError(error -> handleProcessingError(fileUpload, error))
                    .subscribe();

        } catch (Exception e) {
            handleProcessingError(fileUpload, e);
        }
    }

    /**
     * Process individual records
     */
    private Mono<Void> processRecords(FileUpload fileUpload, Flux<ProductConfigurationRecord> records,
                                       String tenantId, String userId) {

        fileUpload.setStatus(FileUpload.FileProcessingStatus.PROCESSING);
        fileUploadRepository.save(fileUpload).subscribe();

        AtomicInteger totalRecords = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> createdSolutionIds = new ArrayList<>();
        List<FileUpload.FileProcessingError> errors = new ArrayList<>();

        return records
                .flatMap(record -> {
                    totalRecords.incrementAndGet();

                    return productServiceClient.configureSolution(tenantId, userId, record)
                            .doOnSuccess(response -> {
                                successCount.incrementAndGet();
                                createdSolutionIds.add(response.getSolutionId());
                                log.debug("Record {} processed successfully: solutionId={}",
                                        record.getLineNumber(), response.getSolutionId());
                            })
                            .onErrorResume(error -> {
                                failureCount.incrementAndGet();
                                FileUpload.FileProcessingError processingError = FileUpload.FileProcessingError.builder()
                                        .lineNumber(record.getLineNumber())
                                        .recordId(record.getRecordId())
                                        .errorCode("PROCESSING_ERROR")
                                        .errorMessage(error.getMessage())
                                        .build();
                                errors.add(processingError);
                                log.error("Failed to process record at line {}: {}",
                                        record.getLineNumber(), error.getMessage());
                                return Mono.empty(); // Continue processing other records
                            });
                })
                .then(Mono.defer(() -> {
                    // Update final status
                    fileUpload.setTotalRecords(totalRecords.get());
                    fileUpload.setProcessedRecords(totalRecords.get());
                    fileUpload.setSuccessfulRecords(successCount.get());
                    fileUpload.setFailedRecords(failureCount.get());
                    fileUpload.setErrors(errors);
                    fileUpload.setCreatedSolutionIds(createdSolutionIds);
                    fileUpload.setProcessingCompletedAt(LocalDateTime.now());
                    fileUpload.setUpdatedAt(LocalDateTime.now());

                    if (failureCount.get() == 0) {
                        fileUpload.setStatus(FileUpload.FileProcessingStatus.COMPLETED);
                        fileUpload.setStatusMessage("All records processed successfully");
                    } else if (successCount.get() > 0) {
                        fileUpload.setStatus(FileUpload.FileProcessingStatus.COMPLETED_WITH_ERRORS);
                        fileUpload.setStatusMessage(String.format("%d successful, %d failed",
                                successCount.get(), failureCount.get()));
                    } else {
                        fileUpload.setStatus(FileUpload.FileProcessingStatus.FAILED);
                        fileUpload.setStatusMessage("All records failed");
                    }

                    return fileUploadRepository.save(fileUpload).then();
                }));
    }

    private void handleProcessingError(FileUpload fileUpload, Throwable error) {
        log.error("File processing failed: {}", fileUpload.getId(), error);

        fileUpload.setStatus(FileUpload.FileProcessingStatus.FAILED);
        fileUpload.setStatusMessage(error.getMessage());
        fileUpload.setProcessingCompletedAt(LocalDateTime.now());
        fileUpload.setUpdatedAt(LocalDateTime.now());

        fileUploadRepository.save(fileUpload).subscribe();
    }

    private FileParser findParser(String contentType, String fileName) {
        return fileParsers.stream()
                .filter(parser -> parser.canParse(contentType, fileName))
                .findFirst()
                .orElse(null);
    }

    private FileUpload.FileFormat determineFileFormat(String filename, String requestedFormat) {
        if (requestedFormat != null) {
            try {
                return FileUpload.FileFormat.valueOf(requestedFormat.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid format requested: {}", requestedFormat);
            }
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".csv")) {
            return FileUpload.FileFormat.CSV;
        } else if (lowerFilename.endsWith(".json")) {
            return FileUpload.FileFormat.JSON;
        } else if (lowerFilename.endsWith(".xml")) {
            return FileUpload.FileFormat.ISO20022;
        }

        return FileUpload.FileFormat.CSV; // Default
    }

    /**
     * Get file processing status
     */
    public Mono<Map<String, Object>> getFileStatus(String tenantId, String fileId) {
        log.debug("Getting file status: tenantId={}, fileId={}", tenantId, fileId);

        return fileUploadRepository.findById(fileId)
                .filter(file -> file.getTenantId().equals(tenantId))
                .map(file -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("fileId", file.getId());
                    result.put("fileName", file.getOriginalFileName());
                    result.put("status", file.getStatus().name());
                    result.put("statusMessage", file.getStatusMessage() != null ? file.getStatusMessage() : "");
                    result.put("recordsTotal", file.getTotalRecords() != null ? file.getTotalRecords() : 0);
                    result.put("recordsProcessed", file.getProcessedRecords() != null ? file.getProcessedRecords() : 0);
                    result.put("recordsSucceeded", file.getSuccessfulRecords() != null ? file.getSuccessfulRecords() : 0);
                    result.put("recordsFailed", file.getFailedRecords() != null ? file.getFailedRecords() : 0);
                    result.put("uploadedAt", file.getUploadedAt());
                    result.put("processingStartedAt", file.getProcessingStartedAt());
                    result.put("processingCompletedAt", file.getProcessingCompletedAt());
                    return result;
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("File not found: " + fileId)));
    }

    /**
     * Get file processing results
     */
    public Mono<Map<String, Object>> getFileResults(String tenantId, String fileId) {
        log.debug("Getting file results: tenantId={}, fileId={}", tenantId, fileId);

        return fileUploadRepository.findById(fileId)
                .filter(file -> file.getTenantId().equals(tenantId))
                .map(file -> Map.of(
                        "fileId", file.getId(),
                        "fileName", file.getOriginalFileName(),
                        "status", file.getStatus().name(),
                        "recordsSucceeded", file.getSuccessfulRecords() != null ? file.getSuccessfulRecords() : 0,
                        "recordsFailed", file.getFailedRecords() != null ? file.getFailedRecords() : 0,
                        "createdSolutionIds", file.getCreatedSolutionIds() != null ? file.getCreatedSolutionIds() : List.of(),
                        "errors", file.getErrors() != null ? file.getErrors() : List.of()
                ))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("File not found: " + fileId)));
    }

    /**
     * Delete file and results
     */
    public Mono<Void> deleteFile(String tenantId, String fileId) {
        log.info("Deleting file: tenantId={}, fileId={}", tenantId, fileId);

        return fileUploadRepository.findById(fileId)
                .filter(file -> file.getTenantId().equals(tenantId))
                .flatMap(file -> fileUploadRepository.delete(file))
                .then();
    }
}
