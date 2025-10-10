package com.bank.product.party.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Top-level business organization in the federated party model.
 * Represents a company or business entity that may have multiple legal entities,
 * subsidiaries, and other organizational components.
 */
@Node("Organization")
@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends Party {

    /**
     * Business name (DBA - Doing Business As)
     */
    private String name;

    /**
     * Legal registered name
     */
    private String legalName;

    /**
     * Business registration/company number
     */
    private String registrationNumber;

    /**
     * Jurisdiction of incorporation
     */
    private String jurisdiction;

    /**
     * Date of incorporation
     */
    private LocalDate incorporationDate;

    /**
     * NAICS industry classification code
     */
    private String industryCode;

    /**
     * Industry description
     */
    private String industry;

    /**
     * Client tier classification (TIER_1, TIER_2, etc.)
     */
    private String tier;

    /**
     * Overall risk rating
     */
    private String riskRating;

    /**
     * AML/KYC status
     */
    private String amlStatus;

    /**
     * Legal Entity Identifier (ISO 17442)
     */
    private String lei;

    /**
     * Website URL
     */
    private String website;

    /**
     * Primary phone number
     */
    private String phoneNumber;

    /**
     * Primary email
     */
    private String email;

    /**
     * Number of employees
     */
    private Integer employeeCount;

    /**
     * Annual revenue (in millions USD)
     */
    private Double annualRevenue;

    // ===== Relationships =====

    /**
     * Subsidiaries owned by this organization
     */
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private List<OwnershipRelationship> subsidiaries = new ArrayList<>();

    /**
     * Parent organization
     */
    @Relationship(type = "SUBSIDIARY_OF", direction = Relationship.Direction.OUTGOING)
    private OwnershipRelationship parent;

    /**
     * Organizations this entity operates on behalf of
     */
    @Relationship(type = "OPERATES_ON_BEHALF_OF", direction = Relationship.Direction.OUTGOING)
    private List<OperationalRelationship> operatesOnBehalfOf = new ArrayList<>();

    /**
     * Organizations that provide services to this organization
     */
    @Relationship(type = "PROVIDES_SERVICES_TO", direction = Relationship.Direction.INCOMING)
    private List<ServiceRelationship> serviceProviders = new ArrayList<>();

    /**
     * Counterparty relationships
     */
    @Relationship(type = "COUNTERPARTY_TO", direction = Relationship.Direction.OUTGOING)
    private List<CounterpartyRelationship> counterparties = new ArrayList<>();

    /**
     * Beneficial owners
     */
    @Relationship(type = "BENEFICIAL_OWNER_OF", direction = Relationship.Direction.INCOMING)
    private List<BeneficialOwnershipRelationship> beneficialOwners = new ArrayList<>();

    /**
     * Legal entities within this organization
     */
    @Relationship(type = "HAS_LEGAL_ENTITY", direction = Relationship.Direction.OUTGOING)
    private List<LegalEntity> legalEntities = new ArrayList<>();

    /**
     * Parties that this organization manages on behalf of (as the manager/agent)
     * Example: Goldman Sachs manages assets for Tesla
     */
    @Relationship(type = "MANAGES_ON_BEHALF_OF", direction = Relationship.Direction.OUTGOING)
    private List<ManagesOnBehalfOfRelationship> managesFor = new ArrayList<>();

    /**
     * Parties that manage on behalf of this organization (this org is the principal/client)
     * Example: Tesla has Goldman Sachs as manager
     */
    @Relationship(type = "MANAGES_ON_BEHALF_OF", direction = Relationship.Direction.INCOMING)
    private List<ManagesOnBehalfOfRelationship> managedBy = new ArrayList<>();

    public Organization() {
        super();
        setPartyType(PartyType.ORGANIZATION);
    }

    public void addSubsidiary(LegalEntity subsidiary, OwnershipRelationship relationship) {
        if (this.subsidiaries == null) {
            this.subsidiaries = new ArrayList<>();
        }
        this.subsidiaries.add(relationship);
    }

    public void addLegalEntity(LegalEntity legalEntity) {
        if (this.legalEntities == null) {
            this.legalEntities = new ArrayList<>();
        }
        this.legalEntities.add(legalEntity);
    }

    /**
     * Calculate total number of subsidiaries recursively
     */
    public int getTotalSubsidiariesCount() {
        int count = subsidiaries != null ? subsidiaries.size() : 0;
        if (subsidiaries != null) {
            for (OwnershipRelationship rel : subsidiaries) {
                if (rel.getTarget() instanceof Organization) {
                    count += ((Organization) rel.getTarget()).getTotalSubsidiariesCount();
                }
            }
        }
        return count;
    }

    /**
     * Get all unique jurisdictions across the organization hierarchy
     */
    public Set<String> getAllJurisdictions() {
        Set<String> jurisdictions = new java.util.HashSet<>();
        if (this.jurisdiction != null) {
            jurisdictions.add(this.jurisdiction);
        }
        if (subsidiaries != null) {
            for (OwnershipRelationship rel : subsidiaries) {
                if (rel.getTarget() instanceof Organization) {
                    Organization sub = (Organization) rel.getTarget();
                    jurisdictions.addAll(sub.getAllJurisdictions());
                }
            }
        }
        return jurisdictions;
    }
}
