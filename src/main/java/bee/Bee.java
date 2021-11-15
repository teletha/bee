/*
 * Copyright (C) 2021 Nameless Production Committee
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
import bee.task.Ide;
import bee.task.Pom;
import bee.task.Prototype;
import bee.util.JavaCompiler;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * Task based project builder for Java.
 * <p>
 * Bee represents a single project build process.
 */
public class Bee {

    private static final String version = "0.13.0";

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
            product("org.projectlombok", "lombok", "1.18.22");
        }
    };

    /** The project build process is aborted by user. */
    public static final RuntimeException AbortedByUser = new RuntimeException();

    static {
        I.load(Bee.class);
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }

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
        inject(new FavricProject());
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

        try {
            // =====================================
            // build your project
            // =====================================
            ui.info("Finding your project...   (Bee" + version + "  Java" + Runtime.version() + ")");

            // build project definition
            buildProjectDefinition(project.getProjectDefinition());

            // load project related classes in system class loader
            BeeLoader.load(project.getClasses());
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

            // synchronize pom automatically
            File pom = Locator.file("pom.xml");
            long lastModified = project.getProjectDefinition().lastModifiedMilli();
            if (pom.lastModifiedMilli() < lastModified) {
                I.make(Pom.class).build();
                pom.lastModifiedTime(lastModified);
            }
        } catch (Throwable e) {
            code = 1;
            if (e == AbortedByUser) {
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
                throw AbortedByUser;
            }

            ui.title("Create New Project");

            String name = ui.ask("Product name", project.getRoot().name());
            String group = ui.ask("Product group", name.toLowerCase().replaceAll("\\s+", "."));
            String version = ui.ask("Product version", "1.0");
            License license = ui.ask("Product license", License.builtins());

            // build temporary project
            inject(new FavricProject(group, name, version, license));

            definition.text(project.toDefinition());
            ui.info("Generate project definition.");

            // build project architecture
            builds.add(new Task() {

                @Override
                public void execute() {
                    require(Prototype::java);
                    require(Ide::execute);
                }
            });
        }

        // compile project sources if needed
        if (definition.lastModifiedMilli() > project.getProjectDefintionClass().lastModifiedMilli()) {
            ui.info("Compile project sources.");

            JavaCompiler compiler = new JavaCompiler();
            compiler.addSourceDirectory(definition.parent());
            compiler.addClassPath(Locator.locate(Bee.class));
            compiler.setOutput(project.getProjectClasses());
            compiler.compile();
        }
    }

    /**
     * Launch bee at the current location with commandline user interface.
     * 
     * @param tasks A list of task commands
     */
    public static void main(String... tasks) {
        List<String> washed = new ArrayList();
        for (String task : tasks) {
            if (!task.startsWith("-")) {
                washed.add(task);
            } else {
                task = task.substring(1);

                String key;
                String value;
                int index = task.indexOf("=");

                if (index != -1) {
                    key = task.substring(0, index);
                    value = task.substring(index + 1);
                } else {
                    key = task;
                    value = "true";
                }

                System.setProperty(key, value);
            }
        }

        System.exit(new Bee().execute(washed.isEmpty() ? List.of("install") : washed));
    }

    /**
     * 
     */
    private static class CommandLineTask extends Task {

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
                execute(task);
            }
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
}