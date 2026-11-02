package com.pulsepay.common;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Applies the JSON error contract only to PulsePay API controllers, never Actuator endpoints. */
@RestControllerAdvice(basePackages = "com.pulsepay")
class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> known(ApiException e) { return ResponseEntity.status(e.status()).body(Map.of("error", Map.of("code", e.code(), "message", e.getMessage(), "request_id", requestId()))); }
 @ExceptionHandler(Exception.class) ResponseEntity<?> unknown(Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", Map.of("code", "INTERNAL_ERROR", "message", "An unexpected error occurred.", "request_id", requestId()))); }
 private String requestId(){String id=MDC.get("request_id");return id==null?UUID.randomUUID().toString():id;}
}

