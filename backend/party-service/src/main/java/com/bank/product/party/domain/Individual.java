package com.bank.product.party.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Natural person in the federated party model.
 * Used for beneficial ownership, authorized signers, officers, and board members.
 */
@Node("Individual")
@Data
@EqualsAndHashCode(callSuper = true)
public class Individual extends Party {

    /**
     * First/given name
     */
    private String firstName;

    /**
     * Middle name
     */
    private String middleName;

    /**
     * Last/family name
     */
    private String lastName;

    /**
     * Full name (computed)
     */
    private String fullName;

    /**
     * Date of birth (encrypted)
     */
    private LocalDate dateOfBirth;

    /**
     * Nationality
     */
    private String nationality;

    /**
     * Country of residence
     */
    private String residency;

    /**
     * Politically Exposed Person status
     */
    private Boolean pepStatus;

    /**
     * Residential address
     */
    private Address residentialAddress;

    /**
     * Email address
     */
    private String email;

    /**
     * Phone number
     */
    private String phoneNumber;

    /**
     * National ID/SSN (encrypted)
     */
    private String nationalId;

    /**
     * Passport number (encrypted)
     */
    private String passportNumber;

    // ===== Relationships =====

    /**
     * Entities this individual has beneficial ownership in
     */
    @Relationship(type = "BENEFICIAL_OWNER_OF", direction = Relationship.Direction.OUTGOING)
    private List<BeneficialOwnershipRelationship> beneficialOwnerships = new ArrayList<>();

    /**
     * Entities this individual is an authorized signer for
     */
    @Relationship(type = "AUTHORIZED_SIGNER", direction = Relationship.Direction.OUTGOING)
    private List<AuthorizedSignerRelationship> authorizedFor = new ArrayList<>();

    /**
     * Entities this individual is an officer of
     */
    @Relationship(type = "OFFICER_OF", direction = Relationship.Direction.OUTGOING)
    private List<OfficerRelationship> officerOf = new ArrayList<>();

    /**
     * Entities this individual is a board member of
     */
    @Relationship(type = "BOARD_MEMBER_OF", direction = Relationship.Direction.OUTGOING)
    private List<BoardMemberRelationship> boardMemberOf = new ArrayList<>();

    public Individual() {
        super();
        setPartyType(PartyType.INDIVIDUAL);
        this.pepStatus = false;
    }

    public void updateFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (middleName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName);
        }
        if (lastName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        this.fullName = sb.toString();
    }

    /**
     * Get all entities this individual controls or has significant influence over
     */
    public List<Party> getControlledEntities() {
        List<Party> controlled = new ArrayList<>();

        // High ownership stake
        beneficialOwnerships.stream()
                .filter(rel -> rel.getOwnershipPercentage() >= 50.0)
                .forEach(rel -> controlled.add(rel.getTarget()));

        // Board chair or CEO positions
        officerOf.stream()
                .filter(rel -> "CEO".equals(rel.getTitle()) || "President".equals(rel.getTitle()))
                .forEach(rel -> controlled.add(rel.getTarget()));

        boardMemberOf.stream()
                .filter(rel -> "Chair".equals(rel.getRole()) || "Chairman".equals(rel.getRole()))
                .forEach(rel -> controlled.add(rel.getTarget()));

        return controlled;
    }
}
