package com.bank.product.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Workflow Service Application
 *
 * Provides Temporal-based workflow orchestration and approval management.
 * Supports extensible maker/checker workflows with rule-based routing.
 */
@SpringBootApplication
@EnableMongoRepositories
@EnableKafka
public class WorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}
