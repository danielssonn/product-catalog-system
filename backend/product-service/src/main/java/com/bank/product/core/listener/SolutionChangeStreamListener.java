package com.bank.product.core.listener;

import com.bank.product.core.config.CoreProvisioningConfig;
import com.bank.product.core.model.CoreProvisioningStatus;
import com.bank.product.core.service.ProvisioningReadinessEvaluator;
import com.bank.product.core.service.CoreProvisioningOrchestrator;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Listens to MongoDB change streams on the solutions collection.
 * Triggers auto-provisioning when solutions are ready.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "core-banking.provisioning.auto-provisioning-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SolutionChangeStreamListener {

    private final MongoClient mongoClient;
    private final MongoTemplate mongoTemplate;
    private final ProvisioningReadinessEvaluator readinessEvaluator;
    private final CoreProvisioningOrchestrator orchestrator;

    private ExecutorService executorService;
    private volatile boolean running = false;

    @PostConstruct
    public void startListening() {
        log.info("Starting MongoDB change stream listener for solutions collection");

        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "solution-change-stream-listener");
            thread.setDaemon(true);
            return thread;
        });

        running = true;
        executorService.submit(this::listenToChangeStream);
    }

    @PreDestroy
    public void stopListening() {
        log.info("Stopping MongoDB change stream listener");
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void listenToChangeStream() {
        String databaseName = mongoTemplate.getDb().getName();
        MongoCollection<Document> collection = mongoClient
                .getDatabase(databaseName)
                .getCollection("solutions");

        log.info("Watching change stream for collection: solutions in database: {}", databaseName);

        ChangeStreamIterable<Document> changeStream = collection.watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP);

        try {
            for (ChangeStreamDocument<Document> change : changeStream) {
                if (!running) {
                    log.info("Change stream listener stopped");
                    break;
                }

                try {
                    processChange(change);
                } catch (Exception e) {
                    log.error("Error processing change stream event", e);
                }
            }
        } catch (Exception e) {
            log.error("Change stream listener encountered error", e);
            if (running) {
                // Attempt to restart after delay
                log.info("Attempting to restart change stream listener in 5 seconds...");
                try {
                    Thread.sleep(5000);
                    if (running) {
                        listenToChangeStream();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        OperationType operationType = change.getOperationType();
        Document fullDocument = change.getFullDocument();

        if (fullDocument == null) {
            return;
        }

        log.debug("Change stream event: {} for solution: {}",
                operationType, fullDocument.getString("_id"));

        // Convert Document to Solution
        Solution solution = mongoTemplate.getConverter().read(Solution.class, fullDocument);

        // Only process updates and inserts
        if (operationType == OperationType.UPDATE || operationType == OperationType.INSERT) {
            handleSolutionChange(solution, operationType);
        } else if (operationType == OperationType.DELETE) {
            log.debug("Solution deleted: {}", solution.getId());
        }
    }

    private void handleSolutionChange(Solution solution, OperationType operationType) {
        log.debug("Processing {} for solution: {} (status: {})",
                operationType, solution.getId(), solution.getStatus());

        // Check if solution is approved and active
        if (solution.getStatus() != SolutionStatus.ACTIVE) {
            log.debug("Solution {} not active, skipping provisioning check", solution.getId());
            return;
        }

        // Check if already provisioned
        if (isAlreadyProvisioned(solution)) {
            log.debug("Solution {} already provisioned, checking for updates", solution.getId());
            handleProvisionedSolutionUpdate(solution);
            return;
        }

        // Check if ready for provisioning
        if (shouldTriggerProvisioning(solution)) {
            log.info("Solution {} is ready for auto-provisioning", solution.getId());
            triggerAutoProvisioning(solution);
        } else {
            log.debug("Solution {} not ready for provisioning", solution.getId());
        }
    }

    private boolean isAlreadyProvisioned(Solution solution) {
        if (solution.getCoreProvisioningRecords() == null ||
            solution.getCoreProvisioningRecords().isEmpty()) {
            return false;
        }

        return solution.getCoreProvisioningRecords().stream()
                .anyMatch(record -> record.getStatus() == CoreProvisioningStatus.PROVISIONED);
    }

    private boolean shouldTriggerProvisioning(Solution solution) {
        // Solution must be active
        if (solution.getStatus() != SolutionStatus.ACTIVE) {
            return false;
        }

        // Check if workflow approval is required and completed
        if (Boolean.TRUE.equals(solution.getApprovalRequired())) {
            if (solution.getWorkflowId() == null) {
                log.debug("Solution {} requires approval but no workflow ID", solution.getId());
                return false;
            }
            // Assume if solution is ACTIVE, workflow is approved
        }

        // Evaluate readiness rules
        boolean isReady = readinessEvaluator.isReadyForProvisioning(solution);

        log.debug("Solution {} readiness evaluation: {}", solution.getId(), isReady);
        return isReady;
    }

    private void handleProvisionedSolutionUpdate(Solution solution) {
        log.info("Handling update for already provisioned solution: {}", solution.getId());

        // Detect configuration changes and sync to core systems
        try {
            orchestrator.updateSolution(solution);
            log.info("Successfully synced solution {} updates to core systems", solution.getId());
        } catch (Exception e) {
            log.error("Failed to sync solution {} updates to core systems", solution.getId(), e);
        }
    }

    private void triggerAutoProvisioning(Solution solution) {
        log.info("Triggering auto-provisioning for solution: {}", solution.getId());

        try {
            orchestrator.provisionSolution(solution);
            log.info("Successfully triggered provisioning for solution: {}", solution.getId());
        } catch (Exception e) {
            log.error("Failed to provision solution: {}", solution.getId(), e);
        }
    }
}
