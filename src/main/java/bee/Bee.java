/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import bee.api.Library;
import bee.api.License;
import bee.api.Project;
import bee.api.Scope;
import bee.api.VCS;
import bee.task.Help;
import bee.task.Prototype;
import bee.util.JavaCompiler;
import bee.util.Profiling;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * Task based project builder for Java.
 */
public class Bee {

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        // detect version
        if (Locator.locate("src/main/java/bee/Bee.java").isPresent()) {
            version = Locator.file("version.txt").text().trim();
        } else {
            String name = Locator.locate(Bee.class).base();
            int start = name.indexOf("-") + 1;
            int end = name.indexOf('-', start);
            if (end == -1) {
                version = name.substring(start);
            } else {
                version = name.substring(start, end);
            }
        }
    }

    private static final String version;

    /** The api project. */
    public static final Project API = new Project() {

        {
            product("com.github.teletha", "bee-api", version);
        }
    };

    /** The build tool project. */
    public static final Project Tool = new Project() {

        {
            product("com.github.teletha", "bee", version);
        }
    };

    /** The lombok project. */
    public static final Project Lombok = new Project() {

        {
            product("org.projectlombok", "lombok", "1.18.34");
        }
    };

    /** The project build process is aborted by user. */
    public static final RuntimeException Abort = new RuntimeException();

    /** The user interface. */
    private UserInterface ui;

    /** The current project. */
    private Project project;

    /** The internal tasks. */
    private final List<Task> builds = new ArrayList();

    /**
     * Create project builder in current location.
     */
    public Bee() {
        this((Directory) null, null);
    }

    /**
     * Create project builder with the specified {@link UserInterface}.
     * 
     * @param ui A user interface.
     */
    public Bee(UserInterface ui) {
        this(null, ui);
    }

    /**
     * Create project builder in the specified location.
     * 
     * @param directory A project root directory.
     * @param ui A user interface.
     */
    public Bee(Directory directory, UserInterface ui) {
        // lazy loading
        I.load(Bee.class);

        if (ui == null) {
            ui = UserInterface.CUI;
        }

        if (directory == null) {
            directory = Locator.directory("");
        }
        directory.create();

        if (!directory.isDirectory()) {
            directory = directory.parent();
        }

        // set up
        inject(ui);
        inject(new ZeroProject());
    }

    /**
     * Inject {@link Project}.
     */
    private void inject(Project project) {
        this.project = project;

        // inject project
        LifestyleForProject.local.set(project);
    }

    /**
     * Inject {@link UserInterface}.
     */
    private void inject(UserInterface ui) {
        this.ui = ui;

        // inject user interface
        LifestyleForUI.local.set(ui);
    }

    /**
     * Execute tasks from the given command expression.
     * 
     * @param tasks A command literal.
     */
    public int execute(List<String> tasks) {
        return execute(new CommandLineTask(tasks));
    }

    /**
     * Build project.
     * 
     * @param build
     */
    public int execute(Task build) {
        int code = 0;
        String result = "SUCCESS";
        LocalTime start = LocalTime.now();

        try (var x = Profiling.of("Bee")) {
            // =====================================
            // build your project
            // =====================================
            ui.info("Finding your project...   (Bee" + version + "  Java" + Runtime.version() + ")");

            // If you are running in help or version mode, there is no need to search for projects.
            // It will also ignore all user-specified tasks.
            if (BeeOption.Help.value()) {
                build.require(Help::option);
                build.require(Help::task);
                return 0;
            } else if (BeeOption.Version.value()) {
                build.require(Help::version);
                return 0;
            }

            // build project definition
            buildProjectDefinition(project.getProjectDefinition());

            // load project related classes in system class loader
            // BeeLoader.load(project.getClasses());
            BeeLoader.load(project.getProjectClasses());

            // create your project
            String projectFQCN = project.getProjectClasses()
                    .relativize(project.getProjectDefintionClass())
                    .path()
                    .replace('/', '.')
                    .replace(".class", "");
            Class projectClass = Class.forName(projectFQCN);
            inject((Project) projectClass.getDeclaredConstructors()[0].newInstance());

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

            // load project related classes in system class loader
            for (Library library : project.getDependency(Scope.Compile)) {
                BeeLoader.load(library.getLocalJar());
            }

            // load new project
            I.load(projectClass);

            // compose build
            builds.add(build);

            // execute build
            for (Task current : builds) {
                current.execute();
            }
        } catch (Throwable e) {
            code = 1;
            if (e == Abort) {
                result = "CANCEL";
            } else {
                result = "FAILURE";
                ui.info("");
                ui.error(e);
            }
        } finally {
            LocalTime end = LocalTime.now();

            String dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            String duration = Duration.between(start, end).truncatedTo(ChronoUnit.MILLIS).toString().substring(2).toLowerCase();
            ui.title(String.format("Build %s \t %s \t %s", result, dateTime, duration));
            if (BeeOption.Profiling.value()) Profiling.show(ui);
        }
        return code;
    }

    /**
     * Create project skeleton.
     */
    private void buildProjectDefinition(File definition) throws Exception {
        // create project sources if needed
        if (definition.isAbsent()) {
            ui.info("Project definition is not found. [" + definition + "]");

            if (!ui.confirm("Create new project?")) {
                ui.info("See you later!");
                throw Abort;
            }

            ui.title("Create New Project");

            String name = ui.ask("Product name", project.getRoot().name());
            String group = ui.ask("Product group", name.toLowerCase().replaceAll("\\s+", "."));
            String version = ui.ask("Product version", "1.0");
            License license = ui.ask("Product license", License.builtins());

            // build temporary project
            inject(new ZeroProject(group, name, version, license, VCS.detect(project.getRoot())));

            definition.text(project.toBeeDefinition());
            ui.info("Generate project definition.");

            // build project architecture
            builds.add(new Task() {

                @Override
                public void execute() {
                    require(Prototype::java);
                    // require(IDE::execute);
                }
            });
        }

        // compile project sources if needed
        if (definition.lastModifiedMilli() > project.getProjectDefintionClass().lastModifiedMilli()) {
            ui.info("Compile project sources.");

            JavaCompiler.with()
                    .addSourceDirectory(definition.parent())
                    .addClassPath(Locator.locate(Bee.class))
                    .setOutput(project.getProjectClasses())
                    .compile();
        }
    }

    /**
     * Launch bee at the current location with commandline user interface.
     * 
     * @param tasks A list of task commands
     */
    public static void main(String... tasks) {
        if (tasks.length == 0) tasks = new String[] {"ide:delete", "ide", "prototype:java", "--root", "../act11", "--input", "1"};

        // The first priority is to parse options.
        // When the Bee is initialized, the CUI is also initialized, so the values of user input
        // options are not properly reflected.
        List<String> washed = BeeOption.parse(tasks);

        // Don't call new Bee() before parsing options
        System.exit(new Bee().execute(washed));
    }

    /**
     * 
     */
    private class CommandLineTask extends Task {

        /** The task list. */
        private final List<String> tasks;

        /**
         * @param tasks
         */
        private CommandLineTask(List<String> tasks) {
            this.tasks = tasks;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute() {
            for (String task : tasks) {
                execute(task, ui);
            }
        }
    }
}