package com.bank.product.party.sync;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Adapter for Commercial Banking party system
 */
@Component("COMMERCIAL_BANKING")
@RequiredArgsConstructor
@Slf4j
public class CommercialBankingAdapter implements SourceSystemAdapter {

    private final RestTemplate restTemplate;

    @Value("${commercial.banking.api.url:http://localhost:8084}")
    private String baseUrl;

    @Override
    public String getSourceSystemId() {
        return "COMMERCIAL_BANKING";
    }

    @Override
    public List<String> fetchAllPartyIds() {
        log.info("Fetching all party IDs from Commercial Banking");
        try {
            String url = baseUrl + "/api/commercial-banking/parties/ids";
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            log.error("Failed to fetch party IDs from Commercial Banking", e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> fetchParty(String partyId) {
        log.info("Fetching party {} from Commercial Banking", partyId);
        try {
            String url = baseUrl + "/api/commercial-banking/parties/" + partyId;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch party {} from Commercial Banking", partyId, e);
            return Map.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Party transformToParty(Map<String, Object> sourceData) {
        Organization org = new Organization();

        org.setName((String) sourceData.get("businessName"));
        org.setLegalName((String) sourceData.get("legalName"));
        org.setRegistrationNumber((String) sourceData.get("registrationNumber"));
        org.setJurisdiction((String) sourceData.get("jurisdiction"));

        if (sourceData.get("incorporationDate") != null) {
            String dateStr = sourceData.get("incorporationDate").toString();
            if (dateStr.length() > 10) {
                dateStr = dateStr.substring(0, 10);
            }
            try {
                org.setIncorporationDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.warn("Failed to parse incorporation date: {}", dateStr);
            }
        }

        org.setIndustry((String) sourceData.get("industry"));
        org.setIndustryCode((String) sourceData.get("industryCode"));
        org.setTier((String) sourceData.get("tier"));
        org.setRiskRating((String) sourceData.get("riskRating"));
        org.setAmlStatus((String) sourceData.get("amlStatus"));
        org.setPhoneNumber((String) sourceData.get("phoneNumber"));
        org.setEmail((String) sourceData.get("email"));
        org.setWebsite((String) sourceData.get("website"));

        if (sourceData.get("employeeCount") != null) {
            org.setEmployeeCount(((Number) sourceData.get("employeeCount")).intValue());
        }
        if (sourceData.get("annualRevenue") != null) {
            org.setAnnualRevenue(((Number) sourceData.get("annualRevenue")).doubleValue());
        }

        return org;
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = baseUrl + "/api/commercial-banking/parties/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Commercial Banking system unavailable", e);
            return false;
        }
    }
}
