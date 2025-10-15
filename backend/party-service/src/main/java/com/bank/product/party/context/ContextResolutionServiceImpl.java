package com.bank.product.party.context;

import com.bank.product.context.*;
import com.bank.product.party.domain.*;
import com.bank.product.party.repository.OrganizationRepository;
import com.bank.product.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of context resolution service.
 *
 * This service is THE foundation of the multi-tenant architecture.
 * It transforms authentication (WHO) into processing context (WHAT/WHERE).
 *
 * Resolution Logic:
 * 1. Principal ID → Party ID (from source records or direct mapping)
 * 2. Party ID → Tenant ID (from party metadata/organization)
 * 3. Build complete context (permissions, jurisdiction, relationships)
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextResolutionServiceImpl implements ContextResolutionService {

    private final PartyRepository partyRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Resolve complete processing context (with caching)
     * Cache key: principalId:partyId
     * TTL: 5 minutes (configured in application.yml)
     */
    @Override
    @Cacheable(value = "context", key = "#request.principalId + ':' + #request.partyId", unless = "#result == null")
    public ProcessingContext resolveContext(ContextResolutionRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Resolving context for principal: {}, partyId: {}",
                request.getPrincipalId(), request.getPartyId());

        // Step 1: Resolve party ID from principal if not provided
        final String resolvedPartyId = (request.getPartyId() != null && !request.getPartyId().isBlank())
                ? request.getPartyId()
                : resolvePartyIdFromPrincipal(request.getPrincipalId());

        // Step 2: Load party entity from graph
        Party party = partyRepository.findByFederatedId(resolvedPartyId)
                .orElseThrow(() -> new PartyNotFoundException(
                        "Party not found: " + resolvedPartyId));

        // Step 3: Validate party status
        if (party.getStatus() != PartyStatus.ACTIVE) {
            throw new InvalidPartyStateException(
                    "Party is not active: " + resolvedPartyId + ", status: " + party.getStatus());
        }

        // Step 4: Resolve tenant from party
        String tenantId = resolveTenantIdFromParty(resolvedPartyId);

        // Step 5: Build complete processing context
        ProcessingContext context = buildProcessingContext(request, party, tenantId);

        long resolutionTime = System.currentTimeMillis() - startTime;
        log.info("Context resolved in {}ms for party: {}, tenant: {}",
                resolutionTime, resolvedPartyId, tenantId);

        return context;
    }

    @Override
    public String resolvePartyIdFromPrincipal(String principalId) {
        log.debug("Resolving party ID from principal: {}", principalId);

        // Strategy 1: Try to find party by source record (principal as source ID)
        // This assumes principals are mapped in source records with sourceSystem = "AUTH_SERVICE"
        Optional<Party> partyByAuth = partyRepository.findBySourceSystemAndSourceId(
                "AUTH_SERVICE", principalId);

        if (partyByAuth.isPresent()) {
            return partyByAuth.get().getFederatedId();
        }

        // Strategy 2: For demo/testing, use principalId as partyId directly
        // In production, you would have a dedicated PrincipalPartyMapping table
        Optional<Party> partyById = partyRepository.findByFederatedId(principalId);
        if (partyById.isPresent()) {
            return principalId;
        }

        throw new PartyNotFoundException(
                "No party mapping found for principal: " + principalId +
                        ". Please ensure principal is linked to a party via SourceRecord or direct mapping.");
    }

    @Override
    public String resolveTenantIdFromParty(String partyId) {
        log.debug("Resolving tenant ID from party: {}", partyId);

        Party party = partyRepository.findByFederatedId(partyId)
                .orElseThrow(() -> new PartyNotFoundException("Party not found: " + partyId));

        // Tenant resolution logic based on party type
        if (party instanceof Organization org) {
            // For organizations, use a tenant mapping strategy:
            // 1. If the organization has a parent, tenant = parent's federatedId
            // 2. If the organization is top-level, tenant = organization's federatedId
            // 3. Use domain/jurisdiction for multi-org tenants (future enhancement)

            String tenantId = resolveTenantFromOrganization(org);
            if (tenantId != null) {
                return tenantId;
            }
        } else if (party instanceof LegalEntity legalEntity) {
            // For legal entities, tenant = parent organization
            // Find the organization that owns this legal entity
            Optional<Party> parentOrg = partyRepository.findParentOrganization(legalEntity.getFederatedId());
            if (parentOrg.isPresent() && parentOrg.get() instanceof Organization org) {
                String tenantId = resolveTenantFromOrganization(org);
                if (tenantId != null) {
                    log.debug("Resolved tenant {} for legal entity {} via parent organization",
                            tenantId, legalEntity.getFederatedId());
                    return tenantId;
                }
            }
            // Fallback: use legal entity's federatedId
            log.info("No parent organization found for legal entity {}, using its federatedId as tenant",
                    legalEntity.getFederatedId());
            return legalEntity.getFederatedId();
        } else if (party instanceof Individual individual) {
            // For individuals, tenant = organization they're employed by
            // Find the organization via EMPLOYED_BY relationship
            Optional<Party> employerOrg = partyRepository.findEmployerOrganization(individual.getFederatedId());
            if (employerOrg.isPresent() && employerOrg.get() instanceof Organization org) {
                String tenantId = resolveTenantFromOrganization(org);
                if (tenantId != null) {
                    log.debug("Resolved tenant {} for individual {} via employer organization",
                            tenantId, individual.getFederatedId());
                    return tenantId;
                }
            }
            // Fallback: use individual's federatedId (for individual tenants)
            log.info("No employer organization found for individual {}, using individual as tenant",
                    individual.getFederatedId());
            return individual.getFederatedId();
        }

        // Fallback: use party ID as tenant ID
        log.warn("Using party ID as tenant ID (fallback): {}", partyId);
        return partyId;
    }

    @Override
    @CacheEvict(value = "context", allEntries = true)
    public void invalidateCache(String partyId) {
        log.info("Invalidating context cache for party: {}", partyId);
    }

    /**
     * Build complete processing context from party and tenant
     */
    private ProcessingContext buildProcessingContext(
            ContextResolutionRequest request,
            Party party,
            String tenantId) {

        // Generate request ID if not provided
        String requestId = request.getRequestId() != null ?
                request.getRequestId() : UUID.randomUUID().toString();

        ProcessingContext.ProcessingContextBuilder builder = ProcessingContext.builder()
                .requestId(requestId)
                .contextResolvedAt(Instant.now())
                .resolutionSource("party-service")
                .principalId(request.getPrincipalId())
                .principalUsername(request.getUsername())
                .principalRoles(request.getRoles() != null ?
                        Set.of(request.getRoles()) : Collections.emptySet())
                .channelId(request.getChannelId() != null ? request.getChannelId() : "UNKNOWN")
                .partyId(party.getFederatedId())
                .tenantId(tenantId);

        // Add party-specific information
        if (party instanceof Organization org) {
            builder.partyName(org.getName())
                    .partyType("ORGANIZATION")
                    .legalEntityId(org.getLei())
                    .partyStatus(org.getStatus().toString())
                    .jurisdictionCountry(extractCountryFromJurisdiction(org.getJurisdiction()))
                    .processingRegion(extractRegionFromJurisdiction(org.getJurisdiction()));

            // Tenant information
            builder.tenantName(org.getName())
                    .tenantType("COMMERCIAL_BANKING");

            // Jurisdiction and regulatory
            Set<String> regulatory = determineRegulatoryFrameworks(org.getJurisdiction());
            builder.regulatoryFrameworks(regulatory);

            if (!regulatory.isEmpty()) {
                builder.complianceTags(Set.of(regulatory.iterator().next() + "_COMPLIANT"));
            }

            // Relationships
            builder.relationshipContext(buildRelationshipContext(org));

            // Permissions
            builder.permissions(buildPermissionContext(org, request.getRoles()));

            // Core banking
            builder.coreSystemType(determineCoreSystem(org))
                    .coreSystemEndpoint(determineCoreEndpoint(org));

        } else if (party instanceof LegalEntity legalEntity) {
            builder.partyName(legalEntity.getLegalName())
                    .partyType("LEGAL_ENTITY")
                    .legalEntityId(legalEntity.getLei())
                    .partyStatus(legalEntity.getStatus().toString());

        } else if (party instanceof Individual individual) {
            builder.partyName(individual.getFirstName() + " " + individual.getLastName())
                    .partyType("INDIVIDUAL")
                    .partyStatus(individual.getStatus().toString());
        }

        return builder.build();
    }

    /**
     * Resolve tenant from organization hierarchy
     */
    private String resolveTenantFromOrganization(Organization org) {
        // If organization has a parent, walk up to find the top-level organization
        if (org.getParent() != null && org.getParent().getTarget() != null) {
            Party parent = org.getParent().getTarget();
            if (parent instanceof Organization parentOrg) {
                // Recursively find top-level organization
                return resolveTenantFromOrganization(parentOrg);
            }
        }

        // This is a top-level organization - use its federatedId as tenant
        return org.getFederatedId();
    }

    /**
     * Build relationship context from organization
     */
    private RelationshipContext buildRelationshipContext(Organization org) {
        RelationshipContext.RelationshipContextBuilder builder = RelationshipContext.builder();

        // Check if managing on behalf of others
        if (org.getManagesFor() != null && !org.getManagesFor().isEmpty()) {
            builder.managingOnBehalfOf(true);

            Set<String> managedPartyIds = org.getManagesFor().stream()
                    .map(ManagesOnBehalfOfRelationship::getPrincipal)
                    .filter(Objects::nonNull)
                    .map(Party::getFederatedId)
                    .collect(Collectors.toSet());

            builder.managedPartyIds(managedPartyIds);
        } else {
            builder.managingOnBehalfOf(false);
        }

        // Build hierarchy path
        List<String> hierarchyPath = buildHierarchyPath(org);
        builder.hierarchyPath(hierarchyPath);

        // Determine relationship type
        if (org.getParent() != null) {
            builder.relationshipType("SUBSIDIARY");
            if (org.getParent().getTarget() != null) {
                builder.parentEntityId(org.getParent().getTarget().getFederatedId());
            }
        } else {
            builder.relationshipType("TOP_LEVEL");
        }

        return builder.build();
    }

    /**
     * Build hierarchy path from root to current organization
     */
    private List<String> buildHierarchyPath(Organization org) {
        List<String> path = new ArrayList<>();
        Organization current = org;

        // Walk up the parent chain
        while (current != null) {
            path.add(0, current.getFederatedId()); // Add to front

            if (current.getParent() != null && current.getParent().getTarget() != null) {
                Party parent = current.getParent().getTarget();
                if (parent instanceof Organization parentOrg) {
                    current = parentOrg;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return path;
    }

    /**
     * Build permission context based on party and roles
     */
    private PermissionContext buildPermissionContext(Organization org, String[] roles) {
        // Start with default permissions
        PermissionContext permissions = PermissionContext.createDefault();

        // Enhance based on roles
        if (roles != null) {
            Set<String> roleSet = Set.of(roles);
            if (roleSet.contains("ROLE_ADMIN") || roleSet.contains("ADMIN")) {
                permissions = PermissionContext.createAdmin();
            }
        }

        // Enhance based on organization tier
        if ("TIER_1".equals(org.getTier())) {
            permissions.setCanOpenAccounts(true);
            permissions.setCanInitiatePayments(true);
            permissions.setMaxTransactionLimit(new BigDecimal("10000000")); // $10M
            permissions.setDailyTransactionLimit(new BigDecimal("50000000")); // $50M
        } else if ("TIER_2".equals(org.getTier())) {
            permissions.setMaxTransactionLimit(new BigDecimal("1000000")); // $1M
            permissions.setDailyTransactionLimit(new BigDecimal("5000000")); // $5M
        }

        // Add approved product types based on organization type
        permissions.setApprovedProductTypes(Set.of(
                "CHECKING", "SAVINGS", "LOAN", "CREDIT_LINE"
        ));

        // Jurisdiction-based restrictions
        if (org.getJurisdiction() != null) {
            permissions.setAllowedCountries(Set.of(extractCountryFromJurisdiction(org.getJurisdiction())));
        }

        return permissions;
    }

    /**
     * Extract country code from jurisdiction string
     */
    private String extractCountryFromJurisdiction(String jurisdiction) {
        if (jurisdiction == null) {
            return "US"; // default
        }

        // Simple extraction: "Delaware, USA" → "US"
        if (jurisdiction.contains("USA") || jurisdiction.contains("United States")) {
            return "US";
        } else if (jurisdiction.contains("UK") || jurisdiction.contains("United Kingdom")) {
            return "GB";
        } else if (jurisdiction.contains("Canada")) {
            return "CA";
        } else if (jurisdiction.contains("Singapore")) {
            return "SG";
        }

        return "US"; // default
    }

    /**
     * Extract region from jurisdiction
     */
    private String extractRegionFromJurisdiction(String jurisdiction) {
        if (jurisdiction == null) {
            return "US-EAST";
        }

        String country = extractCountryFromJurisdiction(jurisdiction);
        return switch (country) {
            case "US" -> "US-EAST";
            case "GB" -> "EU-WEST";
            case "CA" -> "CA-CENTRAL";
            case "SG" -> "APAC-SOUTHEAST";
            default -> "US-EAST";
        };
    }

    /**
     * Determine regulatory frameworks based on jurisdiction
     */
    private Set<String> determineRegulatoryFrameworks(String jurisdiction) {
        if (jurisdiction == null) {
            return Set.of("US_FEDERAL");
        }

        String country = extractCountryFromJurisdiction(jurisdiction);
        return switch (country) {
            case "US" -> Set.of("US_FEDERAL", "FDIC", "OCC");
            case "GB" -> Set.of("FCA", "PRA", "GDPR");
            case "CA" -> Set.of("OSFI", "CDIC");
            case "SG" -> Set.of("MAS", "SGDPA");
            default -> Set.of("US_FEDERAL");
        };
    }

    /**
     * Determine core banking system for organization
     */
    private String determineCoreSystem(Organization org) {
        // Logic: determine based on jurisdiction or organization metadata
        String country = extractCountryFromJurisdiction(org.getJurisdiction());
        return switch (country) {
            case "US" -> "TEMENOS_US";
            case "GB" -> "TEMENOS_UK";
            case "CA" -> "FINASTRA_CA";
            case "SG" -> "TEMENOS_APAC";
            default -> "TEMENOS_US";
        };
    }

    /**
     * Determine core banking endpoint for organization
     */
    private String determineCoreEndpoint(Organization org) {
        String country = extractCountryFromJurisdiction(org.getJurisdiction());
        return switch (country) {
            case "US" -> "http://mock-core-api:3000";
            case "GB" -> "http://mock-core-api:3001";
            case "SG" -> "http://mock-core-api:3002";
            default -> "http://mock-core-api:3000";
        };
    }
}
