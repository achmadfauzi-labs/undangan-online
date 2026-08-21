package com.undangan.online.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(new com.undangan.online.dto.ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validasi gagal");
        return ResponseEntity.badRequest()
                .body(new com.undangan.online.dto.ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new com.undangan.online.dto.ErrorResponse("BAD_REQUEST", "Format request tidak valid"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.badRequest()
                .body(new com.undangan.online.dto.ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new com.undangan.online.dto.ErrorResponse("ERROR", ex.getReason()));
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleNotFound(org.springframework.web.servlet.NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new com.undangan.online.dto.ErrorResponse("NOT_FOUND", "Endpoint tidak ditemukan"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.undangan.online.dto.ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new com.undangan.online.dto.ErrorResponse("INTERNAL_ERROR", "Terjadi kesalahan sistem"));
    }
}
