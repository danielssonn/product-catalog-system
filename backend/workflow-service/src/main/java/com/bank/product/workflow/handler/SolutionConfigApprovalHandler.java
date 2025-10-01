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

/**
 * Callback handler for solution configuration approval
 */
@Slf4j
@Component("SolutionConfigApprovalHandler")
public class SolutionConfigApprovalHandler implements WorkflowCallbackHandler {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;
    private final String authHeader;

    public SolutionConfigApprovalHandler(
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
        log.info("Handling solution configuration approval: entityId={}", subject.getEntityId());

        String solutionId = subject.getEntityId();

        // Call product-service to activate the solution
        activateSolution(solutionId);

        log.info("Solution configuration approved and activated: solutionId={}", solutionId);
    }

    private void activateSolution(String solutionId) {
        String url = productServiceUrl + "/api/v1/solutions/" + solutionId + "/activate";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.set("Content-Type", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Solution activated successfully: solutionId={}", solutionId);
        } catch (Exception e) {
            log.error("Error activating solution: {}", solutionId, e);
            throw new RuntimeException("Failed to activate solution", e);
        }
    }
}
