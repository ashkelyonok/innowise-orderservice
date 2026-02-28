package org.ashkelyonok.orderservice.exception;

import java.io.Serial;

public class ServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8804734578487526630L;

    public ServiceUnavailableException() {
        super("Service unavailable");
    }

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
