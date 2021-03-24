/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.Platform.EOL;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import bee.util.Notation;
import kiss.WiseConsumer;

@SuppressWarnings("serial")
public class Fail extends Error {

    /** The reason, */
    public Notation reason = new Notation();

    /** The solution messages. */
    public List<String> solution = new ArrayList<String>();

    /**
     * Create failure with reason message.
     * 
     * @param reason
     */
    public Fail() {
        this(e -> {
        });
    }

    /**
     * Create failure with reason message.
     * 
     * @param reason
     */
    public Fail(String reason) {
        this($ -> $.p(reason));
    }

    /**
     * Create failure with reason message.
     * 
     * @param reason
     */
    public Fail(WiseConsumer<Notation> reason) {
        reason.accept(this.reason);
    }

    /**
     * Write solution for this failure.
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
        StringBuilder builder = new StringBuilder(reason.toString()).append(EOL);

        int size = solution.size();

        if (size == 0) {
            builder.append("No solution.");
        } else {
            Notation notation = new Notation();
            notation.title("Solution");
            notation.list(solution, Function.identity());
            builder.append(notation);
        }
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }
}