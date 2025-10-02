package com.bank.product.workflow.config;

import com.bank.product.workflow.domain.service.WorkflowHandlerRegistry;
import com.bank.product.workflow.handler.SolutionConfigApprovalHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration for registering workflow callback handlers
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkflowHandlerConfig {

    private final WorkflowHandlerRegistry handlerRegistry;
    private final SolutionConfigApprovalHandler solutionConfigApprovalHandler;

    @EventListener(ApplicationReadyEvent.class)
    public void registerHandlers() {
        log.info("Registering workflow callback handlers...");

        // Register solution configuration handlers
        handlerRegistry.register("onApprove:SOLUTION_CONFIGURATION", solutionConfigApprovalHandler);
        handlerRegistry.register("onReject:SOLUTION_CONFIGURATION", solutionConfigApprovalHandler);

        log.info("Workflow callback handlers registered successfully");
        log.info("Registered handlers: {}", handlerRegistry.getRegisteredHandlers());
    }
}
