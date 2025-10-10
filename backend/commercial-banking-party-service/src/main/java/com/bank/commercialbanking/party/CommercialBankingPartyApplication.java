package com.bank.commercialbanking.party;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class CommercialBankingPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommercialBankingPartyApplication.class, args);
    }
}
