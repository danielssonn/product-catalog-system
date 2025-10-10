package com.bank.product.core.service;

import com.bank.product.domain.solution.model.Solution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates whether a solution is ready for provisioning to core banking systems.
 * Uses business rules to determine readiness (DMN integration planned).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisioningReadinessEvaluator {

    /**
     * Evaluate if a solution is ready to be provisioned to core banking system(s).
     *
     * @param solution the solution to evaluate
     * @return true if ready, false otherwise
     */
    public boolean isReadyForProvisioning(Solution solution) {
        log.debug("Evaluating provisioning readiness for solution: {}", solution.getId());

        List<String> failureReasons = new ArrayList<>();

        // Rule 1: Solution must have a name
        if (solution.getName() == null || solution.getName().trim().isEmpty()) {
            failureReasons.add("Solution name is missing");
        }

        // Rule 2: Solution must have a category/product type
        if (solution.getCategory() == null || solution.getCategory().trim().isEmpty()) {
            failureReasons.add("Solution category is missing");
        }

        // Rule 3: Pricing details must be configured
        if (solution.getPricing() == null) {
            failureReasons.add("Pricing configuration is missing");
        } else {
            // Validate pricing based on product category
            if (!isPricingValid(solution)) {
                failureReasons.add("Pricing configuration is incomplete or invalid");
            }
        }

        // Rule 4: Solution must have at least one channel
        if (solution.getAvailableChannels() == null || solution.getAvailableChannels().isEmpty()) {
            failureReasons.add("No channels configured");
        }

        // Rule 5: Solution must have terms configured
        if (solution.getTerms() == null) {
            failureReasons.add("Solution terms are missing");
        }

        // Rule 6: If approval required, workflow must be completed
        if (Boolean.TRUE.equals(solution.getApprovalRequired())) {
            if (solution.getWorkflowId() == null) {
                failureReasons.add("Approval workflow not started");
            }
            // Note: Assuming ACTIVE status means workflow is approved
        }

        // Rule 7: Catalog product reference must exist
        if (solution.getCatalogProductId() == null || solution.getCatalogProductId().trim().isEmpty()) {
            failureReasons.add("Catalog product reference is missing");
        }

        // Log results
        if (failureReasons.isEmpty()) {
            log.info("Solution {} is ready for provisioning", solution.getId());
            return true;
        } else {
            log.debug("Solution {} is NOT ready for provisioning. Reasons: {}",
                    solution.getId(), String.join(", ", failureReasons));
            return false;
        }
    }

    /**
     * Validate pricing configuration based on product type.
     */
    private boolean isPricingValid(Solution solution) {
        if (solution.getPricing() == null) {
            return false;
        }

        String category = solution.getCategory();

        // Checking account rules
        if ("CHECKING".equalsIgnoreCase(category)) {
            // Must have monthly fee (can be zero) and minimum balance
            return solution.getPricing().getMonthlyFee() != null &&
                   solution.getPricing().getMinimumBalance() != null;
        }

        // Savings account rules
        if ("SAVINGS".equalsIgnoreCase(category)) {
            // Must have interest rate and minimum balance
            return solution.getPricing().getInterestRate() != null &&
                   solution.getPricing().getMinimumBalance() != null;
        }

        // Loan product rules
        if ("LOAN".equalsIgnoreCase(category) || "MORTGAGE".equalsIgnoreCase(category)) {
            // Must have interest rate
            return solution.getPricing().getInterestRate() != null;
        }

        // Credit card rules
        if ("CREDIT_CARD".equalsIgnoreCase(category)) {
            // Must have interest rate and annual fee
            return solution.getPricing().getInterestRate() != null &&
                   solution.getPricing().getAnnualFee() != null;
        }

        // Investment product rules
        if ("INVESTMENT".equalsIgnoreCase(category)) {
            // Must have management fee or base price
            return solution.getPricing().getBasePrice() != null ||
                   (solution.getPricing().getAdditionalFees() != null &&
                    !solution.getPricing().getAdditionalFees().isEmpty());
        }

        // Default: pricing must have at least one non-null field
        return solution.getPricing().getMonthlyFee() != null ||
               solution.getPricing().getInterestRate() != null ||
               solution.getPricing().getAnnualFee() != null ||
               solution.getPricing().getBasePrice() != null;
    }

    /**
     * Get detailed readiness report (for debugging/UI display).
     */
    public ReadinessReport getReadinessReport(Solution solution) {
        List<String> passedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();

        // Check each rule
        if (solution.getName() != null && !solution.getName().trim().isEmpty()) {
            passedRules.add("Solution has a name");
        } else {
            failedRules.add("Solution name is missing");
        }

        if (solution.getCategory() != null && !solution.getCategory().trim().isEmpty()) {
            passedRules.add("Solution has a category");
        } else {
            failedRules.add("Solution category is missing");
        }

        if (solution.getPricing() != null && isPricingValid(solution)) {
            passedRules.add("Pricing is valid");
        } else {
            failedRules.add("Pricing is incomplete or invalid");
        }

        if (solution.getAvailableChannels() != null && !solution.getAvailableChannels().isEmpty()) {
            passedRules.add("Channels are configured");
        } else {
            failedRules.add("No channels configured");
        }

        if (solution.getTerms() != null) {
            passedRules.add("Terms are configured");
        } else {
            failedRules.add("Terms are missing");
        }

        if (solution.getCatalogProductId() != null && !solution.getCatalogProductId().trim().isEmpty()) {
            passedRules.add("Catalog product reference exists");
        } else {
            failedRules.add("Catalog product reference is missing");
        }

        if (Boolean.TRUE.equals(solution.getApprovalRequired())) {
            if (solution.getWorkflowId() != null) {
                passedRules.add("Workflow approval initiated");
            } else {
                failedRules.add("Workflow approval not started");
            }
        } else {
            passedRules.add("No approval required");
        }

        boolean ready = failedRules.isEmpty();

        return new ReadinessReport(ready, passedRules, failedRules);
    }

    /**
     * Readiness report DTO.
     */
    public static class ReadinessReport {
        private final boolean ready;
        private final List<String> passedRules;
        private final List<String> failedRules;

        public ReadinessReport(boolean ready, List<String> passedRules, List<String> failedRules) {
            this.ready = ready;
            this.passedRules = passedRules;
            this.failedRules = failedRules;
        }

        public boolean isReady() {
            return ready;
        }

        public List<String> getPassedRules() {
            return passedRules;
        }

        public List<String> getFailedRules() {
            return failedRules;
        }

        @Override
        public String toString() {
            return String.format("ReadinessReport{ready=%s, passed=%d, failed=%d}",
                    ready, passedRules.size(), failedRules.size());
        }
    }
}
