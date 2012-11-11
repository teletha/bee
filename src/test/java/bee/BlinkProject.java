/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.model.ClassUtil;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import bee.api.Project;

/**
 * @version 2012/04/02 22:16:55
 */
public class BlinkProject extends Project implements TestRule {

    static {
        I.load(ClassUtil.getArchive(Bee.class));
    }

    /** The root. */
    private Path root;

    /** The initialization state. */
    private boolean initialized;

    /**
     * 
     */
    public BlinkProject() {
        ProjectLifestyle.local.set(this);
        UserInterfaceLisfestyle.local.set(Null.UI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        initialize();

        return root;
    }

    /**
     * <p>
     * Show trace message.
     * </p>
     */
    public final void showTrace() {
        UserInterfaceLisfestyle.local.set(new CommandLineUserInterface());
    }

    /**
     * <p>
     * Locate file in main class directory.
     * </p>
     * 
     * @param path A relative path to file.
     * @return A located file path.
     */
    public final Path locateMainOutput(String path) {
        return getClasses().resolve(path);
    }

    /**
     * <p>
     * Locate file in test class directory.
     * </p>
     * 
     * @param path A relative path to file.
     * @return A located file path.
     */
    public final Path locateTestOutput(String path) {
        return getTestClasses().resolve(path);
    }

    /**
     * <p>
     * Locate file in project class directory.
     * </p>
     * 
     * @param path A relative path to file.
     * @return A located file path.
     */
    public final Path locateProjectOutput(String path) {
        return getProjectClasses().resolve(path);
    }

    /**
     * <p>
     * Add main source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path source(Class model) {
        String fqcn = model.getName();
        Path root = I.locate("").toAbsolutePath();
        Path file = root.resolve("src/test/java/" + fqcn.replace('.', '/') + ".java");

        return createJavaSource(fqcn, getRoot().resolve("src/main/java/" + fqcn.replace('.', '/') + ".java"), null);
    }

    /**
     * <p>
     * Add main source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path source(String fqcn, String... contents) {
        return createJavaSource(fqcn, getRoot().resolve("src/main/java/" + fqcn.replace('.', '/') + ".java"), contents);
    }

    /**
     * <p>
     * Add test source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path sourceTest(String fqcn) {
        return createJavaSource(fqcn, getRoot().resolve("src/test/java/" + fqcn.replace('.', '/') + ".java"));
    }

    /**
     * <p>
     * Add project source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path sourceProject(String fqcn) {
        return createJavaSource(fqcn, getRoot().resolve("src/project/java/" + fqcn.replace('.', '/') + ".java"));
    }

    /**
     * <p>
     * Create java source file.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     * @param path A source location.
     */
    private Path createJavaSource(String fqcn, Path path, String... contents) {
        try {
            // create directory
            Files.createDirectories(path.getParent());

            // create java source
            int index = fqcn.lastIndexOf('.');
            String packageName;
            String className;

            if (index == -1) {
                packageName = null;
                className = fqcn;
            } else {
                packageName = fqcn.substring(0, index);
                className = fqcn.substring(index + 1);
            }

            // write source file
            List<String> source = new ArrayList();
            if (packageName != null) source.add("package " + packageName + ";");
            source.add("public class " + className + "{");
            for (String content : contents) {
                source.add(content);
            }
            source.add("}");

            Files.write(path, source, StandardCharsets.UTF_8);

            return path;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Add main resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resource(String fqcn) {
        return createFile(fqcn, getRoot().resolve("src/main/resources/" + fqcn));
    }

    /**
     * <p>
     * Add test resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resourceTest(String fqcn) {
        return createFile(fqcn, getRoot().resolve("src/test/resources/" + fqcn));
    }

    /**
     * <p>
     * Add project resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resourceProject(String fqcn) {
        return createFile(fqcn, getRoot().resolve("src/project/resources/" + fqcn));
    }

    /**
     * <p>
     * Create file.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     * @param path A file location.
     */
    private Path createFile(String fqcn, Path path) {
        try {
            // create directory
            Files.createDirectories(path.getParent());

            // create file
            Files.createFile(path);

            // API definition
            return path;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Initialize project structure.
     * </p>
     */
    private synchronized void initialize() {
        if (!initialized) {
            initialized = true;

            // create root directory
            root = I.locateTemporary();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }
}
