package com.library.booklending.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusinessException(BusinessException exception) {
        return ProblemDetails.create(
                exception.getStatus(),
                exception.getTitle(),
                exception.getMessage(),
                exception.getCode()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String detail = fieldError == null
                ? "Request validation failed."
                : fieldError.getField() + " " + fieldError.getDefaultMessage();

        return ProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                detail,
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "Request body is malformed or unreadable.",
                "MALFORMED_REQUEST"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return handleConstraintViolation(exception, request);
    }

    @ExceptionHandler(JpaSystemException.class)
    ProblemDetail handleJpaSystemException(
            JpaSystemException exception,
            HttpServletRequest request
    ) {
        String message = extractMessage(exception);
        if (message.contains("constraint") || message.contains("unique") || message.contains("foreign key")) {
            return handleConstraintViolation(exception, request);
        }
        throw exception;
    }

    @ExceptionHandler(ErrorResponseException.class)
    ProblemDetail handleSpringErrorResponse(ErrorResponseException exception) {
        ProblemDetail existing = exception.getBody();
        existing.setProperty("code", "REQUEST_FAILED");
        return existing;
    }

    private ProblemDetail handleConstraintViolation(Exception exception, HttpServletRequest request) {
        String message = extractMessage(exception);

        if (message.contains("book.isbn") || message.contains("isbn")) {
            return ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "A book with the same ISBN already exists.",
                    BusinessErrorCode.ISBN_ALREADY_EXISTS
            );
        }

        if (message.contains("member.email") || message.contains("email")) {
            return ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "A member with the same email already exists.",
                    BusinessErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        if (message.contains("foreign key") || message.contains("foreign_key") || message.contains("foreignkey")) {
            return ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "The resource is still referenced by loan history.",
                    BusinessErrorCode.RESOURCE_IN_USE
            );
        }

        return ProblemDetails.create(
                HttpStatus.CONFLICT,
                "Conflict",
                "The request violates a database constraint.",
                inferConstraintCode(request.getRequestURI())
        );
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        StringBuilder messages = new StringBuilder();
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage().toLowerCase(Locale.ROOT)).append(" ");
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    private BusinessErrorCode inferConstraintCode(String requestUri) {
        if (requestUri != null && requestUri.contains("/books")) {
            return BusinessErrorCode.ISBN_ALREADY_EXISTS;
        }
        if (requestUri != null && requestUri.contains("/members")) {
            return BusinessErrorCode.EMAIL_ALREADY_EXISTS;
        }
        return BusinessErrorCode.RESOURCE_IN_USE;
    }
}
