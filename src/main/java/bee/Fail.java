/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.Platform.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        setStackTrace(new StackTraceElement[0]);
    }

    /**
     * Write solution for this failure.
     * 
     * @param messages
     */
    public Fail solve(Object... messages) {
        solutions.add(UserInterface.build(messages));

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder().append(reason).append(EOL);

        int size = solutions.size();

        if (size == 0) {
            builder.append("No solution.");
        } else {
            for (String solution : solutions) {
                builder.append(Platform.EOL).append("  -").append(solution);
            }
            builder.append(Platform.EOL);
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
}