/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import com.google.common.base.Stopwatch;

import bee.api.Library;
import bee.api.License;
import bee.api.Project;
import bee.api.Scope;
import bee.api.StandardLicense;
import bee.api.Task;
import bee.task.IDESupport;
import bee.task.Prototype;
import bee.util.JarArchiver;
import bee.util.JavaCompiler;
import bee.util.PathPattern;
import bee.util.Paths;
import filer.Filer;
import kiss.I;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * <p>
 * Task based project builder for Java.
 * </p>
 * <p>
 * Bee represents a single project build process.
 * </p>
 * 
 * @version 2017/01/20 14:51:27
 */
public class Bee {

    /** The api project. */
    public static final Project API = new Project() {

        {
            product("com.github.teletha", "bee-api", "0.1");
        }
    };

    /** The build tool project. */
    public static final Project TOOL = new Project() {

        {
            product("com.github.teletha", "bee", "0.1");
        }
    };

    public static final Project Lombok = new Project() {

        {
            product("org.projectlombok", "lombok", "1.16.10");
        }
    };

    /** The project build process is aborted by user. */
    public static final RuntimeException AbortedByUser = new RuntimeException();

    /** The project definition file name. */
    private static final String ProjectFile = "Project";

    private static Instrumentation inst;

    static {
        // Bee requires JDK(tools.jar) surely.
        try {
            inst = ByteBuddyAgent.install();
        } catch (Exception e) {
            throw new Error("Bee reqires JDK(tools.jar), but we can't search Java home correctly.");
        }

        I.load(UserInterfaceLisfestyle.class, true);
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
            directory = Filer.locate("");
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
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            // =====================================
            // build your project
            // =====================================
            ui.talk("Finding your project...");

            // build project definition
            buildProjectDefinition(project.getProjectDefinition());

            // load project related classes in system class loader
            load(project.getClasses());
            load(project.getProjectClasses());

            // create your project
            Class projectClass = Class.forName(ProjectFile);
            inject((Project) projectClass.newInstance());

            // =====================================
            // build project develop environment
            // =====================================
            buildDevelopEnvironment();

            // start project build process
            ui.title("Building " + project.getProduct() + " " + project.getVersion());

            // load project related classes in system class loader
            for (Library library : project.getDependency(Scope.Compile)) {
                load(library.getLocalJar());
            }

            // load new project
            I.load(projectClass, false);

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

            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            ui.title(String.format("Build %s \t %s \t %s", result, format.format(LocalDateTime.now()), stopwatch));
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

            Paths.write(definition, project.toDefinition());
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
            Path projectFile = project.getProjectDefinition();
            compiler.addSourceDirectory(new PathPattern(projectFile.getParent(), projectFile.getFileName().toString()));
            compiler.addClassPath(Filer.locate(Bee.class));
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
        List<IDESupport> supports = I.find(IDESupport.class);

        // search existing environment
        for (IDESupport support : supports) {
            if (support.exist(project)) {
                return;
            }
        }

        // build environemnt
        ui.talk("\r\nProject develop environment is not found.");
        builds.add((Task) ui.ask("Bee supports the following IDEs.", supports));
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
     * Dynamic path or module loading.
     * 
     * @param path
     */
    public static void load(Path path) {
        if (Files.exists(path)) {
            try {
                if (Files.isDirectory(path)) {
                    Path file = Filer.locateTemporary();
                    JarArchiver archiver = new JarArchiver();
                    archiver.add(path);
                    archiver.pack(file);
                    path = file;
                }

                inst.appendToSystemClassLoaderSearch(new JarFile(path.toFile()));
            } catch (IOException e) {
                throw I.quiet(e);
            }
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
}
