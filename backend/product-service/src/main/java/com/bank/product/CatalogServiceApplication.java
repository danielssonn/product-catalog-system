package com.bank.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableMongoAuditing
public class CatalogServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(CatalogServiceApplication.class, args);
    }

}