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
import java.nio.file.Files;
import java.nio.file.Path;

import kiss.Disposable;
import kiss.I;
import kiss.PathListener;
import kiss.model.ClassUtil;
import bee.api.Project;
import bee.compiler.JavaCompiler;
import bee.task.TaskManager;
import bee.util.Inputs;
import bee.util.Stopwatch;

/**
 * <p>
 * Task based project builder for Java.
 * </p>
 * 
 * @version 2012/04/15 0:28:54
 */
public class Bee {

    static {
        I.load(Bee.class, true);
    }

    /** The project root directory. */
    private final Path root;

    /** The user interface. */
    private UserInterface ui;

    /** The state whether the current project is updated or not. */
    private final ProjectUpdater updater;

    /**
     * <p>
     * Create project builder in current location.
     * </p>
     */
    public Bee() {
        this((Path) null);
    }

    /**
     * <p>
     * Create project builder in the specified location path.
     * </p>
     * 
     * @param directory A project root directory path.
     */
    public Bee(String directory) {
        this(I.locate(Inputs.normalize(directory, "")));
    }

    /**
     * <p>
     * Create project builder in the specified location.
     * </p>
     * 
     * @param directory A project root directory.
     */
    public Bee(Path directory) {
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
        this.ui = new CommandLineUserInterface();

        // ================================================
        // Create Project
        // ================================================
        // search Project from the specified file systems
        Path sources = root.resolve("src/project/java");
        Path classes = root.resolve("target/project-classes");
        Path projectDefinitionSource = sources.resolve("Project.java");
        Path projectDefinitionClass = classes.resolve("Project.class");

        if (Files.notExists(projectDefinitionSource)) {
            // create Project source

            // compile Project source
            compileProject(sources, classes);
        } else if (Files.notExists(projectDefinitionClass)) {
            // compile Project source
            compileProject(sources, classes);
        }

        // load Project definition class
        updater = new ProjectUpdater(sources, classes);
    }

    /**
     * <p>
     * Set {@link UserInterface} for project build.
     * </p>
     * 
     * @param ui A {@link UserInterface} to use.
     * @return Fluent API.
     */
    public Bee setUserInterface(UserInterface ui) {
        if (ui == null) {
            ui = new CommandLineUserInterface();
        }
        this.ui = ui;

        // Fluent API
        return this;
    }

    /**
     * <p>
     * Execute tasks from the given command expression.
     * </p>
     * 
     * @param tasks A command literal.
     */
    public void execute(String... tasks) {
        Project project = updater.createProject();

        ProjectLifestyle.local.set(project);
        UserInterfaceLisfestyle.local.set(ui);

        ui.title("Building " + project.getProduct() + " " + project.getVersion());

        Stopwatch stopwatch = new Stopwatch().start();
        String result = "SUCCESS";

        try {
            for (String task : tasks) {
                I.make(TaskManager.class).execute(task);
            }
        } catch (Throwable e) {
            result = "FAILURE";
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
        updater.observer.dispose();
        super.finalize();
    }

    /**
     * <p>
     * Compile project definition.
     * </p>
     * 
     * @param input
     * @param output
     */
    private void compileProject(Path input, Path output) {
        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(input);
        compiler.addClassPath(ClassUtil.getArchive(Bee.class));
        compiler.addClassPath(ClassUtil.getArchive(I.class));
        compiler.setOutput(output);
        compiler.compile();
    }

    /**
     * <p>
     * Launch bee at the current location with commandline user interface.
     * </p>
     * 
     * @param commands A list of task commands
     */
    public static void main(String[] commands) {
        Bee bee = new Bee();
        bee.execute("jar:merge");
    }

    /**
     * @version 2012/04/15 14:35:20
     */
    private class ProjectUpdater implements PathListener {

        /** The state whether the current project is updated or not. */
        private boolean updated = true;

        /** The project directory observer. */
        private final Disposable observer;

        /** The project class directory. */
        private final Path classes;

        /** The current project. */
        private Project project;

        /**
         * Create updater.
         */
        private ProjectUpdater(Path sources, Path classes) {
            this.observer = I.observe(sources, this);
            this.classes = classes;
        }

        /**
         * <p>
         * Create project.
         * </p>
         * 
         * @return
         */
        private Project createProject() {
            if (updated) {
                // unload old project
                I.unload(classes);

                // load new project
                ClassLoader loader = I.load(classes);

                try {
                    project = (Project) I.make(Class.forName("Project", true, loader));
                } catch (Exception e) {
                    throw I.quiet(e);
                }
                updated = false;
            }

            // API definition
            return project;
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
}
