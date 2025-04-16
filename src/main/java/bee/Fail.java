/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Signals a recoverable failure during the build process.
 * <p>
 * This exception is thrown when a build-related operation fails in a way that should be reported
 * to the user, such as misconfiguration, missing dependencies, or unsupported environments.
 * <p>
 * Unlike critical runtime errors, this exception is <strong>unchecked</strong> (extends
 * {@link RuntimeException}) and intended to be handled by the build infrastructure
 * or surfaced to the user with actionable context.
 * <p>
 * Each {@code Fail} instance includes:
 * <ul>
 * <li>A <strong>mandatory reason</strong> message that describes what went wrong.</li>
 * <li>An optional list of <strong>suggested solutions</strong> that help resolve the issue.</li>
 * <li>An optional <strong>underlying cause</strong> (wrapped Throwable).</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * throw new Fail("Missing JDK installation")
 *     .solve("Install JDK 21 or later")
 *     .solve("Ensure JAVA_HOME is set correctly")
 *     .reason(e);
 * }</pre>
 */

public class Fail extends RuntimeException {

    private static final long serialVersionUID = 5955376447974779631L;

    /**
     * The mandatory, human-readable reason explaining the cause of the failure.
     * This field should clearly describe the problem encountered.
     */
    private final String reason;

    /**
     * An optional list of messages suggesting potential solutions or next steps
     * to resolve the failure. This list can be modified after creation using the
     * {@link #solve(Object)} method.
     */
    private final List<String> solutions;

    /**
     * Constructs a new {@code Fail} exception with the specified reason message.
     * No solutions are added initially.
     *
     * @param reason The detail message explaining the reason for the failure. Must not be null.
     */
    public Fail(String reason) {
        this(reason, null);
    }

    /**
     * Constructs a new {@code Fail} exception with the specified reason message and an initial list
     * of solutions. If the provided list of solutions is null, an empty list is used internally.
     * The provided list is copied, so subsequent modifications to the original list will not affect
     * this instance.
     *
     * @param reason The detail message explaining the reason for the failure. Must not be null.
     * @param solutions A list of suggested solutions. May be null or empty.
     */
    public Fail(String reason, List<String> solutions) {
        this.reason = reason;
        this.solutions = new ArrayList(solutions == null ? List.of() : solutions);
    }

    /**
     * Sets the underlying cause of this failure.
     *
     * @param reason The throwable that represents the underlying cause of this failure.
     * @return This {@code Fail} instance, allowing for method chaining.
     */
    public Fail reason(Throwable reason) {
        initCause(strip(reason));

        return this;
    }

    /**
     * Adds a suggested solution to the list associated with this failure.
     *
     * @param solution An object representing a potential solution.
     * @return This {@code Fail} instance, allowing for method chaining.
     */
    public Fail solve(Object solution) {
        if (solution != null) {
            solutions.add(solution.toString());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder(reason);

        if (!solutions.isEmpty()) {
            for (String solution : solutions) {
                builder.append(Platform.EOL);
                builder.append(Platform.EOL).append("\t-").append(solution);
            }
        }
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getMessage();
    }

    /**
     * Utility method to unwrap common wrapper exceptions and reveal the underlying root cause.
     *
     * @param error The initial throwable, which might be a wrapper exception. Must not be null.
     * @return The unwrapped root cause throwable, or the original throwable if it's not one of the
     *         known wrappers or has no cause.
     */
    public static Throwable strip(Throwable error) {
        while (error instanceof ExecutionException || error instanceof UndeclaredThrowableException || error instanceof InvocationTargetException) {
            error = error.getCause();
        }
        return error;
    }
}