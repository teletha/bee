/*
 * Copyright (C) 2021 Nameless Production Committee
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
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import bee.Bee;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
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
     * <p>
     * Create eclipse's project file.
     * </p>
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
     * <p>
     * Create project file.
     * </p>
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
     * <p>
     * Create classpath file.
     * </p>
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
        List<Library> libraries = I.signal(project.getDependency(Scope.Compile))
                .concat(I.signal(project.getDependency(Scope.Annotation)))
                .toList();

        // test library
        Set<Library> tests = project.getDependency(Scope.Test);
        tests.removeAll(libraries);

        for (Library library : tests) {
            File jar = library.getLocalJar();
            File source = library.getLocalSourceJar();

            if (jar.isPresent()) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar).effect(this::assignVisibleForTest);

                if (source.isPresent()) {
                    child.attr("sourcepath", source);
                }
            }
        }

        for (Library library : libraries) {
            File jar = library.getLocalJar();
            File source = library.getLocalSourceJar();

            if (jar.isPresent()) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar);

                if (source.isPresent()) {
                    child.attr("sourcepath", source);
                }
            }
        }

        // Bee API
        if (!project.equals(Bee.Tool)) {
            doc.child("classpathentry")
                    .attr("kind", "lib")
                    .attr("path", Bee.API.getLibrary().getLocalJar())
                    .attr("sourcepath", Bee.Tool.getLibrary().getLocalSourceJar())
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
     * <p>
     * Create factorypath file.
     * </p>
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
        properties.put("org.eclipse.jdt.apt.genTestSrcDir", "src/test/auto");
        properties.put("org.eclipse.jdt.apt.reconcileEnabled", String.valueOf(enable));

        if (option != null) {
            properties.put("org.eclipse.jdt.apt.processorOptions/" + option.getKey(), option.getValue());
        }

        // write file
        makeFile(project.getRoot().file(".settings/org.eclipse.jdt.apt.core.prefs"), properties);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
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
     * <p>
     * Locate relative path.
     * </p>
     * 
     * @param path
     * @return
     */
    private Directory relative(Directory path) {
        return project.getRoot().relativize(path);
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
         * <p>
         * Locate the active eclipse application.
         * </p>
         * 
         * @return
         */
        abstract Variable<File> locateActive();

        /**
         * <p>
         * Locate the eclipse application.
         * </p>
         * 
         * @return
         */
        final Variable<File> locate() {
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
            for (String line : locate().get().parent().file("eclipse.ini").lines().toList()) {
                if (line.contains("lombok.jar")) {
                    return true;
                }
            }
            return false;
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