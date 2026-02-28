package org.ashkelyonok.orderservice.exception;

import java.io.Serial;

public class InvalidOrderOperationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3256571288315351263L;

    public InvalidOrderOperationException() {
        super();
    }

    public InvalidOrderOperationException(String message) {
        super(message);
    }

    public InvalidOrderOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
