/*
 * Copyright (C) 2015 Nameless Production Committee
 * 
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import bee.api.Library;
import bee.api.License;
import bee.api.Project;
import bee.api.Scope;
import bee.api.StandardLicense;
import bee.api.Task;
import bee.task.Prototype;
import bee.util.JavaCompiler;
import bee.util.Paths;
import bee.util.Stopwatch;
import kiss.Extensible;
import kiss.I;
import kiss.model.ClassUtil;

/**
 * <p>
 * Task based project builder for Java.
 * </p>
 * <p>
 * Bee represents a single project build process.
 * </p>
 * 
 * @version 2012/04/15 0:28:54
 */
public class Bee {

    /** The api project. */
    public static final Project API = new Project() {

        {
            product("npc", "bee-api", "0.1");
        }
    };

    /** The build tool project. */
    public static final Project TOOL = new Project() {

        {
            product("npc", "Bee", "0.1");
        }
    };

    /** The project build process is aborted by user. */
    public static final RuntimeException AbortedByUser = new RuntimeException();

    /** The project definition file name. */
    private static final String ProjectFile = "Project";

    static {
        I.load(Bee.class, true);
    }

    /** The user interface. */
    private UserInterface ui;

    /** The current project. */
    private Project project;

    /** The internal tasks. */
    private final List<Task> builds = new ArrayList();

    /**
     * <p>
     * Create project builder in current location.
     * </p>
     */
    public Bee() {
        this((Path) null, null);
    }

    /**
     * <p>
     * Create project builder with the specified {@link UserInterface}.
     * </p>
     * 
     * @param ui A user interface.
     */
    public Bee(UserInterface ui) {
        this(null, ui);
    }

    /**
     * <p>
     * Create project builder in the specified location.
     * </p>
     * 
     * @param directory A project root directory.
     * @param ui A user interface.
     */
    public Bee(Path directory, UserInterface ui) {
        if (ui == null) {
            ui = new CommandLineUserInterface();
        }

        if (directory == null) {
            directory = I.locate("");
        }

        if (Files.notExists(directory)) {
            try {
                directory = Files.createDirectories(directory);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
        }

        // set up
        inject(ui);
        inject(new FavricProject());
    }

    /**
     * <p>
     * Inject {@link Project}.
     * </p>
     */
    private void inject(Project project) {
        this.project = project;

        // inject project
        ProjectLifestyle.local.set(project);
    }

    /**
     * <p>
     * Inject {@link UserInterface}.
     * </p>
     */
    private void inject(UserInterface ui) {
        this.ui = ui;

        // inject user interface
        UserInterfaceLisfestyle.local.set(ui);
    }

    /**
     * <p>
     * Execute tasks from the given command expression.
     * </p>
     * 
     * @param tasks A command literal.
     */
    public void execute(final String... tasks) {
        execute(new CommandLineTask(tasks));
    }

    /**
     * <p>
     * Build project.
     * </p>
     * 
     * @param build
     */
    public void execute(Task build) {
        String result = "SUCCESS";
        Stopwatch stopwatch = new Stopwatch().start();

        try {
            // =====================================
            // build your project
            // =====================================
            ui.talk("Finding your project...");

            // unload old project
            I.unload(project.getProjectClasses());

            // build project definition
            buildProjectDefinition(project.getProjectDefinition());

            // load project related classes in system class loader
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(ClassLoader.getSystemClassLoader(), project.getClasses().base.toUri().toURL());
            method.invoke(ClassLoader.getSystemClassLoader(), project.getProjectClasses().toUri().toURL());

            // create your project
            inject((Project) I.make(Class.forName(ProjectFile)));

            // load project related classes in system class loader
            for (Library library : project.getDependency(Scope.Compile)) {
                method.invoke(ClassLoader.getSystemClassLoader(), library.getJar().toUri().toURL());
            }

            // load new project
            I.load(project.getProjectClasses());

            // =====================================
            // build project develop environment
            // =====================================
            buildDevelopEnvironment();

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

            // compose build
            builds.add(build);

            // execute build
            for (Task current : builds) {
                current.execute();
            }
        } catch (Throwable e) {
            if (e == AbortedByUser) {
                result = "CANCEL";
            } else {
                result = "FAILURE";
                ui.talk("");
                ui.error(e);
            }
        } finally {
            stopwatch.stop();

            ui.title("BUILD " + result + "        TOTAL TIME: " + stopwatch);
        }
    }

    /**
     * <p>
     * Create project skeleton.
     * </p>
     */
    private void buildProjectDefinition(Path definition) throws Exception {
        // create project sources if needed
        if (Files.notExists(definition)) {
            ui.talk("Project definition is not found. [" + definition + "]");

            if (!ui.confirm("Create new project?")) {
                ui.talk("See you later!");
                throw AbortedByUser;
            }

            ui.title("Create New Project");

            String name = ui.ask("Product name", project.getRoot().getFileName().toString());
            String group = ui.ask("Product group", name.toLowerCase().replaceAll("\\s+", "."));
            String version = ui.ask("Product version", "1.0");
            StandardLicense license = ui.ask("Product license", StandardLicense.class);

            // build temporary project
            inject(new FavricProject(group, name, version, license));

            Files.createDirectories(definition.getParent());
            Files.write(definition, project.toDefinition(), StandardCharsets.UTF_8);
            ui.talk("Generate project definition.");

            // build project architecture
            builds.add(new Task() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void execute() {
                    require(Prototype.class).java();
                }
            });
        }

        // compile project sources if needed
        if (Paths.getLastModified(definition) > Paths.getLastModified(project.getProjectDefintionClass())) {
            ui.talk("Compile project sources.");

            JavaCompiler compiler = new JavaCompiler();
            compiler.addSourceDirectory(project.getProjectSources());
            compiler.addClassPath(ClassUtil.getArchive(Bee.class));
            compiler.setOutput(project.getProjectClasses());
            compiler.compile();
        }
    }

