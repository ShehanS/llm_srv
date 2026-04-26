package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseMessage {
    private String code;
    private String message;
    private Object data;
    private Object error;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static ResponseMessage getInstance(ResponseCode responseCode, Object data, Object error) {
        return ResponseMessage.builder()
                .code(responseCode.getCode())
                .message(responseCode.getMessage())
                .data(data)
                .error(error)
                .timestamp(Instant.now())
                .build();
    }
}
