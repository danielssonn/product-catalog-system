package com.bank.product.party.document;

import com.bank.product.party.document.IncumbencyCertificateData.*;
import com.bank.product.party.domain.CollateralDocument;
import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.PartyStatus;
import com.bank.product.party.domain.PartyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for RelationshipExtractionService.
 *
 * Tests predictive graph construction from document data:
 * - Parent-subsidiary extraction from incorporation certificates
 * - Officer/director extraction from incumbency certificates
 * - Beneficial ownership extraction (25%+ threshold)
 * - Authorized signer extraction
 * - Confidence scoring for predictions
 */
class RelationshipExtractionServiceTest {

    private RelationshipExtractionService relationshipExtractionService;

    @BeforeEach
    void setUp() {
        relationshipExtractionService = new RelationshipExtractionService();
    }

    // ===== Parent-Subsidiary Extraction Tests =====

    @Test
    @DisplayName("Should extract parent from 'subsidiary of' pattern in legal name")
    void testSubsidiaryOfPattern() {
        Organization subject = createOrganization("ABC Bank USA");
        subject.setFederatedId("abc-bank-usa-001");

        IncorporationData data = IncorporationData.builder()
                .legalName("ABC Bank USA, a subsidiary of ABC Holdings Corporation")
                .registrationNumber("12345")
                .jurisdiction("Delaware")
                .build();

        CollateralDocument doc = createDocument("INC-001");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractHierarchyFromIncorporation(doc, data, subject);

        assertFalse(predictions.isEmpty(), "Should extract parent relationship");
        assertEquals(1, predictions.size(), "Should extract exactly one parent");

        RelationshipPrediction pred = predictions.get(0);
        assertEquals("SUBSIDIARY_OF", pred.getRelationshipType());
        assertEquals("abc-bank-usa-001", pred.getSourcePartyId());
        assertEquals("ABC Holdings Corporation", pred.getTargetPartyName());
        assertTrue(pred.getConfidence() >= 0.85, "Should have high confidence: " + pred.getConfidence());
        assertFalse(pred.needsReview(), "High confidence should not need review");
    }

    @Test
    @DisplayName("Should extract parent from 'affiliate of' pattern")
    void testAffiliateOfPattern() {
        Organization subject = createOrganization("XYZ Capital");
        subject.setFederatedId("xyz-capital-001");

        IncorporationData data = IncorporationData.builder()
                .legalName("XYZ Capital, an affiliate of XYZ Global Partners")
                .registrationNumber("67890")
                .jurisdiction("New York")
                .build();

        CollateralDocument doc = createDocument("INC-002");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractHierarchyFromIncorporation(doc, data, subject);

        assertFalse(predictions.isEmpty(), "Should extract affiliate relationship");
        RelationshipPrediction pred = predictions.get(0);
        assertEquals("SUBSIDIARY_OF", pred.getRelationshipType());
        assertEquals("XYZ Global Partners", pred.getTargetPartyName());
    }

    @Test
    @DisplayName("Should extract parent from 'wholly owned by' pattern")
    void testWhollyOwnedPattern() {
        Organization subject = createOrganization("Tech Solutions Ltd");
        subject.setFederatedId("tech-solutions-001");

        IncorporationData data = IncorporationData.builder()
                .legalName("Tech Solutions Ltd, wholly owned by MegaCorp International")
                .registrationNumber("99999")
                .jurisdiction("California")
                .build();

        CollateralDocument doc = createDocument("INC-003");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractHierarchyFromIncorporation(doc, data, subject);

        assertFalse(predictions.isEmpty(), "Should extract wholly owned relationship");
        RelationshipPrediction pred = predictions.get(0);
        assertEquals("MegaCorp International", pred.getTargetPartyName());
        assertTrue(pred.getConfidence() >= 0.85);
    }

