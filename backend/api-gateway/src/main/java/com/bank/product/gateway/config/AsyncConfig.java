package com.bank.product.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enable async processing for file uploads
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
