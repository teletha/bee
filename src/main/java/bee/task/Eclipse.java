/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import bee.Bee;
import bee.BeeInstaller;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Repository;
import bee.api.Scope;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.Config;
import bee.util.Config.Description;
import bee.util.Java;
import bee.util.Java.JVM;
import bee.util.Process;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import kiss.Variable;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;

public class Eclipse extends Task implements IDESupport {

    /**
     * Create eclipse's project file.
     */
    @Override
    @Command(value = "Generate configuration files for Eclipse.", defaults = true)
    public void execute() {
        createClasspath(project.getRoot().file(".classpath"));
        createProject(project.getRoot().file(".project"));

        Set<Location> processors = project.getAnnotationProcessors();
        boolean enableAnnotationProcessor = !processors.isEmpty();
        createFactorypath(enableAnnotationProcessor, processors);
        createAPT(enableAnnotationProcessor, new ProjectInfo(project));
        createJDT(enableAnnotationProcessor);
        ui.info("Create Eclipse configuration files.");

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
        return project.getRoot().file(".classpath").isReadable();
    }

    /**
     * Create project file.
     * 
     * @param file
     */
    private void createProject(File file) {
        if (file.isAbsent()) {
            XML doc = I.xml("projectDescription");
            doc.child("name").text(project.getProduct());
            doc.child("comment").text(project.getDescription());
            doc.child("buildSpec").child("buildCommand").child("name").text("org.eclipse.jdt.core.javabuilder");
            doc.child("natures").child("nature").text("org.eclipse.jdt.core.javanature");

            // write file
            makeFile(file, doc);
        }
    }

    /**
     * Create classpath file.
     * 
     * @param file
     */
    private void createClasspath(File file) {
        XML doc = I.xml("classpath");

        // tests
        project.getTestSourceSet().to(dir -> {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(dir))
                    .attr("output", relative(project.getTestClasses()))
                    .effect(this::assignVisibleForTest);
        });

        // sources
        project.getSourceSet().to(dir -> {
            doc.child("classpathentry").attr("kind", "src").attr("path", relative(dir)).attr("output", relative(project.getClasses()));
        });

