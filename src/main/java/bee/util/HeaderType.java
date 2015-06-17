/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import kiss.Extensible;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2015/06/17 13:33:00
 */
public interface HeaderType extends Extensible {

    /**
     * Tells if the given content line must be skipped according to this header definition. The
     * header is outputted after any skipped line if any pattern defined on this point or on the
     * first line if not pattern defined.
     *
     * @param line The line to test.
     * @return true if this line must be skipped or false.
     */
    boolean isSkipLine(String line);

    /**
     * Tells if the given content line is the first line of a possible header of this definition
     * kind.
     *
     * @param line The line to test.
     * @return true if the first line of a header have been recognized or false.
     */
    boolean isFirstHeaderLine(String line);

    /**
     * Tells if the given content line is the end line of a possible header of this definition kind.
     *
     * @param line The end to test.
     * @return true if the end line of a header have been recognized or false.
     */
    boolean isEndHeaderLine(String line);

    /**
     * Build header text by using the specified text.
     */
    List<String> text(List<String> text);

    /**
     * @version 2015/06/17 13:34:43
     */
    @Manageable(lifestyle = Singleton.class)
    class Unknown implements HeaderType {

        private final String firstLine;

        private final String beforeEachLine;

        private final String endLine;

        private final String afterEachLine;

        private final Pattern skipLinePattern;

        private final Pattern firstLineDetectionPattern;

        private final Pattern endLineDetectionPattern;

        private final boolean allowBlankLines;

        private final boolean isMultiline;

        private final boolean padLines;

        /**
         * @param firstLine
         * @param beforeEachLine
         * @param endLine
         * @param afterEachLine
         * @param skipLinePattern
         * @param firstLineDetectionPattern
         * @param endLineDetectionPattern
         * @param allowBlankLines
         * @param isMultiline
         * @param padLines
         */
        private Unknown(String firstLine, String beforeEachLine, String endLine, String afterEachLine, String skipLinePattern, String firstLineDetectionPattern, String endLineDetectionPattern, boolean allowBlankLines, boolean isMultiline, boolean padLines) {
            this.firstLine = firstLine;
            this.beforeEachLine = beforeEachLine;
            this.endLine = endLine;
            this.afterEachLine = afterEachLine;
            this.skipLinePattern = compile(skipLinePattern);
            this.firstLineDetectionPattern = compile(firstLineDetectionPattern);
            this.endLineDetectionPattern = compile(endLineDetectionPattern);
            this.allowBlankLines = allowBlankLines;
            this.isMultiline = isMultiline;
            this.padLines = padLines;
        }

        /**
         * <p>
         * Helper method to compile to {@link Pattern}.
         * </p>
         * 
         * @param regexp
         * @return
         */
        private Pattern compile(String regexp) {
            return regexp == null ? null : Pattern.compile(regexp);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSkipLine(String line) {
            return skipLinePattern != null && line != null && skipLinePattern.matcher(line).matches();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFirstHeaderLine(String line) {
            return firstLineDetectionPattern != null && line != null && firstLineDetectionPattern.matcher(line)
                    .matches();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEndHeaderLine(String line) {
            return endLineDetectionPattern != null && line != null && endLineDetectionPattern.matcher(line).matches();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> text(List<String> text) {
            List<String> header = new ArrayList();

            header.add(firstLine);
            for (String line : text) {
                String concat = beforeEachLine.concat(line).concat(afterEachLine);
                while (Character.isWhitespace(concat.charAt(concat.length() - 1))) {
                    concat = concat.substring(0, concat.length() - 1);
                }
                header.add(concat);
            }
            header.add(endLine);

            return header;
        }
    }

    /**
     * @version 2015/06/17 13:37:18
     */
    class JavaDoc extends Unknown {

        /**
         * 
         */
        private JavaDoc() {
            super("/**", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false);
        }
    }

    /**
     * @version 2015/06/17 13:38:02
     */
    class Script extends Unknown {

        /**
         * 
         */
        private Script() {
            super("#", "# ", "#EOL", "", "^#!.*$", "#.*$", "#.*$", false, false, false);
        }
    }

    /**
     * @version 2015/06/17 13:38:31
     */
    class XML extends Unknown {

        /**
         * 
         */
        private XML() {
            super("<!--EOL", "    ", "EOL-->", "", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true, true, false);
        }
    }

    /**
     * @version 2015/06/17 13:41:52
     */
    class Semicolon extends Unknown {

        /**
         * 
         */
        private Semicolon() {
            super(";", "; ", ";EOL", "", null, ";.*$", ";.*$", false, false, false);
        }
    }

    /**
     * @version 2015/06/17 13:41:52
     */
    class TripleSlash extends Unknown {

        /**
         * 
         */
        private TripleSlash() {
            super("///", "/// ", "///EOL", "", null, "///.*$", "///.*$", false, false, false);
        }
    }

    /**
     * @version 2015/06/17 13:41:52
     */
    class SlashStar extends Unknown {

        /**
         * 
         */
        private SlashStar() {
            super("/*", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false);
        }
    }

    /**
     * @version 2015/06/17 13:48:31
     */
    class PHP extends Unknown {

        /**
         * 
         */
        private PHP() {
            super("/*", " * ", " */", "", "^<\\?php.*$", "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false);
        }
    }

    /**
     * @version 2015/06/17 13:49:24
     */
    class Batch extends Unknown {

        /**
         * 
         */
        private Batch() {
            super("@REM", "@REM ", "@REMEOL", "", null, "@REM.*$", "@REM.*$", false, false, false);
        }
    }
}
