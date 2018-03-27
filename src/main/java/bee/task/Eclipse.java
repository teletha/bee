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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import bee.Bee;
import bee.Platform;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Repository;
import bee.api.Scope;
import bee.api.Task;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.Config;
import bee.util.Config.Description;
import bee.util.Java;
import bee.util.Java.JVM;
import bee.util.PathPattern;
import bee.util.Process;
import filer.Filer;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.Variable;
import kiss.XML;

/**
 * @version 2018/03/28 8:13:56
 */
public class Eclipse extends Task implements IDESupport {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Override
    @Command(value = "Generate configuration files for Eclipse.", defaults = true)
    public void execute() {
        createClasspath(project.getRoot().resolve(".classpath"));
        createProject(project.getRoot().resolve(".project"));

        Set<Path> processors = project.getAnnotationProcessors();
        boolean enableAnnotationProcessor = !processors.isEmpty();
        createFactorypath(enableAnnotationProcessor, processors);
        createAPT(enableAnnotationProcessor, new ProjectInfo(project));
        createJDT(enableAnnotationProcessor);
        ui.talk("Create Eclipse configuration files.");

        // check lombok
        if (project.hasDependency(Bee.Lombok.getGroup(), Bee.Lombok.getProduct())) {
            EclipseApplication eclipse = EclipseApplication.create();
            Library lombok = project.getLibrary(Bee.Lombok.getGroup(), Bee.Lombok.getProduct(), Bee.Lombok.getVersion()).iterator().next();

            if (!eclipse.isLomboked()) {
                // install lombok
                Java.with()
                        .classPath(I.class, Bee.class)
                        .classPath(lombok.getLocalJar())
                        .encoding(project.getEncoding())
                        .run(LombokInstaller.class, "install", eclipse.locate().get());

                // restart eclipse
                ui.warn("Restart your Eclipse to enable Lombok.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exist(Project project) {
        return Files.isReadable(project.getRoot().resolve(".classpath"));
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
                    .attr("output", relative(project.getClasses()));
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
            Path jar = library.getLocalJar();
            Path source = library.getLocalSourceJar();

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
                doc.child("classpathentry").attr("kind", "lib").attr("path", lib.getLocalJar()).attr("sourcepath", lib.getLocalSourceJar());
            }
        }

        // Eclipse configurations
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project.getClasses()));

