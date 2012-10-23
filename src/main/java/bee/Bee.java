/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.model.ClassUtil;
import bee.api.License;
import bee.api.Project;
import bee.compiler.JavaCompiler;
import bee.task.TaskManager;
import bee.util.Paths;
import bee.util.Stopwatch;

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
            name("npc", "bee-api", "0.1");
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
    protected final UserInterface ui;

    /** The current project. */
    private Project project;

    /** The current develop environment. */
    private IDE ide;

    /** The internal tasks. */
    private final List<String> tasks = new ArrayList();

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

        // configure project root directory and message notifier
        this.ui = ui;
        this.project = new FavricProject();

        injectProject();
    }

    /**
     * <p>
     * Inject {@link Project} and {@link UserInterface}.
     * </p>
     */
    private void injectProject() {
        ProjectLifestyle.local.set(project);
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
        execute(new CommandTasks(tasks));
    }

    /**
     * <p>
     * Build project.
     * </p>
     * 
     * @param tasks
     */
    public void execute(Build tasks) {
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

            // load new project
            ClassLoader loader = I.load(project.getProjectClasses());

            // create your project
            project = (Project) I.make(Class.forName(ProjectFile, true, loader));
            injectProject();

            // =====================================
            // build project develop environment
            // =====================================
            buildDevelopEnvironment();

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

            // internal tasks
            for (String task : this.tasks) {
                I.make(TaskManager.class).execute(task);
            }

            // user tasks
            tasks.build(project);
        } catch (Throwable e) {
            if (e == AbortedByUser) {
                result = "CANCEL";
            } else {
                result = "FAILURE";
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

            String project = ui.ask("Project name");
            String product = ui.ask("Product name", project);
            String version = ui.ask("Product version", "1.0");
            License license = ui.ask("Product license", License.class);

            List<String> code = new ArrayList();
            code.addAll(license.forJava());
            code.add("public class " + ProjectFile + " extends " + Project.class.getName() + " {");
            code.add("");
            code.add("  {");
            code.add("      name(\"" + project + "\", \"" + product + "\", \"" + version + "\");");
            code.add("  }");
            code.add("}");

            Files.createDirectories(definition.getParent());
            Files.write(definition, code, StandardCharsets.UTF_8);
            ui.talk("Generate project definition.");

            // build project architecture
            tasks.add("prototype:java");
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
        // search existing environment
        for (IDE ide : IDE.values()) {
            if (ide.exist(project)) {
                this.ide = ide;
                return;
            }
        }

        // build environemnt
        ui.title("Create Your Develop Environment");

        ide = ui.ask("Bee supports the following IDEs.", IDE.class);
        ide.create(project);

        ui.talk("Create ", ide.name(), "'s configuration files.");
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
            bee.execute("install");
        } else {
            Bee bee = new Bee();
            bee.execute(tasks);
        }
    }

    /**
     * @version 2012/10/21 14:49:32
     */
    private static class FavricProject extends Project {
    }

    /**
     * @version 2012/04/17 16:33:58
     */
    private static class CommandTasks extends Build {

        /** The task list. */
        private final String[] tasks;

        /**
         * @param tasks
         */
        private CommandTasks(String[] tasks) {
            this.tasks = tasks;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void build(Project project) {
            for (String task : tasks) {
                I.make(TaskManager.class).execute(task);
            }
        }
    }
}
