package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent extends BaseClass<Agent, AgentEntity> {
    private Integer id;
    private String agentName;
    private String displayName;
    private String description;
    private String expertise;
    private Boolean isDefault;
    private ModelConfig model;
    private String systemPrompt;
    private List<Tool> tools;
}
