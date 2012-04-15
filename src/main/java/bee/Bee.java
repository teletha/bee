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

import kiss.I;
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

    /** The common task manager. */
    static final TaskManager tasks = I.make(TaskManager.class);

    static {
        I.load(Bee.class, true);
    }

    /** The project root directory. */
    public final Path root;

    /** The actual project. */
    public final Project project;

    /** The user interface. */
    private UserInterface ui;

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
        ClassLoader loader = I.load(classes);

        try {
            this.project = (Project) I.make(Class.forName("Project", true, loader));
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Compile project definition.
     * </p>
     * 
     * @param input
     * @param output
     */
    private final void compileProject(Path input, Path output) {
        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(input);
        compiler.addClassPath(ClassUtil.getArchive(Bee.class));
        compiler.addClassPath(ClassUtil.getArchive(I.class));
        compiler.setOutput(output);
        compiler.compile();
    }

    /**
     * <p>
     * Set {@link UserInterface} for project build.
     * </p>
     * 
     * @param ui A {@link UserInterface} to use.
     * @return Fluent API.
     */
    public final Bee setUserInterface(UserInterface ui) {
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
     * @param commands A command literal.
     */
    public void execute(String... commands) {
        ProjectLifestyle.local.set(project);
        UserInterfaceLisfestyle.local.set(ui);

        ui.title("Building " + project.getProduct() + " " + project.getVersion());

        Stopwatch stopwatch = new Stopwatch().start();
        String result = "SUCCESS";

        try {
            for (String command : commands) {
                I.make(TaskManager.class).execute(command);
            }
        } catch (Throwable e) {
            result = "FAILURE";
        } finally {
            stopwatch.stop();

            ui.title("BUILD " + result + "        TOTAL TIME: " + stopwatch);
        }
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
        bee.execute("test");
    }
}
