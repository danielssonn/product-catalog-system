package com.bank.product.version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Version Service Application
 * Manages API versioning, schema evolution, and backward compatibility
 */
@SpringBootApplication
@EnableMongoRepositories
public class VersionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VersionServiceApplication.class, args);
    }
}
