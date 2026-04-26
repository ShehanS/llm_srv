package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainConfig {

    private List<Agent> agents;
    private RoutingAgent routing;
    private Set<String> dangerousTools;
}
