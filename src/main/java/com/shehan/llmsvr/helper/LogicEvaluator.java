package com.shehan.llmsvr.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class LogicEvaluator {
    private static final ExpressionParser parser = new SpelExpressionParser();

    public static String evaluateLogic(List<Map<String, Object>> logicConfigs, Map<String, Object> rootData) {
        if (logicConfigs == null || logicConfigs.isEmpty()) return null;

        StandardEvaluationContext context = new StandardEvaluationContext(rootData);
        context.addPropertyAccessor(new MapAccessor());

        for (Map<String, Object> logicEntry : logicConfigs) {
            String condition = getConditionString(logicEntry);
            if (condition == null) continue;

            try {
                if (evaluate(condition, context)) {
                    return extractTargetId(logicEntry, condition);
                }
            } catch (Exception e) {
                log.error("Logic Evaluation Failed for [{}]: {}", condition, e.getMessage());
            }
        }
        return null;
    }

    public static List<String> evaluateAllLogic(List<Map<String, Object>> logicConfigs, Map<String, Object> rootData) {
        if (logicConfigs == null || logicConfigs.isEmpty()) return Collections.emptyList();

        StandardEvaluationContext context = new StandardEvaluationContext(rootData);
        context.addPropertyAccessor(new MapAccessor());
        List<String> matches = new ArrayList<>();

        for (Map<String, Object> logicEntry : logicConfigs) {
            String condition = getConditionString(logicEntry);
            if (condition == null) continue;

            try {
                if (evaluate(condition, context)) {
                    String targetId = extractTargetId(logicEntry, condition);
                    if (targetId != null) matches.add(targetId);
                }
            } catch (Exception e) {
                log.error("Logic Evaluation Failed for [{}]: {}", condition, e.getMessage());
            }
        }
        return matches;
    }

    private static String getConditionString(Map<String, Object> logicEntry) {
        Object conditionObj = logicEntry.get("value");
        if (conditionObj == null || String.valueOf(conditionObj).isBlank()) return null;
        return String.valueOf(conditionObj);
    }

    private static boolean evaluate(String condition, StandardEvaluationContext context) {
        Boolean result = parser.parseExpression(condition).getValue(context, Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    private static String extractTargetId(Map<String, Object> logicEntry, String condition) {
        Object targetNodeObj = logicEntry.get("targetNode");
        if (targetNodeObj instanceof Map<?, ?> targetMap) {
            Object targetId = targetMap.get("id");
            if (targetId != null) return String.valueOf(targetId);
        }
        log.warn("Logic matched but targetNode.id was not found for condition: {}", condition);
        return logicEntry.containsKey("id") ? String.valueOf(logicEntry.get("id")) : null;
    }
}
