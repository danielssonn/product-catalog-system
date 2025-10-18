package com.bank.product.party.document;

import com.bank.product.party.document.IncumbencyCertificateData.*;
import com.bank.product.party.domain.CollateralDocument;
import com.bank.product.party.domain.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting relationship predictions from document data.
 *
 * Enables predictive graph construction by automatically identifying relationships
 * from documents without manual data entry.
 *
 * Relationship sources:
 * 1. Certificate of Incorporation → Parent-Subsidiary relationships
 * 2. Incumbency Certificate → Officer, Director, Authorized Signer relationships
 * 3. Beneficial Ownership Documentation → Beneficial Owner relationships
 * 4. W-9 Forms → Tax entity relationships
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Section 1C: Predictive Graph Construction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipExtractionService {

    /**
     * Extract hierarchy relationships from incorporation certificate
     *
     * Looks for parent company mentions in:
     * - Legal name ("ABC Corporation, a subsidiary of XYZ Holdings")
     * - Business purpose statements
     * - Explicit parent company fields
     *
     * @param incorporationDoc The incorporation certificate document
     * @param subject The organization being incorporated
     * @return List of predicted parent-subsidiary relationships
     */
    public List<RelationshipPrediction> extractHierarchyFromIncorporation(
            CollateralDocument incorporationDoc,
            IncorporationData incorporationData,
            Party subject) {

        log.info("Extracting hierarchy relationships from incorporation certificate for party: {}",
                subject.getFederatedId());

        List<RelationshipPrediction> predictions = new ArrayList<>();

        // Pattern 1: "subsidiary of" in legal name
        if (incorporationData.getLegalName() != null) {
            List<String> parentNames = extractParentFromSubsidiaryPattern(
                    incorporationData.getLegalName()
            );

            for (String parentName : parentNames) {
                RelationshipPrediction pred = RelationshipPrediction.builder()
                        .sourcePartyId(subject.getFederatedId())
                        .sourcePartyName(incorporationData.getLegalName())
                        .targetPartyName(parentName)
                        .relationshipType("SUBSIDIARY_OF")
                        .confidence(0.85) // High confidence from legal name pattern
                        .evidenceDocumentId(incorporationDoc.getId())
                        .predictionSource("INCORPORATION_CERTIFICATE")
                        .extractionContext("Parent company mentioned in legal name")
                        .build();

                predictions.add(pred);
                log.info("Predicted SUBSIDIARY_OF relationship: {} → {}",
                        incorporationData.getLegalName(), parentName);
            }
        }

        // Pattern 2: Explicit parent company mention field
        if (incorporationData.getParentCompanyMention() != null) {
            RelationshipPrediction pred = RelationshipPrediction.builder()
                    .sourcePartyId(subject.getFederatedId())
                    .sourcePartyName(incorporationData.getLegalName())
                    .targetPartyName(incorporationData.getParentCompanyMention())
                    .relationshipType("SUBSIDIARY_OF")
                    .confidence(0.90) // Very high confidence from explicit field
                    .evidenceDocumentId(incorporationDoc.getId())
                    .predictionSource("INCORPORATION_CERTIFICATE")
                    .extractionContext("Explicit parent company field in incorporation certificate")
                    .build();

            predictions.add(pred);
            log.info("Predicted SUBSIDIARY_OF relationship from explicit field: {} → {}",
                    incorporationData.getLegalName(), incorporationData.getParentCompanyMention());
        }

        // Pattern 3: Registered agent patterns (corporate service providers)
        if (incorporationData.hasThirdPartyRegisteredAgent()) {
            log.debug("Third-party registered agent detected: {}. No direct ownership relationship inferred.",
                    incorporationData.getRegisteredAgentName());
        }

        return predictions;
    }

    /**
     * Extract officer and director relationships from incumbency certificate
     *
     * Creates predictions for:
     * - OFFICER_OF (CEO, CFO, COO, etc.)
     * - DIRECTOR_OF (Board members)
     * - AUTHORIZED_SIGNER (individuals with signing authority)
     * - BENEFICIAL_OWNER_OF (25%+ ownership)
     *
     * @param incumbencyDoc The incumbency certificate document
     * @param incumbencyData Extracted incumbency data
     * @param subject The organization
     * @return List of predicted individual-organization relationships
     */
    public List<RelationshipPrediction> extractOfficersFromIncumbency(
            CollateralDocument incumbencyDoc,
            IncumbencyCertificateData incumbencyData,
            Party subject) {

        log.info("Extracting officer/director relationships from incumbency certificate for party: {}",
                subject.getFederatedId());

        List<RelationshipPrediction> predictions = new ArrayList<>();

        // Extract officers
        if (incumbencyData.getOfficers() != null) {
            for (OfficerInfo officer : incumbencyData.getOfficers()) {
                RelationshipPrediction pred = RelationshipPrediction.builder()
                        .sourcePartyName(officer.getName())
                        .targetPartyId(subject.getFederatedId())
                        .targetPartyName(incumbencyData.getCompanyLegalName())
                        .relationshipType("OFFICER_OF")
                        .confidence(0.95) // Very high confidence from incumbency certificate
                        .evidenceDocumentId(incumbencyDoc.getId())
                        .predictionSource("INCUMBENCY_CERTIFICATE")
                        .extractionContext("Officer listed in incumbency certificate")
                        .relationshipProperties(String.format(
                                "{\"title\":\"%s\",\"appointmentDate\":\"%s\"}",
                                officer.getTitle(),
                                officer.getAppointmentDate() != null ? officer.getAppointmentDate().toString() : ""
                        ))
                        .build();

                predictions.add(pred);
                log.info("Predicted OFFICER_OF relationship: {} ({}) → {}",
                        officer.getName(), officer.getTitle(), incumbencyData.getCompanyLegalName());
            }
        }

        // Extract directors
        if (incumbencyData.getDirectors() != null) {
            for (DirectorInfo director : incumbencyData.getDirectors()) {
                RelationshipPrediction pred = RelationshipPrediction.builder()
                        .sourcePartyName(director.getName())
                        .targetPartyId(subject.getFederatedId())
                        .targetPartyName(incumbencyData.getCompanyLegalName())
                        .relationshipType("DIRECTOR_OF")
                        .confidence(0.95)
                        .evidenceDocumentId(incumbencyDoc.getId())
                        .predictionSource("INCUMBENCY_CERTIFICATE")
                        .extractionContext("Director listed in incumbency certificate")
                        .relationshipProperties(String.format(
                                "{\"title\":\"%s\",\"independent\":%s}",
                                director.getTitle(),
                                director.getIndependent() != null ? director.getIndependent().toString() : "null"
                        ))
                        .build();

                predictions.add(pred);
                log.info("Predicted DIRECTOR_OF relationship: {} ({}) → {}",
                        director.getName(), director.getTitle(), incumbencyData.getCompanyLegalName());
            }
        }

        // Extract authorized signers
        if (incumbencyData.getAuthorizedSigners() != null) {
            for (AuthorizedSignerInfo signer : incumbencyData.getAuthorizedSigners()) {
                RelationshipPrediction pred = RelationshipPrediction.builder()
                        .sourcePartyName(signer.getName())
                        .targetPartyId(subject.getFederatedId())
                        .targetPartyName(incumbencyData.getCompanyLegalName())
                        .relationshipType("AUTHORIZED_SIGNER")
                        .confidence(0.95)
                        .evidenceDocumentId(incumbencyDoc.getId())
                        .predictionSource("INCUMBENCY_CERTIFICATE")
                        .extractionContext("Authorized signer listed in incumbency certificate")
                        .relationshipProperties(String.format(
                                "{\"authorityLevel\":\"%s\",\"amountLimit\":%s,\"authorityScope\":\"%s\"}",
                                signer.getAuthorityLevel(),
                                signer.getAmountLimit() != null ? signer.getAmountLimit().toString() : "null",
                                signer.getAuthorityScope()
                        ))
                        .build();

                predictions.add(pred);
                log.info("Predicted AUTHORIZED_SIGNER relationship: {} ({}) → {}",
                        signer.getName(), signer.getAuthorityLevel(), incumbencyData.getCompanyLegalName());
            }
        }

        // Extract beneficial owners
        if (incumbencyData.getBeneficialOwners() != null) {
            for (BeneficialOwnerInfo owner : incumbencyData.getBeneficialOwners()) {
                // Only predict if ownership >= 25% (FinCEN UBO threshold)
                if (owner.getOwnershipPercentage() != null && owner.getOwnershipPercentage() >= 25.0) {
                    RelationshipPrediction pred = RelationshipPrediction.builder()
                            .sourcePartyName(owner.getName())
                            .targetPartyId(subject.getFederatedId())
                            .targetPartyName(incumbencyData.getCompanyLegalName())
                            .relationshipType("BENEFICIAL_OWNER_OF")
                            .confidence(0.90) // High confidence for UBO
                            .evidenceDocumentId(incumbencyDoc.getId())
                            .predictionSource("INCUMBENCY_CERTIFICATE")
                            .extractionContext("Beneficial owner listed with 25%+ ownership")
                            .relationshipProperties(String.format(
                                    "{\"ownershipPercentage\":%.2f,\"ownershipType\":\"%s\",\"nationality\":\"%s\"}",
                                    owner.getOwnershipPercentage(),
                                    owner.getOwnershipType(),
                                    owner.getNationality()
                            ))
                            .build();

                    predictions.add(pred);
                    log.info("Predicted BENEFICIAL_OWNER_OF relationship: {} ({}%) → {}",
                            owner.getName(), owner.getOwnershipPercentage(), incumbencyData.getCompanyLegalName());
                }
            }
        }

        return predictions;
    }

    /**
     * Extract beneficial ownership relationships from ownership documentation
     *
     * @param ownershipDoc Beneficial ownership documentation
     * @param subject The organization
     * @return List of predicted beneficial ownership relationships
     */
    public List<RelationshipPrediction> extractBeneficialOwnership(
            CollateralDocument ownershipDoc,
            Party subject) {

        log.info("Extracting beneficial ownership relationships from document for party: {}",
                subject.getFederatedId());

        // TODO: Implement when beneficial ownership extraction is available
        List<RelationshipPrediction> predictions = new ArrayList<>();

        log.warn("Beneficial ownership extraction not yet implemented");

        return predictions;
    }

    // ===== Helper Methods: Pattern Extraction =====

    /**
     * Extract parent company names from subsidiary pattern matching
     *
     * Patterns:
     * - "subsidiary of XYZ Corporation"
     * - "a wholly owned subsidiary of ABC Holdings"
     * - "an affiliate of XYZ Group"
     * - "a division of ABC Company"
     */
    private List<String> extractParentFromSubsidiaryPattern(String text) {
        List<String> parentNames = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parentNames;
        }

        // Pattern 1: "subsidiary of [Parent Name]"
        Pattern subsidiaryPattern = Pattern.compile(
                "(?i)subsidiary\\s+of\\s+([A-Za-z0-9\\s,\\.&-]+?)(?:,|\\.|$)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher subsidiaryMatcher = subsidiaryPattern.matcher(text);
        while (subsidiaryMatcher.find()) {
            String parentName = subsidiaryMatcher.group(1).trim();
            parentNames.add(parentName);
            log.debug("Extracted parent from 'subsidiary of' pattern: {}", parentName);
        }

        // Pattern 2: "affiliate of [Parent Name]"
        Pattern affiliatePattern = Pattern.compile(
                "(?i)affiliate\\s+of\\s+([A-Za-z0-9\\s,\\.&-]+?)(?:,|\\.|$)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher affiliateMatcher = affiliatePattern.matcher(text);
        while (affiliateMatcher.find()) {
            String parentName = affiliateMatcher.group(1).trim();
            parentNames.add(parentName);
            log.debug("Extracted parent from 'affiliate of' pattern: {}", parentName);
        }

        // Pattern 3: "division of [Parent Name]"
        Pattern divisionPattern = Pattern.compile(
                "(?i)division\\s+of\\s+([A-Za-z0-9\\s,\\.&-]+?)(?:,|\\.|$)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher divisionMatcher = divisionPattern.matcher(text);
        while (divisionMatcher.find()) {
            String parentName = divisionMatcher.group(1).trim();
            parentNames.add(parentName);
            log.debug("Extracted parent from 'division of' pattern: {}", parentName);
        }

        // Pattern 4: "wholly owned by [Parent Name]"
        Pattern whollyOwnedPattern = Pattern.compile(
                "(?i)wholly\\s+owned\\s+by\\s+([A-Za-z0-9\\s,\\.&-]+?)(?:,|\\.|$)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher whollyOwnedMatcher = whollyOwnedPattern.matcher(text);
        while (whollyOwnedMatcher.find()) {
            String parentName = whollyOwnedMatcher.group(1).trim();
            parentNames.add(parentName);
            log.debug("Extracted parent from 'wholly owned by' pattern: {}", parentName);
        }

        return parentNames;
    }
}
