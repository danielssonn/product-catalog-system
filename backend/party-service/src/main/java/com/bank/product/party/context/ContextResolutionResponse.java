package com.bank.product.party.context;

import com.bank.product.context.ProcessingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for context resolution endpoint.
 *
 * Contains the fully resolved processing context plus metadata about the resolution.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextResolutionResponse {

    /**
     * The fully resolved processing context
     */
    private ProcessingContext context;

    /**
     * Context as JSON string (for convenience)
     */
    private String contextJson;

    /**
     * Resolution time in milliseconds
     */
    private long resolutionTimeMs;

    /**
     * Whether context was served from cache
     */
    private boolean cached;

    /**
     * Request ID for correlation
     */
    private String requestId;
}
