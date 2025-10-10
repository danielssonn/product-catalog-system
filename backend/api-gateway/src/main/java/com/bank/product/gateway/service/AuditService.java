package com.bank.product.gateway.service;

import com.bank.product.gateway.model.ApiAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for logging API audit events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final KafkaTemplate<String, ApiAuditLog> kafkaTemplate;

    /**
     * Log API request to MongoDB and Kafka
     */
    public Mono<ApiAuditLog> logRequest(ApiAuditLog auditLog) {
        return mongoTemplate.save(auditLog)
            .doOnSuccess(saved -> {
                // Also publish to Kafka for real-time analytics
                try {
                    kafkaTemplate.send("api-audit-logs", saved.getRequestId(), saved);
                } catch (Exception e) {
                    log.error("Failed to publish audit log to Kafka: {}", e.getMessage());
                }
            })
            .doOnError(error -> log.error("Failed to save audit log: {}", error.getMessage()));
    }
}
