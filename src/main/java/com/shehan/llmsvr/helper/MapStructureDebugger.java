package com.shehan.llmsvr.helper;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class MapStructureDebugger {

    /**
     * Prints the entire structure of a nested Map for debugging
     */
    public static void printStructure(Object obj) {
        printStructure(obj, "", 0);
    }

    public static void printStructure(Object obj, String prefix, int depth) {
        if (depth > 10) { // Prevent infinite recursion
            log.debug("{}... (max depth reached)", prefix);
            return;
        }

        if (obj == null) {
            log.debug("{}null", prefix);
            return;
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            log.debug("{}Map (size={})", prefix, map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                log.debug("{}  [{}] ->", prefix, entry.getKey());
                printStructure(entry.getValue(), prefix + "    ", depth + 1);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            log.debug("{}List (size={})", prefix, list.size());
            for (int i = 0; i < Math.min(list.size(), 5); i++) {
                log.debug("{}  [{}] ->", prefix, i);
                printStructure(list.get(i), prefix + "    ", depth + 1);
            }
            if (list.size() > 5) {
                log.debug("{}  ... ({} more items)", prefix, list.size() - 5);
            }
        } else {
            String valueStr = String.valueOf(obj);
            if (valueStr.length() > 100) {
                valueStr = valueStr.substring(0, 100) + "...";
            }
            log.debug("{}{} = {}", prefix, obj.getClass().getSimpleName(), valueStr);
        }
    }

    /**
     * Finds all paths in a nested map structure that lead to a specific key
     */
    public static List<String> findPathsToKey(Map<String, Object> map, String targetKey) {
        List<String> paths = new ArrayList<>();
        findPathsToKeyRecursive(map, targetKey, "", paths, 0);
        return paths;
    }

    private static void findPathsToKeyRecursive(
            Object obj,
            String targetKey,
            String currentPath,
            List<String> paths,
            int depth
    ) {
        if (depth > 10 || obj == null) return;

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;

                if (key.equals(targetKey)) {
                    paths.add(newPath);
                }

                findPathsToKeyRecursive(entry.getValue(), targetKey, newPath, paths, depth + 1);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                String newPath = currentPath + "." + i;
                findPathsToKeyRecursive(list.get(i), targetKey, newPath, paths, depth + 1);
            }
        }
    }

    /**
     * Gets the value at a specific path, useful for testing expressions
     */
    public static Object getValueAtPath(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List && isNumeric(part)) {
                List<?> list = (List<?>) current;
                int index = Integer.parseInt(part);
                current = (index >= 0 && index < list.size()) ? list.get(index) : null;
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
