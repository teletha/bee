/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import bee.Bee;
import bee.Platform;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.Java;
import bee.util.Java.JVM;
import bee.util.PathPattern;
import bee.util.Process;
import kiss.I;
import kiss.XML;

/**
 * @version 2016/09/08 12:04:28
 */
public class Eclipse extends Task {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Command("Generate configuration files for Eclipse.")
    public void eclipse() {
        createClasspath(project.getRoot().resolve(".classpath"));
        createProject(project.getRoot().resolve(".project"));

        Set<Path> processors = project.getAnnotationProcessors();

        if (!processors.isEmpty()) {
            createFactorypath(project.getRoot().resolve(".factorypath"), processors);
            createAPT(project.getRoot().resolve(".settings/org.eclipse.jdt.apt.core.prefs"), new ProjectInfo(project));
            createJDT(project.getRoot().resolve(".settings/org.eclipse.jdt.core.prefs"));
        }
        ui.talk("Create Eclipse configuration files.");

        // check lombok
        if (project.hasDependency(Bee.Lombok.getGroup(), Bee.Lombok.getProduct())) {
            Path eclipse = locateActiveEclipse();
            Library lombok = project.getLibrary(Bee.Lombok.getGroup(), Bee.Lombok.getProduct(), Bee.Lombok.getVersion()).iterator().next();

            if (!isLomboked(eclipse)) {
                // install lombok
                Java.with()
                        .classPath(I.class, Bee.class)
                        .classPath(lombok.getJar())
                        .encoding(project.getEncoding())
                        .run(LombokInstaller.class, "install", eclipse.toAbsolutePath().toString());

                // restart eclipse
                ui.warn("Restart your Eclipse to enable Lombok.");
            }
        }
    }

    /**
     * <p>
     * Create project file.
     * </p>
     * 
     * @param file
     */
    private void createProject(Path file) {
        XML doc = I.xml("projectDescription");
        doc.child("name").text(project.getProduct());
        doc.child("comment").text(project.getDescription());
        doc.child("buildSpec").child("buildCommand").child("name").text("org.eclipse.jdt.core.javabuilder");
        doc.child("natures").child("nature").text("org.eclipse.jdt.core.javanature");

        // write file
        makeFile(file, doc);
    }

    /**
     * <p>
     * Create classpath file.
     * </p>
     * 
     * @param file
     */
    private void createClasspath(Path file) {
        XML doc = I.xml("classpath");

        // tests
        for (PathPattern path : project.getTestSourceSet()) {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path.base))
                    .attr("output", relative(project.getTestClasses()));
        }

        // sources
        for (PathPattern path : project.getSourceSet()) {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path.base))
                    .attr("output", relative(project.getClasses().base));
        }

        // projects
        for (PathPattern path : project.getProjectSourceSet()) {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path.base))
                    .attr("output", relative(project.getProjectClasses()));
        }

        // library
        for (Library library : project.getDependency(Scope.Test)) {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar);

                if (Files.exists(source)) {
                    child.attr("sourcepath", source);
                }
            }
        }

        // Bee API
        if (!project.equals(Bee.TOOL)) {
            for (Library lib : project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion())) {
                doc.child("classpathentry").attr("kind", "lib").attr("path", lib.getJar()).attr("sourcepath", lib.getSourceJar());
            }
        }

        // Eclipse configurations
        doc.child("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER");
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project.getClasses().base));

        // write file
        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createFactorypath(Path file, Set<Path> processors) {
        XML doc = I.xml("factorypath");

        for (Path processor : processors) {
            doc.child("factorypathentry").attr("kind", "EXTJAR").attr("id", processor).attr("enabled", true).attr("runInBatchMode", false);
        }

        // write file
        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createAPT(Path file, Entry<String, String> option) {
        Properties properties = new Properties();
        properties.put("eclipse.preferences.version", "1");
        properties.put("org.eclipse.jdt.apt.aptEnabled", "true");
        properties.put("org.eclipse.jdt.apt.genSrcDir", "src/main/auto");
        properties.put("org.eclipse.jdt.apt.reconcileEnabled", "true");

        if (option != null) {
            properties.put("org.eclipse.jdt.apt.processorOptions/" + option.getKey(), option.getValue());
        }

        // write file
        makeFile(file, properties);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createJDT(Path file) {
        try {
            if (Files.notExists(file)) {
                makeFile(file, "");
            }

            Properties doc = new Properties();
            doc.load(Files.newInputStream(file));

            doc.put("org.eclipse.jdt.core.compiler.processAnnotations", "enabled");

            doc.store(Files.newOutputStream(file), "");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Locate relative path.
     * </p>
     * 
     * @param path
     * @return
     */
    private Path relative(Path path) {
        return project.getRoot().relativize(path);
    }

    /**
     * <p>
     * Locate eclipse execution file which is activating now.
     * </p>
     * 
     * @return
     */
    private Path locateActiveEclipse() {
        Path eclipse = null;

        if (Platform.isWindows()) {
            String result = Process.with().read(Arrays.asList("PowerShell", "Get-Process Eclipse | Format-List Path"));

            if (result.startsWith("Path :")) {
                result = result.substring(6).trim();
            }
            eclipse = I.locate(result);
        } else {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error("Not Implement for this platform.");
        }

        if (Files.exists(eclipse)) {
            return eclipse;
        } else {
            throw new Error("Please activate eclipse application.");
        }
    }

    /**
     * <p>
     * Check whether the specified eclipse application is customized or not.
     * </p>
     * 
     * @return A result.
     */
    private boolean isLomboked(Path eclipse) {
        try {
            for (String line : Files.readAllLines(eclipse.resolveSibling("eclipse.ini"))) {
                if (line.contains("lombok.jar")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw I.quiet(e);
        }

    }

    /**
     * @version 2016/09/08 12:03:54
     */
    private static class LombokInstaller extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean process() {
            try {
                Class main = I.type("lombok.launch.Main");
                Method method = main.getMethod("main", String[].class);
                method.setAccessible(true);
                method.invoke(null, new Object[] {args});

                return true;
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }
}
