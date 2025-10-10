package com.bank.product.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Gateway request metadata for audit and routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRequest {
    private String requestId;
    private Channel channel;
    private String tenantId;
    private String userId;
    private String partyId;
    private String path;
    private String method;
    private Map<String, String> headers;
    private Instant timestamp;
    private String sourceIp;
    private String userAgent;
    
    // For file processing
    private String fileId;
    private String fileName;
    private String fileFormat;
    
    // For versioning
    private String apiVersion;
    
    // For idempotency
    private String idempotencyKey;
}
