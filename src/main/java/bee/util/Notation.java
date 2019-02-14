/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import static bee.Platform.EOL;

import java.util.function.Function;

import kiss.Decoder;
import kiss.Encoder;
import kiss.I;
import kiss.Signal;
import kiss.WiseRunnable;

/**
 * Simple Markdown-like memo.
 */
public class Notation {

    /** Indent character. */
    static final String INDENT = "\t";

    /** Line pattern. */
    private static final String LINE = "(\r\n|[\n\r\u2028\u2029\u0085])";

    /** Actual buffer. */
    private final StringBuilder builder = new StringBuilder();

    /** The current section level. */
    private int level = 0;

    /**
     * Write title.
     * 
     * @param title
     */
    public void title(String title) {
        switch (level) {
        case 0:
            builder.append(title).append(EOL);
            builder.append("=".repeat(title.length())).append(EOL);
            break;

        case 1:
            builder.append(title).append(EOL);
            builder.append("-".repeat(title.length())).append(EOL);
            break;

        default:
            builder.append(INDENT.repeat(level)).append("#".repeat(level + 1)).append(" ").append(title).append(EOL);
            break;
        }
    }

    /**
     * Declare section nest.
     * 
     * @param section
     */
    public void section(WiseRunnable section) {
        level++;
        section.run();
        level--;
    }

    /**
     * Write paragraph.
     * 
     * @param paragraph
     */
    public void p(String paragraph) {
        builder.append(INDENT.repeat(level)).append(paragraph.replaceAll(LINE, "$1" + INDENT.repeat(level))).append(EOL).append(EOL);
    }

    /**
     * Write list.
     * 
     * @param items
     * @param descriptor
     */
    public <T> void list(T[] items, Function<T, String> descriptor) {
        list(I.signal(items), descriptor);
    }

    /**
     * Write list.
     * 
     * @param items
     * @param descriptor
     */
    public <T> void list(Iterable<T> items, Function<T, String> descriptor) {
        list(I.signal(items), descriptor);
    }

    /**
     * Write list.
     * 
     * @param items
     * @param descriptor
     */
    public <T> void list(Signal<T> items, Function<T, String> descriptor) {
        items.to(item -> {
            builder.append(INDENT.repeat(level)).append("* ").append(descriptor.apply(item)).append(EOL);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * 
     */
    @SuppressWarnings("unused")
    private static class Codec implements Encoder<Notation>, Decoder<Notation> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Notation decode(String value) {
            Notation markdown = new Notation();
            markdown.builder.append(value);
            return markdown;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String encode(Notation value) {
            return value.toString();
        }
    }
}
