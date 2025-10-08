package com.bank.commercialbanking.party.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Commercial Banking Party entity.
 * Represents clients in the commercial banking system.
 */
@Document(collection = "commercial_parties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialParty {

    @Id
    private String id;

    @Indexed(unique = true)
    private String partyId; // CB-001, CB-002, etc.

    private String legalName;
    private String businessName; // DBA

    @Indexed
    private String registrationNumber;

    private String jurisdiction;
    private LocalDate incorporationDate;

    private String industry;
    private String industryCode; // NAICS

    private String tier; // TIER_1, TIER_2, etc.
    private String riskRating; // LOW, MEDIUM, HIGH
    private String amlStatus; // CLEARED, PENDING, FLAGGED

    private Address registeredAddress;
    private Address mailingAddress;

    private String primaryContact;
    private String phoneNumber;
    private String email;
    private String website;

    private Integer employeeCount;
    private Double annualRevenue; // in millions USD

    private String accountManager;
    private String relationship; // PRIMARY, SECONDARY

    @Builder.Default
    private List<String> productTypes = new ArrayList<>(); // CHECKING, SAVINGS, LOAN, etc.

    @Builder.Default
    private List<Subsidiary> subsidiaries = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String street2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String countryCode; // ISO 3166-1
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subsidiary {
        private String partyId; // Reference to another party
        private String name;
        private Double ownershipPercentage;
        private String jurisdiction;
    }
}
