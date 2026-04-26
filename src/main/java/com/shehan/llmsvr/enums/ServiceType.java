package com.shehan.llmsvr.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ServiceType {
    CLOUD,
    LOCAL;
    @JsonCreator
    public static ToolType fromValue(String value) {
        return ToolType.valueOf(value.toUpperCase());
    }
}
