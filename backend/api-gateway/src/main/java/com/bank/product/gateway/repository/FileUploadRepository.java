package com.bank.product.gateway.repository;

import com.bank.product.gateway.model.FileUpload;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FileUploadRepository extends ReactiveMongoRepository<FileUpload, String> {

    Flux<FileUpload> findByTenantIdOrderByUploadedAtDesc(String tenantId);

    Flux<FileUpload> findByTenantIdAndStatus(String tenantId, FileUpload.FileProcessingStatus status);

    Mono<Long> countByTenantIdAndStatus(String tenantId, FileUpload.FileProcessingStatus status);
}
