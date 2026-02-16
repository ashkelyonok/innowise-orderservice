package org.ashkelyonok.orderservice.exception;

import java.io.Serial;

public class InconsistentDataException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4193341303346479577L;

    public InconsistentDataException() {
        super("Data consistency error");
    }

    public InconsistentDataException(String message) {
        super(message);
    }

    public InconsistentDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
