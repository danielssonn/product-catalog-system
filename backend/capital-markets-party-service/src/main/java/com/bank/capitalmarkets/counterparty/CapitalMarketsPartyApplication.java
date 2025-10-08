package com.bank.capitalmarkets.counterparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class CapitalMarketsPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapitalMarketsPartyApplication.class, args);
    }
}
