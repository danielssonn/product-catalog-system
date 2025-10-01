package com.bank.product.workflow.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for workflow callback handlers
 * Allows plugins to register handlers for entity-specific actions
 */
@Slf4j
@Component
public class WorkflowHandlerRegistry {

    private final Map<String, WorkflowCallbackHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Register a callback handler
     *
     * @param key unique key (e.g., "onApprove:SOLUTION_CONFIGURATION")
     * @param handler callback handler implementation
     */
    public void register(String key, WorkflowCallbackHandler handler) {
        log.info("Registering workflow handler: {}", key);
        handlers.put(key, handler);
    }

    /**
     * Get callback handler
     *
     * @param callbackType callback type (onApprove, onReject, onTimeout)
     * @param entityType entity type
     * @return handler or null if not found
     */
    public WorkflowCallbackHandler getHandler(String callbackType, String entityType) {
        String key = callbackType + ":" + entityType;
        return handlers.get(key);
    }

    /**
     * Check if handler exists
     *
     * @param callbackType callback type
     * @param entityType entity type
     * @return true if handler is registered
     */
    public boolean hasHandler(String callbackType, String entityType) {
        String key = callbackType + ":" + entityType;
        return handlers.containsKey(key);
    }

    /**
     * Remove handler
     *
     * @param key handler key
     */
    public void unregister(String key) {
        log.info("Unregistering workflow handler: {}", key);
        handlers.remove(key);
    }

    /**
     * Get all registered handler keys
     *
     * @return set of handler keys
     */
    public java.util.Set<String> getRegisteredHandlers() {
        return handlers.keySet();
    }
}