    @Test
    @DisplayName("Should extract parent from explicit parentCompanyMention field")
    void testExplicitParentField() {
        Organization subject = createOrganization("Beta Corporation");
        subject.setFederatedId("beta-corp-001");

        IncorporationData data = IncorporationData.builder()
                .legalName("Beta Corporation")
                .parentCompanyMention("Alpha Holdings Inc")
                .parentCompanyJurisdiction("Delaware")
                .registrationNumber("11111")
                .jurisdiction("Nevada")
                .build();

        CollateralDocument doc = createDocument("INC-004");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractHierarchyFromIncorporation(doc, data, subject);

        assertFalse(predictions.isEmpty(), "Should extract from explicit field");
        RelationshipPrediction pred = predictions.get(0);
        assertEquals("Alpha Holdings Inc", pred.getTargetPartyName());
        assertTrue(pred.getConfidence() >= 0.90, "Explicit field should have very high confidence");
        assertTrue(pred.shouldAutoApprove(), "Confidence ≥ 0.90 should auto-approve");
    }

    @Test
    @DisplayName("Should not extract parent when no indicators present")
    void testNoParentIndicators() {
        Organization subject = createOrganization("Independent Corp");
        subject.setFederatedId("independent-001");

        IncorporationData data = IncorporationData.builder()
                .legalName("Independent Corp")
                .registrationNumber("22222")
                .jurisdiction("Delaware")
                .build();

        CollateralDocument doc = createDocument("INC-005");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractHierarchyFromIncorporation(doc, data, subject);

        assertTrue(predictions.isEmpty(), "Should not extract any relationships");
    }

    // ===== Officer Extraction Tests =====

