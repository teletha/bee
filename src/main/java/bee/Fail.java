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
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            for (String solution : this.solution) {
                builder.append("・").append(solution).append(EOL);
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

    /**
     * Describe fail condition and its solution.
     */
    public static void when(boolean condition, String message, Object... solution) {
        if (condition == true) {
            Fail fail = new Fail();
            List<StackFrame> traces = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                    .walk(s -> s.skip(1).filter(Fail::take).collect(Collectors.toList()));

            System.out.println(convert(message, traces.get(0).getDeclaringClass(), traces.get(0).getDescriptor()));
            throw fail;
        }
    }

    private static String convert(String text, Class caller, Method method) {
    }

    private static final List<String> excludes = List
            .of("java.", "jdk.", "org.junit.platform.", "org.junit.jupiter.engine.", "org.eclipse.jdt.internal.junit.runner.", "org.eclipse.jdt.internal.junit5.runner.");

    private static boolean take(StackFrame trace) {
        String className = trace.getClassName();

        for (String exclude : excludes) {
            if (className.startsWith(exclude)) {
                return false;
            }
        }
        return true;
    }
}
