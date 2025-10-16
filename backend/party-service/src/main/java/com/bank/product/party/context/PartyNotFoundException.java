package com.bank.product.party.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when party cannot be found during context resolution
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(String message) {
        super(message);
    }

    public PartyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
