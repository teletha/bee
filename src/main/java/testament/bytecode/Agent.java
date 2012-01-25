/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.bytecode;

import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import kiss.I;
import kiss.model.ClassUtil;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import testament.ReusableRule;
import testament.UnsafeUtility;

/**
 * <p>
 * Provide functionality to transform bytecode.
 * </p>
 * 
 * @version 2012/01/10 19:09:14
 */
public class Agent extends ReusableRule {

    /** The entry point for Attach API. */
    private static Method attach;

    /** The entry point for Attach API. */
    private static Method loadAgent;

    // load Attach API
    static {
        // search attach method
        try {
            Class clazz = UnsafeUtility.getTool("com.sun.tools.attach.VirtualMachine");

            attach = clazz.getMethod("attach", String.class);
            loadAgent = clazz.getMethod("loadAgent", String.class);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /** The redefined classes. */
    private static final Set<String> redefines = new HashSet();

    /** The Instrumentation tool. */
    private volatile static Instrumentation tool;

    /** The actual agent. */
    private final ClassFileTransformer agent;

    /**
     * <p>
     * Create dynamic Agent.
     * </p>
     * 
     * @param translator
     */
    public Agent(Class<? extends Translator> translator) {
        this(new TranslatorTransformer(translator));
    }

    /**
     * <p>
     * Create dynamic Agent.
     * </p>
     * 
     * @param agent
     */
    public Agent(ClassFileTransformer agent) {
        synchronized (Agent.class) {
            if (tool == null) {
                createTool();
            }
        }

        // store agent
        this.agent = agent;

        // register agent
        tool.addTransformer(agent, true);
    }

    /**
     * <p>
     * Force to transform the target class.
     * </p>
     * 
     * @param target
     */
    public void transform(Class target) {
        try {
            redefines.add(target.getName().replace('.', '/'));
            tool.retransformClasses(target);
        } catch (UnmodifiableClassException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterClass() {
        tool.removeTransformer(agent);
    }

    /**
     * <p>
     * Create instrumentation tool.
     * </p>
     */
    private static void createTool() {
        // Build manifest.
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Agent-Class", Agent.class.getName());
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");
        attributes.putValue("Can-Set-Native-Method-Prefix", "true");

        try {
            // Build temporary agent jar.
            Path jar = I.locateTemporary();
            new JarOutputStream(Files.newOutputStream(jar), manifest).close();

            // Load agent dynamically.
            String name = ManagementFactory.getRuntimeMXBean().getName();
            loadAgent.invoke(attach.invoke(null, name.substring(0, name.indexOf('@'))), jar.toString());
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Agent entry point.
     */
    @SuppressWarnings("unused")
    private static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        tool = instrumentation;
    }

    /**
     * @version 2012/01/14 13:09:23
     */
    private static final class TranslatorTransformer implements ClassFileTransformer {

        /** The delegator. */
        private final Class<? extends Translator> translator;

        /**
         * @param translator
         */
        private TranslatorTransformer(Class<? extends Translator> translator) {
            this.translator = translator;
        }

        /**
         * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
         *      java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
         */
        @Override
        public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] bytes) {
            if (!redefines.contains(name)) {
                return bytes;
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassTranslator visitor = new ClassTranslator(writer, name);
            ClassReader reader = new ClassReader(bytes);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            return writer.toByteArray();
        }

        /**
         * @version 2012/01/14 13:16:21
         */
        private class ClassTranslator extends ClassVisitor {

            /** The internal class name. */
            private final String className;

            /**
             * @param arg0
             */
            private ClassTranslator(ClassWriter writer, String className) {
                super(Opcodes.ASM4, writer);

                this.className = className;
            }

            /**
             * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String,
             *      java.lang.String, java.lang.String, java.lang.String[])
             */
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
                LocalVariableSorter sorter = new LocalVariableSorter(access, desc, visitor);
                Translator translator = I.make(TranslatorTransformer.this.translator);
                translator.set(sorter, className, name, Type.getMethodType(desc));

                return translator;
            }
        }
    }

    /**
     * @version 2012/01/14 13:08:33
     */
    public static abstract class Translator extends MethodVisitor {

        /** The internal class name. */
        protected String className;

        /** The internal method name. */
        protected String methodName;

        /** The internal method type. */
        protected Type methodType;

        /**
         * 
         */
        protected Translator() {
            super(Opcodes.ASM4, null);
        }

        /**
         * <p>
         * Lazy set up.
         * </p>
         */
        final void set(LocalVariableSorter visitor, String className, String methodName, Type methodDescriptor) {
            mv = visitor;
            this.className = className;
            this.methodName = methodName;
            this.methodType = methodDescriptor;
        }

        /**
         * Creates a new local variable of the given type.
         * 
         * @param type the type of the local variable to be created.
         * @return the identifier of the newly created local variable.
         */
        protected final LocalVariable newLocal(Type type) {
            return new LocalVariable(type, (LocalVariableSorter) mv);
        }

        /**
         * <p>
         * Create a new insntance and store it into new local variable.
         * </p>
         * 
         * @param api
         * @param instantiator
         * @return
         */
        protected final <S> S instantiate(Class<S> api, Class<? extends S> instantiator) {
            Type type = Type.getType(instantiator);
            mv.visitTypeInsn(NEW, type.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, type.getInternalName(), "<init>", Type.getConstructorDescriptor(ClassUtil.getMiniConstructor(instantiator)));

            LocalVariable local = newLocal(type);
            local.store();

            return createAPI(local, api);
        }

        /**
         * <p>
         * Load the specified object as possible as we can.
         * </p>
         * 
         * @param object
         */
        protected final void load(Object object) {
            if (object == null) {
                mv.visitInsn(ACONST_NULL);
            } else if (object instanceof String) {
                mv.visitLdcInsn(object);
            } else if (object instanceof LocalVariable) {
                ((LocalVariable) object).load();
            } else if (Proxy.isProxyClass(object.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(object);

                if (handler instanceof InterfaceCaller) {
                    ((InterfaceCaller) handler).invoker.load();
                }
            }
        }

        /**
         * <p>
         * Helper method to write below code.
         * </p>
         * 
         * <pre>
         * mv.visitVisitInsn(Opcodes.DUP);
         * 
         * LocalVariable local = newLocal(type);
         * 
         * local.store();
         * </pre>
         * 
         * @param type
         * @return
         */
        protected final LocalVariable copy(Type type) {
            mv.visitInsn(type.getSize() == 1 ? DUP : DUP2);

            LocalVariable local = newLocal(type);
            local.store();

            return local;
        }

        /**
         * <p>
         * Write local variable code.
         * </p>
         * 
         * @param opcode
         * @param index
         * @return
         */
        protected final Bytecode local(int opcode, int index) {
            return new LocalVariable(opcode, index);
        }

        /**
         * <p>
         * Write instruction code.
         * </p>
         * 
         * @param opcode
         * @return
         */
        protected final Bytecode insn(int opcode) {
            return new Instruction(opcode);
        }

        /**
         * <p>
         * Write int value code.
         * </p>
         * 
         * @param opcode
         * @param operand
         * @return
         */
        protected final Bytecode intInsn(int opcode, int operand) {
            return new IntValue(opcode, operand);
        }

        /**
         * <p>
         * Write constant value code.
         * </p>
         * 
         * @param value
         * @return
         */
        protected final Bytecode ldc(Object value) {
            return new Constant(value);
        }

        /**
         * <p>
         * Helper method to write bytecode which wrap the primitive value.
         * </p>
         * 
         * @param type
         */
        protected final void wrap(Type type) {
            Type wrapper = Bytecode.getWrapperType(type);

            if (wrapper != type) {
                mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf", Type.getMethodDescriptor(wrapper, type));
            }
        }

        /**
         * <p>
         * Create API.
         * </p>
         * 
         * @param invoker
         * @param api
         * @return
         */
        protected final <S> S createAPI(LocalVariable invoker, Class<S> api) {
            return (S) Proxy.newProxyInstance(api.getClassLoader(), new Class[] {api}, new InterfaceCaller(invoker));
        }

        /**
         * @version 2012/01/18 1:18:40
         */
        private class InterfaceCaller implements InvocationHandler {

            /** The method invoker. */
            private final LocalVariable invoker;

            /** The invocation type. */
            private final String invocation;

            /**
             * @param invoker
             * @param invocation
             */
            private InterfaceCaller(LocalVariable invoker) {
                this.invoker = invoker;
                this.invocation = invoker == null ? className : invoker.type.getInternalName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (invoker == null) {
                    mv.visitVarInsn(ALOAD, 0); // load this
                } else {
                    invoker.load(); // load insntance
                }

                // build parameter stacks
                Class[] parameters = method.getParameterTypes();

                for (int i = 0; i < parameters.length; i++) {
                    Class parameter = parameters[i];
                    Object value = args[i];

                    if (value instanceof Bytecode) {
                        Bytecode bytecode = (Bytecode) value;
                        bytecode.write(mv, !parameter.isPrimitive());
                    } else if (parameter == int.class || parameter == long.class || parameter == String.class) {
                        mv.visitLdcInsn(value);
                    }
                }
                // call interface method
                mv.visitMethodInsn(INVOKEVIRTUAL, invocation, method.getName(), Type.getMethodDescriptor(method));
                return null;
            }
        }
    }
}
