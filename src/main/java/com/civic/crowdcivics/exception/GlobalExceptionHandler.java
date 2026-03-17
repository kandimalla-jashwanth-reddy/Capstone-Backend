package com.civic.crowdcivics.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<Map<String, String>> handleVerificationException(VerificationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Verification Failed");
        response.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
