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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import bee.api.License;
import bee.api.Project;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * @version 2012/11/12 13:20:32
 */
public class BlinkProject extends Project {

    static {
        I.load(Task.class, false);
    }

    /** The root. */
    private Directory root;

    /** The initialization state. */
    private boolean initialized;

    /**
     * 
     */
    public BlinkProject() {
        product("blink", "Blink", "1.0");
        ProjectLifestyle.local.set(this);
        UserInterfaceLisfestyle.local.set(Null.UI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Directory getRoot() {
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
     * Locate a source file of the specified class.
     * </p>
     * 
     * @param clazz A target class.
     * @return A source code location.
     */
    public final File locateByteCode(Class clazz) {
        Path file = buildJavaBytecodeFilePath(clazz.getName());

        return locateMainOutput(file.toString());
    }

    /**
     * <p>
     * Locate file in main class directory.
     * </p>
     * 
     * @param path A relative path to file.
     * @return A located file path.
     */
    public final File locateMainOutput(String path) {
        return getClasses().file(path);
    }

    /**
     * <p>
     * Locate file in test class directory.
     * </p>
     * 
     * @param path A relative path to file.
     * @return A located file path.
     */
    public final File locateTestOutput(String path) {
        return getTestClasses().file(path);
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
        return getProjectClasses().file(path).asJavaPath();
    }

    /**
     * <p>
     * Import the specified main source file.
     * </p>
     * 
     * @param model A class to import.
     */
    public final File importBy(Class model) {
        File file = buildJavaSourceFilePath(model.getName());
        File original = Locator.directory("src/test/java").file(file);
        File copy = getRoot().directory("src/main/java").file(file);

        original.copyTo(copy);

        return copy;
    }

    /**
     * <p>
     * Import all main source files in the specified class package.
     * </p>
     * 
     * @param model A sample class to import.
     */
    public final Directory importByPackageOf(Class model) {
        Directory directory = buildJavaSourceFilePath(model.getName()).parent();
        Directory original = Locator.directory("src/test/java").directory(directory);
        Directory copy = getRoot().directory("src/main/java").directory(directory);

        original.copyTo(copy);

        return copy;
    }

    /**
     * <p>
     * Add main source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path source(String fqcn, String... contents) {
        return createJavaSource(fqcn, getRoot().asJavaPath().resolve("src/main/java/" + fqcn.replace('.', '/') + ".java"), contents);
    }

    /**
     * <p>
     * Add test source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path sourceTest(String fqcn) {
        return createJavaSource(fqcn, getRoot().asJavaPath().resolve("src/test/java/" + fqcn.replace('.', '/') + ".java"));
    }

    /**
     * <p>
     * Add project source file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified class name.
     */
    public final Path sourceProject(String fqcn) {
        return createJavaSource(fqcn, getRoot().asJavaPath().resolve("src/project/java/" + fqcn.replace('.', '/') + ".java"));
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
     * Build source file path from fully qualified class name.
     * </p>
     * 
     * @param fqcn
     * @return
     */
    private File buildJavaSourceFilePath(String fqcn) {
        return Locator.file(fqcn.replace('.', '/').concat(".java"));
    }

    /**
     * <p>
     * Build bytecode file path from fully qualified class name.
     * </p>
     * 
     * @param fqcn
     * @return
     */
    private Path buildJavaBytecodeFilePath(String fqcn) {
        return Paths.get(fqcn.replace('.', '/').concat(".class"));
    }

    /**
     * <p>
     * Add main resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resource(String fqcn) {
        return createFile(fqcn, getRoot().asJavaPath().resolve("src/main/resources/" + fqcn));
    }

    /**
     * <p>
     * Add test resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resourceTest(String fqcn) {
        return createFile(fqcn, getRoot().asJavaPath().resolve("src/test/resources/" + fqcn));
    }

    /**
     * <p>
     * Add project resource file with the specified name.
     * </p>
     * 
     * @param fqcn A fully qualified file name.
     */
    public final Path resourceProject(String fqcn) {
        return createFile(fqcn, getRoot().asJavaPath().resolve("src/project/resources/" + fqcn));
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
            root = Locator.temporaryDirectory();
        }
    }

    /**
     * <p>
     * Assign {@link License}.
     * </p>
     * 
     * @param license
     */
    public void set(License license) {
        super.license(license);
    }
}
