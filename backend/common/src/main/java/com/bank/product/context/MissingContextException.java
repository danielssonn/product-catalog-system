package com.bank.product.context;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when X-Processing-Context header is missing from request
 *
 * This typically indicates the request did not pass through the API Gateway
 * or the context resolution step failed.
 *
 * @author System Architecture Team
 * @since 1.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingContextException extends RuntimeException {

    public MissingContextException(String message) {
        super(message);
    }

    public MissingContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
