package com.bank.product.party.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for context resolution endpoint.
 *
 * This is the input to the context resolution process that transforms
 * authentication principal information into a complete processing context.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextResolutionRequest {

    /**
     * Principal ID from authentication service (required)
     * This is the authenticated user/system identifier
     */
    private String principalId;

    /**
     * Username from authentication (optional, for logging)
     */
    private String username;

    /**
     * Roles from authentication (optional)
     */
    private String[] roles;

    /**
     * Channel ID from which request originated (optional)
     * Examples: WEB, MOBILE, HOST_TO_HOST, INTERNAL
     */
    private String channelId;

    /**
     * Optional: Specific party ID to resolve context for
     * If not provided, will be resolved from principalId
     */
    private String partyId;

    /**
     * Request ID for correlation/tracing (optional)
     * If not provided, will be generated
     */
    private String requestId;
}
