/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc.builder;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import kiss.I;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import stylist.StyleDSL;
import stylist.StyleDeclarable;
import stylist.Stylist;

public class SiteBuilder {

    /** GUILTY ACCESSOR */
    static SiteBuilder current;

    /**
     * Configure root directory.
     * 
     * @param pathToRootDirectory
     * @return
     */
    public static final SiteBuilder root(String pathToRootDirectory) {
        return root(Locator.directory(pathToRootDirectory));
    }

    /**
     * Configure root directory.
     * 
     * @param pathToRootDirectory
     * @return
     */
    public static final SiteBuilder root(Path pathToRootDirectory) {
        return root(Locator.directory(pathToRootDirectory));
    }

    /**
     * Configure root directory.
     * 
     * @param rootDirectory
     * @return
     */
    public static final SiteBuilder root(Directory rootDirectory) {
        return new SiteBuilder(rootDirectory);
    }

    /** The root directory. */
    private final Directory root;

    /** The initialize flag. */
    private boolean initialized = false;

    /** The initial protectable file pattern. */
    private List<String> protectable = I.list("!**@.*");

    /**
     * @param rootDirectory
     */
    private SiteBuilder(Directory rootDirectory) {
        this.root = Objects.requireNonNull(rootDirectory);
        current = this;
    }

    /**
     * Initialize only once.
     */
    private synchronized void initialize() {
        if (initialized == false) {
            initialized = true;

            // delete all existing files
            root.create().delete(protectable.toArray(String[]::new));

            // There is a time lag until the OS releases the handle of the deleted file, so wait a
            // little. AccessDeniedException may occur when going straight.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * Specify a pattern for files that you do not want to delete during initialization.
     * 
     * @param pattern
     * @return
     */
    public final SiteBuilder guard(String... patterns) {
        for (String pattern : patterns) {
            if (pattern != null && pattern.length() != 0) {
                protectable.add("!" + pattern);
            }
        }
        return this;
    }

    private static final String[] characterType = {"figcaption", "figure", "a", "abbr", "b", "bdi", "bdo", "cite", "code", "data", "dfn",
            "em", "i", "kbd", "mark", "q", "rb", "rp", "rt", "rtc", "s", "samp", "span", "strong", "sub", "sup", "time", "u", "var", "del",
            "ins", "&script", "&nav", "&article", "&aside", "&dl", "&div", "&i", "&td"};

    /**
     * Build HTML file.
     * 
     * @param path
     * @param html
     */
    public final void buildHTML(String path, HTML html) {
        initialize();

        html.declare();

        root.file(path).write(output -> {
            output.append("<!DOCTYPE html>\r\n");

            for (XML node : html.root) {
                node.to(output, "\t", characterType);
            }
        });
    }

    /**
     * Build CSS file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    public final String buildCSS(String path) {
        initialize();

        String formatted = Stylist.pretty().importNormalizeStyle().format();

        File file = root.file(path);
        file.write(output -> output.append(formatted));
        return root.relativize(file).path();
    }

    /**
     * Build CSS file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    public final String buildCSS(String path, Class<? extends StyleDSL> styles) {
        initialize();

        String formatted = Stylist.pretty().importNormalizeStyle().styles(styles).format();

        File file = root.file(path);
        file.write(output -> output.append(formatted));
        return root.relativize(file).path();
    }

    /**
     * Build CSS file and return the path of the generated file.
     * 
     * @param styles A style definition class to write.
     * @return A path to the generated file.
     */
    public final String buildCSS(String path, StyleDeclarable styles) {
        initialize();

        String formatted = Stylist.pretty().importNormalizeStyle().styles(styles).format();

        File file = root.file(path);
        file.write(output -> output.append(formatted));
        return root.relativize(file).path();
    }

    /**
     * Build JSON file with padding.
     * 
     * @param path
     * @param html
     */
    public final String buildJSONP(String path, Object object) {
        initialize();

        File file = root.file(path);
        file.write(output -> {
            output.append("const " + file.base() + " = ");
            I.write(object, output);
        });
        return root.relativize(file).path();
    }
}
