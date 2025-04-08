/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;

import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Comment;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import kiss.Variable;

/**
 * Provides tasks to find special entry point classes within the project:
 * <ul>
 * <li>{@code main}: Classes containing a standard {@code public static void main(String[] args)}
 * method.</li>
 * <li>{@code premain}: Classes containing a Java Agent {@code premain} method.</li>
 * <li>{@code agentmain}: Classes containing a Java Agent {@code agentmain} method (for dynamic
 * attachment).</li>
 * </ul>
 * This task scans the project's compiled classes and allows configuration or user interaction
 * to select the desired entry point if multiple candidates are found or if none are explicitly
 * configured.
 */
public interface FindMain extends Task<FindMain.Config> {

    /**
     * Finds or determines the main class for the project.
     * <p>
     * It follows these steps:
     * <ol>
     * <li>Checks if a main class is already specified in the {@link Config#main} setting.</li>
     * <li>If not configured, it uses the {@link Search} service to find all classes containing a
     * valid {@code public static void main(String[] args)} method.</li>
     * <li>If exactly one main class is found, it's used automatically.</li>
     * <li>If multiple main classes are found, it prompts the user (via {@code ui().ask()}) to
     * select one.</li>
     * <li>If no main class is found or configured, it reports that none exists.</li>
     * <li>The determined main class name (or null if none) is stored back into {@link Config#main}
     * for subsequent uses and returned as a {@link Variable}.</li>
     * </ol>
     *
     * @return A {@link Variable} containing the fully qualified name of the main class, or an empty
     *         Variable if none is found or selected.
     */
    @Command(value = "Find main class for the project.", defaults = true)
    default Variable<String> main() {
        Config config = config();

        if (config.main == null) {
            config.main = ui().ask("Multiple main classes were detected. Which one do you use?", I.make(Search.class).mains);
        }

        if (config.main == null) {
            ui().info("No main class found or configured.");
        } else {
            ui().info("Using ", config.main, " as main class.");
        }

        return Variable.of(config.main);
    }

    /**
     * Finds or determines the premain class for the project (Java Agent).
     * <p>
     * Similar logic to {@link #main()}: checks configuration first, then searches using
     * {@link Search},
     * asks the user if multiple candidates exist, reports the outcome, stores the result in
     * {@link Config#premain}, and returns it as a {@link Variable}.
     * </p>
     * It looks for classes containing a method with the signature
     * {@code public static void premain(String agentArgs)}
     * or
     * {@code public static void premain(String agentArgs, java.lang.instrument.Instrumentation inst)}.
     *
     * @return A {@link Variable} containing the fully qualified name of the premain class, or an
     *         empty Variable if none is found or selected.
     */
    @Command("Find premain class for Java Agent.")
    default Variable<String> premain() {
        Config config = config();

        if (config.premain == null) {
            config.premain = ui().ask("Multiple premain classes were detected. Which one do you use?", I.make(Search.class).premains);
        }

        if (config.premain == null) {
            ui().info("No premain class found or configured.");
        } else {
            ui().info("Using ", config.premain, " as premain class.");
        }

        return Variable.of(config.premain);
    }

    /**
     * Finds or determines the agentmain class for the project (Java Agent for dynamic attach).
     * <p>
     * Similar logic to {@link #main()}: checks configuration first, then searches using
     * {@link Search},
     * asks the user if multiple candidates exist, reports the outcome, stores the result in
     * {@link Config#agentmain}, and returns it as a {@link Variable}.
     * </p>
     * It looks for classes containing a method with the signature
     * {@code public static void agentmain(String agentArgs)}
     * or
     * {@code public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst)}.
     *
     * @return A {@link Variable} containing the fully qualified name of the agentmain class, or an
     *         empty Variable if none is found or selected.
     */
    @Command("Find agentmain class for Java Agent (attach API).")
    default Variable<String> agentmain() {
        Config config = config();

        if (config.agentmain == null) {
            config.agentmain = ui().ask("Multiple agentmain classes were detected. Which one do you use?", I.make(Search.class).agentmains);
        }

        if (config.agentmain == null) {
            ui().info("No agentmain class found or configured.");
        } else {
            ui().info("Using ", config.agentmain, " as agentmain class.");
        }

        return Variable.of(config.agentmain);
    }

    /**
     * Internal singleton service responsible for scanning project classes
     * to find potential main, premain, and agentmain classes.
     * <p>
     * This class is managed by Kiss DI as a singleton. Upon first instantiation,
     * its constructor walks through all compiled class files of the current project,
     * parses them using the Class-File API, and checks methods for the specific
     * signatures and modifiers required for each entry point type. The fully qualified
     * names of the classes containing these methods are stored in lists.
     * </p>
     */
    @Managed(value = Singleton.class)
    class Search {

        /** A list of fully qualified names of classes containing a valid main method. */
        private final List<String> mains = new ArrayList<>();

        /** A list of fully qualified names of classes containing a valid premain method. */
        private final List<String> premains = new ArrayList<>();

        /** A list of fully qualified names of classes containing a valid agentmain method. */
        private final List<String> agentmains = new ArrayList<>();

        /**
         * Private constructor to enforce singleton pattern via Kiss DI.
         * Initializes the service by scanning project classes.
         */
        private Search() {
            TaskOperations.project().getClasses().walkFile("**.class").to(file -> {
                ClassModel model = ClassFile.of().parse(file.bytes());

                for (MethodModel method : model.methods()) {
                    AccessFlags flags = method.flags();
                    if (flags.has(AccessFlag.PUBLIC) && flags.has(AccessFlag.STATIC)) {
                        String name = method.methodName().stringValue();
                        String desc = method.methodTypeSymbol().descriptorString();

                        if (name.equals("main")) {
                            if (desc.equals("([Ljava/lang/String;)V")) {
                                mains.add(model.thisClass().asInternalName().replace('/', '.'));
                            }
                        } else if (name.equals("premain")) {
                            if (desc.equals("(Ljava/lang/String;)V") || desc
                                    .equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V")) {
                                premains.add(model.thisClass().asInternalName().replace('/', '.'));
                            }
                        } else if (name.equals("agentmain")) {
                            if (desc.equals("(Ljava/lang/String;)V") || desc
                                    .equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V")) {
                                agentmains.add(model.thisClass().asInternalName().replace('/', '.'));
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Configuration class for the {@link FindMain} task, allowing users to explicitly
     * specify the fully qualified names of the main, premain, and agentmain classes.
     * If a value is set here, the automatic search might be skipped for that specific entry point.
     */
    public static class Config {

        /**
         * Specifies the fully qualified class name (e.g., {@code com.example.MyApp})
         * to be used as the main entry point for the application.
         */
        @Comment("Specify the fully qualified class name for project main class.")
        public String main;

        /**
         * Specifies the fully qualified class name to be used as the
         * {@code Premain-Class} attribute in the JAR manifest for Java Agents loaded at startup.
         */
        @Comment("Specify the fully qualified class name for project premain class.")
        public String premain;

        /**
         * Specifies the fully qualified class name to be used as the
         * {@code Agent-Class} attribute in the JAR manifest for Java Agents attached to a running
         * JVM.
         */
        @Comment("Specify the fully qualified class name for project agentmain class.")
        public String agentmain;
    }
}