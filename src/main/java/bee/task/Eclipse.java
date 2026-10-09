/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import bee.Bee;
import bee.BeeInstaller;
import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Repository;
import bee.api.Scope;
import bee.task.AnnotationProcessor.ProjectInfo;
import bee.util.Java;
import bee.util.Java.JVM;
import kiss.I;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;
import psychopath.Locator;

public interface Eclipse extends Task, IDESupport {

    /** The default JRE container which uses the workspace default JRE. */
    String DEFAULT_JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER";

    /** The prefix of the Eclipse JRE container path which identifies a specific JRE. */
    String JRE_CONTAINER = DEFAULT_JRE_CONTAINER + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";

    /** The standard VM type identifier of Eclipse. */
    String VM_TYPE = "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType";

    /** The workspace preference file which stores the installed JREs. */
    String INSTALLED_JRE = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs";

    /** The preference key of the installed JREs. */
    String PREF_INSTALLED_JRE = "org.eclipse.jdt.launching.PREF_VM_XML";

    /** The preference key of the recent workspaces. */
    String PREF_RECENT_WORKSPACES = "RECENT_WORKSPACES";

    /**
     * {@inheritDoc}
     */
    @Override
    @Command(value = "Generate configuration files for Eclipse.", defaults = true)
    default void create() {
        createClasspath(project().getRoot().file(".classpath"), registerInstalledJRE());
        createProject(project().getRoot().file(".project"));

        Set<Location> processors = project().getAnnotationProcessors();
        boolean enableAnnotationProcessor = !processors.isEmpty();
        createFactorypath(enableAnnotationProcessor, processors);
        createAPT(enableAnnotationProcessor, new ProjectInfo(project()));
        createJDT(enableAnnotationProcessor);
        ui().info("Create Eclipse configuration files.");

        // check lombok
        if (project().hasDependency(Bee.Lombok.getGroup(), Bee.Lombok.getProduct())) {
            File eclipse = locateActiveEclipse();
            Library lombok = project().getLibrary(Bee.Lombok.getGroup(), Bee.Lombok.getProduct(), Bee.Lombok.getVersion())
                    .iterator()
                    .next();

            if (!isLomboked(eclipse)) {
                // install lombok
                Java.with()
                        .classPath(I.class, Bee.class)
                        .classPath(lombok.getLocalJar())
                        .encoding(project().getEncoding())
                        .run(LombokInstaller.class, "install", eclipse);

                // restart eclipse
                ui().warn("Restart your Eclipse to enable Lombok.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Command("Delete configuration files for Eclipse.")
    default void delete() {
        deleteFile(".classpath");
        deleteFile(".factorypath");
        deleteFile(".project");
        deleteDirectory(".settings");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean exist(Project project) {
        return project().getRoot().file(".classpath").isReadable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default String name() {
        return "Eclipse";
    }

    /**
     * Create project file.
     * 
     * @param file
     */
    private void createProject(File file) {
        if (file.isAbsent()) {
            // create
            XML doc = I.xml("projectDescription");
            doc.child("name").text(project().getProduct());
            doc.child("comment").text(project().getDescription());
            doc.child("projects");
            doc.child("buildSpec").child("buildCommand", com -> {
                com.child("name").text("org.eclipse.jdt.core.javabuilder");
                com.child("arguments");
            });
            doc.child("natures").child("nature").text("org.eclipse.jdt.core.javanature");

            makeFile(file, doc);
        } else {
            // modify
            XML doc = I.xml(file.asJavaPath());

            if (doc.find("buildCommand name:contains(org.eclipse.jdt.core.javabuilder)").size() == 0) {
                doc.find("buildSpec").child("buildCommand", com -> {
                    com.child("name").text("org.eclipse.jdt.core.javabuilder");
                    com.child("arguments");
                });
            }

            if (doc.find("nature:contains(org.eclipse.jdt.core.javanature)").size() == 0) {
                doc.find("natures").child("nature").text("org.eclipse.jdt.core.javanature");
            }

            makeFile(file, doc);
        }
    }

    /**
     * Create classpath file.
     * 
     * @param file
     */
    private void createClasspath(File file, String installedJRE) {
        XML doc = I.xml("classpath");

        // tests
        project().getTestSourceSet().to(dir -> {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(dir))
                    .attr("output", relative(project().getTestClasses()))
                    .effect(this::assignVisibleForTest);
        });

        // sources
        project().getSourceSet().to(dir -> {
            doc.child("classpathentry").attr("kind", "src").attr("path", relative(dir)).attr("output", relative(project().getClasses()));
        });

        // projects
        project().getProjectSourceSet().to(dir -> {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(dir))
                    .attr("output", relative(project().getProjectClasses()))
                    .effect(this::assignVisibleForTest);
        });

        Set<Library> compileLike = project().getDependency(Scope.Compile, Scope.Annotation);
        compileLike.add(project().asLibrary());

        Set<Library> testOnly = project().getDependency(Scope.Test);
        testOnly.remove(project().asLibrary());
        testOnly.removeIf(testLib -> compileLike.stream().anyMatch(compileLib -> testLib.isSame(compileLib)));
        compileLike.remove(project().asLibrary());

        I.signal(testOnly).joinAll(lib -> I.pair(lib.getLocalJar(), lib.getLocalSourceJar())).to(x -> {
            File jar = x.ⅰ;
            File source = x.ⅱ;

            if (jar.isPresent()) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar).effect(this::assignVisibleForTest);

                if (source.isPresent()) {
                    child.attr("sourcepath", source);
                }
            }
        });

        boolean isModuledProject = project().getSources().existFile("*/module-info.java");

        I.signal(compileLike).joinAll(lib -> I.pair(lib.getLocalJar(), lib.getLocalSourceJar())).to(x -> {
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
        if (!project().equals(Bee.Tool)) {
            BeeInstaller.install(false, true, false);
            doc.child("classpathentry")
                    .attr("kind", "lib")
                    .attr("path", Bee.API.asLibrary().getLocalJar())
                    .attr("sourcepath", Bee.Tool.asLibrary().getLocalSourceJar())
                    .effect(this::assignVisibleForTest);
        }

        // Eclipse configurations
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project().getClasses()));
        doc.child("classpathentry").attr("kind", "con").attr("path", jreContainer(installedJRE));

        // write file
        makeFile(file, doc);
    }

    /**
     * Resolve the JRE container. When the JDK currently used by Bee is registered in the active
     * Eclipse workspace, the container refers to it by name so that the project uses exactly that
     * JDK. Otherwise the default JRE container is emitted as-is, so Bee never forces a JRE
     * configuration which could break the Eclipse environment.
     * 
     * @param installedJRE The name of the JRE registered in Eclipse, or <code>null</code>.
     * @return An Eclipse JRE container path.
     */
    private String jreContainer(String installedJRE) {
        if (installedJRE != null) {
            return JRE_CONTAINER + installedJRE;
        }
        return DEFAULT_JRE_CONTAINER;
    }

    /**
     * Register the JDK currently used by Bee in the installed JREs of the active Eclipse workspace
     * so
     * that the generated projects can use it. The operation is skipped when Eclipse is not active
     * or
     * its workspace can not be located.
     * 
     * @return The name of the installed JRE, or <code>null</code> when it is not registered.
     */
    private String registerInstalledJRE() {
        File eclipse;
        try {
            eclipse = locateActiveEclipse();
        } catch (Exception e) {
            // Eclipse is not active, so there is no workspace to update.
            return null;
        }

        int required = project().getJavaRequiredVersion().runtimeVersion().feature();
        Directory jdk = resolveJDK(required);
        if (jdk == null) {
            try {
                jdk = JDK.install(required);
            } catch (Exception e) {
                ui().warn("Failed to install the JDK [", required, "] required by the project.");
                return null;
            }
        }

        try {
            Directory workspace = locateWorkspace(eclipse);
            if (workspace == null) {
                return null;
            }

            File file = workspace.file(INSTALLED_JRE);
            Properties properties = new Properties();
            if (file.isPresent()) {
                try (InputStream in = file.newInputStream()) {
                    properties.load(in);
                }
            }

            String xml = properties.getProperty(PREF_INSTALLED_JRE);
            XML root = xml == null || !xml.strip().startsWith("<") ? I.xml("vmSettings") : I.xml(xml);
            XML type = null;
            for (XML candidate : root.find("vmType")) {
                if (VM_TYPE.equals(candidate.attr("id"))) {
                    type = candidate;
                    break;
                }
            }
            if (type == null) {
                type = root.child("vmType").attr("id", VM_TYPE);
            }

            String home = jdk.absolutize().asJavaPath().toString();
            String name = null;
            for (XML vm : type.find("vm")) {
                if (samePath(vm.attr("path"), home)) {
                    name = vm.attr("name");
                    break;
                }
            }
            if (name != null && !name.isBlank()) {
                return name;
            }

            // The JDK is not registered yet, so add it to the installed JREs.
            name = uniqueName(type, jdk.base());
            type.child("vm").attr("id", System.currentTimeMillis()).attr("name", name).attr("path", home);
            properties.setProperty(PREF_INSTALLED_JRE, root.toString());
            properties.putIfAbsent("eclipse.preferences.version", "1");

            file.parent().create();
            try (OutputStream out = file.newOutputStream()) {
                properties.store(out, "Bee");
            }

            ui().info("Register the JDK [", name, "] in the installed JREs of Eclipse.");
            ui().warn("Restart Eclipse to use the JDK [", name, "].");
            return name;
        } catch (Exception e) {
            ui().warn("Failed to register the JDK in the installed JREs of Eclipse.");
            return null;
        }
    }

    /**
     * Resolve the JDK to register in Eclipse. The JDK selected in Bee has the highest priority, so
     * that Eclipse uses the same JDK as Bee. The selected link is used as-is, so switching the
     * selected JDK also switches the registered JRE. When it is absent, the JDK whose feature
     * version matches the Java version required by the project is used.
     * 
     * @param feature The required Java feature version.
     * @return A JDK, or <code>null</code> when no matching JDK is installed.
     */
    private Directory resolveJDK(int feature) {
        Directory selected = Platform.BeeHome.directory("jdk").directory(JDK.SELECTED_LINK);
        if (Platform.feature(selected) != -1) {
            return selected;
        }

        if (Platform.feature(Platform.JavaHome) == feature) {
            return Platform.JavaHome;
        }

        for (Directory candidate : Platform.BeeHome.directory("jdk").walkDirectory("*").toList()) {
            if (Platform.feature(candidate) == feature && candidate.file(Platform.isWindows() ? "bin/javac.exe" : "bin/javac")
                    .isPresent()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Locate the workspace of the active Eclipse from its recent workspaces.
     * 
     * @param eclipse An Eclipse executable.
     * @return The active workspace, or <code>null</code> when it can not be located.
     */
    private Directory locateWorkspace(File eclipse) {
        File preference = eclipse.parent().file("configuration/.settings/org.eclipse.ui.ide.prefs");
        if (preference.isAbsent()) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream in = preference.newInputStream()) {
            properties.load(in);
        } catch (IOException e) {
            return null;
        }

        String recent = properties.getProperty(PREF_RECENT_WORKSPACES);
        if (recent != null) {
            for (String path : recent.split("\\R")) {
                path = path.strip();
                if (!path.isEmpty()) {
                    Directory workspace = Locator.directory(path);
                    if (workspace.directory(".metadata").isPresent()) {
                        return workspace;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Test whether the two paths point to the same location.
     * 
     * @param one A path.
     * @param other Another path.
     * @return A result.
     */
    private boolean samePath(String one, String other) {
        return one != null && other != null && Locator.directory(one)
                .absolutize()
                .path()
                .equalsIgnoreCase(Locator.directory(other).absolutize().path());
    }

    /**
     * Resolve an unused JRE name.
     * 
     * @param type A VM type.
     * @param name A preferred name.
     * @return A unique name.
     */
    private String uniqueName(XML type, String name) {
        if (name == null || name.isBlank()) {
            name = "Bee";
        }

        Set<String> names = new HashSet();
        for (XML vm : type.find("vm")) {
            names.add(vm.attr("name"));
        }

        if (!names.contains(name)) {
            return name;
        }
        for (int i = 2;; i++) {
            String candidate = name + " (" + i + ")";
            if (!names.contains(candidate)) {
                return candidate;
            }
        }
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
        makeFile(project().getRoot().file(".factorypath"), doc);
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
        makeFile(project().getRoot().file(".settings/org.eclipse.jdt.apt.core.prefs"), properties);
    }

    /**
     * Create factorypath file.
     * 
     * @param localFile
     */
    private void createJDT(boolean enabled) {
        File file = project().getRoot().file(".settings/org.eclipse.jdt.core.prefs");

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
        return project().getRoot().relativize(path);
    }

    /**
     * Rewrite sibling eclipse projects to use the current project directly.
     */
    @Command("Rewrite sibling eclipse projects to use the current project directly.")
    default void live() {
        syncProject(true);
    }

    /**
     * Rewrite sibling eclipse projects to use the repository.
     */
    @Command("Rewrite sibling eclipse projects to use the current project in repository.")
    default void repository() {
        syncProject(false);
    }

    /**
     * Rewrite sibling eclipse projects.
     */
    private void syncProject(boolean live) {
        String jar = I.make(Repository.class).resolveJar(project().asLibrary()).toString();
        String currentProjectName = project().getRoot().base();

        String oldPath = live ? jar.substring(0, jar.lastIndexOf(java.io.File.separator + project().getVersion() + java.io.File.separator))
                : "/" + currentProjectName;
        String newPath = live ? "/" + currentProjectName : jar;

        for (File file : project().getRoot().parent().walkFile("*/.classpath").toList()) {
            if (!file.parent().equals(project().getRoot())) {
                String targetProjectName = file.parent().base();

                XML root = I.xml(file.newBufferedReader());
                XML classpath = root.find("classpathentry[path^=\"" + oldPath + "\"]");

                if (classpath.size() != 0) {
                    // use project source directly
                    classpath.attr("kind", live ? "src" : "lib").attr("path", newPath);

                    // rewrite
                    root.to(file.newBufferedWriter());

                    ui().info("Project ", targetProjectName, " references ", currentProjectName, live ? " directly." : " in repository.");
                }
            }
        }
    }

    class LombokInstaller extends JVM {

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
     * Locate the active eclipse application.
     * 
     * @return
     */
    private File locateActiveEclipse() {
        if (!Platform.isWindows()) {
            throw new Fail("Unsupported platform.");
        }

        for (String line : bee.util.Process.readWith("PowerShell", "Get-Process Eclipse | Format-List Path").lines().toList()) {
            line = line.strip();
            if (line.startsWith("Path :")) {
                File locate = psychopath.Locator.file(line.substring(6).strip());
                if (locate.isPresent()) {
                    return locate;
                }
            }
        }
        throw new Fail("Process is not found, activate Eclipse application.");
    }

    /**
     * Check whether the specified eclipse application is customized or not.
     * 
     * @return A result.
     */
    private boolean isLomboked(File eclipse) {
        for (String line : eclipse.parent().file("eclipse.ini").lines().toList()) {
            if (line.contains("lombok.jar")) {
                return true;
            }
        }
        return false;
    }
}