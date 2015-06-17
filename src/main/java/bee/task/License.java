/*
 * Copyright (C) 2015 Nameless Production Committee
 * 
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import bee.api.Command;
import bee.api.Task;
import bee.util.FileType;
import bee.util.PathSet;
import kiss.I;

/**
 * @version 2015/06/14 17:50:58
 */
public class License extends Task {

    /** The file encoding. */
    protected Charset encoding = project.getEncoding();

    /** The license text. */
    protected List<String> license = project.getLicense().text();

    /** The header style mapping. */
    protected Map<String, Header> headers = new HashMap();

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @throws IOException
     */
    @Command
    public void update() throws IOException {
        update(project.getSources());
    }

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @param set
     * @throws IOException
     */
    private void update(PathSet set) throws IOException {
        for (Path path : set.getFiles()) {
            FileType type = FileType.of(path);

            List<String> lines = Files.readAllLines(path, encoding);

            if (type.header() == Header.UNKNOWN) {
                ui.talk("Unknown Format ", project.getRoot().relativize(path));
            } else {
                ui.talk("Update ", project.getRoot().relativize(path));
                Files.write(path, convert(lines, type.header()), encoding);
            }
        }
    }

    /**
     * <p>
     * Read license file.
     * </p>
     * 
     * @param path A path to license file.
     */
    protected void license(String path) {
        license(I.locate(path));
    }

