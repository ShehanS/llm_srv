package com.shehan.llmsvr.helper;

import java.util.List;
import java.util.Map;

public class ExpressionResolver {

    public static Object resolve(
            String expression,
            Map<String, Object> context
    ) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        // Remove the {{ }} markers and trim
        String exp = expression
                .replace("{{", "")
                .replace("}}", "")
                .trim();

        // If requesting "all", return entire context
        if ("all".equals(exp)) {
            return context;
        }

        // Split by dots to get the path
        String[] parts = exp.split("\\.");

        if (parts.length == 0) {
            return null;
        }

        // Start with the root key
        Object current = context.get(parts[0]);

        if (current == null) {
            return null;
        }

        // Navigate through the path
        for (int i = 1; i < parts.length; i++) {
            current = navigateToKey(current, parts[i]);

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private static Object navigateToKey(Object obj, String key) {
        if (obj == null) {
            return null;
        }

        // Handle Map navigation
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            return map.get(key);
        }

        // Handle List navigation (for array indices)
        if (obj instanceof List && isNumeric(key)) {
            List<?> list = (List<?>) obj;
            int index = Integer.parseInt(key);
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
            return null;
        }

        // Try to access as property using reflection (optional, for POJOs)
        try {
            var field = obj.getClass().getDeclaredField(key);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            // Field doesn't exist or can't access
            return null;
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
