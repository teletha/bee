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
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

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
import kiss.Variable;
import kiss.XML;

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
            EclipseManipulator eclipse = EclipseManipulator.create();
            Library lombok = project.getLibrary(Bee.Lombok.getGroup(), Bee.Lombok.getProduct(), Bee.Lombok.getVersion()).iterator().next();

            if (!eclipse.isLomboked()) {
                // install lombok
                Java.with()
                        .classPath(I.class, Bee.class)
                        .classPath(lombok.getJar())
                        .encoding(project.getEncoding())
                        .run(LombokInstaller.class, "install", eclipse.locate());

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
    private void addJRE(String name, List<Path> jars) {
        try {
            EclipseManipulator eclipse = EclipseManipulator.create();

            Properties properties = new Properties();
            properties.load(Files.newInputStream(eclipse.locateJREPreference()));

            XML root = I.xml(properties.getProperty("org.eclipse.jdt.launching.PREF_VM_XML"));
            XML vm = root.find("vm[name=\"" + name + "\"]");

            if (vm.size() == 0) {
                vm = root.find("vmType")
                        .child("vm")
                        .attr("name", name)
                        .attr("id", name.hashCode())
                        .attr("javadocURL", "http://docs.oracle.com/javase/jp/8/docs/api/")
                        .attr("path", Platform.JavaRuntime.getParent().getParent());
                XML locations = vm.child("libraryLocations");
                for (Path path : jars) {
                    locations.child("libraryLocation")
                            .attr("jreJar", path)
                            .attr("jreSrc", Platform.JavaHome.resolve("src.zip"))
                            .attr("pkgRoot", "");
                }
            } else {
                Path jar = Events.from(jars).take(path -> path.getFileName().toString().startsWith("rt-")).to().get();

                for (XML location : vm.find("libraryLocation")) {
                    if (location.attr("jreJar").matches(".+rt-.+\\.jar")) {
                        location.attr("jreJar", jar);

                        String doc = location.attr("jreSrc");

                        if (doc == null || doc.isEmpty()) {
                            location.attr("jreSrc", Platform.JavaHome.resolve("src.zip"));
                        }
                    }
                }
            }

            Path temp = I.locateTemporary();
            properties.setProperty("org.eclipse.jdt.launching.PREF_VM_XML", root.toString());
            properties.store(Files.newBufferedWriter(temp), "");

            Java.with()
                    .classPath(I.locate(Bee.class))
                    .classPath(I.locate(I.class))
                    .encoding(project.getEncoding())
                    .run(UpdateEclipseConfiguration.class, eclipse.locate(), temp);
        } catch (IOException e) {
            throw I.quiet(e);
        }
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
     * @version 2016/12/12 14:47:01
     */
    private static class UpdateEclipseConfiguration extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process() throws Exception {
            try {
                EclipseManipulator eclipse = EclipseManipulator.create();

                // close eclipse
                eclipse.close();

                // locate configuration
                Path updater = I.locate(args[1]);

                // update configuration
                I.copy(Files.newInputStream(updater), Files.newOutputStream(eclipse.locateJREPreference()), true);

                // open eclipse
                eclipse.open();
            } catch (Throwable e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2016/12/26 9:58:04
     */
    private static abstract class EclipseManipulator {

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
         * Locate the active eclipse application.
         * </p>
         * 
         * @return
         */
        abstract Variable<Path> locateActiveApplication();

        /**
         * <p>
         * Locate the eclipse application.
         * </p>
         * 
         * @return
         */
        final Variable<Path> locate() {
            Preferences prefs = Preferences.userNodeForPackage(EclipseManipulator.class);
            Variable<Path> application = Variable.of(prefs.get("eclipse", null)).map(I::locate);

            if (application.isAbsent() || application.is(Files::notExists)) {
                application.set(locateActiveApplication());

                if (application.isPresent()) {
                    prefs.put("eclipse", application.toString());
                }
            }
            return application;
        }

        /**
         * <p>
         * Locate eclipse workspace directory which is activating now.
         * </p>
         * 
         * @return
         */
        final Path locateWorkspace() {
            try {
                Properties properties = new Properties();
                properties.load(Files.newInputStream(locate().get().resolveSibling("configuration/.settings/org.eclipse.ui.ide.prefs")));
                return I.locate(properties.getProperty("RECENT_WORKSPACES"));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * <p>
         * Locate eclipse workspace directory which is activating now.
         * </p>
         * 
         * @return
         */
        final Path locateJREPreference() throws IOException {
            Path prefs = locateWorkspace().resolve(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs");

            if (Files.notExists(prefs)) {
                Files.createFile(prefs);
            }
            return prefs;
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
         * Create the platform specific {@link EclipseManipulator}.
         * </p>
         * 
         * @return
         */
        private static EclipseManipulator create() {
            if (Platform.isWindows()) {
                return new ForWindows();
            } else {
                throw new Error("Unsupported platform.");
            }
        }
    }

    /**
     * @version 2016/12/26 9:58:41
     */
    private static class ForWindows extends EclipseManipulator {

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
        Variable<Path> locateActiveApplication() {
            String result = Process.readWith("PowerShell", "Get-Process Eclipse | Format-List Path");

            if (result.startsWith("Path :")) {
                result = result.substring(6).trim();
            }

            return Variable.of(result).map(I::locate).require(Files::exists);
        }
    }
}
