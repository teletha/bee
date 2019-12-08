/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bee.Task;
import bee.api.Command;
import bee.api.Project;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

public class FindMain extends Task {

    /** In subclass, you can specify the fully qualified class name for project main class. */
    protected String main;

    /** In subclass, you can specify the fully qualified class name for project premain class. */
    protected String premain;

    /** In subclass, you can specify the fully qualified class name for project agentmain class. */
    protected String agentmain;

    /**
     * <p>
     * Find main class.
     * </p>
     * 
     * @return A main class name.
     */
    @Command(value = "Find main class.", defaults = true)
    public String main() {
        if (main == null) {
            main = ui.ask("Multiple main classes were detected. Which one do you use?", I.make(Search.class).mains);
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
            premain = ui.ask("Multiple premain classes were detected. Which one do you use?", I.make(Search.class).premains);
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
            agentmain = ui.ask("Multiple agentmain classes were detected. Which one do you use?", I.make(Search.class).agentmains);
        }

        ui.talk("Using ", agentmain, " as agentmain class.");

        return agentmain;
    }

    /**
     * 
     */
    @Managed(value = Singleton.class)
    private static class Search extends ClassVisitor {

        /** The main classes. */
        private List<String> mains = new ArrayList();

        /** The premain classes. */
        private List<String> premains = new ArrayList();

        /** The agentmain classes. */
        private List<String> agentmains = new ArrayList();

        /** The current processing internal class name. */
        private String internalClassName;

        /**
        
         */
        private Search() {
            super(Opcodes.ASM7);

            I.make(Project.class).getClasses().walkFile("**.class").to(file -> {
                try {
                    ClassReader reader = new ClassReader(file.newInputStream());
                    reader.accept(this, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            });
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