    /**
     * <p>
     * Read license file.
     * </p>
     * 
     * @param path A path to license file.
     */
    protected void license(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                license.clear();
                license.addAll(Files.readAllLines(path, encoding));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @param sources
     * @param header
     */
    public List<String> convert(List<String> sources, Header header) {
        int first = -1;
        int end = -1;

        for (int i = 0; i < sources.size(); i++) {
            String line = sources.get(i);

            if (header.isFirstHeaderLine(line)) {
                first = i;
            } else if (first != -1 && header.isEndHeaderLine(line)) {
                end = i;
                break;
            }
        }

        // remove existing header
        if (first != -1 && end != -1) {
            for (int i = end; first <= i; i--) {
                sources.remove(i);
            }
        }

        if (first == -1) {
            first = 0;
        }

        // add specified header
        sources.addAll(first, header.text(license));

        return sources;
    }

    /**
     * @version 2015/06/15 15:32:32
     */
    public enum Header {

        /** Header Definition. */
        JAVADOC_STYLE("/**", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

        /** Header Definition. */
        SCRIPT_STYLE("#", "# ", "#EOL", "", "^#!.*$", "#.*$", "#.*$", false, false, false),

        /** Header Definition. */
        HAML_STYLE("-#", "-# ", "-#EOL", "", "^-#!.*$", "-#.*$", "-#.*$", false, false, false),

        /** Header Definition. */
        XML_STYLE("<!--EOL", "    ", "EOL-->", "", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true, true,
                false),

        /** Header Definition. */
        XML_PER_LINE("EOL", "<!-- ", "EOL", " -->", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true,
                false, true),

        /** Header Definition. */
        SEMICOLON_STYLE(";", "; ", ";EOL", "", null, ";.*$", ";.*$", false, false, false),

        /** Header Definition. */
        APOSTROPHE_STYLE("'", "' ", "'EOL", "", null, "'.*$", "'.*$", false, false, false),

        /** Header Definition. */
        EXCLAMATION_STYLE("!", "! ", "!EOL", "", null, "!.*$", "!.*$", false, false, false),

        /** Header Definition. */
        DOUBLEDASHES_STYLE("--", "-- ", "--EOL", "", null, "--.*$", "--.*$", false, false, false),

        /** Header Definition. */
        SLASHSTAR_STYLE("/*", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

        /** Header Definition. */
        BRACESSTAR_STYLE("\\{*", " * ", " *\\}", "", null, "(\\s|\\t)*\\{\\*.*$", ".*\\*\\}(\\s|\\t)*$", false, true,
                false),

        /** Header Definition. */
        SHARPSTAR_STYLE("#*", " * ", " *#", "", null, "(\\s|\\t)*#\\*.*$", ".*\\*#(\\s|\\t)*$", false, true, false),

        /** Header Definition. */
        DOUBLETILDE_STYLE("~~", "~~ ", "~~EOL", "", null, "~~.*$", "~~.*$", false, false, false),

        /** Header Definition. */
        DYNASCRIPT_STYLE("<%--EOL", "    ", "EOL--%>", "", null, "(\\s|\\t)*<%--.*$", ".*--%>(\\s|\\t)*$", true, true,
                false),

        /** Header Definition. */
        DYNASCRIPT3_STYLE("<!---EOL", "    ", "EOL--->", "", null, "(\\s|\\t)*<!---.*$", ".*--->(\\s|\\t)*$", true,
                true, false),

        /** Header Definition. */
        PERCENT3_STYLE("%%%", "%%% ", "%%%EOL", "", null, "%%%.*$", "%%%.*$", false, false, false),

        /** Header Definition. */
        EXCLAMATION3_STYLE("!!!", "!!! ", "!!!EOL", "", null, "!!!.*$", "!!!.*$", false, false, false),

        /** Header Definition. */
        DOUBLESLASH_STYLE("//", "// ", "//EOL", "", null, "//.*$", "//.*$", false, false, false),

        /** Header Definition. */
        TRIPLESLASH_STYLE("///", "/// ", "///EOL", "", null, "///.*$", "///.*$", false, false, false),

        /** Header Definition. */
        PHP("/*", " * ", " */", "", "^<\\?php.*$", "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),

        /** Header Definition. */
        ASP("<%", "    ", "%>", "", null, "(\\s|\\t)*<%[^@].*$", ".*%>(\\s|\\t)*$", true, true, false),

        /** Header Definition. */
        LUA("--[[EOL", "    ", "EOL]]", "", null, "--\\[\\[$", "\\]\\]$", true, true, false),

        /** Header Definition. */
        FTL("<#--EOL", "    ", "EOL-->", "", null, "(\\s|\\t)*<#--.*$", ".*-->(\\s|\\t)*$", true, true, false),

        /** Header Definition. */
        FTL_ALT("[#--EOL", "    ", "EOL--]", "", "\\[#ftl(\\s.*)?\\]", "(\\s|\\t)*\\[#--.*$", ".*--\\](\\s|\\t)*$",
                true, true, false), /** Header Definition. */
        TEXT("====", "    ", "====EOL", "", null, "====.*$", "====.*$", true, true, false),

        /** Header Definition. */
        BATCH("@REM", "@REM ", "@REMEOL", "", null, "@REM.*$", "@REM.*$", false, false, false),

        /** Header Definition. */
        MUSTACHE_STYLE("{{!", "    ", "}}", "", null, "\\{\\{\\!.*$", "\\}\\}.*$", false, true, false),

        /** Header Definition. */
        UNKNOWN("", "", "", "", null, null, null, false, false, false);

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
        private Header(String firstLine, String beforeEachLine, String endLine, String afterEachLine, String skipLinePattern, String firstLineDetectionPattern, String endLineDetectionPattern, boolean allowBlankLines, boolean isMultiline, boolean padLines) {
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
         * Tells if the given content line must be skipped according to this header definition. The
         * header is outputted after any skipped line if any pattern defined on this point or on the
         * first line if not pattern defined.
         *
         * @param line The line to test.
         * @return true if this line must be skipped or false.
         */
        private boolean isSkipLine(String line) {
            return skipLinePattern != null && line != null && skipLinePattern.matcher(line).matches();
        }

        /**
         * Tells if the given content line is the first line of a possible header of this definition
         * kind.
         *
         * @param line The line to test.
         * @return true if the first line of a header have been recognized or false.
         */
        private boolean isFirstHeaderLine(String line) {
            return firstLineDetectionPattern != null && line != null && firstLineDetectionPattern.matcher(line)
                    .matches();
        }

        /**
         * Tells if the given content line is the end line of a possible header of this definition
         * kind.
         *
         * @param line The end to test.
         * @return true if the end line of a header have been recognized or false.
         */
        private boolean isEndHeaderLine(String line) {
            return endLineDetectionPattern != null && line != null && endLineDetectionPattern.matcher(line).matches();
        }

        /**
         * Build header text by using the specified text.
         */
        private List<String> text(List<String> text) {
            List<String> header = new ArrayList();

            header.add(firstLine);
            for (String line : text) {
                header.add(beforeEachLine.concat(line).concat(afterEachLine));
            }
            header.add(endLine);

            return header;
        }
    }
}
