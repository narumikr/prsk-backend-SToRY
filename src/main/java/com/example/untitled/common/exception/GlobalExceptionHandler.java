package com.example.untitled.common.exception;

import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.dto.ErrorResponse;
import com.example.untitled.common.dto.ErrorResponseWithDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseWithDetails> handleBadRequestError(
            MethodArgumentNotValidException exception
    ) {
        List<ErrorDetails> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErrorDetails(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ErrorResponseWithDetails error = new ErrorResponseWithDetails(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "Validation failed",
                details

        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseWithDetails> handleConstrainViolation(
            ConstraintViolationException exception
    ) {
        List<ErrorDetails> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String field = path.substring(path.lastIndexOf('.') + 1);
                    return new ErrorDetails(
                            field,
                            violation.getMessage()
                    );
                })
                .collect(Collectors.toList());

        ErrorResponseWithDetails error = new ErrorResponseWithDetails(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "Validation failed",
                details
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseWithDetails> handleBadRequestError(
            BadRequestException exception
    ) {
        ErrorResponseWithDetails error = new ErrorResponseWithDetails(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                exception.getMessage(),
                exception.getDetails()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseWithDetails> handleUnauthorizedError(
            UnauthorizedException exception
    ) {
        ErrorResponseWithDetails error = new ErrorResponseWithDetails(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                exception.getMessage(),
                exception.getDetails()
        );

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundError(
            EntityNotFoundException exception
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.name(),
                exception.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * 409 Conflict
     */
    @ExceptionHandler(DuplicationResourceException.class)
    public ResponseEntity<ErrorResponseWithDetails> handleDuplicateResourceError(
            DuplicationResourceException exception
    ) {
        ErrorResponseWithDetails error = new ErrorResponseWithDetails(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.name(),
                exception.getMessage(),
                exception.getDetails()
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}
