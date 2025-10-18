package com.bank.product.party.document;

import com.bank.product.party.domain.CollateralDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Service for extracting structured data from party documents using AI.
 *
 * Supports document types:
 * - W-9 (US Tax Form)
 * - W-8BEN (Foreign Entity Tax Form)
 * - Certificate of Incorporation
 * - Incumbency Certificate
 * - Beneficial Ownership Documentation
 * - Articles of Organization (LLC)
 *
 * Uses Claude AI for document analysis and structured data extraction.
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Section 1B: Document-Based Identity Verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionService {

    private final ObjectMapper objectMapper;
    // TODO: Inject ClaudeService when AI integration is ready
    // private final ClaudeService claudeService;

    /**
     * Extract structured data from W-9 tax form
     *
     * @param document The W-9 document (PDF or image)
     * @return Extracted W-9 data
     */
    public W9Data extractW9Data(CollateralDocument document) {
        log.info("Extracting W-9 data from document: {}", document.getId());

        try {
            // Convert document content to Base64 for Claude API
            String base64Content = Base64.getEncoder().encodeToString(
                    document.getContent()
            );

            // Build Claude prompt for W-9 extraction
            String prompt = buildW9ExtractionPrompt();

            // TODO: Call Claude API when integration is ready
            // ClaudeResponse response = claudeService.analyzeDocument(base64Content, prompt);
            // W9Data data = parseW9Response(response.getContent());

            // PLACEHOLDER: Return mock data for now
            log.warn("Claude AI integration not yet implemented. Returning placeholder data.");
            W9Data mockData = W9Data.builder()
                    .legalName("PLACEHOLDER - AI extraction pending")
                    .taxId("XX-XXXXXXX")
                    .confidence(0.0)
                    .build();

            // Store extracted data in document
            document.setExtractedData(objectMapper.writeValueAsString(mockData));

            return mockData;

        } catch (Exception e) {
            log.error("Failed to extract W-9 data from document {}: {}",
                    document.getId(), e.getMessage(), e);
            throw new RuntimeException("W-9 extraction failed", e);
        }
    }

    /**
     * Extract structured data from Certificate of Incorporation
     *
     * @param document The incorporation certificate (PDF or image)
     * @return Extracted incorporation data
     */
    public IncorporationData extractIncorporationData(CollateralDocument document) {
        log.info("Extracting incorporation data from document: {}", document.getId());

        try {
            String base64Content = Base64.getEncoder().encodeToString(
                    document.getContent()
            );

            String prompt = buildIncorporationExtractionPrompt();

            // TODO: Call Claude API
            // ClaudeResponse response = claudeService.analyzeDocument(base64Content, prompt);
            // IncorporationData data = parseIncorporationResponse(response.getContent());

            // PLACEHOLDER
            log.warn("Claude AI integration not yet implemented. Returning placeholder data.");
            IncorporationData mockData = IncorporationData.builder()
                    .legalName("PLACEHOLDER - AI extraction pending")
                    .registrationNumber("PLACEHOLDER")
                    .jurisdiction("PLACEHOLDER")
                    .confidence(0.0)
                    .build();

            document.setExtractedData(objectMapper.writeValueAsString(mockData));

            return mockData;

        } catch (Exception e) {
            log.error("Failed to extract incorporation data from document {}: {}",
                    document.getId(), e.getMessage(), e);
            throw new RuntimeException("Incorporation extraction failed", e);
        }
    }

    /**
     * Extract structured data from Incumbency Certificate
     *
     * @param document The incumbency certificate (PDF or image)
     * @return Extracted incumbency data
     */
    public IncumbencyCertificateData extractIncumbencyData(CollateralDocument document) {
        log.info("Extracting incumbency data from document: {}", document.getId());

        try {
            String base64Content = Base64.getEncoder().encodeToString(
                    document.getContent()
            );

            String prompt = buildIncumbencyExtractionPrompt();

            // TODO: Call Claude API
            // ClaudeResponse response = claudeService.analyzeDocument(base64Content, prompt);
            // IncumbencyCertificateData data = parseIncumbencyResponse(response.getContent());

            // PLACEHOLDER
            log.warn("Claude AI integration not yet implemented. Returning placeholder data.");
            IncumbencyCertificateData mockData = IncumbencyCertificateData.builder()
                    .companyLegalName("PLACEHOLDER - AI extraction pending")
                    .confidence(0.0)
                    .build();

            document.setExtractedData(objectMapper.writeValueAsString(mockData));

            return mockData;

        } catch (Exception e) {
            log.error("Failed to extract incumbency data from document {}: {}",
                    document.getId(), e.getMessage(), e);
            throw new RuntimeException("Incumbency extraction failed", e);
        }
    }

    // ===== Prompt Building Methods =====

    /**
     * Build Claude prompt for W-9 extraction
     */
    private String buildW9ExtractionPrompt() {
        return """
            Analyze this IRS Form W-9 (Request for Taxpayer Identification Number and Certification)
            and extract the following information in JSON format:

            {
              "legalName": "Legal name as shown on Line 1",
              "businessName": "Business name/DBA from Line 2 (if different from legal name)",
              "taxClassification": "Federal tax classification (Individual, C Corp, S Corp, Partnership, Trust/Estate, LLC, Other)",
              "llcTaxClassification": "If LLC, the tax classification letter (C, S, or P)",
              "taxId": "Employer Identification Number (EIN) or Social Security Number from Part I",
              "streetAddress": "Street address from Line 5",
              "city": "City from Line 6",
              "state": "State from Line 6",
              "zipCode": "ZIP code from Line 6",
              "country": "USA (default for W-9)",
              "signatureDate": "Signature date",
              "backupWithholding": true/false (whether Part II is checked),
              "confidence": 0.0-1.0 (your confidence in the extraction accuracy)
            }

            Important notes:
            - Tax ID format: XX-XXXXXXX for EIN, XXX-XX-XXXX for SSN
            - If a field is not visible or unclear, use null
            - Set confidence lower if the document quality is poor or text is unclear
            - Look for the official IRS form structure and formatting

            Return ONLY the JSON object, no additional text.
            """;
    }

    /**
     * Build Claude prompt for incorporation certificate extraction
     */
    private String buildIncorporationExtractionPrompt() {
        return """
            Analyze this Certificate of Incorporation and extract the following information in JSON format:

            {
              "legalName": "Legal registered name of the corporation",
              "registrationNumber": "Company/registration number issued by the jurisdiction",
              "jurisdiction": "Jurisdiction of incorporation (state for US, country for international)",
              "incorporationDate": "Date of incorporation (YYYY-MM-DD format)",
              "registeredOfficeAddress": "Registered office address",
              "registeredAgentName": "Registered agent name",
              "registeredAgentAddress": "Registered agent address",
              "corporationType": "Type (C Corporation, S Corporation, LLC, PLC, Limited Company, etc.)",
              "authorizedShares": number of authorized shares (if stated),
              "parValue": par value per share (if stated),
              "initialDirectors": ["List of initial director names"],
              "incorporators": ["List of incorporator names"],
              "businessPurpose": "Business purpose/activities as stated",
              "parentCompanyMention": "Parent company name if mentioned (e.g., 'a subsidiary of XYZ Corp')",
              "parentCompanyJurisdiction": "Parent company jurisdiction if mentioned",
              "confidence": 0.0-1.0 (your confidence in the extraction accuracy)
            }

            Important notes:
            - Pay special attention to phrases indicating subsidiary relationships:
              * "subsidiary of"
              * "affiliate of"
              * "division of"
              * "wholly owned by"
            - Extract exact legal name as it appears on the certificate
            - Jurisdiction should be specific (e.g., "Delaware" not "USA", "England and Wales" not "UK")
            - Set confidence based on document clarity and completeness

            Return ONLY the JSON object, no additional text.
            """;
    }

    /**
     * Build Claude prompt for incumbency certificate extraction
     */
    private String buildIncumbencyExtractionPrompt() {
        return """
            Analyze this Incumbency Certificate (Certificate of Officers/Directors) and extract
            the following information in JSON format:

            {
              "companyLegalName": "Company legal name",
              "companyRegistrationNumber": "Company registration number (if stated)",
              "jurisdiction": "Jurisdiction (if stated)",
              "issueDate": "Certificate issue date (YYYY-MM-DD)",
              "expirationDate": "Expiration date if stated (YYYY-MM-DD)",
              "officers": [
                {
                  "name": "Full name",
                  "title": "Title (CEO, CFO, COO, President, VP, Secretary, Treasurer, etc.)",
                  "appointmentDate": "Date appointed (YYYY-MM-DD) if stated",
                  "email": "Email if provided",
                  "phoneNumber": "Phone if provided"
                }
              ],
              "directors": [
                {
                  "name": "Full name",
                  "title": "Title (Director, Chairperson, Vice Chairperson, Lead Independent Director)",
                  "appointmentDate": "Date appointed (YYYY-MM-DD) if stated",
                  "independent": true/false if stated
                }
              ],
              "authorizedSigners": [
                {
                  "name": "Full name",
                  "title": "Title/position",
                  "authorityLevel": "Full authority / Limited authority / Requires co-signer",
                  "amountLimit": dollar amount limit if specified,
                  "authorityScope": "Banking / Contracts / All transactions / specific scope"
                }
              ],
              "beneficialOwners": [
                {
                  "name": "Full name",
                  "ownershipPercentage": percentage (e.g., 35.5 for 35.5%),
                  "ownershipType": "Direct / Indirect / Voting Rights / Economic Interest",
                  "nationality": "Country of nationality",
                  "residenceCountry": "Country of residence"
                }
              ],
              "issuedBy": "Name of person who issued certificate (usually Corporate Secretary)",
              "signed": true/false,
              "notarized": true/false,
              "confidence": 0.0-1.0 (your confidence in the extraction accuracy)
            }

            Important notes:
            - This document is critical for establishing relationships between individuals and the company
            - Extract ALL individuals mentioned, even if some fields are incomplete
            - Distinguish between officers, directors, and authorized signers carefully
            - Beneficial owners are typically individuals with 25%+ ownership (FinCEN requirement)
            - Pay attention to authority limits and scopes for authorized signers

            Return ONLY the JSON object, no additional text.
            """;
    }

    // TODO: Implement response parsing methods when Claude integration is ready
    /*
    private W9Data parseW9Response(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, W9Data.class);
        } catch (Exception e) {
            log.error("Failed to parse W-9 response: {}", e.getMessage());
            throw new RuntimeException("W-9 response parsing failed", e);
        }
    }

    private IncorporationData parseIncorporationResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, IncorporationData.class);
        } catch (Exception e) {
            log.error("Failed to parse incorporation response: {}", e.getMessage());
            throw new RuntimeException("Incorporation response parsing failed", e);
        }
    }

    private IncumbencyCertificateData parseIncumbencyResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, IncumbencyCertificateData.class);
        } catch (Exception e) {
            log.error("Failed to parse incumbency response: {}", e.getMessage());
            throw new RuntimeException("Incumbency response parsing failed", e);
        }
    }
    */
}
