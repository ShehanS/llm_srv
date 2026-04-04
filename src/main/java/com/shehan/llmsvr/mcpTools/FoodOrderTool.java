package com.shehan.llmsvr.mcpTools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FoodOrderTool {

    @Tool(name = "food_order_tool", description = "Mandatory tool to finalize and submit a food order. Use this as soon as Name, Address, Phone, and Items are known.")
    public String placeFoodOrder(
            @ToolParam(description = "User's full name") String name,
            @ToolParam(description = "Delivery address") String address,
            @ToolParam(description = "Contact phone number") String phone,
            @ToolParam(description = "List of food items and quantities (e.g., 2 Burgers, 1 Coke)") String orderItems,
            @ToolParam(description = "Any special delivery instructions") String instructions
    ) {
        return String.format("ORDER CONFIRMED: %s, your order for [%s] is being prepared for delivery to %s.",
                name, orderItems, address);
    }
}
