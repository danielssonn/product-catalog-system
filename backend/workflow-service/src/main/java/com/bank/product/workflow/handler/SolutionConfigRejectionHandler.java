package com.bank.product.workflow.handler;

import com.bank.product.workflow.domain.service.WorkflowCallbackHandler;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Callback handler for solution configuration rejection
 */
@Slf4j
@Component("SolutionConfigRejectionHandler")
public class SolutionConfigRejectionHandler implements WorkflowCallbackHandler {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;
    private final String authHeader;

    public SolutionConfigRejectionHandler(
            RestTemplate restTemplate,
            @Value("${product.service.url:http://product-service:8082}") String productServiceUrl,
            @Value("${product.service.username:admin}") String username,
            @Value("${product.service.password:admin123}") String password) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    @Override
    public void handle(WorkflowSubject subject) throws Exception {
        log.info("Handling solution configuration rejection: entityId={}", subject.getEntityId());

        String solutionId = subject.getEntityId();
        String rejectionReason = extractRejectionReason(subject);

        // Call product-service to mark solution as rejected
        rejectSolution(solutionId, rejectionReason);

        log.info("Solution configuration rejected: solutionId={}", solutionId);
    }

    private String extractRejectionReason(WorkflowSubject subject) {
        // Extract rejection reason from subject metadata or use default
        if (subject.getEntityMetadata() != null &&
                subject.getEntityMetadata().containsKey("rejectionReason")) {
            return (String) subject.getEntityMetadata().get("rejectionReason");
        }
        return "Workflow approval rejected";
    }

    private void rejectSolution(String solutionId, String reason) {
        String url = productServiceUrl + "/api/v1/solutions/" + solutionId + "/reject";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.set("Content-Type", "application/json");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", reason);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Solution rejected successfully: solutionId={}", solutionId);
        } catch (Exception e) {
            log.error("Error rejecting solution: {}", solutionId, e);
            throw new RuntimeException("Failed to reject solution", e);
        }
    }
}
