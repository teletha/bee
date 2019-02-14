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

import static bee.Platform.EOL;

import java.util.ArrayList;
import java.util.List;

import bee.util.Notation;
import kiss.I;
import kiss.WiseConsumer;

@SuppressWarnings("serial")
public class Fail extends Error {

    /** The reason, */
    public Notation reason = new Notation();

    /** The solution messages. */
    public List<String> solution = new ArrayList<String>();

    /**
     * <p>
     * Create failure with reason message.
     * </p>
     * 
     * @param reason
     */
    public Fail() {
        this(I.NoOP.asConsumer());
    }

    /**
     * <p>
     * Create failure with reason message.
     * </p>
     * 
     * @param reason
     */
    public Fail(String reason) {
        this($ -> $.p(reason));
    }

    /**
     * <p>
     * Create failure with reason message.
     * </p>
     * 
     * @param reason
     */
    public Fail(WiseConsumer<Notation> reason) {
        reason.accept(this.reason);
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
        StringBuilder builder = new StringBuilder(reason.toString()).append(EOL);

        int size = solution.size();

        if (size == 0) {
            builder.append("No solution.");
        } else {
            builder.append("Solution").append(EOL);
            for (int i = 0; i < solution.size(); i++) {
                builder.append("\t・ ").append(solution.get(i)).append(EOL);
            }
        }
        return builder.toString().trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }
}
