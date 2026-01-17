package com.shehan.llmsvr.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionResolver {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    public static Map<String, Object> resolve(
            Map<String, Object> config,
            String mapperName,
            Map<String, Object> context,
            Map<String, Object> fallback
    ) {
        List<Map<String, String>> mappings = NodeConfigUtil.getMapperMap(config, mapperName, null);
        Map<String, String> flowIdMap = new HashMap<>();
        if (mappings == null || mappings.isEmpty()) {
            return new HashMap<>(fallback);
        }

        Map<String, Object> result = new HashMap<>();

        for (Map<String, String> mapping : mappings) {
            String key = mapping.get("key");
            String valueExpr = mapping.get("value");

            if (key == null || key.isBlank()) continue;

            result.put(key, resolve(valueExpr, context));
        }

        return result;
    }

    public static Object resolve(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        if (expression.startsWith("{{") && expression.endsWith("}}") && countOccurrences(expression, "{{") == 1) {
            String path = expression.substring(2, expression.length() - 2).trim();
            return resolvePath(path, context);
        }

        StringBuilder sb = new StringBuilder();
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(expression, lastEnd, matcher.start());
            Object resolved = resolvePath(matcher.group(1).trim(), context);
            sb.append(resolved != null ? resolved.toString() : "");
            lastEnd = matcher.end();
        }
        sb.append(expression.substring(lastEnd));

        return sb.toString();
    }

    private static Object resolvePath(String path, Map<String, Object> context) {
        if ("all".equals(path)) return context;

        String[] parts = path.split("\\.");
        Object current = context;

        for (String part : parts) {
            current = navigate(current, part);
            if (current == null) return null;
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Object navigate(Object obj, String key) {
        if (obj == null) return null;

        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(key);
        }

        if (obj instanceof List && isNumeric(key)) {
            List<?> list = (List<?>) obj;
            int index = Integer.parseInt(key);
            return (index >= 0 && index < list.size()) ? list.get(index) : null;
        }

        try {
            var field = obj.getClass().getDeclaredField(key);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private static int countOccurrences(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
