package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.time.Instant;

/**
 * Represents a collateral document supporting party relationships
 * Examples: Service agreements, custody agreements, power of attorney, etc.
 */
@Node("CollateralDocument")
@Data
public class CollateralDocument {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    /**
     * Type of document (SERVICE_AGREEMENT, CUSTODY_AGREEMENT, POWER_OF_ATTORNEY, etc.)
     */
    private DocumentType documentType;

    /**
     * Document reference number or ID
     */
    private String documentReference;

    /**
     * Document title/name
     */
    private String title;

    /**
     * Document description
     */
    private String description;

    /**
     * Date document was executed/signed
     */
    private LocalDate executionDate;

    /**
     * Document effective date
     */
    private LocalDate effectiveDate;

    /**
     * Document expiration date (if applicable)
     */
    private LocalDate expirationDate;

    /**
     * Current status of the document
     */
    private DocumentStatus status;

    /**
     * URL or path to document storage
     */
    private String documentUrl;

    /**
     * Jurisdiction where document is governed
     */
    private String jurisdiction;

    /**
     * Governing law
     */
    private String governingLaw;

    /**
     * Authorized signatory for the principal party
     */
    private String principalSignatory;

    /**
     * Authorized signatory for the agent/manager party
     */
    private String agentSignatory;

    /**
     * Scope of authority granted (for power of attorney, custody agreements)
     */
    private String scopeOfAuthority;

    /**
     * Any special terms or conditions
     */
    private String specialTerms;

    /**
     * Document version
     */
    private String version;

    /**
     * Created timestamp
     */
    private Instant createdAt;

    /**
     * Last updated timestamp
     */
    private Instant updatedAt;

    /**
     * Created by user
     */
    private String createdBy;

    /**
     * Last updated by user
     */
    private String updatedBy;

    public CollateralDocument() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = DocumentStatus.DRAFT;
        this.version = "1.0";
    }

    public enum DocumentType {
        SERVICE_AGREEMENT("Service Agreement"),
        CUSTODY_AGREEMENT("Custody Agreement"),
        POWER_OF_ATTORNEY("Power of Attorney"),
        MANAGEMENT_AGREEMENT("Management Agreement"),
        DELEGATION_AGREEMENT("Delegation Agreement"),
        COLLATERAL_AGREEMENT("Collateral Agreement"),
        NETTING_AGREEMENT("Netting Agreement"),
        ISDA_MASTER("ISDA Master Agreement"),
        OTHER("Other");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DocumentStatus {
        DRAFT("Draft"),
        PENDING_APPROVAL("Pending Approval"),
        APPROVED("Approved"),
        EXECUTED("Executed"),
        ACTIVE("Active"),
        EXPIRED("Expired"),
        TERMINATED("Terminated"),
        SUSPENDED("Suspended");

        private final String displayName;

        DocumentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public boolean isActive() {
        if (status != DocumentStatus.ACTIVE && status != DocumentStatus.EXECUTED) {
            return false;
        }

        LocalDate now = LocalDate.now();

        if (effectiveDate != null && now.isBefore(effectiveDate)) {
            return false;
        }

        if (expirationDate != null && now.isAfter(expirationDate)) {
            return false;
        }

        return true;
    }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
