package com.bank.product.party.service;

import com.bank.product.party.domain.CollateralDocument;
import com.bank.product.party.domain.ManagesOnBehalfOfRelationship;
import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.repository.CollateralDocumentRepository;
import com.bank.product.party.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing party relationships including "manages on behalf of" relationships
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipManagementService {

    private final OrganizationRepository organizationRepository;
    private final CollateralDocumentRepository collateralDocumentRepository;

    /**
     * Create a "manages on behalf of" relationship between two parties
     *
     * @param managerId ID of the managing party (e.g., Goldman Sachs)
     * @param principalId ID of the principal party (e.g., Tesla)
     * @param relationship The relationship details
     * @param document Supporting collateral document
     * @return The updated manager organization
     */
    @Transactional
    public Organization createManagementRelationship(
            String managerId,
            String principalId,
            ManagesOnBehalfOfRelationship relationship,
            CollateralDocument document) {

        log.info("Creating management relationship: {} manages for {}", managerId, principalId);

        // Find both parties
        Organization manager = organizationRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerId));

        Organization principal = organizationRepository.findById(principalId)
                .orElseThrow(() -> new IllegalArgumentException("Principal not found: " + principalId));

        // Save the collateral document first
        CollateralDocument savedDocument = collateralDocumentRepository.save(document);
        log.info("Saved collateral document: {}", savedDocument.getDocumentReference());

        // Set the principal in the relationship
        relationship.setPrincipal(principal);

        // Add document reference to relationship
        relationship.addCollateralDocument(savedDocument.getId());

        // Add relationship to manager's list
        if (manager.getManagesFor() == null) {
            manager.setManagesFor(List.of(relationship));
        } else {
            manager.getManagesFor().add(relationship);
        }

        // Save and return
        Organization updated = organizationRepository.save(manager);
        log.info("Created management relationship with ID: {}", relationship.getId());

        return updated;
    }

    /**
     * Get all parties managed by a specific organization
     */
    @Transactional(readOnly = true)
    public List<Party> getManagedParties(String managerId) {
        Organization manager = organizationRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerId));

        return manager.getManagesFor().stream()
                .map(ManagesOnBehalfOfRelationship::getPrincipal)
                .toList();
    }

    /**
     * Get all managers for a specific party
     */
    @Transactional(readOnly = true)
    public List<Party> getManagers(String principalId) {
        Organization principal = organizationRepository.findById(principalId)
                .orElseThrow(() -> new IllegalArgumentException("Principal not found: " + principalId));

        // This would need a custom query to get the source nodes of MANAGES_ON_BEHALF_OF relationships
        return List.of(); // Placeholder
    }

    /**
     * Create a collateral document
     */
    @Transactional
    public CollateralDocument createCollateralDocument(CollateralDocument document) {
        log.info("Creating collateral document: {}", document.getDocumentReference());
        return collateralDocumentRepository.save(document);
    }

    /**
     * Find documents expiring within the next N days
     */
    @Transactional(readOnly = true)
    public List<CollateralDocument> findExpiringSoon(int days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);
        return collateralDocumentRepository.findExpiringSoon(now, endDate);
    }

    /**
     * Get collateral document by ID
     */
    @Transactional(readOnly = true)
    public CollateralDocument getDocument(String documentId) {
        return collateralDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    /**
     * Update document status
     */
    @Transactional
    public CollateralDocument updateDocumentStatus(String documentId, CollateralDocument.DocumentStatus newStatus) {
        CollateralDocument document = getDocument(documentId);
        document.setStatus(newStatus);
        document.updateTimestamp();
        return collateralDocumentRepository.save(document);
    }
}
