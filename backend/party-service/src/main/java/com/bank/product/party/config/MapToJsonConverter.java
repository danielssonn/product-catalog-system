package com.bank.product.party.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

import java.util.Map;

/**
 * Converts Map objects to/from JSON strings for Neo4j storage
 */
public class MapToJsonConverter implements Neo4jPersistentPropertyConverter<Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Value write(Map<String, Object> source) {
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
    public Map<String, Object> read(Value source) {
        try {
            if (source.isNull()) {
                return null;
            }
            String json = source.asString();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }
}
