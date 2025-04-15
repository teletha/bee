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
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
public class Fail extends Error {

    /** The reason, */
    public String reason;

    /** The solution messages. */
    public List<String> solutions;

    /**
     * Create failure with reason message.
     */
    public Fail(String reason) {
        this(reason, null);
    }

    /**
     * Create failure with reason message.
     */
    public Fail(String reason, List<String> solutions) {
        this.reason = Objects.requireNonNull(reason);
        this.solutions = new ArrayList(solutions == null ? List.of() : solutions);

        // setStackTrace(new StackTraceElement[0]);
    }

    public Fail reason(Throwable reason) {
        initCause(strip(reason));

        return this;
    }

    /**
     * Write solution for this failure.
     * 
     * @param solution
     */
    public Fail solve(Object solution) {
        solutions.add(Objects.toString(solution));

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder().append(reason);

        int size = solutions.size();

        if (size != 0) {
            for (String solution : solutions) {
                builder.append(Platform.EOL);
                builder.append(Platform.EOL).append("  -").append(solution);
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

    public static Throwable strip(Throwable error) {
        while (error instanceof ExecutionException || error instanceof UndeclaredThrowableException || error instanceof InvocationTargetException) {
            error = error.getCause();
        }
        return error;
    }
}