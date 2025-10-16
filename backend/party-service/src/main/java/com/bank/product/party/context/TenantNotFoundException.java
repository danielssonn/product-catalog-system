package com.bank.product.party.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when tenant cannot be resolved from party
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }

    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
