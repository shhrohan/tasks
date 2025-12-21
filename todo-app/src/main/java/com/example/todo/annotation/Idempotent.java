package com.example.todo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as idempotent, preventing duplicate executions within a time
 * window.
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * &#64;Idempotent(keyExpression = "'createTask:' + #task.name + ':' + #task.swimLane.id")
 * public Task createTask(Task task) { ... }
 * </pre>
 * 
 * <p>
 * The keyExpression uses Spring Expression Language (SpEL) to generate a unique
 * key
 * from method parameters. If a duplicate operation is detected within the
 * window,
 * a {@link com.example.todo.exception.DuplicateOperationException} is thrown.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression to generate the idempotency key.
     * Method parameters are available as #paramName.
     */
    String keyExpression();

    /**
     * Time window in seconds during which duplicate operations are blocked.
     * Default is 5 seconds.
     */
    int windowSeconds() default 5;
}
