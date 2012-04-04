/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import kiss.ClassListener;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.Table;
import kiss.model.ClassUtil;
import bee.compiler.JavaCompiler;
import bee.definition.Project;
import bee.task.Command;
import bee.task.Task;
import bee.util.Stopwatch;

/**
 * @version 2010/04/02 3:44:35
 */
@Manageable(lifestyle = Singleton.class)
public class Bee implements ClassListener<Task> {

    /** The executable file for Java. */
    public static final Path Java;

    /** The executable file for Bee. */
    public static final Path Bee;

    /** The root directory for Java. */
    public static final Path JavaHome;

    // initialization
    static {
        Path bin = null;
        Path java = null;
        Path bee = null;

        // Search Java SDK from path. Don't use java.home system property to avoid JRE.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(File.pathSeparator)) {
                    Path directory = I.locate(value);
                    Path linux = directory.resolve("javac");
                    Path windows = directory.resolve("javac.exe");

                    if (Files.exists(linux)) {
                        bin = directory;
                        java = linux;
                        bee = directory.resolve("bee");

                        break root;
                    } else if (Files.exists(windows)) {
                        bin = directory;
                        java = windows;
                        bee = directory.resolve("bee.bat");

                        break root;
                    }
                }
            }
        }

        if (bin == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        Bee = bee;
        Java = java;
        JavaHome = java.getParent().getParent();

        I.load(ClassUtil.getArchive(Bee.class));
    }

    /** The task repository. */
    private Map<String, TaskInfo> infos = new HashMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(Class<Task> clazz) {
        infos.put(clazz.getSimpleName().toLowerCase(), new TaskInfo(clazz));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Task> clazz) {
        infos.remove(clazz.getSimpleName().toLowerCase());
    }

    /**
     * <p>
     * Create the specified task.
     * </p>
     * 
     * @param taskClass
     * @return
     */
    public <T extends Task> T createTask(Class<T> taskClass) {
        UserInterface ui = UserInterfaceLisfestyle.ui;
        String name = taskClass.getSimpleName().toLowerCase();
        TaskInfo info = infos.get(name);

        if (info == null) {
            throw ui.error("Task [", name, "] is not found.");
        }

        // API definition
        return (T) I.make(info.task);
    }

    /**
     * <p>
     * Execute task by user input.
     * </p>
     * 
     * @param project
     * @param input
     * @param ui
     */
    private void executeTask(Project project, String input, UserInterface ui) {
        // parse command
        if (input == null) {
            return;
        }

        // remove head and tail white space
        input = input.trim();

        if (input.length() == 0) {
            return;
        }

        // search task name
        String taskName;
        int index = input.indexOf(' ');

        if (index == -1) {
            taskName = input;
        } else {
            taskName = input.substring(0, index);
            input = input.substring(index + 1);
        }

        // analyze task name
        String taskGroupName = "";
        String commandName = "";
        index = taskName.indexOf(':');

        if (index == -1) {
            taskGroupName = taskName;
        } else {
            taskGroupName = taskName.substring(0, index);
            commandName = taskName.substring(index + 1);
        }

        // search task
        TaskInfo taskInfo = infos.get(taskGroupName.toLowerCase());

        if (taskInfo == null) {
            ui.error("Task '" + taskName + "' is not found.");
            return;
        }

        if (commandName.length() == 0) {
            commandName = taskInfo.defaults;
        }

        // search command
        Method command = taskInfo.infos.get(commandName.toLowerCase());

        if (command == null) {
            return;
        }

        // create task and initialize
        Task task = I.make(taskInfo.task);

        // execute task
        ui.title("Building " + project.getProduct() + " " + project.getVersion());

        Stopwatch stopwatch = new Stopwatch().start();
        String result = "SUCCESS";

        try {
            command.invoke(task);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                InvocationTargetException exception = (InvocationTargetException) e;

                e = exception.getTargetException();
            }

            ui.error(e);
            result = "FAILURE";
        } finally {
            stopwatch.stop();

            ui.title("BUILD " + result + "        TOTAL TIME: " + stopwatch);
        }
    }

    /**
     * <p>
     * Create project.
     * </p>
     * 
     * @param home
     * @param ui
     * @return
     */
    public final Project createProject(String home, UserInterface ui) {
        return createProject(home == null ? null : Paths.get(home), ui);
    }

    /**
     * <p>
     * Create project.
     * </p>
     * 
     * @param home
     * @param ui
     * @return
     */
    public final Project createProject(Path home, UserInterface ui) {
        // Use current directory if user doesn't specify.
        if (home == null) {
            home = I.locate("");
        }

        // We need absolute path.
        home = home.toAbsolutePath();

        // We need present directory path.
        if (Files.notExists(home)) {
            try {
                Files.createDirectories(home);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        } else if (!Files.isDirectory(home)) {
            home = home.getParent();
        }

        // validate user interface and register it
        if (ui == null) {
            ui = new CommandLineUserInterface();
        }
        UserInterfaceLisfestyle.ui = ui;

        // search Project from the specified file systems
        Path sources = home.resolve("src/project/java");
        Path classes = home.resolve("target/project-classes");
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
        I.load(classes);

        try {
            Project project = (Project) I.make(Class.forName("Project"));
            ProjectLifestyle.project = project;

            return project;
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
     * Launch bee at the current location with commandline user interface.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        Bee bee = I.make(Bee.class);
        bee.executeTask(bee.createProject("", null), "install", I.make(UserInterface.class));
    }

    /**
     * @version 2012/03/20 16:29:25
     */
    private static class TaskInfo {

        /** The task definition. */
        private final Class<Task> task;

        /** The default command name. */
        private String defaults;

        /** The command infos. */
        private Map<String, Method> infos = new HashMap();

        /**
         * @param taskClass
         */
        private TaskInfo(Class<Task> taskClass) {
            this.task = taskClass;

            Table<Method, Annotation> methods = ClassUtil.getAnnotations(taskClass);

            for (Entry<Method, List<Annotation>> info : methods.entrySet()) {
                for (Annotation annotation : info.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Command command = (Command) annotation;
                        Method method = info.getKey();

                        // compute command name
                        String name = method.getName().toLowerCase();

                        // register
                        infos.put(name, method);

                        // check default
                        if (command.defaults()) {
                            defaults = name;
                        }
                    }
                }
            }
        }
    }
}
