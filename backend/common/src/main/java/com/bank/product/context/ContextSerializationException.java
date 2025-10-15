package com.bank.product.context;

/**
 * Exception thrown when ProcessingContext serialization/deserialization fails
 *
 * @author System Architecture Team
 * @since 1.0
 */
public class ContextSerializationException extends RuntimeException {

    public ContextSerializationException(String message) {
        super(message);
    }

    public ContextSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