        // projects
        project.getProjectSourceSet().to(dir -> {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(dir))
                    .attr("output", relative(project.getProjectClasses()))
                    .effect(this::assignVisibleForTest);
        });

        // library
        Set<Library> libraries = project.getDependency(Scope.Compile, Scope.Annotation);
        libraries.remove(project.asLibrary());

        // test library
        Set<Library> tests = project.getDependency(Scope.Test);
        tests.removeAll(libraries);
        tests.remove(project.asLibrary());

        ForkJoinPool fork = new ForkJoinPool(24);

        I.signal(tests).joinAll(lib -> I.pair(lib.getLocalJar(), lib.getLocalSourceJar()), fork).to(x -> {
            File jar = x.ⅰ;
            File source = x.ⅱ;

            if (jar.isPresent()) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar).effect(this::assignVisibleForTest);

                if (source.isPresent()) {
                    child.attr("sourcepath", source);
                }
            }
        });

        boolean isModuledProject = project.getSources().existFile("*/module-info.java");

        I.signal(libraries).joinAll(lib -> I.pair(lib.getLocalJar(), lib.getLocalSourceJar()), fork).to(x -> {
            File jar = x.ⅰ;
            File source = x.ⅱ;

            if (jar.isPresent()) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar);

                if (source.isPresent()) {
                    child.attr("sourcepath", source);
                }

                if (isModuledProject && jar.asArchive().existFile("module-info.class")) {
                    child.child("attributes").child("attribute").attr("name", "module").attr("value", true);
                }
            }
        });

        // Bee API
        if (!project.equals(Bee.Tool)) {
            BeeInstaller.install(false, true, false);
            doc.child("classpathentry")
                    .attr("kind", "lib")
                    .attr("path", Bee.API.asLibrary().getLocalJar())
                    .attr("sourcepath", Bee.Tool.asLibrary().getLocalSourceJar())
                    .effect(this::assignVisibleForTest);
        }

        // Eclipse configurations
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project.getClasses()));
        doc.child("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER");

        // write file
        makeFile(file, doc);
    }

    /**
     * Helper to assign visible for test attribute.
     * 
     * @param xml
     * @return
     */
    private XML assignVisibleForTest(XML xml) {
        xml.child("attributes").child("attribute").attr("name", "test").attr("value", true);

        return xml;
    }

    /**
     * Create factorypath file.
     * 
     * @param localFile
     */
    private void createFactorypath(boolean enable, Set<Location> processors) {
        XML doc = I.xml("factorypath");

        for (Location processor : processors) {
            doc.child("factorypathentry")
                    .attr("kind", "EXTJAR")
                    .attr("id", processor)
                    .attr("enabled", enable)
                    .attr("runInBatchMode", false);
        }

        // write file
        makeFile(project.getRoot().file(".factorypath"), doc);
    }

    /**
     * Create factorypath file.
     * 
     * @param localFile
     */
    private void createAPT(boolean enable, Entry<String, String> option) {
        Properties properties = new Properties();
        properties.put("eclipse.preferences.version", "1");
        properties.put("org.eclipse.jdt.apt.aptEnabled", String.valueOf(enable));
        properties.put("org.eclipse.jdt.apt.genSrcDir", "src/main/auto");
        properties.put("org.eclipse.jdt.apt.genTestSrcDir", "src/test/auto");
        properties.put("org.eclipse.jdt.apt.reconcileEnabled", String.valueOf(enable));

        if (option != null) {
            properties.put("org.eclipse.jdt.apt.processorOptions/" + option.getKey(), option.getValue());
        }

        // write file
        makeFile(project.getRoot().file(".settings/org.eclipse.jdt.apt.core.prefs"), properties);
    }

    /**
     * Create factorypath file.
     * 
     * @param localFile
     */
    private void createJDT(boolean enabled) {
        File file = project.getRoot().file(".settings/org.eclipse.jdt.core.prefs");

        try {
            if (file.isAbsent()) {
                makeFile(file, "");
            }

            Properties doc = new Properties();
            doc.load(file.newInputStream());
            doc.put("org.eclipse.jdt.core.compiler.processAnnotations", enabled ? "enabled" : "disabled");
            doc.store(file.newOutputStream(), "");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Locate relative path.
     * 
     * @param path
     * @return
     */
    private Directory relative(Directory path) {
        return project.getRoot().relativize(path);
    }

    /**
     * Rewrite sibling eclipse projects to use the current project directly.
     */
    @Command("Rewrite sibling eclipse projects to use the current project directly.")
    public void live() {
        syncProject(true);
    }

    /**
     * Rewrite sibling eclipse projects to use the repository.
     */
    @Command("Rewrite sibling eclipse projects to use the current project in repository.")
    public void repository() {
        syncProject(false);
    }

    /**
     * Rewrite sibling eclipse projects.
     */
    private void syncProject(boolean live) {
        String jar = I.make(Repository.class).resolveJar(project.asLibrary()).toString();
        String currentProjectName = project.getRoot().base();

        String oldPath = live ? jar.substring(0, jar.lastIndexOf(java.io.File.separator + project.getVersion() + java.io.File.separator))
                : "/" + currentProjectName;
        String newPath = live ? "/" + currentProjectName : jar;

        for (File file : project.getRoot().parent().walkFile("*/.classpath").toList()) {
            if (!file.parent().equals(project.getRoot())) {
                String targetProjectName = file.parent().base();

                XML root = I.xml(file.newBufferedReader());
                XML classpath = root.find("classpathentry[path^=\"" + oldPath + "\"]");

                if (classpath.size() != 0) {
                    // use project source directly
                    classpath.attr("kind", live ? "src" : "lib").attr("path", newPath);

                    // rewrite
                    root.to(file.newBufferedWriter());

                    ui.info("Project ", targetProjectName, " references ", currentProjectName, live ? " directly." : " in repository.");
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
     * 
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
     * 
     */
    @Managed(value = Singleton.class)
    private static abstract class EclipseApplication {

        /**
         * Locate the active eclipse application.
         * 
         * @return
         */
        abstract Variable<File> locateActive();

        /**
         * Locate the eclipse application.
         * 
         * @return
         */
        final Variable<File> locate() {
            return Variable.of(Config.user(Locator.class).locate());
        }

        /**
         * Check whether the specified eclipse application is customized or not.
         * 
         * @return A result.
         */
        final boolean isLomboked() {
            for (String line : locate().get().parent().file("eclipse.ini").lines().toList()) {
                if (line.contains("lombok.jar")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Create the platform specific {@link EclipseApplication}.
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
            default File locate() {
                return EclipseApplication.create().locateActive().get();
            }
        }

        /**
         * 
         */
        private static class ForWindows extends EclipseApplication {

            /**
             * {@inheritDoc}
             */
            @Override
            Variable<File> locateActive() {
                String result = Process.readWith("PowerShell", "Get-Process Eclipse | Format-List Path");

                if (result.startsWith("Path :")) {
                    result = result.substring(6).trim();
                }

                File locate = psychopath.Locator.file(result);

                if (locate.isAbsent()) {
                    return Variable.empty();
                } else {
                    return Variable.of(locate);
                }
            }
        }
    }
}