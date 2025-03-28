/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import bee.Task;
import bee.api.Command;
import bee.api.Project;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import kiss.Variable;

public interface FindMain extends Task<FindMain.Config> {

    /**
     * Find main class.
     * 
     * @return A main class name.
     */
    @Command(value = "Find main class.", defaults = true)
    default Variable<String> main() {
        Config config = config();

        if (config.main == null) {
            config.main = ui().ask("Multiple main classes were detected. Which one do you use?", I.make(Search.class).mains);
        }

        if (config.main == null) {
            ui().info("No main class.");
        } else {
            ui().info("Using ", config.main, " as main class.");
        }

        return Variable.of(config.main);
    }

    /**
     * Find premain class.
     * 
     * @return A premain class name.
     */
    @Command("Find premain class.")
    default Variable<String> premain() {
        Config config = config();

        if (config.premain == null) {
            config.premain = ui().ask("Multiple premain classes were detected. Which one do you use?", I.make(Search.class).premains);
        }

        if (config.premain == null) {
            ui().info("No premain class.");
        } else {
            ui().info("Using ", config.premain, " as premain class.");
        }

        return Variable.of(config.premain);
    }

    /**
     * Find agentmain class.
     * 
     * @return A agentmain class name.
     */
    @Command("Find agentmain class.")
    default Variable<String> agentmain() {
        Config config = config();

        if (config.agentmain == null) {
            config.agentmain = ui().ask("Multiple agentmain classes were detected. Which one do you use?", I.make(Search.class).agentmains);
        }

        if (config.agentmain == null) {
            ui().info("No agentmain class.");
        } else {
            ui().info("Using ", config.agentmain, " as agentmain class.");
        }

        return Variable.of(config.agentmain);
    }

    /**
     * 
     */
    @Managed(value = Singleton.class)
    class Search extends ClassVisitor {

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
            super(Opcodes.ASM9);

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

    /**
     * User configuration.
     */
    public static class Config {

        /** In subclass, you can specify the fully qualified class name for project main class. */
        public String main;

        /**
         * In subclass, you can specify the fully qualified class name for project premain class.
         */
        public String premain;

        /**
         * In subclass, you can specify the fully qualified class name for project agentmain class.
         */
        public String agentmain;
    }
}