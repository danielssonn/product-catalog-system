package com.bank.product.workflow.config;

import com.bank.product.workflow.temporal.activity.ValidationActivityImpl;
import com.bank.product.workflow.temporal.activity.EventPublisherActivityImpl;
import com.bank.product.workflow.temporal.activity.WorkflowActivitiesImpl;
import com.bank.product.workflow.temporal.workflow.ApprovalWorkflowImpl;
import com.bank.product.workflow.temporal.workflow.ApprovalWorkflowImplV2;
import com.bank.product.workflow.temporal.workflow.ApprovalWorkflowImplV3;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import org.springframework.web.client.RestTemplate;

/**
 * Temporal configuration for workflow service
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalConfiguration {

    @Value("${temporal.connection.target:localhost:7233}")
    private String temporalTarget;

    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

    @Value("${temporal.workflows.task-queue:workflow-task-queue}")
    private String workflowTaskQueue;

    @Value("${temporal.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${temporal.worker.max-concurrent-workflow-task-executors:100}")
    private int maxConcurrentWorkflowExecutors;

    @Value("${temporal.worker.max-concurrent-activity-executors:100}")
    private int maxConcurrentActivityExecutors;

    private final WorkflowActivitiesImpl workflowActivities;
    private final EventPublisherActivityImpl eventPublisherActivity;
    private final ValidationActivityImpl validationActivity;

    private WorkerFactory workerFactory;

    /**
     * Workflow service stubs for connecting to Temporal server
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Connecting to Temporal server: {}", temporalTarget);

        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalTarget)
                .build();

        return WorkflowServiceStubs.newServiceStubs(options);
    }

    /**
     * Workflow client for starting and signaling workflows
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Workflow client for namespace: {}", temporalNamespace);

        return WorkflowClient.newInstance(
                serviceStubs,
                io.temporal.client.WorkflowClientOptions.newBuilder()
                        .setNamespace(temporalNamespace)
                        .build()
        );
    }

    /**
     * Worker factory for managing workers
     */
    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating Worker factory");
        workerFactory = WorkerFactory.newInstance(workflowClient);
        return workerFactory;
    }

    /**
     * Workflow worker for executing workflows and activities
     */
    @Bean
    public Worker workflowWorker(WorkerFactory workerFactory) {
        if (!workerEnabled) {
            log.warn("Workflow worker is disabled");
            return null;
        }

        log.info("Creating Worker for task queue: {}", workflowTaskQueue);

        WorkerOptions workerOptions = WorkerOptions.newBuilder()
                .setMaxConcurrentWorkflowTaskExecutionSize(maxConcurrentWorkflowExecutors)
                .setMaxConcurrentActivityExecutionSize(maxConcurrentActivityExecutors)
                .build();

        Worker worker = workerFactory.newWorker(workflowTaskQueue, workerOptions);

        // Register workflow implementations
        // Note: V1, V2, V3 all implement the same ApprovalWorkflow interface
        // Can only register one at a time. Using V3 (with validation) for now.
        // V1: Callback-based workflow
        // V2: Event-driven workflow (Kafka)
        // V3: Validation-enhanced workflow (rules-based, MCP, GraphRAG)
        // worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
        // worker.registerWorkflowImplementationTypes(ApprovalWorkflowImplV2.class);
        worker.registerWorkflowImplementationTypes(ApprovalWorkflowImplV3.class);
        log.info("Registered workflows: ApprovalWorkflowImplV3 (with validation)");

        // Register activities implementations
        worker.registerActivitiesImplementations(workflowActivities, eventPublisherActivity, validationActivity);
        log.info("Registered activities: WorkflowActivitiesImpl, EventPublisherActivityImpl, ValidationActivityImpl");

        // Start worker
        workerFactory.start();
        log.info("Temporal worker started successfully on task queue: {}", workflowTaskQueue);

        return worker;
    }

    /**
     * RestTemplate for callback handlers
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Shutdown worker on application stop
     */
    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            log.info("Shutting down Temporal worker factory");
            workerFactory.shutdown();
        }
    }
}
