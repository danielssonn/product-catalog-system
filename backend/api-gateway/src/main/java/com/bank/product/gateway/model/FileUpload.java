package com.bank.product.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * File upload metadata stored in MongoDB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_uploads")
public class FileUpload {

    @Id
    private String id;

    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private FileFormat fileFormat;

    private String tenantId;
    private String userId;
    private Channel channel;

    private FileProcessingStatus status;
    private String statusMessage;

    private Integer totalRecords;
    private Integer processedRecords;
    private Integer successfulRecords;
    private Integer failedRecords;

    private List<FileProcessingError> errors;
    private List<String> createdSolutionIds;

    private String callbackUrl;
    private Boolean callbackSent;

    private LocalDateTime uploadedAt;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum FileProcessingStatus {
        UPLOADED,
        VALIDATING,
        VALIDATION_FAILED,
        PROCESSING,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }

    public enum FileFormat {
        CSV,
        FIXED_WIDTH,
        JSON,
        ISO20022
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileProcessingError {
        private Integer lineNumber;
        private String recordId;
        private String errorCode;
        private String errorMessage;
        private String fieldName;
    }
}
