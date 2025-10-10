package com.bank.product.gateway.parser;

import com.bank.product.gateway.dto.ProductConfigurationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSV File Parser for product configurations
 * Expected CSV format:
 * catalogProductId,solutionName,description,customInterestRate,monthlyFee,annualFee,riskLevel,pricingVariance,businessJustification,priority
 */
@Slf4j
@Component
public class CsvFileParser implements FileParser {

    private static final String DELIMITER = ",";
    private static final int MIN_COLUMNS = 3; // catalogProductId, solutionName, description

    @Override
    public Flux<ProductConfigurationRecord> parse(InputStream inputStream) {
        return Flux.create(sink -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String headerLine = reader.readLine();
                if (headerLine == null) {
                    sink.error(new IllegalArgumentException("CSV file is empty"));
                    return;
                }

                String[] headers = headerLine.split(DELIMITER, -1);
                log.info("CSV headers: {}", String.join(", ", headers));

                AtomicInteger lineNumber = new AtomicInteger(1); // Start at 1 (header is line 0)
                String line;

                while ((line = reader.readLine()) != null) {
                    lineNumber.incrementAndGet();

                    if (line.trim().isEmpty()) {
                        continue; // Skip empty lines
                    }

                    try {
                        ProductConfigurationRecord record = parseLine(line, headers, lineNumber.get());
                        sink.next(record);
                    } catch (Exception e) {
                        log.error("Error parsing line {}: {}", lineNumber.get(), e.getMessage());
                        sink.error(new IllegalArgumentException(
                                String.format("Error parsing line %d: %s", lineNumber.get(), e.getMessage())));
                        return;
                    }
                }

                sink.complete();

            } catch (IOException e) {
                log.error("Error reading CSV file", e);
                sink.error(e);
            }
        });
    }

    private ProductConfigurationRecord parseLine(String line, String[] headers, int lineNumber) {
        String[] values = line.split(DELIMITER, -1);

        if (values.length < MIN_COLUMNS) {
            throw new IllegalArgumentException(
                    String.format("Line has %d columns, minimum %d required", values.length, MIN_COLUMNS));
        }

        Map<String, String> fieldMap = new HashMap<>();
        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
            fieldMap.put(headers[i].trim(), values[i].trim());
        }

        // Parse required fields
        String catalogProductId = getRequiredField(fieldMap, "catalogProductId", lineNumber);
        String solutionName = getRequiredField(fieldMap, "solutionName", lineNumber);
        String description = fieldMap.getOrDefault("description", "");

        // Parse optional pricing fields
        BigDecimal customInterestRate = parseBigDecimal(fieldMap.get("customInterestRate"));
        Map<String, BigDecimal> customFees = new HashMap<>();

        if (fieldMap.containsKey("monthlyFee") && !fieldMap.get("monthlyFee").isEmpty()) {
            customFees.put("MONTHLY", parseBigDecimal(fieldMap.get("monthlyFee")));
        }
        if (fieldMap.containsKey("annualFee") && !fieldMap.get("annualFee").isEmpty()) {
            customFees.put("ANNUAL", parseBigDecimal(fieldMap.get("annualFee")));
        }
        if (fieldMap.containsKey("overdraftFee") && !fieldMap.get("overdraftFee").isEmpty()) {
            customFees.put("OVERDRAFT", parseBigDecimal(fieldMap.get("overdraftFee")));
        }

        // Parse workflow metadata
        String riskLevel = fieldMap.getOrDefault("riskLevel", "LOW");
        Double pricingVariance = parseDouble(fieldMap.get("pricingVariance"));

        // Parse business fields
        String businessJustification = fieldMap.getOrDefault("businessJustification", "");
        String priority = fieldMap.getOrDefault("priority", "MEDIUM");

        return ProductConfigurationRecord.builder()
                .lineNumber(lineNumber)
                .catalogProductId(catalogProductId)
                .solutionName(solutionName)
                .description(description)
                .customInterestRate(customInterestRate)
                .customFees(customFees.isEmpty() ? null : customFees)
                .riskLevel(riskLevel)
                .pricingVariance(pricingVariance)
                .businessJustification(businessJustification)
                .priority(priority)
                .build();
    }

    private String getRequiredField(Map<String, String> fieldMap, String fieldName, int lineNumber) {
        String value = fieldMap.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Required field '%s' is missing or empty at line %d", fieldName, lineNumber));
        }
        return value.trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid decimal value: {}", value);
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid double value: {}", value);
            return null;
        }
    }

    @Override
    public boolean canParse(String contentType, String fileName) {
        return (contentType != null && contentType.contains("csv")) ||
                (fileName != null && fileName.toLowerCase().endsWith(".csv"));
    }
}