        // write file
        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param localFile
     */
    private void createFactorypath(boolean enable, Set<Path> processors) {
        XML doc = I.xml("factorypath");

        for (Path processor : processors) {
            doc.child("factorypathentry")
                    .attr("kind", "EXTJAR")
                    .attr("id", processor)
                    .attr("enabled", enable)
                    .attr("runInBatchMode", false);
        }

        // write file
        makeFile(project.getRoot().resolve(".factorypath"), doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param localFile
     */
    private void createAPT(boolean enable, Entry<String, String> option) {
        Properties properties = new Properties();
        properties.put("eclipse.preferences.version", "1");
        properties.put("org.eclipse.jdt.apt.aptEnabled", String.valueOf(enable));
        properties.put("org.eclipse.jdt.apt.genSrcDir", "src/main/auto");
        properties.put("org.eclipse.jdt.apt.reconcileEnabled", String.valueOf(enable));

        if (option != null) {
            properties.put("org.eclipse.jdt.apt.processorOptions/" + option.getKey(), option.getValue());
        }

        // write file
        makeFile(project.getRoot().resolve(".settings/org.eclipse.jdt.apt.core.prefs"), properties);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param localFile
     */
    private void createJDT(boolean enabled) {
        Path file = project.getRoot().resolve(".settings/org.eclipse.jdt.core.prefs");

        try {
            if (Files.notExists(file)) {
                makeFile(file, "");
            }

            Properties doc = new Properties();
            doc.load(Files.newInputStream(file));
            doc.put("org.eclipse.jdt.core.compiler.processAnnotations", enabled ? "enabled" : "disabled");
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
     * Rewrite sibling eclipse projects to use the current project directly.
     * </p>
     */
    @Command("Rewrite sibling eclipse projects to use the current project directly.")
    public void live() {
        syncProject(true);
    }

    /**
     * <p>
     * Rewrite sibling eclipse projects to use the repository.
     * </p>
     */
    @Command("Rewrite sibling eclipse projects to use the current project directly.")
    public void repository() {
        syncProject(false);
    }

    /**
     * <p>
     * Rewrite sibling eclipse projects.
     * </p>
     */
    private void syncProject(boolean live) {
        String jar = I.make(Repository.class).resolveJar(project.getLibrary()).toString();
        String currentProjectName = project.getRoot().getFileName().toString();

        String oldPath = live ? jar : "/" + currentProjectName;
        String newPath = live ? "/" + currentProjectName : jar;

        for (Path file : Filer.walk(project.getRoot().getParent(), "*/.classpath")) {
            if (!file.startsWith(project.getRoot())) {
                String targetProjectName = file.getParent().getFileName().toString();

                try {
                    XML root = I.xml(file.toFile());
                    XML classpath = root.find("classpathentry[path=\"" + oldPath + "\"]");

                    if (classpath.size() != 0) {
                        // use project source directly
                        classpath.attr("kind", live ? "src" : "lib").attr("path", newPath);

                        // rewrite
                        root.to(Files.newBufferedWriter(file));

                        ui.talk("Project ", targetProjectName, " references ", currentProjectName, live ? " directly." : " in repository.");
                    }
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Eclipse";
    }

    /**
     * @version 2016/12/12 14:44:57
     */
    private static class LombokInstaller extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process() throws Exception {
            Class main = I.type("lombok.launch.Main");
            Method method = main.getMethod("main", String[].class);
            method.setAccessible(true);
            method.invoke(null, new Object[] {args});
        }
    }

    /**
     * @version 2016/12/29 16:19:46
     */
    @Manageable(lifestyle = Singleton.class)
    private static abstract class EclipseApplication {

        /**
         * @param project
         */
        private EclipseApplication() {
        }

        /**
         * <p>
         * Open eclipse application.
         * </p>
         */
        abstract void open();

        /**
         * <p>
         * Close eclipse application.
         * </p>
         */
        abstract void close();

        /**
         * <p>
         * Check whether eclipse application is active or not.
         * </p>
         * 
         * @return A result.
         */
        abstract boolean isActive();

        /**
         * <p>
         * Locate the active eclipse application.
         * </p>
         * 
         * @return
         */
        abstract Variable<Path> locateActive();

        /**
         * <p>
         * Locate the eclipse application.
         * </p>
         * 
         * @return
         */
        final Variable<Path> locate() {
            return Variable.of(Config.user(Locator.class).locate());
        }

        /**
         * <p>
         * Check whether the specified eclipse application is customized or not.
         * </p>
         * 
         * @return A result.
         */
        final boolean isLomboked() {
            try {
                for (String line : Files.readAllLines(locate().get().resolveSibling("eclipse.ini"))) {
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
         * <p>
         * Create the platform specific {@link EclipseApplication}.
         * </p>
         * 
         * @return
         */
        private static EclipseApplication create() {
            if (Platform.isWindows()) {
                return I.make(ForWindows.class);
            } else {
                throw new Error("Unsupported platform.");
            }
        }

        /**
         * @version 2017/01/10 15:49:34
         */
        @Description("Eclipse Location")
        public interface Locator {

            @Description("The location of eclipse application file.")
            default Path locate() {
                return EclipseApplication.create().locateActive().get();
            }
        }

        /**
         * @version 2016/12/26 9:58:41
         */
        private static class ForWindows extends EclipseApplication {

            /**
             * 
             */
            private ForWindows() {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            void open() {
                Path eclipse = locate().get();

                try {
                    Process.with()
                            .workingDirectory(eclipse.getParent())
                            .run("cmd", "/c", "start", "/d", eclipse.getParent(), eclipse.getFileName());
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw I.quiet(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            void close() {
                Process.runWith("PowerShell", "Get-Process Eclipse | %{ $_.Kill(); $_.WaitForExit(10000) }");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            boolean isActive() {
                String result = Process.readWith("PowerShell", "Get-Process | Format-List Name");
                return result.contains("Name : eclipse");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            Variable<Path> locateActive() {
                String result = Process.readWith("PowerShell", "Get-Process Eclipse | Format-List Path");

                if (result.startsWith("Path :")) {
                    result = result.substring(6).trim();
                }

                return Variable.of(result).map(Filer::locate).require(Files::exists);
            }
        }
    }
}
