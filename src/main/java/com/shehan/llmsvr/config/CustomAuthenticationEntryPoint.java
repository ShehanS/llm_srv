package com.shehan.llmsvr.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shehan.llmsvr.dtos.ResponseMessage;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException e) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ResponseMessage response = ResponseMessage.builder()
                    .code("401")
                    .message("Unauthorized: Missing or invalid token")
                    .error(e.getMessage())
                    .timestamp(Instant.now())
                    .build();

            try {
                byte[] data = objectMapper.writeValueAsBytes(response);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(data);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (JsonProcessingException ex) {
                return Mono.error(ex);
            }
        });
    }
}
