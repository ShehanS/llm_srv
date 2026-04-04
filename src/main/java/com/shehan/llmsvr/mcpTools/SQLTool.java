package com.shehan.llmsvr.mcpTools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SQLTool {
    private final JdbcClient jdbcClient;

    public SQLTool(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Tool(name = "sql_tool", description = "Execute SQL queries. Use for SELECT, INSERT, UPDATE, or DELETE.")
    public Object executeQuery(
            @ToolParam(description = "The full SQL query") String query
    ) {
        String trimmed = query.trim().toUpperCase();
        if (trimmed.startsWith("SELECT") || trimmed.startsWith("DESCRIBE") || trimmed.startsWith("SHOW")) {
            return jdbcClient.sql(query).query().listOfRows();
        } else {
            // This handles INSERT, UPDATE, DELETE
            int rowsAffected = jdbcClient.sql(query).update();
            return Map.of("status", "success", "rows_affected", rowsAffected);
        }
    }
}
