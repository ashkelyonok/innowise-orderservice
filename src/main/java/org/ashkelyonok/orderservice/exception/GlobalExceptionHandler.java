package org.ashkelyonok.orderservice.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.model.dto.error.ErrorResponseDto;
import org.ashkelyonok.orderservice.model.dto.error.ValidationErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Endpoint not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderOperationException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOrderOperation(InvalidOrderOperationException ex, HttpServletRequest request) {
        log.warn("Invalid operation attempt: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed for {}", request.getRequestURI());

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        ValidationErrorResponseDto errorResponse = ValidationErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Parameter", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Critical service unavailable: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponseDto> handleFeignNotFound(FeignException.NotFound ex, HttpServletRequest request) {
        log.warn("External resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "External Resource Not Found", "The requested user or resource was not found in the external service.", request);
    }

    @ExceptionHandler(FeignException.BadRequest.class)
    public ResponseEntity<ErrorResponseDto> handleFeignBadRequest(FeignException.BadRequest ex, HttpServletRequest request) {
        log.warn("Invalid request to external service: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid External Request", "The request rejected by the external service due to invalid data.", request);
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponseDto> handleFeignForbidden(FeignException.Forbidden ex, HttpServletRequest request) {
        log.warn("Access denied by external service: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied", "You are not allowed to perform operations on this user.", request);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponseDto> handleFeignException(FeignException ex, HttpServletRequest request) {
        log.error("External Feign Error: Status {} - Body {}", ex.status(), ex.contentUTF8());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "External Dependency Error", "Failed to communicate with external service", request);
    }

    @ExceptionHandler(InconsistentDataException.class)
    public ResponseEntity<ErrorResponseDto> handleDataInconsistency(InconsistentDataException ex, HttpServletRequest request) {
        log.error("DATA INTEGRITY ERROR: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Data Integrity Error", "An internal data consistency check failed", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String errorType, String message, HttpServletRequest request) {
        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorType)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
