package com.bank.product.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * Relationship Context for Party Hierarchies
 *
 * Captures party relationships and hierarchy information from Neo4j graph.
 * Used for "Manages On Behalf Of" scenarios and permission inheritance.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelationshipContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Is this request made on behalf of another party?
     */
    @Builder.Default
    private boolean managingOnBehalfOf = false;

    /**
     * List of party IDs this principal can manage
     */
    @Builder.Default
    private Set<String> managedPartyIds = new HashSet<>();

    /**
     * Parent entity in the party hierarchy
     */
    private String parentEntityId;

    /**
     * Parent entity name (for audit/logging)
     */
    private String parentEntityName;

    /**
     * Full hierarchy path from root to current party
     * Example: ["root-entity-id", "subsidiary-1-id", "current-party-id"]
     */
    @Builder.Default
    private List<String> hierarchyPath = new ArrayList<>();

    /**
     * Relationship type to parent
     * Values: SUBSIDIARY, BRANCH, AFFILIATE, CLIENT, AGENT
     */
    private String relationshipType;

    /**
     * Relationship permissions inherited from parent
     */
    @Builder.Default
    private Set<String> inheritedPermissions = new HashSet<>();

    /**
     * Additional relationship metadata
     */
    @Builder.Default
    private Map<String, String> relationshipMetadata = new HashMap<>();

    /**
     * Get depth in hierarchy (0 = root)
     */
    public int getHierarchyDepth() {
        return hierarchyPath.size() - 1;
    }

    /**
     * Check if this is a root entity
     */
    public boolean isRootEntity() {
        return parentEntityId == null || hierarchyPath.size() <= 1;
    }

    /**
     * Check if can manage specific party
     */
    public boolean canManage(String targetPartyId) {
        return managedPartyIds.contains(targetPartyId);
    }

    /**
     * Add managed party
     */
    public void addManagedParty(String partyId) {
        if (managedPartyIds == null) {
            managedPartyIds = new HashSet<>();
        }
        managedPartyIds.add(partyId);
        managingOnBehalfOf = !managedPartyIds.isEmpty();
    }

    /**
     * Create empty relationship context
     */
    public static RelationshipContext empty() {
        return RelationshipContext.builder()
                .managingOnBehalfOf(false)
                .managedPartyIds(new HashSet<>())
                .hierarchyPath(new ArrayList<>())
                .inheritedPermissions(new HashSet<>())
                .relationshipMetadata(new HashMap<>())
                .build();
    }
}
