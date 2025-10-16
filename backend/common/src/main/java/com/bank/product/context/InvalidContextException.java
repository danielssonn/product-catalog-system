package com.bank.product.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when ProcessingContext is invalid or expired
 *
 * This can occur when:
 * - Context is older than 5 minutes
 * - Context is malformed
 * - Party status is not ACTIVE
 * - Required fields are missing
 *
 * @author System Architecture Team
 * @since 1.0
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidContextException extends RuntimeException {

    public InvalidContextException(String message) {
        super(message);
    }

    public InvalidContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
