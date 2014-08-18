/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import kiss.I;
import bee.api.Command;
import bee.api.Task;

/**
 * @version 2012/05/18 10:48:41
 */
public class FindMain extends Task {

    /** In subclass, you can specify the fully qualified class name for project main class. */
    protected String main;

    /** In subclass, you can specify the fully qualified class name for project premain class. */
    protected String premain;

    /** In subclass, you can specify the fully qualified class name for project agentmain class. */
    protected String agentmain;

    /** The main classes. */
    private List<String> mains = new ArrayList();

    /** The premain classes. */
    private List<String> premains = new ArrayList();

    /** The agentmain classes. */
    private List<String> agentmains = new ArrayList();

    /** The state. */
    private boolean analyzed = false;

    /**
     * <p>
     * Find main class.
     * </p>
     * 
     * @return A main class name.
     */
    @Command("Find main class.")
    public String main() {
        if (main == null) {
            analyze();

            main = ui.ask("Multiple main classes were detected. Which one do you use?", mains);
        }

        ui.talk("Using ", main, " as main class.");

        return main;
    }

    /**
     * <p>
     * Find premain class.
     * </p>
     * 
     * @return A premain class name.
     */
    @Command("Find premain class.")
    public String premain() {
        if (premain == null) {
            analyze();

            premain = ui.ask("Multiple premain classes were detected. Which one do you use?", premains);
        }

        ui.talk("Using ", premain, " as premain class.");

        return premain;
    }

    /**
     * <p>
     * Find agentmain class.
     * </p>
     * 
     * @return A agentmain class name.
     */
    @Command("Find agentmain class.")
    public String agentmain() {
        if (agentmain == null) {
            analyze();

            agentmain = ui.ask("Multiple agentmain classes were detected. Which one do you use?", agentmains);
        }

        ui.talk("Using ", agentmain, " as agentmain class.");

        return agentmain;
    }

    /**
     * <p>
     * Analyze project classes.
     * </p>
     */
    private void analyze() {
        if (!analyzed) {
            try {
                require(Compile.class).source();

                for (Path path : project.getClasses().list("**.class")) {
                    ClassReader reader = new ClassReader(Files.newInputStream(path));
                    reader.accept(new Search(), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }

                // update
                analyzed = true;
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2012/04/11 14:20:51
     */
    private class Search extends ClassVisitor {

        /** The current processing internal class name. */
        private String internalClassName;

        /**

         */
        private Search() {
            super(Opcodes.ASM4);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.internalClassName = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // method name is "main"
            if (name.equals("main")) {
                // method has String[] parameter and returns void
                if (desc.equals("([Ljava/lang/String;)V")) {
                    // method is public and static
                    if ((Opcodes.ACC_STATIC & access) != 0 && (Opcodes.ACC_PUBLIC & access) != 0) {
                        mains.add(internalClassName.replace('/', '.'));
                    }
                }
            }

            // method name is "premain"
            if (name.equals("premain")) {
                // method has String[] parameter and returns void
                if (desc.equals("(Ljava/lang/String;)V") || desc.equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V")) {
                    // method is public and static
                    if ((Opcodes.ACC_STATIC & access) != 0 && (Opcodes.ACC_PUBLIC & access) != 0) {
                        premains.add(internalClassName.replace('/', '.'));
                    }
                }
            }

            // method name is "agentmain"
            if (name.equals("agentmain")) {
                // method has String[] parameter and returns void
                if (desc.equals("(Ljava/lang/String;)V") || desc.equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V")) {
                    // method is public and static
                    if ((Opcodes.ACC_STATIC & access) != 0 && (Opcodes.ACC_PUBLIC & access) != 0) {
                        agentmains.add(internalClassName.replace('/', '.'));
                    }
                }
            }
            return null;
        }
    }
}
