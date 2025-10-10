package com.bank.product.gateway.parser;

import com.bank.product.gateway.dto.ProductConfigurationRecord;
import reactor.core.publisher.Flux;

import java.io.InputStream;

/**
 * Interface for file parsers
 */
public interface FileParser {

    /**
     * Parse file and return stream of product configuration records
     */
    Flux<ProductConfigurationRecord> parse(InputStream inputStream);

    /**
     * Validate file format
     */
    boolean canParse(String contentType, String fileName);
}