    @Test
    @DisplayName("Should extract officers from incumbency certificate")
    void testOfficerExtraction() {
        Organization subject = createOrganization("Acme Corporation");
        subject.setFederatedId("acme-corp-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("Acme Corporation")
                .officers(Arrays.asList(
                        OfficerInfo.builder()
                                .name("John Smith")
                                .title("Chief Executive Officer")
                                .appointmentDate(LocalDate.of(2020, 1, 15))
                                .build(),
                        OfficerInfo.builder()
                                .name("Jane Doe")
                                .title("Chief Financial Officer")
                                .appointmentDate(LocalDate.of(2021, 3, 1))
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-001");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        assertEquals(2, predictions.size(), "Should extract both officers");

        RelationshipPrediction ceo = predictions.get(0);
        assertEquals("OFFICER_OF", ceo.getRelationshipType());
        assertEquals("John Smith", ceo.getSourcePartyName());
        assertEquals("acme-corp-001", ceo.getTargetPartyId());
        assertTrue(ceo.getConfidence() >= 0.95, "Incumbency cert should have very high confidence");
        assertTrue(ceo.shouldAutoApprove(), "Should auto-approve at 0.95 confidence");

        RelationshipPrediction cfo = predictions.get(1);
        assertEquals("Jane Doe", cfo.getSourcePartyName());
        assertEquals("Chief Financial Officer", cfo.getRelationshipProperties().contains("title") ? "CFO" : "CFO");
    }

    // ===== Director Extraction Tests =====

    @Test
    @DisplayName("Should extract directors from incumbency certificate")
    void testDirectorExtraction() {
        Organization subject = createOrganization("TechStart Inc");
        subject.setFederatedId("techstart-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("TechStart Inc")
                .directors(Arrays.asList(
                        DirectorInfo.builder()
                                .name("Alice Johnson")
                                .title("Chairperson")
                                .independent(false)
                                .build(),
                        DirectorInfo.builder()
                                .name("Bob Williams")
                                .title("Independent Director")
                                .independent(true)
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-002");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        assertEquals(2, predictions.size(), "Should extract both directors");

        RelationshipPrediction chair = predictions.get(0);
        assertEquals("DIRECTOR_OF", chair.getRelationshipType());
        assertEquals("Alice Johnson", chair.getSourcePartyName());
        assertTrue(chair.getConfidence() >= 0.95);

        RelationshipPrediction independentDir = predictions.get(1);
        assertEquals("Bob Williams", independentDir.getSourcePartyName());
        assertTrue(independentDir.getRelationshipProperties().contains("independent"));
    }

    // ===== Authorized Signer Extraction Tests =====

    @Test
    @DisplayName("Should extract authorized signers from incumbency certificate")
    void testAuthorizedSignerExtraction() {
        Organization subject = createOrganization("Financial Services LLC");
        subject.setFederatedId("finserv-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("Financial Services LLC")
                .authorizedSigners(Arrays.asList(
                        AuthorizedSignerInfo.builder()
                                .name("Carol Martinez")
                                .title("Treasurer")
                                .authorityLevel("Full authority")
                                .authorityScope("Banking")
                                .build(),
                        AuthorizedSignerInfo.builder()
                                .name("David Chen")
                                .title("Controller")
                                .authorityLevel("Limited authority")
                                .amountLimit(100000.0)
                                .authorityScope("Contracts")
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-003");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        assertEquals(2, predictions.size(), "Should extract both signers");

        RelationshipPrediction signer1 = predictions.get(0);
        assertEquals("AUTHORIZED_SIGNER", signer1.getRelationshipType());
        assertEquals("Carol Martinez", signer1.getSourcePartyName());
        assertTrue(signer1.getRelationshipProperties().contains("Full authority"));

        RelationshipPrediction signer2 = predictions.get(1);
        assertEquals("David Chen", signer2.getSourcePartyName());
        assertTrue(signer2.getRelationshipProperties().contains("100000"));
    }

    // ===== Beneficial Owner Extraction Tests =====

    @Test
    @DisplayName("Should extract beneficial owners with 25%+ ownership")
    void testBeneficialOwnerExtraction() {
        Organization subject = createOrganization("Private Investment Fund");
        subject.setFederatedId("priv-fund-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("Private Investment Fund")
                .beneficialOwners(Arrays.asList(
                        BeneficialOwnerInfo.builder()
                                .name("Michael Bloomberg")
                                .ownershipPercentage(60.0)
                                .ownershipType("Direct")
                                .nationality("USA")
                                .build(),
                        BeneficialOwnerInfo.builder()
                                .name("Sarah Cohen")
                                .ownershipPercentage(30.0)
                                .ownershipType("Indirect")
                                .nationality("USA")
                                .build(),
                        BeneficialOwnerInfo.builder()
                                .name("Tom Lee")
                                .ownershipPercentage(10.0)  // Below 25% threshold
                                .ownershipType("Direct")
                                .nationality("USA")
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-004");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        // Should only extract owners with >= 25% (FinCEN UBO threshold)
        long uboCount = predictions.stream()
                .filter(p -> p.getRelationshipType().equals("BENEFICIAL_OWNER_OF"))
                .count();

        assertEquals(2, uboCount, "Should extract only UBOs with 25%+ ownership");

        RelationshipPrediction ubo1 = predictions.stream()
                .filter(p -> p.getSourcePartyName().equals("Michael Bloomberg"))
                .findFirst()
                .orElseThrow();

        assertEquals("BENEFICIAL_OWNER_OF", ubo1.getRelationshipType());
        assertTrue(ubo1.getConfidence() >= 0.90, "UBO should have high confidence");
        assertTrue(ubo1.getRelationshipProperties().contains("60"));
    }

    @Test
    @DisplayName("Should not extract beneficial owners below 25% threshold")
    void testBelowUBOThreshold() {
        Organization subject = createOrganization("Small Business Inc");
        subject.setFederatedId("smallbiz-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("Small Business Inc")
                .beneficialOwners(Arrays.asList(
                        BeneficialOwnerInfo.builder()
                                .name("Owner 1")
                                .ownershipPercentage(15.0)
                                .ownershipType("Direct")
                                .build(),
                        BeneficialOwnerInfo.builder()
                                .name("Owner 2")
                                .ownershipPercentage(10.0)
                                .ownershipType("Direct")
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-005");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        // Should not extract any UBOs (all below 25%)
        long uboCount = predictions.stream()
                .filter(p -> p.getRelationshipType().equals("BENEFICIAL_OWNER_OF"))
                .count();

        assertEquals(0, uboCount, "Should not extract UBOs below 25% threshold");
    }

    // ===== Comprehensive Incumbency Test =====

    @Test
    @DisplayName("Should extract all relationship types from comprehensive incumbency certificate")
    void testComprehensiveIncumbencyExtraction() {
        Organization subject = createOrganization("Global Finance Corporation");
        subject.setFederatedId("globalfin-001");

        IncumbencyCertificateData data = IncumbencyCertificateData.builder()
                .companyLegalName("Global Finance Corporation")
                .officers(Arrays.asList(
                        OfficerInfo.builder()
                                .name("CEO Person")
                                .title("Chief Executive Officer")
                                .build()
                ))
                .directors(Arrays.asList(
                        DirectorInfo.builder()
                                .name("Director Person")
                                .title("Director")
                                .build()
                ))
                .authorizedSigners(Arrays.asList(
                        AuthorizedSignerInfo.builder()
                                .name("Signer Person")
                                .title("Treasurer")
                                .authorityLevel("Full authority")
                                .build()
                ))
                .beneficialOwners(Arrays.asList(
                        BeneficialOwnerInfo.builder()
                                .name("Owner Person")
                                .ownershipPercentage(50.0)
                                .ownershipType("Direct")
                                .build()
                ))
                .build();

        CollateralDocument doc = createDocument("INC-CERT-FULL");

        List<RelationshipPrediction> predictions =
                relationshipExtractionService.extractOfficersFromIncumbency(doc, data, subject);

        assertEquals(4, predictions.size(), "Should extract all 4 relationship types");

        // Verify all types are present
        assertTrue(predictions.stream().anyMatch(p -> p.getRelationshipType().equals("OFFICER_OF")));
        assertTrue(predictions.stream().anyMatch(p -> p.getRelationshipType().equals("DIRECTOR_OF")));
        assertTrue(predictions.stream().anyMatch(p -> p.getRelationshipType().equals("AUTHORIZED_SIGNER")));
        assertTrue(predictions.stream().anyMatch(p -> p.getRelationshipType().equals("BENEFICIAL_OWNER_OF")));

        // All should have high confidence (≥0.90)
        for (RelationshipPrediction pred : predictions) {
            assertTrue(pred.getConfidence() >= 0.90,
                    "All incumbency predictions should have ≥0.90 confidence");
        }
    }

    // ===== Confidence Threshold Tests =====

    @Test
    @DisplayName("Should mark predictions for auto-approval at ≥0.90 confidence")
    void testAutoApprovalThreshold() {
        RelationshipPrediction highConf = RelationshipPrediction.builder()
                .confidence(0.95)
                .build();

        assertTrue(highConf.shouldAutoApprove(), "0.95 should auto-approve");
        assertFalse(highConf.needsReview(), "0.95 should not need review");
        assertFalse(highConf.isSuggestion(), "0.95 should not be suggestion");
    }

    @Test
    @DisplayName("Should mark predictions for review at 0.75-0.89 confidence")
    void testReviewThreshold() {
        RelationshipPrediction mediumConf = RelationshipPrediction.builder()
                .confidence(0.85)
                .build();

        assertFalse(mediumConf.shouldAutoApprove(), "0.85 should not auto-approve");
        assertTrue(mediumConf.needsReview(), "0.85 should need review");
        assertFalse(mediumConf.isSuggestion(), "0.85 should not be suggestion");
    }

    @Test
    @DisplayName("Should mark predictions as suggestions at <0.75 confidence")
    void testSuggestionThreshold() {
        RelationshipPrediction lowConf = RelationshipPrediction.builder()
                .confidence(0.65)
                .build();

        assertFalse(lowConf.shouldAutoApprove(), "0.65 should not auto-approve");
        assertFalse(lowConf.needsReview(), "0.65 should not need review");
        assertTrue(lowConf.isSuggestion(), "0.65 should be suggestion");
    }

    // ===== Helper Methods =====

    private Organization createOrganization(String legalName) {
        Organization org = new Organization();
        org.setLegalName(legalName);
        org.setPartyType(PartyType.ORGANIZATION);
        org.setStatus(PartyStatus.ACTIVE);
        return org;
    }

    private CollateralDocument createDocument(String id) {
        CollateralDocument doc = new CollateralDocument();
        doc.setId(id);
        return doc;
    }
}
