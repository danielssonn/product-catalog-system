package com.bank.product.party.controller;

import com.bank.product.party.domain.CollateralDocument;
import com.bank.product.party.domain.ManagesOnBehalfOfRelationship;
import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.service.RelationshipManagementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API for managing party relationships
 */
@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Slf4j
public class RelationshipController {

    private final RelationshipManagementService relationshipService;

    /**
     * Create a "manages on behalf of" relationship
     *
     * POST /api/v1/relationships/manages-on-behalf-of
     */
    @PostMapping("/manages-on-behalf-of")
    public ResponseEntity<Organization> createManagementRelationship(
            @RequestBody CreateManagementRelationshipRequest request) {

        log.info("Creating management relationship: {} manages {}",
                 request.getManagerId(), request.getPrincipalId());

        // Build the relationship
        ManagesOnBehalfOfRelationship relationship = new ManagesOnBehalfOfRelationship();
        relationship.setManagementType(request.getManagementType());
        relationship.setScope(request.getScope());
        relationship.setAuthorityLevel(request.getAuthorityLevel());
        relationship.setStartDate(request.getStartDate());
        relationship.setEndDate(request.getEndDate());
        relationship.setStatus(request.getStatus());
        relationship.setServicesProvided(request.getServicesProvided());
        relationship.setAssetsUnderManagement(request.getAssetsUnderManagement());
        relationship.setAumCurrency(request.getAumCurrency());
        relationship.setFeeStructure(request.getFeeStructure());
        relationship.setRelationshipManager(request.getRelationshipManager());
        relationship.setPrincipalContact(request.getPrincipalContact());
        relationship.setManagerContact(request.getManagerContact());
        relationship.setNotificationRequirements(request.getNotificationRequirements());
        relationship.setReportingFrequency(request.getReportingFrequency());
        relationship.setReviewDate(request.getReviewDate());
        relationship.setCreatedBy(request.getCreatedBy());
        relationship.setNotes(request.getNotes());

        // Build the collateral document
        CollateralDocument document = new CollateralDocument();
        document.setDocumentType(request.getDocumentType());
        document.setDocumentReference(request.getDocumentReference());
        document.setTitle(request.getDocumentTitle());
        document.setDescription(request.getDocumentDescription());
        document.setExecutionDate(request.getExecutionDate());
        document.setEffectiveDate(request.getEffectiveDate());
        document.setExpirationDate(request.getExpirationDate());
        document.setStatus(request.getDocumentStatus());
        document.setDocumentUrl(request.getDocumentUrl());
        document.setJurisdiction(request.getJurisdiction());
        document.setGoverningLaw(request.getGoverningLaw());
        document.setPrincipalSignatory(request.getPrincipalSignatory());
        document.setAgentSignatory(request.getAgentSignatory());
        document.setScopeOfAuthority(request.getScopeOfAuthority());
        document.setSpecialTerms(request.getSpecialTerms());
        document.setCreatedBy(request.getCreatedBy());

        Organization result = relationshipService.createManagementRelationship(
                request.getManagerId(),
                request.getPrincipalId(),
                relationship,
                document
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Get all parties managed by a specific organization
     *
     * GET /api/v1/relationships/managed-by/{managerId}
     */
    @GetMapping("/managed-by/{managerId}")
    public ResponseEntity<List<Party>> getManagedParties(@PathVariable String managerId) {
        List<Party> managed = relationshipService.getManagedParties(managerId);
        return ResponseEntity.ok(managed);
    }

    /**
     * Get collateral document
     *
     * GET /api/v1/relationships/documents/{documentId}
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<CollateralDocument> getDocument(@PathVariable String documentId) {
        CollateralDocument document = relationshipService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }

    /**
     * Get documents expiring soon
     *
     * GET /api/v1/relationships/documents/expiring-soon?days=30
     */
    @GetMapping("/documents/expiring-soon")
    public ResponseEntity<List<CollateralDocument>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int days) {
        List<CollateralDocument> documents = relationshipService.findExpiringSoon(days);
        return ResponseEntity.ok(documents);
    }

    /**
     * Update document status
     *
     * PUT /api/v1/relationships/documents/{documentId}/status
     */
    @PutMapping("/documents/{documentId}/status")
    public ResponseEntity<CollateralDocument> updateDocumentStatus(
            @PathVariable String documentId,
            @RequestBody UpdateDocumentStatusRequest request) {

        CollateralDocument updated = relationshipService.updateDocumentStatus(
                documentId, request.getStatus());
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class CreateManagementRelationshipRequest {
        // Party IDs
        private String managerId;
        private String principalId;

        // Relationship details
        private ManagesOnBehalfOfRelationship.ManagementType managementType;
        private String scope;
        private ManagesOnBehalfOfRelationship.AuthorityLevel authorityLevel;
        private LocalDate startDate;
        private LocalDate endDate;
        private ManagesOnBehalfOfRelationship.RelationshipStatus status;
        private List<String> servicesProvided;
        private Double assetsUnderManagement;
        private String aumCurrency;
        private String feeStructure;
        private String relationshipManager;
        private String principalContact;
        private String managerContact;
        private String notificationRequirements;
        private String reportingFrequency;
        private LocalDate reviewDate;
        private String notes;
        private String createdBy;

        // Document details
        private CollateralDocument.DocumentType documentType;
        private String documentReference;
        private String documentTitle;
        private String documentDescription;
        private LocalDate executionDate;
        private LocalDate effectiveDate;
        private LocalDate expirationDate;
        private CollateralDocument.DocumentStatus documentStatus;
        private String documentUrl;
        private String jurisdiction;
        private String governingLaw;
        private String principalSignatory;
        private String agentSignatory;
        private String scopeOfAuthority;
        private String specialTerms;
    }

    @Data
    public static class UpdateDocumentStatusRequest {
        private CollateralDocument.DocumentStatus status;
    }
}
