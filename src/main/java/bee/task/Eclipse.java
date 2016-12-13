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
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import bee.Bee;
import bee.Platform;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Scope;
import bee.api.Task;
import bee.extension.JavaExtension;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.Java;
import bee.util.Java.JVM;
import bee.util.PathPattern;
import bee.util.Process;
import kiss.Events;
import kiss.I;
import kiss.XML;
import lombok.SneakyThrows;

/**
 * @version 2016/12/12 20:45:06
 */
public class Eclipse extends Task implements IDESupport {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Override
    @Command("Generate configuration files for Eclipse.")
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
            EclipseManipulator eclipse = new EclipseManipulator();
            Library lombok = project.getLibrary(Bee.Lombok.getGroup(), Bee.Lombok.getProduct(), Bee.Lombok.getVersion()).iterator().next();

            if (!eclipse.isLomboked()) {
                // install lombok
                Java.with()
                        .classPath(I.class, Bee.class)
                        .classPath(lombok.getJar())
                        .encoding(project.getEncoding())
                        .run(LombokInstaller.class, "install", eclipse.locateExe());

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
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project.getClasses()));

        // Java extensions
        JavaExtension extension = I.make(JavaExtension.class);

        boolean needEnhanceJRE = extension.hasJREExtension();
        String JREName = needEnhanceJRE ? "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + project.getProduct() : "";
        doc.child("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER" + JREName);

        if (needEnhanceJRE) {
            // don't call in lambda because enhance process will be failed, why?
            List<Path> JRE = extension.enhancedJRE();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> addJRE(project.getProduct(), JRE)));
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
     * @param file
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
     * @param file
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
     * Locate eclipse installed JRE preference.
     * </p>
     * 
     * @return
     */
    @SneakyThrows
    private void addJRE(String name, List<Path> jars) {
        EclipseManipulator eclipse = new EclipseManipulator();

        Properties properties = new Properties();
        properties.load(Files.newInputStream(eclipse.locateJREPreference()));

        XML root = I.xml(properties.getProperty("org.eclipse.jdt.launching.PREF_VM_XML"));
        XML vm = root.find("vm[name=\"" + name + "\"]");

        Path jar = Events.from(jars).take(path -> path.getFileName().toString().startsWith("rt-")).to().get();

        for (XML location : vm.find("libraryLocation")) {
            if (location.attr("jreJar").matches(".+rt-.+\\.jar")) {
                location.attr("jreJar", jar);
            }
        }

        Path temp = I.locateTemporary();
        properties.setProperty("org.eclipse.jdt.launching.PREF_VM_XML", root.toString());
        properties.store(Files.newBufferedWriter(temp), "");

        Java.with()
                .classPath(project.getClasses())
                .classPath(project.getTestClasses())
                .classPath(project.getDependency(Scope.Test))
                .run(UpdateEclipseConfiguration.class, eclipse.locateExe(), temp);
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
     * @version 2016/12/12 15:05:05
     */
    private static class EclipseManipulator {

        /** The base eclipse.exe. */
        private Path eclipse;

        /**
         * <p>
         * Build eclipse related locator.
         * </p>
         * 
         * @param eclipse
         * @return
         */
        private EclipseManipulator() {
            this(null);
        }

        /**
         * <p>
         * Build eclipse related locator.
         * </p>
         * 
         * @param eclipse
         * @return
         */
        private EclipseManipulator(Path eclipse) {
            this.eclipse = eclipse == null ? locateActiveEclipse() : eclipse;
        }

        /**
         * <p>
         * Locate eclipse.exe.
         * </p>
         * 
         * @return
         */
        private Path locateExe() {
            return eclipse;
        }

        /**
         * <p>
         * Locate eclipse workspace directory which is activating now.
         * </p>
         * 
         * @return
         */
        @SneakyThrows
        private Path locateWorkspace() {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(eclipse.resolveSibling("configuration/.settings/org.eclipse.ui.ide.prefs")));
            return I.locate(properties.getProperty("RECENT_WORKSPACES"));
        }

        /**
         * <p>
         * Locate eclipse workspace directory which is activating now.
         * </p>
         * 
         * @return
         */
        private Path locateJREPreference() {
            return locateWorkspace().resolve(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs");
        }

        /**
         * <p>
         * Check whether the specified eclipse application is customized or not.
         * </p>
         * 
         * @return A result.
         */
        private boolean isLomboked() {
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
         * Open eclipse application.
         */
        private void open() {
            try {
                Process.with()
                        .workingDirectory(eclipse.getParent())
                        .run(Arrays.asList("cmd", "/c", "cd", "/d", eclipse.getParent().toString(), "&", eclipse.getFileName().toString()));
            } catch (Throwable e) {
                e.printStackTrace();
                throw I.quiet(e);
            }
        }

        /**
         * <p>
         * Close eclipse application.
         * </p>
         */
        private void close() {
            Process.with().run(Arrays.asList("PowerShell", "Get-Process Eclipse | %{ $_.CloseMainWindow(); $_.WaitForExit(10000) }"));
        }

        /**
         * <p>
         * Locate eclipse execution file which is activating now.
         * </p>
         * 
         * @return
         */
        private static Path locateActiveEclipse() {
            Path eclipse = null;

            if (Platform.isWindows()) {
                String result = Process.with().read(Arrays.asList("PowerShell", "Get-Process Eclipse | Format-List Path"));

                if (result.startsWith("Path :")) {
                    result = result.substring(6).trim();
                }
                eclipse = I.locate(result);
            } else {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error("Not Implement for this platform.");
            }

            if (Files.exists(eclipse)) {
                return eclipse;
            } else {
                throw new Error("Please activate eclipse application.");
            }
        }
    }

    /**
     * @version 2016/12/12 14:47:01
     */
    private static class UpdateEclipseConfiguration extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process() throws Exception {
            EclipseManipulator eclipse = new EclipseManipulator(I.locate(args[0]));

            // close eclipse
            eclipse.close();

            // locate configuration
            Path updater = I.locate(args[1]);

            // update configuration
            I.copy(Files.newInputStream(updater), Files.newOutputStream(eclipse.locateJREPreference()), true);

            // open eclipse
            eclipse.open();
        }
    }
}
