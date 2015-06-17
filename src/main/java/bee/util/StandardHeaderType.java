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

/**
 * @version 2015/06/17 14:21:23
 */
public enum StandardHeaderType implements HeaderType {

    /** The header style. */
    JavaDoc("/**", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

    /** The header style. */
    Script("#", "# ", "#EOL", "", "^#!.*$", "#.*$", "#.*$", false, false, false),

    /** The header style. */
    XML("<!--EOL", "    ", "EOL-->", "", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true, true, false),

    /** The header style. */
    Semicolon(";", "; ", ";EOL", "", null, ";.*$", ";.*$", false, false, false),

    /** The header style. */
    TripleSlash("///", "/// ", "///EOL", "", null, "///.*$", "///.*$", false, false, false),

    /** The header style. */
    SlashStar("/*", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

    /** The header style. */
    PHP("/*", " * ", " */", "", "^<\\?php.*$", "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

    /** The header style. */
    Batch("@REM", "@REM ", "@REMEOL", "", null, "@REM.*$", "@REM.*$", false, false, false),

    /** The header style. */
    Unknown("", "", "", "", null, null, null, false, false, false);

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
    private StandardHeaderType(String firstLine, String beforeEachLine, String endLine, String afterEachLine, String skipLinePattern, String firstLineDetectionPattern, String endLineDetectionPattern, boolean allowBlankLines, boolean isMultiline, boolean padLines) {
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
        return firstLineDetectionPattern != null && line != null && firstLineDetectionPattern.matcher(line).matches();
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