    /**
     * <p>
     * Build develop environemnt.
     * </p>
     */
    private void buildDevelopEnvironment() {
        List<IDE> ides = I.find(IDE.class);

        // search existing environment
        for (IDE ide : ides) {
            if (ide.exist(project)) {
                return;
            }
        }

        // build environemnt
        ui.talk("\r\nProject develop environment is not found.");
        builds.add(ui.ask("Bee supports the following IDEs.", ides));
    }

    /**
     * <p>
     * Launch bee at the current location with commandline user interface.
     * </p>
     * 
     * @param tasks A list of task commands
     */
    public static void main(String[] tasks) {
        if (tasks == null || tasks.length == 0) {
            Bee bee = new Bee();
            // bee.execute("install");
            bee.execute("license");
        } else {
            Bee bee = new Bee();
            bee.execute(tasks);
        }
    }

    /**
     * @version 2012/11/03 1:47:40
     */
    private static class CommandLineTask extends Task {

        /** The task list. */
        private final String[] tasks;

        /**
         * @param tasks
         */
        private CommandLineTask(String[] tasks) {
            this.tasks = tasks;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute() {
            require(tasks);
        }
    }

    /**
     * @version 2012/10/21 14:49:32
     */
    private static class FavricProject extends Project {

        /**
         * 
         */
        private FavricProject() {
        }

        /**
         * @param projectName
         */
        private FavricProject(String group, String name, String version, License license) {
            product(group, name, version);
            license(license);
        }
    }

    /**
     * @version 2012/10/25 21:33:00
     */
    private static abstract class IDE extends Task implements Extensible {

        /**
         * <p>
         * Analyze environment.
         * </p>
         * 
         * @param project
         * @return
         */
        abstract boolean exist(Project project);

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        /**
         * @version 2012/10/25 21:34:05
         */
        @SuppressWarnings("unused")
        private static final class Eclipse extends IDE {

            /**
             * {@inheritDoc}
             */
            @Override
            boolean exist(Project project) {
                return Files.isReadable(project.getRoot().resolve(".classpath"));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void execute() {
                require(bee.task.Eclipse.class).eclipse();
            }
        }

        /**
         * @version 2012/10/25 21:34:05
         */
        @SuppressWarnings("unused")
        private static final class NetBeans extends IDE {

            /**
             * {@inheritDoc}
             */
            @Override
            boolean exist(Project project) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void execute() {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * @version 2012/10/25 21:34:05
         */
        @SuppressWarnings("unused")
        private static final class IDEA extends IDE {

            /**
             * {@inheritDoc}
             */
            @Override
            boolean exist(Project project) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void execute() {
                throw new UnsupportedOperationException();
            }
        }
    }

}
