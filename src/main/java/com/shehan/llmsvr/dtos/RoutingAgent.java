package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.ModelConfig;
import com.shehan.llmsvr.entites.RoutingAgentEntity;
import com.shehan.llmsvr.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RoutingAgent extends BaseClass<RoutingAgent, RoutingAgentEntity> {
    private Integer id;
    private String routeName;
    private ModelConfig classifierModel;
    private String fallbackAgent;
    private String routingPrompt;
    private Set<Agent> agents;
    private ServiceType serviceType;
    private String serviceURL;
}
