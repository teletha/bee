/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.Platform.*;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Fail extends Error {

    /** The message, */
    public String reason;

    /** The solution messages. */
    public List<String> solution = new ArrayList<String>();

    /**
     * <p>
     * Create failure with reason message.
     * </p>
     * 
     * @param reason
     */
    public Fail(String reason) {
        super(reason);

        this.reason = reason;
    }

    /**
     * <p>
     * Create failure with reason message.
     * </p>
     * 
     * @param reason
     */
    public Fail(Object... reason) {
        this(UserInterface.build(reason));
    }

    /**
     * <p>
     * Write solution for this failure.
     * </p>
     * 
     * @param messages
     */
    public final Fail solve(Object... messages) {
        solution.add(UserInterface.build(messages));

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder(reason).append(EOL);

        int size = solution.size();

        if (size == 0) {
            builder.append("No solution.");
        } else {
            for (int i = 0; i < solution.size(); i++) {
                builder.append(i + 1).append(" ").append(solution.get(i)).append(EOL);
            }
        }
        return builder.toString().trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintStream stream) {
        printStackTrace(new PrintWriter(stream));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintWriter writer) {
        writer.append(getLocalizedMessage());
    }
}
