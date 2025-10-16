package com.bank.product.party.context;

import com.bank.product.context.ProcessingContext;

/**
 * Service interface for resolving processing context from principal and party information.
 *
 * This is THE core service that transforms authentication information (WHO)
 * into complete processing context (WHAT/WHERE) for business transactions.
 *
 * Context Resolution Chain:
 * 1. Principal ID → Party ID (from user-party mapping)
 * 2. Party ID → Tenant ID (from party metadata)
 * 3. Party + Tenant → Full Context (jurisdiction, permissions, relationships)
 *
 * @author System Architecture Team
 * @since 1.0
 */
public interface ContextResolutionService {

    /**
     * Resolve complete processing context from request
     *
     * @param request Context resolution request containing principal and optional party ID
     * @return Fully resolved processing context
     * @throws PartyNotFoundException if party cannot be found or resolved
     * @throws TenantNotFoundException if tenant cannot be resolved from party
     * @throws InvalidPartyStateException if party is inactive or invalid
     */
    ProcessingContext resolveContext(ContextResolutionRequest request);

    /**
     * Resolve party ID from principal ID
     *
     * @param principalId Principal ID from authentication
     * @return Party ID associated with this principal
     * @throws PartyNotFoundException if no party mapping found
     */
    String resolvePartyIdFromPrincipal(String principalId);

    /**
     * Resolve tenant ID from party ID
     *
     * @param partyId Party ID
     * @return Tenant ID associated with this party
     * @throws TenantNotFoundException if no tenant mapping found
     */
    String resolveTenantIdFromParty(String partyId);

    /**
     * Clear cached context for a party (useful after party updates)
     *
     * @param partyId Party ID to invalidate cache for
     */
    void invalidateCache(String partyId);
}
