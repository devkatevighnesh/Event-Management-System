package com.event.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (ex.getMessage().contains("Venue not found") || ex.getMessage().contains("Event not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getMessage().contains("You are not authorized")) {
            status = HttpStatus.FORBIDDEN;
        }

        body.put("status", status.value());
        return new ResponseEntity<>(body, status);
    }
}
