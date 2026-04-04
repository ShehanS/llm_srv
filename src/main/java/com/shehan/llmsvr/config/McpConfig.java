package com.shehan.llmsvr.config;

import com.shehan.llmsvr.mcpTools.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class McpConfig {

    @Bean
    public List<ToolCallback> mcpToolCallbacks(WeatherTool weatherTool, SQLTool sqlTool, ApprovalTool approvalTool, MovieTool movieTool, FoodOrderTool foodOrderTool) {
        List<ToolCallback> allTools = new ArrayList<>();
        allTools.addAll(Arrays.asList(ToolCallbacks.from(weatherTool)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(sqlTool)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(approvalTool)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(movieTool)));
        //allTools.addAll(Arrays.asList(ToolCallbacks.from(foodOrderTool)));
        return allTools;
    }
}
