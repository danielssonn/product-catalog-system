package com.bank.product.party.controller;

import com.bank.product.party.domain.Individual;
import com.bank.product.party.domain.LegalEntity;
import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.repository.IndividualRepository;
import com.bank.product.party.repository.LegalEntityRepository;
import com.bank.product.party.repository.OrganizationRepository;
import com.bank.product.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

/**
 * GraphQL controller for party queries.
 * Provides relationship traversal and graph queries.
 */
@Controller
@RequiredArgsConstructor
public class PartyGraphQLController {

    private final PartyRepository partyRepository;
    private final OrganizationRepository organizationRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final IndividualRepository individualRepository;

    /**
     * Find party by federated ID
     */
    @QueryMapping
    public Party party(@Argument String federatedId) {
        return partyRepository.findByFederatedId(federatedId).orElse(null);
    }

    /**
     * Find party by LEI
     */
    @QueryMapping
    public Party partyByLei(@Argument String lei) {
        Optional<Organization> org = organizationRepository.findByLei(lei);
        if (org.isPresent()) return org.get();

        return legalEntityRepository.findByLei(lei).orElse(null);
    }

    /**
     * Search parties by name
     */
    @QueryMapping
    public List<Organization> searchParties(@Argument String name) {
        return organizationRepository.searchByName(name);
    }

    /**
     * Get organization hierarchy
     */
    @QueryMapping
    public Organization organizationHierarchy(@Argument String rootId, @Argument Integer depth) {
        return organizationRepository.findHierarchy(rootId);
    }

    /**
     * Find ultimate parent organization
     */
    @QueryMapping
    public Organization ultimateParent(@Argument String childId) {
        return organizationRepository.findUltimateParent(childId).orElse(null);
    }

    /**
     * Find all subsidiaries
     */
    @QueryMapping
    public List<Organization> subsidiaries(@Argument String parentId) {
        return organizationRepository.findAllSubsidiaries(parentId);
    }

    /**
     * Find ultimate beneficial owners
     */
    @QueryMapping
    public List<Object[]> ultimateBeneficialOwners(@Argument String entityId) {
        return legalEntityRepository.findUltimateBeneficialOwners(entityId);
    }

    /**
     * Find ownership chain
     */
    @QueryMapping
    public List<Object[]> ownershipChain(@Argument String entityId) {
        return legalEntityRepository.findOwnershipChain(entityId);
    }

    /**
     * Find parties in multiple systems
     */
    @QueryMapping
    public List<Party> partiesInMultipleSystems(@Argument Integer minSystems) {
        return partyRepository.findCrossDomainParties(minSystems != null ? minSystems : 2);
    }

    /**
     * Find duplicate candidates
     */
    @QueryMapping
    public List<Party> duplicateCandidates(@Argument Double threshold) {
        return partyRepository.findDuplicateCandidates(threshold != null ? threshold : 0.75);
    }

    /**
     * Find relationship path between two organizations
     */
    @QueryMapping
    public List<Object[]> relationshipPath(@Argument String org1Id, @Argument String org2Id) {
        return organizationRepository.findRelationshipPath(org1Id, org2Id);
    }

    /**
     * Field resolver for subsidiaries count
     */
    @SchemaMapping(typeName = "Organization", field = "totalSubsidiaries")
    public int totalSubsidiaries(Organization organization) {
        return organization.getTotalSubsidiariesCount();
    }

    /**
     * Field resolver for jurisdictions
     */
    @SchemaMapping(typeName = "Organization", field = "jurisdictions")
    public List<String> jurisdictions(Organization organization) {
        return organization.getAllJurisdictions().stream().toList();
    }
}
