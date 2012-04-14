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

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @version 2012/04/11 14:17:35
 */
@Manageable(lifestyle = Singleton.class)
public class FindMain extends Task {

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
    @Command(description = "Find main class.")
    public String main() {
        analyze();

        return ui.ask("Multiple main classes were detected. Which one do you use?", mains);
    }

    /**
     * <p>
     * Find premain class.
     * </p>
     * 
     * @return A premain class name.
     */
    @Command(description = "Find premain class.")
    public String premain() {
        analyze();

        return ui.ask("Multiple premain classes were detected. Which one do you use?", premains);
    }

    /**
     * <p>
     * Find agentmain class.
     * </p>
     * 
     * @return A agentmain class name.
     */
    @Command(description = "Find agentmain class.")
    public String agentmain() {
        analyze();

        return ui.ask("Multiple agentmain classes were detected. Which one do you use?", agentmains);
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

                for (Path path : I.walk(project.getClasses(), "**.class")) {
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
