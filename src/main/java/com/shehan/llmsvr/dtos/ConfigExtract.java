package com.shehan.llmsvr.dtos;

import lombok.Data;

@Data
public class ConfigExtract {
    private String method;
    private String path;
    private String mediaType;
}
