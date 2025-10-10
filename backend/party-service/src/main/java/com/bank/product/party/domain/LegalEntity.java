package com.bank.product.party.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Legal entity within an organizational structure.
 * Represents a legally registered entity such as a corporation, LLC, partnership, or trust.
 */
@Node("LegalEntity")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalEntity extends Party {

    /**
     * Entity identifier from source systems
     */
    private String entityId;

    /**
     * Type of legal entity
     */
    private EntityType entityType;

    /**
     * Tax identification number (encrypted)
     */
    private String taxId;

    /**
     * Legal registered name
     */
    private String legalName;

    /**
     * Registered address
     */
    private Address registeredAddress;

    /**
     * Mailing address (if different from registered)
     */
    private Address mailingAddress;

    /**
     * Operating addresses
     */
    private List<Address> operatingAddresses = new ArrayList<>();

    /**
     * Legal Entity Identifier (ISO 17442)
     */
    private String lei;

    /**
     * Jurisdiction of registration
     */
    private String jurisdiction;

    /**
     * Registration number
     */
    private String registrationNumber;

    // ===== Relationships =====

    /**
     * Parent organization
     */
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private Organization parentOrganization;

    /**
     * Subsidiaries
     */
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private List<OwnershipRelationship> subsidiaries = new ArrayList<>();

    /**
     * Direct owners
     */
    @Relationship(type = "OWNS", direction = Relationship.Direction.INCOMING)
    private List<OwnershipRelationship> owners = new ArrayList<>();

    /**
     * Beneficial owners
     */
    @Relationship(type = "BENEFICIAL_OWNER_OF", direction = Relationship.Direction.INCOMING)
    private List<BeneficialOwnershipRelationship> beneficialOwners = new ArrayList<>();

    /**
     * Authorized signers
     */
    @Relationship(type = "AUTHORIZED_SIGNER", direction = Relationship.Direction.INCOMING)
    private List<AuthorizedSignerRelationship> authorizedSigners = new ArrayList<>();

    /**
     * Officers
     */
    @Relationship(type = "OFFICER_OF", direction = Relationship.Direction.INCOMING)
    private List<OfficerRelationship> officers = new ArrayList<>();

    /**
     * Board members
     */
    @Relationship(type = "BOARD_MEMBER_OF", direction = Relationship.Direction.INCOMING)
    private List<BoardMemberRelationship> boardMembers = new ArrayList<>();

    public LegalEntity() {
        super();
        setPartyType(PartyType.LEGAL_ENTITY);
    }

    public void addOperatingAddress(Address address) {
        if (this.operatingAddresses == null) {
            this.operatingAddresses = new ArrayList<>();
        }
        this.operatingAddresses.add(address);
    }

    /**
     * Calculate total effective ownership percentage
     */
    public double getTotalOwnershipPercentage() {
        return owners.stream()
                .mapToDouble(OwnershipRelationship::getOwnershipPercentage)
                .sum();
    }

    /**
     * Get ultimate beneficial owners (>25% threshold per FinCEN rules)
     */
    public List<BeneficialOwnershipRelationship> getUltimateBeneficialOwners() {
        return beneficialOwners.stream()
                .filter(BeneficialOwnershipRelationship::isUbo)
                .filter(rel -> rel.getOwnershipPercentage() >= 25.0)
                .toList();
    }
}
