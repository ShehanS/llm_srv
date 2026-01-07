package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.AgentEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agent extends BaseClass<Agent, AgentEntity> {
    private Integer id;
    private String agentName;
    private String displayName;
    private String description;
    private String expertise;
    private Boolean isDefault = false;
    private ModelConfig model;
    private String systemPrompt;
    private String[] tools;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelConfig {
        private String provider;
        private String name;
        private Double temperature;
        private String apiKey;
    }
}
