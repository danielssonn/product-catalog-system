package com.bank.product.party.sync;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Adapter for Capital Markets counterparty system
 */
@Component("CAPITAL_MARKETS")
@RequiredArgsConstructor
@Slf4j
public class CapitalMarketsAdapter implements SourceSystemAdapter {

    private final RestTemplate restTemplate;

    @Value("${capital.markets.api.url:http://localhost:8085}")
    private String baseUrl;

    @Override
    public String getSourceSystemId() {
        return "CAPITAL_MARKETS";
    }

    @Override
    public List<String> fetchAllPartyIds() {
        log.info("Fetching all counterparty IDs from Capital Markets");
        try {
            String url = baseUrl + "/api/capital-markets/counterparties/ids";
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            log.error("Failed to fetch counterparty IDs from Capital Markets", e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> fetchParty(String partyId) {
        log.info("Fetching counterparty {} from Capital Markets", partyId);
        try {
            String url = baseUrl + "/api/capital-markets/counterparties/" + partyId;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch counterparty {} from Capital Markets", partyId, e);
            return Map.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Party transformToParty(Map<String, Object> sourceData) {
        Organization org = new Organization();

        org.setLegalName((String) sourceData.get("legalName"));
        org.setLei((String) sourceData.get("lei"));
        org.setRiskRating((String) sourceData.get("riskRating"));
        org.setJurisdiction((String) sourceData.get("jurisdiction"));

        return org;
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = baseUrl + "/api/capital-markets/counterparties/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Capital Markets system unavailable", e);
            return false;
        }
    }
}
