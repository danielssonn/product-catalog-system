package com.bank.product.client;

import com.bank.product.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Client for communicating with the workflow service
 */
@Slf4j
@Component
public class WorkflowClient {

    private final RestTemplate restTemplate;
    private final String workflowServiceUrl;
    private final String authHeader;

    public WorkflowClient(
            RestTemplate restTemplate,
            @Value("${workflow.service.url:http://workflow-service:8089}") String workflowServiceUrl,
            @Value("${workflow.service.username:admin}") String username,
            @Value("${workflow.service.password:admin}") String password) {
        this.restTemplate = restTemplate;
        this.workflowServiceUrl = workflowServiceUrl;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    /**
     * Submit a workflow for approval
     */
    public WorkflowSubmitResponse submitWorkflow(WorkflowSubmitRequest request) {
        log.info("Submitting workflow to workflow service: entity={}, id={}",
                request.getEntityType(), request.getEntityId());

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<WorkflowSubmitRequest> entity =
                    new org.springframework.http.HttpEntity<>(request, headers);

            ResponseEntity<WorkflowSubmitResponse> response = restTemplate.postForEntity(
                    workflowServiceUrl + "/api/v1/workflows/submit",
                    entity,
                    WorkflowSubmitResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Workflow submitted successfully: workflowId={}",
                        response.getBody().getWorkflowId());
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to submit workflow: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error submitting workflow", e);
            throw new RuntimeException("Failed to submit workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Get workflow status
     */
    public WorkflowStatusResponse getWorkflowStatus(String workflowId) {
        log.info("Getting workflow status: workflowId={}", workflowId);

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", authHeader);

            org.springframework.http.HttpEntity<Void> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<WorkflowStatusResponse> response = restTemplate.exchange(
                    workflowServiceUrl + "/api/v1/workflows/" + workflowId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    WorkflowStatusResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get workflow status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error getting workflow status", e);
            throw new RuntimeException("Failed to get workflow status: " + e.getMessage(), e);
        }
    }
}
