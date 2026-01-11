package com.shehan.llmsvr.helper;

import java.util.List;
import java.util.Map;

public final class NodeConfigUtil {

    private NodeConfigUtil() {}

    // --------------------------------------------------
    // Generic getter - returns Object
    // --------------------------------------------------
    @SuppressWarnings("unchecked")
    public static <T> T getInputProp(
            Map<String, Object> config,
            String name,
            T fallback
    ) {
        if (config == null) return fallback;

        Object propsObj = config.get("inputProps");
        if (!(propsObj instanceof List<?> props)) return fallback;

        for (Object p : props) {
            if (!(p instanceof Map<?, ?> prop)) continue;

            if (!name.equals(prop.get("name"))) continue;

            Object value = prop.get("value");
            if (value != null) {
                // Handle different types
                if (fallback instanceof Boolean) {
                    return (T) Boolean.valueOf(String.valueOf(value));
                } else if (fallback instanceof Integer) {
                    return (T) Integer.valueOf(String.valueOf(value));
                } else if (fallback instanceof List) {
                    return (T) value;
                } else {
                    // String or other types
                    if (!String.valueOf(value).isBlank()) {
                        return (T) value;
                    }
                }
            }

            Object def = prop.get("defaultValue");
            if (def != null) {
                if (fallback instanceof Boolean) {
                    return (T) Boolean.valueOf(String.valueOf(def));
                } else if (fallback instanceof Integer) {
                    return (T) Integer.valueOf(String.valueOf(def));
                } else if (fallback instanceof List) {
                    return (T) def;
                } else {
                    if (!String.valueOf(def).isBlank()) {
                        return (T) def;
                    }
                }
            }
        }
        return fallback;
    }

    // --------------------------------------------------
    // String-specific getter (backward compatibility)
    // --------------------------------------------------
    public static String getInputProp(
            Map<String, Object> config,
            String name,
            String fallback
    ) {
        return getInputProp(config, name, (Object) fallback) != null
                ? String.valueOf(getInputProp(config, name, (Object) fallback))
                : fallback;
    }

    // --------------------------------------------------
    // Boolean-specific getter
    // --------------------------------------------------
    public static boolean getInputPropBoolean(
            Map<String, Object> config,
            String name,
            boolean fallback
    ) {
        if (config == null) return fallback;

        Object propsObj = config.get("inputProps");
        if (!(propsObj instanceof List<?> props)) return fallback;

        for (Object p : props) {
            if (!(p instanceof Map<?, ?> prop)) continue;

            if (!name.equals(prop.get("name"))) continue;

            Object value = prop.get("value");
            if (value != null) {
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                return Boolean.parseBoolean(String.valueOf(value));
            }

            Object def = prop.get("defaultValue");
            if (def != null) {
                if (def instanceof Boolean) {
                    return (Boolean) def;
                }
                return Boolean.parseBoolean(String.valueOf(def));
            }
        }
        return fallback;
    }

    // --------------------------------------------------
    // List-specific getter
    // --------------------------------------------------
    @SuppressWarnings("unchecked")
    public static <T> List<T> getInputPropList(
            Map<String, Object> config,
            String name,
            List<T> fallback
    ) {
        if (config == null) return fallback;

        Object propsObj = config.get("inputProps");
        if (!(propsObj instanceof List<?> props)) return fallback;

        for (Object p : props) {
            if (!(p instanceof Map<?, ?> prop)) continue;

            if (!name.equals(prop.get("name"))) continue;

            Object value = prop.get("value");
            if (value instanceof List<?>) {
                return (List<T>) value;
            }

            Object def = prop.get("defaultValue");
            if (def instanceof List<?>) {
                return (List<T>) def;
            }
        }
        return fallback;
    }

    // --------------------------------------------------
    // Mapper-specific getter - extracts nested mapper value
    // --------------------------------------------------
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getInputPropMapper(
            Map<String, Object> config,
            String name,
            Map<String, Object> fallback
    ) {
        if (config == null) return fallback;

        Object propsObj = config.get("inputProps");
        if (!(propsObj instanceof List<?> props)) return fallback;

        for (Object p : props) {
            if (!(p instanceof Map<?, ?> prop)) continue;

            if (!name.equals(prop.get("name"))) continue;

            Object value = prop.get("value");
            if (value instanceof Map<?, ?>) {
                return (Map<String, Object>) value;
            }

            Object def = prop.get("defaultValue");
            if (def instanceof Map<?, ?>) {
                return (Map<String, Object>) def;
            }
        }
        return fallback;
    }

    // --------------------------------------------------
    // Get payloadSource from mapper
    // --------------------------------------------------
    public static String getMapperPayloadSource(
            Map<String, Object> config,
            String name,
            String fallback
    ) {
        Map<String, Object> mapper = getInputPropMapper(config, name, null);
        if (mapper == null) return fallback;

        Object payloadSource = mapper.get("payloadSource");
        return payloadSource != null ? String.valueOf(payloadSource) : fallback;
    }

    // --------------------------------------------------
    // Get payloadExpression from mapper
    // --------------------------------------------------
    public static String getMapperPayloadExpression(
            Map<String, Object> config,
            String name,
            String fallback
    ) {
        Map<String, Object> mapper = getInputPropMapper(config, name, null);
        if (mapper == null) return fallback;

        Object payloadExpression = mapper.get("payloadExpression");
        return payloadExpression != null ? String.valueOf(payloadExpression) : fallback;
    }

    // --------------------------------------------------
    // Get map array from mapper
    // --------------------------------------------------
    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getMapperMap(
            Map<String, Object> config,
            String name,
            List<Map<String, String>> fallback
    ) {
        Map<String, Object> mapper = getInputPropMapper(config, name, null);
        if (mapper == null) return fallback;

        Object map = mapper.get("map");
        if (map instanceof List<?>) {
            return (List<Map<String, String>>) map;
        }
        return fallback;
    }
}
