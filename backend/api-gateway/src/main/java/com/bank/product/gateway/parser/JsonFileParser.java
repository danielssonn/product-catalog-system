package com.bank.product.gateway.parser;

import com.bank.product.gateway.dto.ProductConfigurationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON File Parser for product configurations
 * Expected JSON format: Array of product configuration objects
 * [{
 *   "catalogProductId": "premium-checking-001",
 *   "solutionName": "My Premium Checking",
 *   "description": "Custom checking account",
 *   "customInterestRate": 2.5,
 *   "customFees": {"MONTHLY": 15.00, "OVERDRAFT": 30.00},
 *   "riskLevel": "LOW",
 *   "pricingVariance": 5.0,
 *   "businessJustification": "Special customer tier",
 *   "priority": "MEDIUM"
 * }, ...]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final ObjectMapper objectMapper;

    @Override
    public Flux<ProductConfigurationRecord> parse(InputStream inputStream) {
        return Flux.create(sink -> {
            try {
                List<ProductConfigurationRecord> records = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<ProductConfigurationRecord>>() {}
                );

                if (records == null || records.isEmpty()) {
                    sink.error(new IllegalArgumentException("JSON file contains no records"));
                    return;
                }

                log.info("Parsed {} records from JSON file", records.size());

                AtomicInteger lineNumber = new AtomicInteger(0);
                records.forEach(record -> {
                    record.setLineNumber(lineNumber.incrementAndGet());

                    // Validate required fields
                    if (record.getCatalogProductId() == null || record.getCatalogProductId().trim().isEmpty()) {
                        sink.error(new IllegalArgumentException(
                                String.format("Record %d: catalogProductId is required", lineNumber.get())));
                        return;
                    }
                    if (record.getSolutionName() == null || record.getSolutionName().trim().isEmpty()) {
                        sink.error(new IllegalArgumentException(
                                String.format("Record %d: solutionName is required", lineNumber.get())));
                        return;
                    }

                    sink.next(record);
                });

                sink.complete();

            } catch (IOException e) {
                log.error("Error parsing JSON file", e);
                sink.error(new IllegalArgumentException("Invalid JSON format: " + e.getMessage()));
            }
        });
    }

    @Override
    public boolean canParse(String contentType, String fileName) {
        return (contentType != null && contentType.contains("json")) ||
                (fileName != null && fileName.toLowerCase().endsWith(".json"));
    }
}
