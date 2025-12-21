package com.example.todo.aspect;

import com.example.todo.annotation.Idempotent;
import com.example.todo.exception.DuplicateOperationException;
import com.example.todo.service.IdempotencyService;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect that intercepts methods annotated with @Idempotent and prevents
 * duplicate executions within the configured time window.
 */
@Aspect
@Component
@Log4j2
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public IdempotencyAspect(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = generateKey(joinPoint, idempotent.keyExpression());
        int windowSeconds = idempotent.windowSeconds();

        log.debug("[Idempotency] Checking key: {} (window: {}s)", key, windowSeconds);

        if (idempotencyService.isDuplicate(key, windowSeconds)) {
            log.warn("[Idempotency] Duplicate operation detected: {}", key);
            throw new DuplicateOperationException("Duplicate operation detected", key);
        }

        try {
            Object result = joinPoint.proceed();
            idempotencyService.complete(key);
            log.debug("[Idempotency] Operation completed: {}", key);
            return result;
        } catch (DuplicateOperationException e) {
            // Re-throw without completing (keep the lock)
            throw e;
        } catch (Exception e) {
            // On error, remove the key to allow retry
            idempotencyService.complete(key);
            log.debug("[Idempotency] Operation failed, allowing retry: {}", key);
            throw e;
        }
    }

    /**
     * Generate idempotency key using SpEL expression evaluation.
     */
    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer);

        try {
            Object keyValue = expressionParser.parseExpression(keyExpression).getValue(context);
            return keyValue != null ? keyValue.toString() : "null";
        } catch (Exception e) {
            log.error("[Idempotency] Failed to evaluate key expression: {}", keyExpression, e);
            // Fallback to method signature + args hash
            return method.getName() + ":" + java.util.Arrays.hashCode(args);
        }
    }
}
