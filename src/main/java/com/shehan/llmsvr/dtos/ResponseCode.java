package com.shehan.llmsvr.dtos;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("Success", "CODE-0001"),
    ERROR("error", "CODE-0002");

    private final String message;
    private final String code;


    ResponseCode(String message, String code) {
        this.message = message;
        this.code = code;
    }
}
