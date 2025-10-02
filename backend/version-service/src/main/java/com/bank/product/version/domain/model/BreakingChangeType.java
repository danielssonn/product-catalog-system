package com.bank.product.version.domain.model;

/**
 * Types of Breaking Changes
 */
public enum BreakingChangeType {
    /**
     * Field removed from request/response
     */
    FIELD_REMOVED,

    /**
     * Field renamed
     */
    FIELD_RENAMED,

    /**
     * Field type changed
     */
    FIELD_TYPE_CHANGED,

    /**
     * Required field added
     */
    REQUIRED_FIELD_ADDED,

    /**
     * Endpoint removed
     */
    ENDPOINT_REMOVED,

    /**
     * Endpoint URL changed
     */
    ENDPOINT_URL_CHANGED,

    /**
     * HTTP method changed
     */
    HTTP_METHOD_CHANGED,

    /**
     * Response format changed
     */
    RESPONSE_FORMAT_CHANGED,

    /**
     * Error codes changed
     */
    ERROR_CODES_CHANGED,

    /**
     * Authentication mechanism changed
     */
    AUTH_CHANGED,

    /**
     * Validation rules changed
     */
    VALIDATION_CHANGED,

    /**
     * Default behavior changed
     */
    BEHAVIOR_CHANGED
}
