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

import kiss.Disposable;
import kiss.I;
import kiss.PathListener;
import kiss.model.ClassUtil;
import bee.api.Project;
import bee.compiler.JavaCompiler;
import bee.task.TaskManager;
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

    /** The project root directory. */
    private final Path root;

    /** The project builder. */
    private final ProjectBuilder builder;

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
        this.root = directory.toAbsolutePath();
        this.ui = ui;

        // find project and build it
        this.builder = new ProjectBuilder();
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
            // build project
            ui.talk("Finding your project...");
            Project project = builder.build();

            ProjectLifestyle.local.set(project);
            UserInterfaceLisfestyle.local.set(ui);

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

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
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        builder.observer.dispose();
        super.finalize();
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
     * @version 2012/04/15 14:35:20
     */
    private class ProjectBuilder implements PathListener {

        /** The state whether the current project is updated or not. */
        private boolean updated = true;

        /** The project directory observer. */
        private Disposable observer;

        /** The current project. */
        private Project project;

        /** The current develop environment. */
        private IDE ide;

        /**
         * <p>
         * Build the current project.
         * </p>
         * 
         * @return A project.
         */
        private synchronized Project build() {
            if (updated) {
                try {
                    // search Project from the specified file systems
                    Path sources = root.resolve("src");
                    Path projectSources = sources.resolve("project/java");
                    Path projectClasses = root.resolve("target/project-classes");
                    Path projectFileSource = projectSources.resolve(ProjectFile + ".java");

                    // unload old project
                    I.unload(projectClasses);

                    // write project source if needed
                    scaffold(projectSources, projectFileSource);

                    // compile project sources if needed
                    compile(projectSources, projectClasses);

                    // load new project
                    ClassLoader loader = I.load(projectClasses);

                    // create project
                    project = (Project) I.make(Class.forName(ProjectFile, true, loader));

                    if (observer == null) observer = I.observe(projectSources, this);

                    if (Files.notExists(projectClasses.resolve(ProjectFile + ".class"))) searchDevelopEnvironment();
                } catch (Exception e) {
                    throw I.quiet(e);
                }
                updated = false;
            }

            // API definition
            return project;
        }

        /**
         * <p>
         * Create project skeleton.
         * </p>
         */
        private void scaffold(Path sourceDirectory, Path projectFile) throws Exception {
            if (Files.notExists(projectFile)) {
                ui.talk("Project definition is not found. [" + projectFile + "]");

                if (!ui.confirm("Create new project?")) {
                    ui.talk("See you later!");
                    throw AbortedByUser;
                }

                ui.title("Create New Project");

                String project = ui.ask("Project name");
                String product = ui.ask("Product name", project);
                String version = ui.ask("Product version", "1.0");

                List<String> code = new ArrayList();
                code.add("public class " + ProjectFile + " extends " + Project.class.getName() + " {");
                code.add("");
                code.add("  {");
                code.add("      name(\"" + project + "\", \"" + product + "\", \"" + version + "\");");
                code.add("  }");
                code.add("}");

                Files.createDirectories(projectFile.getParent());
                Files.write(projectFile, code, StandardCharsets.UTF_8);
                ui.talk("Generate project definition.");
            }
        }

        /**
         * <p>
         * Compile project definition.
         * </p>
         * 
         * @param input A project sources.
         * @param output A project classes.
         */
        private void compile(Path input, Path output) {
            ui.talk("Compile project sources.");

            JavaCompiler compiler = new JavaCompiler();
            compiler.addSourceDirectory(input);
            compiler.addClassPath(ClassUtil.getArchive(Bee.class));
            compiler.setOutput(output);
            compiler.compile();
        }

        /**
         * <p>
         * Search develop environemnt.
         * </p>
         */
        private void searchDevelopEnvironment() {
            // search existing environment
            for (IDE ide : IDE.values()) {
                if (ide.exist(root)) {
                    this.ide = ide;
                    return;
                }
            }

            // build environemnt
            ui.talk("Develop environemnt is not found.");

            if (!ui.confirm("Create new develop environemnt?")) {
                ui.talk("See you later!");
                throw Bee.AbortedByUser;
            }

            ui.title("Create New Environment");

            IDE ide = ui.ask("Bee supports the following IDEs.", IDE.class);
            ide.create(root);

            ui.talk("Create ", ide.name(), "'s configuration files.");
        }

        /**
         * <p>
         * Build project develop environment.
         * </p>
         */
        private void layout() {
            if (ide == null) {
                // build environemnt
                ui.talk("Develop environemnt is not found.");

                if (!ui.confirm("Create new develop environemnt?")) {
                    ui.talk("See you later!");
                    throw Bee.AbortedByUser;
                }

                ui.title("Create New Environment");

                IDE ide = ui.ask("Bee supports the following IDEs.", IDE.class);
                ide.create(root);

                ui.talk("Create ", ide.name(), "'s configuration files.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void create(Path path) {
            updated = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void delete(Path path) {
            updated = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void modify(Path path) {
            updated = true;
        }
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
