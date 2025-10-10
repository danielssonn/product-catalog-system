package com.bank.product.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Audit log for all API requests through the gateway
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_timestamp_idx", def = "{'tenantId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "channel_timestamp_idx", def = "{'channel': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "user_timestamp_idx", def = "{'userId': 1, 'timestamp': -1}")
})
public class ApiAuditLog {
    @Id
    private String id;
    
    @Indexed
    private String requestId;
    
    @Indexed
    private String tenantId;
    
    @Indexed
    private String userId;
    
    private String partyId;
    
    @Indexed
    private Channel channel;
    
    private String path;
    private String method;
    private String apiVersion;
    
    @Indexed
    private Instant timestamp;
    
    private Integer statusCode;
    private Long durationMs;
    
    private String sourceIp;
    private String userAgent;
    
    // For errors
    private String errorMessage;
    private String errorCode;
    
    // For rate limiting
    private Boolean rateLimited;
    
    // For circuit breaker
    private Boolean circuitBreakerTriggered;
    
    // For file processing
    private String fileId;
    private String fileName;
    
    // For idempotency
    private String idempotencyKey;
    private Boolean idempotentHit;
}
