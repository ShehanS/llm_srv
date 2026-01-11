package com.shehan.llmsvr.helper;

import java.util.Map;

public class ExpressionResolver {

    public static Object resolve(
            String expression,
            Map<String, Object> context
    ) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        String exp = expression
                .replace("{{", "")
                .replace("}}", "")
                .trim();

        if ("all".equals(exp)) {
            return context;
        }

        String[] parts = exp.split("\\.");

        Object current = context.get(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(parts[i]);
        }

        return current;
    }
}
