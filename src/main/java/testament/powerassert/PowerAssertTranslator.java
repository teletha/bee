/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.powerassert;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import testament.bytecode.Bytecode;
import testament.bytecode.LocalVariable;
import testament.bytecode.Agent.Translator;

/**
 * @version 2012/01/14 22:48:47
 */
class PowerAssertTranslator extends Translator {

    /** The state. */
    private boolean startAssertion = false;

    /** The state. */
    private boolean skipNextJump = false;

    /** The state. */
    private boolean processAssertion = false;

    /** The state. */
    private boolean compare = false;

    /** The context for code log. */
    private Journal journal;

    /**
     * <p>
     * Compute simple class name.
     * </p>
     * 
     * @return
     */
    private String computeClassName(String internalName) {
        int index = internalName.lastIndexOf('$');

        if (index == -1) {
            index = internalName.lastIndexOf('/');
        }
        return index == -1 ? internalName : internalName.substring(index + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);

        if (!startAssertion && opcode == GETSTATIC && name.equals("$assertionsDisabled")) {
            startAssertion = true;
            skipNextJump = true;
            return;
        }

        if (processAssertion) {
            // store current value
            LocalVariable local = copy(Type.getType(desc));

            switch (opcode) {
            case GETFIELD:
                journal.field(name, desc, local, hashCode());
                break;

            case GETSTATIC:
                String className = computeClassName(owner);

                journal.fieldStatic(className, this.className.equals(owner) ? name : className + "." + name, desc, local);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);

        if (processAssertion) {
            switch (opcode) {
            case IFEQ:
                if (!compare) {
                    journal.constant(insn(ICONST_0));
                }
                compare = false;
                journal.condition("==");
                break;

            case IF_ICMPEQ:
            case IF_ACMPEQ:
                journal.condition("==");
                break;

            case IFNE:
                if (!compare) {
                    journal.constant(insn(ICONST_0));
                }
                compare = false;
                journal.condition("!=");
                break;

            case IF_ICMPNE:
            case IF_ACMPNE:
                journal.condition("!=");
                break;

            case IF_ICMPLT:
                journal.condition("<");
                break;

            case IF_ICMPLE:
                journal.condition("<=");
                break;

            case IF_ICMPGT:
                journal.condition(">");
                break;

            case IF_ICMPGE:
                journal.condition(">=");
                break;

            case IFNULL:
                // recode null constant
                journal.constant(insn(ACONST_NULL));

                // recode == expression
                journal.condition("==");
                break;

            case IFNONNULL:
                // recode null constant
                journal.constant(insn(ACONST_NULL));

                // recode != expression
                journal.condition("!=");
                break;
            }
        }

        if (skipNextJump) {
            skipNextJump = false;
            processAssertion = true;

            // create new journal for this assertion
            journal = instantiate(Journal.class, PowerAssertContext.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (processAssertion && opcode == NEW && type.equals("java/lang/AssertionError")) {
            processAssertion = false;

            // replace AssertionError with PowerAssertionError
            super.visitTypeInsn(opcode, Type.getType(PowerAssertionError.class).getInternalName());
            return;
        }

        super.visitTypeInsn(opcode, type);

        if (processAssertion) {
            switch (opcode) {
            case INSTANCEOF:
                journal.instanceOf(computeClassName(type));
                break;

            case ANEWARRAY:
                LocalVariable local = copy(Type.getType(type));

                journal.arrayNew(computeClassName(type), local);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // replace invocation of AssertionError constructor.
        if (startAssertion && opcode == INVOKESPECIAL && owner.equals("java/lang/AssertionError")) {
            load(journal); // load context

            // append parameter for context
            StringBuilder builder = new StringBuilder(desc);
            builder.insert(builder.length() - 2, "L");
            builder.insert(builder.length() - 2, Type.getType(PowerAssertContext.class).getInternalName());
            builder.insert(builder.length() - 2, ";");

            // instantiate PowerAssertError
            super.visitMethodInsn(opcode, Type.getType(PowerAssertionError.class).getInternalName(), name, builder.toString());

            // reset state
            startAssertion = false;
            skipNextJump = false;
            processAssertion = false;
            return;
        }

        super.visitMethodInsn(opcode, owner, name, desc);

        if (processAssertion) {
            Type type = Type.getType(desc);
            boolean constructor = name.charAt(0) == '<';

            // save current value
            LocalVariable local = copy(constructor ? Type.getType(owner) : type.getReturnType());

            switch (opcode) {
            case INVOKESTATIC:
                journal.methodStatic(computeClassName(owner), name, desc, local);
                break;

            case INVOKESPECIAL:
                if (constructor) {
                    journal.constructor(computeClassName(owner), desc, local);
                    break;
                }
                // fall-through for private method call
            default:
                journal.method(name, desc, local);
                break;
            }
        }
    }

    /**
     * @see org.objectweb.asm.MethodVisitor#visitIincInsn(int, int)
     */
    @Override
    public void visitIincInsn(int index, int increment) {
        super.visitIincInsn(index, increment);

        if (processAssertion) {
            journal.increment(hashCode(), index, increment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);

        if (processAssertion) {
            switch (opcode) {
            case NEWARRAY:
                LocalVariable local = copy(Bytecode.OBJECT_TYPE);

                switch (operand) {
                case T_BOOLEAN:
                    journal.arrayNew("boolean", local);
                    break;

                case T_BYTE:
                    journal.arrayNew("byte", local);
                    break;

                case T_CHAR:
                    journal.arrayNew("char", local);
                    break;

                case T_DOUBLE:
                    journal.arrayNew("double", local);
                    break;

                case T_FLOAT:
                    journal.arrayNew("float", local);
                    break;

                case T_INT:
                    journal.arrayNew("int", local);
                    break;

                case T_LONG:
                    journal.arrayNew("long", local);
                    break;

                case T_SHORT:
                    journal.arrayNew("short", local);
                    break;
                }
                break;

            default:
                journal.constant(intInsn(opcode, operand));
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);

        if (processAssertion) {
            switch (opcode) {
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case LCONST_0:
            case LCONST_1:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case DCONST_0:
            case DCONST_1:
                journal.constant(insn(opcode));
                break;

            case IALOAD:
                journal.arrayIndex(copy(Type.INT_TYPE));
                break;

            case LALOAD:
                journal.arrayIndex(copy(Type.LONG_TYPE));
                break;

            case FALOAD:
                journal.arrayIndex(copy(Type.FLOAT_TYPE));
                break;

            case DALOAD:
                journal.arrayIndex(copy(Type.DOUBLE_TYPE));
                break;

            case BALOAD:
                journal.arrayIndex(copy(Type.BOOLEAN_TYPE));
                break;

            case CALOAD:
                journal.arrayIndex(copy(Type.CHAR_TYPE));
                break;

            case AALOAD:
                journal.arrayIndex(copy(Bytecode.OBJECT_TYPE));
                break;

            case IASTORE:
            case LASTORE:
            case DASTORE:
            case FASTORE:
            case BASTORE:
            case CASTORE:
            case AASTORE:
                journal.arrayStore();
                break;

            case ARRAYLENGTH:
                journal.field("length", "I", copy(Type.INT_TYPE), hashCode());
                break;

            case IADD:
            case LADD:
            case FADD:
            case DADD:
                journal.operator("+");
                break;

            case ISUB:
            case LSUB:
            case FSUB:
            case DSUB:
                journal.operator("-");
                break;

            case IMUL:
            case LMUL:
            case FMUL:
            case DMUL:
                journal.operator("*");
                break;

            case IDIV:
            case LDIV:
            case FDIV:
            case DDIV:
                journal.operator("/");
                break;

            case IREM:
            case LREM:
            case FREM:
            case DREM:
                journal.operator("%");
                break;

            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
                journal.negative();
                break;

            case ISHL:
            case LSHL:
                journal.operator("<<");
                break;

            case ISHR:
            case LSHR:
                journal.operator(">>");
                break;

            case IUSHR:
            case LUSHR:
                journal.operator(">>>");
                break;

            case IOR:
            case LOR:
                journal.operator("|");
                break;

            case IXOR:
            case LXOR:
                journal.operator("^");
                break;

            case IAND:
            case LAND:
                journal.operator("&");
                break;

            case LCMP:
            case FCMPL:
            case DCMPL:
                compare = true;
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);

        if (processAssertion) {
            journal.constant(ldc(value));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitVarInsn(int opcode, int index) {
        super.visitVarInsn(opcode, index);

        if (processAssertion) {
            journal.local(hashCode(), index, local(opcode, index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);

        PowerAssertContext.registerLocalVariable(hashCode(), name, desc);
    }

}