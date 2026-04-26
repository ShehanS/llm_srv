package com.shehan.llmsvr.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ToolType {
    MCP,
    DYNAMIC,
    DEFAULT,
    JS,
    JSON,
    COMMON;
    @JsonCreator
    public static ToolType fromValue(String value) {
        return ToolType.valueOf(value.toUpperCase());
    }
}
