/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @version 2015/06/17 14:40:29
 */
public enum StandardHeaderStyle implements HeaderStyle {

    /** The header style. */
    JavaDoc("/**", " * ", "", " */", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

    /** The header style. */
    SlashStar("/*", " * ", "", " */", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

    /** The header style. */
    Sharp("###", "# ", "", "###", "^#!.*$", "#.*$", "#.*$", false, false, false),

    /** The header style. */
    XML("<!--", "  ", "", "-->", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true, true, false),

    /** The header style. */
    Semicolon(";", "; ", "", ";", null, ";.*$", ";.*$", false, false, false),

    /** The header style. */
    SlashDouble("//", "// ", "", "//", null, "//.*$", "//.*$", false, false, false),

    /** The header style. */
    SlashTriple("///", "/// ", "", "///", null, "///.*$", "///.*$", false, false, false),

    /** The header style. */
    Batch("@REM", "@REM ", "", "@REM", null, "@REM.*$", "@REM.*$", false, false, false),

    /** The header style. */
    Unknown("", "", "", "", null, null, null, false, false, false);

    /** The first line. */
    private final String firstLine;

    /** The line prefix. */
    private final String beforeEachLine;

    /** The line suffix. */
    private final String afterEachLine;

    /** The end line. */
    private final String endLine;

    /** The pattern of skip line. */
    private final Pattern skipLinePattern;

    /** The pattern of first line. */
    private final Pattern firstLineDetectionPattern;

    /** The pattern of end line. */
    private final Pattern endLineDetectionPattern;

    @SuppressWarnings("unused")
    private final boolean allowBlankLines;

    @SuppressWarnings("unused")
    private final boolean isMultiline;

    @SuppressWarnings("unused")
    private final boolean padLines;

    /**
     * @param firstLine
     * @param beforeEachLine
     * @param afterEachLine
     * @param endLine
     * @param skipLinePattern
     * @param firstLineDetectionPattern
     * @param endLineDetectionPattern
     * @param allowBlankLines
     * @param isMultiline
     * @param padLines
     */
    private StandardHeaderStyle(String firstLine, String beforeEachLine, String afterEachLine, String endLine, String skipLinePattern, String firstLineDetectionPattern, String endLineDetectionPattern, boolean allowBlankLines, boolean isMultiline, boolean padLines) {
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
    public List<String> decorate(List<String> text) {
        List<String> header = new ArrayList();

        header.add(firstLine);
        for (String line : text) {
            String concat = beforeEachLine.concat(line).concat(afterEachLine);
            while (!concat.isEmpty() && Character.isWhitespace(concat.charAt(concat.length() - 1))) {
                concat = concat.substring(0, concat.length() - 1);
            }
            header.add(concat);
        }
        header.add(endLine);

        return header;
    }
}