package com.bank.product.party.repository;

import com.bank.product.party.domain.CollateralDocument;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollateralDocumentRepository extends Neo4jRepository<CollateralDocument, String> {

    /**
     * Find document by reference number
     */
    Optional<CollateralDocument> findByDocumentReference(String documentReference);

    /**
     * Find all documents of a specific type
     */
    List<CollateralDocument> findByDocumentType(CollateralDocument.DocumentType documentType);

    /**
     * Find all active documents
     */
    List<CollateralDocument> findByStatus(CollateralDocument.DocumentStatus status);

    /**
     * Find documents expiring soon
     */
    @Query("MATCH (d:CollateralDocument) " +
           "WHERE d.expirationDate >= $startDate AND d.expirationDate <= $endDate " +
           "RETURN d ORDER BY d.expirationDate")
    List<CollateralDocument> findExpiringSoon(LocalDate startDate, LocalDate endDate);

    /**
     * Find documents by jurisdiction
     */
    List<CollateralDocument> findByJurisdiction(String jurisdiction);
}
