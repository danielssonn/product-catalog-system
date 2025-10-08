package com.bank.product.party.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

import java.util.Map;

/**
 * Converts Map<String, Double> objects to/from JSON strings for Neo4j storage
 */
public class DoubleMapToJsonConverter implements Neo4jPersistentPropertyConverter<Map<String, Double>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Double>> TYPE_REF = new TypeReference<>() {};

    @Override
    public Value write(Map<String, Double> source) {
        try {
            if (source == null || source.isEmpty()) {
                return Values.NULL;
            }
            String json = objectMapper.writeValueAsString(source);
            return Values.value(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Map to JSON", e);
        }
    }

    @Override
    public Map<String, Double> read(Value source) {
        try {
            if (source.isNull()) {
                return null;
            }
            String json = source.asString();
            return objectMapper.readValue(json, TYPE_REF);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }
}
