package com.bank.product.party.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when party is in an invalid state for context resolution
 * (e.g., inactive, suspended, closed)
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class InvalidPartyStateException extends RuntimeException {

    public InvalidPartyStateException(String message) {
        super(message);
    }

    public InvalidPartyStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
