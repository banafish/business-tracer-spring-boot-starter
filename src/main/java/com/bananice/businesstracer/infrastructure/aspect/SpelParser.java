package com.bananice.businesstracer.infrastructure.aspect;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpelParser {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String parse(String spel, Method method, Object[] args) {
        if (spel == null || spel.isEmpty()) {
            return null;
        }

        // Simple caching strategy
        String cacheKey = method.toString() + ":" + spel;
        Expression expression = cache.computeIfAbsent(cacheKey, k -> parser.parseExpression(spel));

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] params = discoverParameterNames(method);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i < args.length) {
                    context.setVariable(params[i], args[i]);
                }
            }
        }

        Object value = expression.getValue(context);
        return convertToString(value);
    }

    public String parse(String spel, Method method, Object[] args, Object result) {
        if (spel == null || spel.isEmpty()) {
            return null;
        }

        String cacheKey = method.toString() + ":" + spel;
        Expression expression = cache.computeIfAbsent(cacheKey, k -> parser.parseExpression(spel));

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] params = discoverParameterNames(method);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i < args.length) {
                    context.setVariable(params[i], args[i]);
                }
            }
        }

        context.setVariable("result", result);

        Object value = expression.getValue(context);
        return convertToString(value);
    }

    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map || value instanceof Collection || value.getClass().isArray()) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                return value.toString();
            }
        }
        return value.toString();
    }

    // Basic parameter name discovery (In Spring, usually ParameterNameDiscoverer is
    // better,
    // but for simplicity/universality here we use a basic approach or assume
    // compilation with -parameters)
    private String[] discoverParameterNames(Method method) {
        // NOTE: Reliable parameter name discovery requires Spring's
        // DefaultParameterNameDiscoverer
        // or compilation with '-parameters'.
        // For this starter, let's assume we can use Spring's utilities if we want to be
        // robust.
        return new DefaultParameterNameDiscoverer().getParameterNames(method);
    }
}
