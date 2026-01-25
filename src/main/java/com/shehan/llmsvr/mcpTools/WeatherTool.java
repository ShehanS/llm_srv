package com.shehan.llmsvr.mcpTools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WeatherTool {
    @Tool(name = "get_weather", description = "Get current weather")
    public Map<String, Object> getWeather(
            @ToolParam(description = "The name of the city") String city
    ) {
        return Map.of(
                "city", city,
                "temperature", 32,
                "condition", "Sunny"
        );
    }
}
