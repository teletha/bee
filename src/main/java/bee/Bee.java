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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import auto483.JEP483;
import bee.api.Library;
import bee.api.License;
import bee.api.Project;
import bee.api.Scope;
import bee.api.VCS;
import bee.task.Help;
import bee.task.Prototype;
import bee.util.Inputs;
import bee.util.JavaCompiler;
import bee.util.Profiling;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * Bee is a task-based build tool for Java projects.
 * It manages project compilation, dependency resolution, task execution,
 * and provides user interaction through a {@link UserInterface}.
 *
 * <p>
 * The build process typically involves:
 * </p>
 * <ol>
 * <li>Parsing command-line options ({@link BeeOption}).</li>
 * <li>Locating or creating the project structure.</li>
 * <li>Compiling the project definition file ({@code project.bee}).</li>
 * <li>Loading the project definition using a dedicated class loader.</li>
 * <li>Executing the specified tasks (e.g., "compile", "test", "jar").</li>
 * <li>Reporting the build result and duration.</li>
 * </ol>
 *
 * @see Project
 * @see Task
 * @see UserInterface
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

    /**
     * Creates a project builder instance targeting the current working directory
     * and using the default console user interface ({@link UserInterface#CUI}).
     */
    public Bee() {
        this((Directory) null, null);
    }

    /**
     * Creates a project builder instance targeting the current working directory
     * with the specified {@link UserInterface}.
     *
     * @param ui The user interface to use for output and input. If null, defaults to CUI.
     */
    public Bee(UserInterface ui) {
        this(null, ui);
    }

    /**
     * Creates a project builder instance targeting the specified project root directory
     * and using the specified {@link UserInterface}.
     *
     * @param directory The root directory of the project. If null or invalid, attempts to use
     *            the current working directory or its parent.
     * @param ui The user interface to use for output and input. If null, defaults to CUI.
     */
    public Bee(Directory directory, UserInterface ui) {
        // lazy loading
        I.load(Bee.class);

        if (ui == null) {
            ui = UserInterface.CUI;
        }

        if (directory == null) {
            directory = BeeOption.Root.value();
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
     * Injects the specified {@link Project} instance into the Bee context.
     * This updates the internal project reference and sets the project-specific
     * lifestyle context (used for project-scoped dependency injection).
     *
     * @param project The project instance to inject.
     */
    private void inject(Project project) {
        this.project = project;

        // inject project
        LifestyleForProject.local.set(project);
    }

    /**
     * Injects the specified {@link UserInterface} instance into the Bee context.
     * This updates the internal UI reference and sets the UI-specific
     * lifestyle context (used for UI-scoped dependency injection).
     *
     * @param ui The user interface instance to inject.
     */
    private void inject(UserInterface ui) {
        this.ui = ui;

        // inject user interface
        LifestyleForUI.local.set(ui);
    }

    /**
     * Executes a sequence of tasks based on the provided list of command strings.
     * This method orchestrates the main build lifecycle, including project definition loading,
     * task execution, and result reporting.
     *
     * @param tasks A list of task command strings (e.g., "compile", "test:run", "jar").
     * @return The process exit code (0 for success, non-zero for failure or cancellation).
     */
    public int execute(List<String> tasks) {
        int exitCode = 0;
        String result = "SUCCESS";
        LocalTime start = LocalTime.now();

        try (var x = Profiling.of("Bee Core Process")) {
            // =====================================
            // build your project
            // =====================================
            ui.info("Finding your project in ", project.getRoot().absolutize().normalize(), "   (Bee", version, "  Java", Runtime
                    .version(), ")");

            // If you are running in help or version mode, there is no need to search for projects.
            // It will also ignore all user-specified tasks.
            if (BeeOption.Help.value()) {
                TaskOperations.require(Help::option);
                TaskOperations.require(Help::task);
                return 0;
            } else if (BeeOption.Version.value()) {
                TaskOperations.require(Help::version);
                return 0;
            }

            // =====================================
            // Build Project Definition
            // =====================================
            tasks.addAll(0, buildProjectDefinition(project.getProjectDefinition()));

            // load project related classes
            @SuppressWarnings("resource")
            PriorityClassLoader loader = new PriorityClassLoader(ui).addClassPath(project.getProjectClasses());
            Thread.currentThread().setContextClassLoader(loader);

            // create your project
            String projectFQCN = project.getProjectClasses()
                    .relativize(project.getProjectDefintionClass())
                    .path()
                    .replace('/', '.')
                    .replace(".class", "");
            Class projectClass = Class.forName(projectFQCN, false, loader);
            inject((Project) projectClass.getDeclaredConstructors()[0].newInstance());

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

            // load project related classes in system class loader
            for (Library library : project.getDependency(Scope.Compile)) {
                loader.addClassPath(library.getLocalJar());
            }

            // load your project
            I.load(projectClass);

            // =====================================
            // Execute Tasks
            // =====================================
            for (String task : tasks) {
                execute(task);
            }
        } catch (Throwable e) {
            exitCode = 1;
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
        return exitCode;
    }

    /**
     * Parses and executes a single task command string (e.g., "compile", "test:run").
     * It identifies the task and command, finds the corresponding method,
     * creates the task instance, and invokes the command method.
     * Handles potential misspellings and checks skip options.
     *
     * @param input The task command string.
     * @return The result returned by the invoked command method, or null if skipped or input is
     *         empty.
     * @throws Fail If the task or command cannot be found or is misspelled and not confirmed.
     * @throws RuntimeException If any other error occurs during task execution.
     */
    static final Object execute(String input) {
        // parse command
        if (input == null) {
            return null;
        }

        // remove head and tail white space
        input = input.trim();

        if (input.length() == 0) {
            return null;
        }

        UserInterface ui = TaskOperations.ui();

        // analyze task name
        String taskName = "";
        String commandName = "";
        int index = input.indexOf(':');

        if (index == -1) {
            taskName = input;
        } else {
            taskName = input.substring(0, index);
            commandName = input.substring(index + 1);
        }

        // search task
        TaskInfo info = TaskInfo.by(taskName);

        if (commandName.isEmpty()) {
            commandName = info.defaultCommnad;
        }
        commandName = commandName.toLowerCase();

        // search command
        Method command = info.commands.get(commandName);

        if (command == null) {
            // Search for command with similar names for possible misspellings.
            String recommend = Inputs.recommend(commandName, info.commands.keySet());
            if (recommend != null && ui.confirm("Isn't it a misspelling of command [" + recommend + "] ?")) {
                command = info.commands.get(recommend);
            } else {
                Fail failure = new Fail("Task [" + taskName + "] doesn't have the command [" + commandName + "]. Task [" + taskName + "] can use the following commands.");
                for (Entry<String, String> entry : info.descriptions.entrySet()) {
                    failure.solve(String.format("%s:%-8s \t%s", taskName, entry.getKey(), entry.getValue()));
                }
                throw failure;
            }
        }

        String fullname = taskName + ":" + commandName;

        // skip option
        if (BeeOption.Skip.value.contains(taskName) || BeeOption.Skip.value.contains(fullname)) {
            return null;
        }

        // create task and initialize
        Task task = info.create();

        // execute task
        try (var x = Profiling.of("Task [" + fullname + "]")) {
            return command.invoke(task);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw I.quiet(e);
        } finally {
            if (ui instanceof TaskOperations.ParallelInterface parallel) {
                parallel.finish();
            }
        }
    }

    /**
     * Builds the project definition file (`project.bee`) if it doesn't exist or compiles it if it's
     * outdated. If the project is created, it prompts the user for basic project details.
     *
     * @param definition The file representing the project definition (`project.bee`).
     * @return A list of tasks to be executed initially (e.g., ["prototype:java"] if a new project
     *         was created).
     * @throws Exception If project creation or compilation fails.
     */
    private List<String> buildProjectDefinition(File definition) throws Exception {
        List<String> tasks = new ArrayList();

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
            tasks.add(TaskInfo.computeTaskName(Prototype::java));
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

        return tasks;
    }

    /**
     * The main entry point for the Bee command-line application.
     * Parses arguments into options and tasks, initializes Bee, and executes the tasks.
     *
     * @param tasks Command-line arguments representing tasks and options.
     */
    public static void main(String... tasks) {
        // 1. Measure JVM startup time immediately
        Profiling.measureJVMStartup();

        // 2. Enable Ahead-of-Time (AOT) compilation cache if available
        JEP483.enable(Locator.locate(Bee.class) + ".aot");

        // 3. Default task if none provided
        if (tasks.length == 0) tasks = new String[] {"help", "-p"};

        // 4. Parse command-line arguments into options and remaining tasks
        // Options (like --help, --root) are processed first.
        List<String> washed = BeeOption.parse(tasks);

        // 5. Initialize Bee and execute the remaining tasks
        // Bee constructor handles UI and project setup. execute() runs the build lifecycle.
        // System.exit is called with the result code from execute().
        // Do NOT create 'new Bee()' before BeeOption.parse() as constructor might initialize
        // things prematurely based on default option values.
        System.exit(new Bee().execute(washed));
    }
}