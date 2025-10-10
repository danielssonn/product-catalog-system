package com.bank.product.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for core banking provisioning.
 */
@Configuration
@ConfigurationProperties(prefix = "core-banking.provisioning")
public class CoreProvisioningConfig {

    private boolean enabled = true;
    private boolean autoProvisioningEnabled = true;
    private String kafkaTopic = "core-provisioning-events";
    private RetryConfig retry = new RetryConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoProvisioningEnabled() {
        return autoProvisioningEnabled;
    }

    public void setAutoProvisioningEnabled(boolean autoProvisioningEnabled) {
        this.autoProvisioningEnabled = autoProvisioningEnabled;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 5000;
        private double backoffMultiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }
    }
}
