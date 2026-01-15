package com.shehan.llmsvr.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shehan.llmsvr.entites.ToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tool extends BaseClass<Tool, ToolEntity> {
    private Integer id;
    private String name;
    private String description;
    private String type;
    private String code;
    private String url;

    @JsonProperty("schema")
    private JsonNode toolSchema;
}
