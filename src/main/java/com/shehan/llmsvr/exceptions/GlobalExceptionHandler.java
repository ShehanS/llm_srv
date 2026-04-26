package com.shehan.llmsvr.exceptions;

import com.shehan.llmsvr.dtos.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ResponseMessage>> handleUnauthorized(AuthenticationException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid or missing authentication token.", exchange);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ResponseMessage>> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied: You do not have the required roles.", exchange);
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ResponseMessage>> handleRuntimeException(RuntimeException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ResponseMessage>> handleGeneralException(Exception ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", exchange);
    }

    private Mono<ResponseEntity<ResponseMessage>> buildResponse(HttpStatus status, String message, ServerWebExchange exchange) {
        ResponseMessage body = ResponseMessage.builder()
                .code(String.valueOf(status.value()))
                .message(message)
                .error(status.getReasonPhrase())
                .data(Map.of("path", exchange.getRequest().getPath().value()))
                .timestamp(Instant.now())
                .build();

        return Mono.just(new ResponseEntity<>(body, status));
    }
}
