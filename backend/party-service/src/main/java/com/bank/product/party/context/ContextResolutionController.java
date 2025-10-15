package com.bank.product.party.context;

import com.bank.product.context.ProcessingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST controller for context resolution.
 *
 * This is THE most critical endpoint in the system architecture.
 * It transforms authentication (WHO) into processing context (WHAT/WHERE).
 *
 * All requests to business services MUST have context resolved through this endpoint.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/context")
@RequiredArgsConstructor
@Slf4j
public class ContextResolutionController {

    private final ContextResolutionService contextResolutionService;

    /**
     * Resolve processing context from principal and party information
     *
     * This endpoint is called by the API Gateway after authentication.
     *
     * @param request Context resolution request
     * @return Complete processing context
     */
    @PostMapping("/resolve")
    public ResponseEntity<ContextResolutionResponse> resolveContext(
            @Valid @RequestBody ContextResolutionRequest request) {

        long startTime = System.currentTimeMillis();

        log.info("Context resolution request received for principal: {}, partyId: {}",
                request.getPrincipalId(), request.getPartyId());

        try {
            // Resolve context
            ProcessingContext context = contextResolutionService.resolveContext(request);

            // Calculate resolution time
            long resolutionTime = System.currentTimeMillis() - startTime;

            // Build response
            ContextResolutionResponse response = ContextResolutionResponse.builder()
                    .context(context)
                    .contextJson(context.toJson())
                    .resolutionTimeMs(resolutionTime)
                    .cached(false) // TODO: detect if served from cache
                    .requestId(context.getRequestId())
                    .build();

            log.info("Context resolved successfully in {}ms for principal: {}, tenant: {}",
                    resolutionTime, request.getPrincipalId(), context.getTenantId());

            return ResponseEntity.ok(response);

        } catch (PartyNotFoundException e) {
            log.error("Party not found during context resolution: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (TenantNotFoundException e) {
            log.error("Tenant not found during context resolution: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (InvalidPartyStateException e) {
            log.error("Invalid party state during context resolution: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("Unexpected error during context resolution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolve party ID from principal ID
     *
     * Utility endpoint for debugging/testing
     *
     * @param principalId Principal ID from authentication
     * @return Party ID
     */
    @GetMapping("/resolve/party/{principalId}")
    public ResponseEntity<String> resolvePartyId(@PathVariable String principalId) {
        log.debug("Resolving party ID for principal: {}", principalId);

        try {
            String partyId = contextResolutionService.resolvePartyIdFromPrincipal(principalId);
            return ResponseEntity.ok(partyId);

        } catch (PartyNotFoundException e) {
            log.error("Party not found for principal: {}", principalId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Resolve tenant ID from party ID
     *
     * Utility endpoint for debugging/testing
     *
     * @param partyId Party ID
     * @return Tenant ID
     */
    @GetMapping("/resolve/tenant/{partyId}")
    public ResponseEntity<String> resolveTenantId(@PathVariable String partyId) {
        log.debug("Resolving tenant ID for party: {}", partyId);

        try {
            String tenantId = contextResolutionService.resolveTenantIdFromParty(partyId);
            return ResponseEntity.ok(tenantId);

        } catch (TenantNotFoundException e) {
            log.error("Tenant not found for party: {}", partyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Invalidate cached context for a party
     *
     * Call this endpoint when party information changes
     *
     * @param partyId Party ID to invalidate
     * @return Success response
     */
    @DeleteMapping("/cache/{partyId}")
    public ResponseEntity<Void> invalidateCache(@PathVariable String partyId) {
        log.info("Invalidating context cache for party: {}", partyId);
        contextResolutionService.invalidateCache(partyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Context Resolution Service is healthy");
    }
}
