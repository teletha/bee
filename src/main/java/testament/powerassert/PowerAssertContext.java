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

import static org.objectweb.asm.Type.*;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kiss.I;

import org.objectweb.asm.Type;

/**
 * @version 2012/01/24 20:49:58
 */
public class PowerAssertContext implements Journal, PowerAssertRenderer {

    /** The local variable name mapping. */
    private static final Map<Integer, List<String[]>> locals = new ConcurrentHashMap();

    /** The operand stack frame. */
    ArrayDeque<Operand> stack = new ArrayDeque();

    /** The using operand list. */
    ArrayList<Operand> operands = new ArrayList();

    /** The incremetn state. */
    private String nextIncrement;

    /**
     * <p>
     * Register local variable.
     * </p>
     * 
     * @param methodId
     * @param name
     * @param description
     */
    public static void registerLocalVariable(int methodId, String name, String description) {
        List<String[]> local = locals.get(methodId);

        if (local == null) {
            local = new ArrayList();

            locals.put(methodId, local);
        }
        local.add(new String[] {name, description});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrayIndex(Object value) {
        Operand index = stack.pollLast();
        Operand array = stack.pollLast();
        Operand operand = new Variable(array + "[" + index + "]", array.getType().getElementType(), value);

        stack.add(operand);
        operands.add(operand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrayNew(String className, Object value) {
        // remove previous array size constant
        stack.pollLast();

        Operand operand = new NewArray(className, value);
        stack.add(operand);
        operands.add(operand);
    }

    /**
     * @see testament.powerassert.Journal#arrayStore()
     */
    @Override
    public void arrayStore() {
        // remove previous two operand
        Operand value = stack.pollLast(); // value
        Operand index = stack.pollLast(); // index
        NewArray array = (NewArray) stack.peekLast();

        array.add((Integer) index.value, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void constant(Object constant) {
        stack.add(new Constant(constant));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void condition(String contionalExpression) {
        Condition condition = new Condition(contionalExpression, null);
        stack.add(condition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void constructor(String name, String description, Object value) {
        // build method invocation
        StringBuilder invocation = new StringBuilder("()");
        Type[] params = Type.getMethodType(description).getArgumentTypes();
        int size = params.length;

        for (int i = 0; i < size; i++) {
            Type type = params[i];
            Operand operand = stack.pollLast();

            if (type.getSort() == Type.BOOLEAN && operand.value instanceof Integer) {
                // format
                operand = new Constant(Boolean.valueOf(operand.value.toString()));
            }
            invocation.insert(1, operand);

            if (i + 1 != size) {
                invocation.insert(1, ", ");
            }
        }
        invocation.insert(0, name).insert(0, "new ");

        Operand operand = new Operand(invocation.toString(), value);
        stack.add(operand);
        operands.add(operand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void field(String expression, String description, Object variable, int methodId) {
        Operand owner = stack.pollLast();
        boolean qualified = !owner.name.equals("this") || hasLocal(methodId, expression);

        Operand operand = new Variable(qualified ? owner + "." + expression : expression, Type.getType(description), variable);
        stack.add(operand);
        operands.add(operand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fieldStatic(String className, String fieldName, String description, Object variable) {
        Operand operand = new Variable(fieldName, Type.getType(description), variable);
        stack.add(operand);
        operands.add(operand);
    }

    /**
     * @see testament.powerassert.PowerAssert.Increment#increment(int, int)
     */
    @Override
    public void increment(int methodId, int index, int increment) {
        String[] local = locals.get(methodId).get(index);
        Operand latest = stack.peekLast();

        if (latest == null || !latest.toString().equals(local[0])) {
            // pre increment
            switch (increment) {
            case 1:
                nextIncrement = "++";
                break;

            case -1:
                nextIncrement = "--";
                break;
            }
        } else {
            // post increment
            switch (increment) {
            case 1:
                stack.add(new Operand(stack.pollLast() + "++", null));
                break;

            case -1:
                stack.add(new Operand(stack.pollLast() + "--", null));
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void instanceOf(String className) {
        stack.add(new Operand(stack.pollLast() + " instanceof " + className, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void local(int methodId, int index, Object variable) {
        Operand operand;
        String[] local = locals.get(methodId).get(index);
        String name = local[0];

        if (nextIncrement != null) {
            name = nextIncrement.concat(name);

            nextIncrement = null;
        }

        Type type = Type.getType(local[1]);

        switch (type.getSort()) {
        case BOOLEAN: // boolean
            operand = new Operand(local[0], (int) variable == 1);
            break;

        case CHAR: // char
            operand = new Operand(local[0], (char) ((Integer) variable).intValue());
            break;

        default:
            operand = new Operand(local[0], variable);
            break;
        }

        stack.add(new Variable(name, type, operand.value));
        operands.add(operand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void method(String name, String description, Object value) {
        // build method invocation
        Invocation method = new Invocation(name, description, value);

        stack.add(method);
        operands.add(method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void methodStatic(String className, String methodName, String description, Object value) {
        // build method invocation
        Invocation method = new Invocation(new Operand(className, null), methodName, description, value);

        stack.add(method);
        operands.add(method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void negative() {
        stack.add(new Operand("-" + stack.pollLast(), null));
    }

    /**
     * @see testament.powerassert.Journal#operator(java.lang.String)
     */
    @Override
    public void operator(String operator) {
        if (1 < stack.size()) {
            Operand right = stack.pollLast();
            Operand left = stack.pollLast();

            if (operator.equals("==") || operator.equals("!=")) {
                // check operands
                if (right.value instanceof Integer && ((Integer) right.value).intValue() == 0 && left.value instanceof Boolean) {

                    // boolean == 0 or boolean != 0
                    stack.add(left);
                    return;
                }
            }
            stack.add(new Operand(left + " " + operator + " " + right, null));
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("assert ");

        // top level operand must be conditional operand because of assert statement
        Operand top = stack.peekLast();

        // write assertion code
        builder.append(top).append("\n");

        // strip result operand if the top operand is boolean condition clearly
        if (top instanceof Condition) {
            Condition condition = (Condition) top;

            if (condition.right.value instanceof Integer && condition.left.value instanceof Boolean) {
                top = condition.left;
            }
        }

        // collect all variable operands
        List<Operand> variables = new ArrayList();

        for (Operand operand : operands) {
            if (operand.isVariableHolder() && operand != top) {
                variables.add(operand);
            }
        }

        if (variables.size() != 0) {
            Iterator<Operand> iterator = variables.iterator();
            builder.append("┌─────────────────────────────────────────\n");

            if (iterator.hasNext()) {
                render(builder, iterator.next());

                while (iterator.hasNext()) {
                    builder.append("├─────────────────────────────────────────\n");
                    render(builder, iterator.next());
                }
            }
            builder.append("└─────────────────────────────────────────\n");
        }
        return builder.toString();
    }

    /**
     * @see testament.powerassert.PowerAssertRenderer#render(java.lang.Object)
     */
    @Override
    public String render(Object value) {
        if (value instanceof CharSequence) {
            return "\"" + value + "\"";
        }

        if (value instanceof Enum) {
            Enum enumration = (Enum) value;
            return enumration.getDeclaringClass().getSimpleName() + '.' + enumration.name();
        }

        if (value instanceof Character) {
            return "'" + value + "'";
        }

        Class clazz = value.getClass();

        if (clazz == Class.class) {
            return ((Class) value).getSimpleName() + ".class";
        }

        if (clazz.isArray()) {
            switch (clazz.getComponentType().getSimpleName()) {
            case "int":
                return Arrays.toString((int[]) value);

            case "long":
                return Arrays.toString((long[]) value);

            case "float":
                return Arrays.toString((float[]) value);

            case "double":
                return Arrays.toString((double[]) value);

            case "char":
                return Arrays.toString((char[]) value);

            case "boolean":
                return Arrays.toString((boolean[]) value);

            case "short":
                return Arrays.toString((short[]) value);

            case "byte":
                return Arrays.toString((byte[]) value);

            default:
                return Arrays.toString((Object[]) value);
            }
        }
        return value.toString();
    }

    /**
     * <p>
     * Render the specified operand for human.
     * </p>
     * 
     * @param builder
     * @param operand
     */
    private void render(StringBuilder builder, Operand operand) {
        builder.append("│").append(operand);

        Object value = operand.value;

        if (value != null && !isPrimitive(operand.getType())) {
            builder.append("　　　　#")
                    .append(value.getClass().getName())
                    .append("@")
                    .append(Integer.toHexString(System.identityHashCode(value)));
        }
        builder.append("\n");

        if (value == null) {
            builder.append("│　　").append("null").append("\n");
        } else {
            PowerAssertRenderer renderer = I.find(PowerAssertRenderer.class, value.getClass());

            if (renderer == null) {
                renderer = this;
            }

            for (String line : renderer.render(value).split("\r\n|\r|\n")) {
                builder.append("│　　").append(line).append("\n");
            }
        }
    }

    /**
     * <p>
     * Helper method to chech whether the specified method declare the spcified local variable or
     * not.
     * </p>
     * 
     * @param methodId
     * @param name
     * @return
     */
    private static boolean hasLocal(int methodId, String name) {
        for (String[] local : locals.get(methodId)) {
            if (local[0].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Helper method to decide whether the specified type is primitive or not.
     * </p>
     * 
     * @param type
     * @return
     */
    private static boolean isPrimitive(Type type) {
        switch (type.getSort()) {
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case BOOLEAN:
        case CHAR:
        case BYTE:
        case SHORT:
            return true;

        default:
            return false;
        }
    }

    /**
     * @param operand
     * @param hint
     * @return
     */
    private static Operand infer(Operand operand, Operand hint) {
        return infer(operand, hint.getType());
    }

    /**
     * @param operand
     * @param hint
     * @return
     */
    private static Operand infer(Operand operand, Type hint) {
        // Integer value represents various types (int, char and boolean).
        // We have to check the opposite term' type to infer its actual type.
        if (operand instanceof Constant && operand.value instanceof Integer) {
            Integer value = (Integer) operand.value;

            if (hint == Type.CHAR_TYPE) {
                return new Constant((char) value.intValue());
            } else if (hint == Type.BOOLEAN_TYPE) {
                return new Constant(value.intValue() == 1);
            }
        }
        return operand;
    }

    /**
     * <p>
     * Represents constant value.
     * </p>
     * 
     * @version 2012/01/22 15:15:01
     */
    private static class Constant extends Operand {

        /**
         * @param value An actual value.
         */
        private Constant(Object value) {
            super(String.valueOf(value), value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isVariableHolder() {
            return false;
        }

        /**
         * @see testament.powerassert.Operand#toString()
         */
        @Override
        public String toString() {
            if (value == null) {
                return "null";
            }

            if (value instanceof String) {
                return "\"" + value + "\"";
            }

            if (value instanceof Character) {
                return "'" + value + "'";
            }

            if (value instanceof Class) {
                return ((Class) value).getSimpleName() + ".class";
            }

            return super.toString();
        }
    }

    /**
     * <p>
     * Represents variable value.
     * </p>
     * 
     * @version 2012/01/22 14:15:51
     */
    private static class Variable extends Operand {

        /** The variable type for inference. */
        private final Type type;

        /**
         * @param name A variable name.
         * @param type A variable type.
         * @param value An actual value.
         */
        private Variable(String name, Type type, Object value) {
            super(name, value);

            this.type = type;
        }

        /**
         * @see testament.powerassert.Operand#getType()
         */
        @Override
        Type getType() {
            return type;
        }
    }

    /**
     * <p>
     * Represents Condition expression.
     * </p>
     * 
     * @version 2012/01/20 17:43:57
     */
    private class Condition extends Operand {

        /** The left operand to evaluate. */
        private final Operand left;

        /** The right operand to evaluate. */
        private final Operand right;

        /**
         * @param expression A conditional expression.
         * @param value A current value.
         */
        private Condition(String expression, Object value) {
            super(expression, value);

            this.right = stack.pollLast();
            this.left = stack.pollLast();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            // Integer value represents various types (int, char and boolean).
            // We have to check the opposite term' type to infer its actual type.
            if (left.value instanceof Integer && right.value instanceof Boolean) {
                return right.toString();
            }

            if (right.value instanceof Integer && left.value instanceof Boolean) {
                return left.toString();
            }

            return infer(left, right) + " " + name + " " + infer(right, left);
        }
    }

    /**
     * <p>
     * Represents Array initialization expression.
     * </p>
     * 
     * @version 2012/01/19 16:18:02
     */
    private class NewArray extends Operand {

        /** The array type. */
        private final Type type;

        /** The array size. */
        private final int size;

        /** The actual array elements. */
        private final List<Operand> elements = new ArrayList();

        /**
         * @param type An array type.
         * @param value An actual array value.
         */
        private NewArray(String type, Object value) {
            super(type, value);

            this.type = Type.getType(value.getClass().getComponentType());

            // Boolean array is initialized with false values, other type arrays are initialized
            // with null values. Array store operation will be invoked array length times but false
            // value will not be invoked. So we should fill with false values to normalize setup.
            this.size = Array.getLength(value);

            for (int i = 0; i < size; i++) {
                elements.add(new Constant(false));
            }
        }

        /**
         * <p>
         * Add element value.
         * </p>
         * 
         * @param operand
         */
        private void add(int index, Operand operand) {
            elements.set(index, infer(operand, type));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isVariableHolder() {
            return false;
        }

        /**
         * @see testament.powerassert.Operand#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("new ");
            builder.append(name).append("[] {");

            Iterator<Operand> iterator = elements.iterator();

            if (iterator.hasNext()) {
                builder.append(iterator.next());

                while (iterator.hasNext()) {
                    builder.append(", ").append(iterator.next());
                }
            }

            builder.append('}');
            return builder.toString();
        }
    }

    /**
     * <p>
     * Represents method or constructor invocation.
     * </p>
     * 
     * @version 2012/01/22 14:41:13
     */
    private class Invocation extends Operand {

        /** The invoker. */
        private final Operand invoker;

        /** The method name. */
        private final String methodName;

        /** The return type. */
        private final Type returnType;

        /** The paramter types. */
        private final Type[] parameterTypes;

        /** The parameter operands. */
        private final List<Operand> parameters = new ArrayList();

        /**
         * <p>
         * Normal method invocation.
         * </p>
         * 
         * @param name A method name.
         * @param description A method description.
         * @param value A method result.
         */
        private Invocation(String name, String description, Object value) {
            this(null, name, description, value);
        }

        /**
         * <p>
         * Repesents method invocation.
         * </p>
         * 
         * @param invoker A method invoker.
         * @param name A method name.
         * @param description A method description.
         * @param value A method result.
         */
        private Invocation(Operand invoker, String name, String description, Object value) {
            super(name, value);

            Type type = Type.getType(description);

            this.methodName = name;
            this.returnType = type.getReturnType();
            this.parameterTypes = type.getArgumentTypes();

            int size = parameterTypes.length - 1;

            for (int i = size; 0 <= i; i--) {
                parameters.add(0, infer(stack.pollLast(), parameterTypes[i]));
            }
            this.invoker = invoker != null ? invoker : stack.pollLast();
        }

        /**
         * @see testament.powerassert.Operand#getType()
         */
        @Override
        Type getType() {
            return returnType;
        }

        /**
         * @see testament.powerassert.Operand#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            // write invoker
            if (!invoker.toString().equals("this")) {
                builder.append(invoker).append('.');
            }

            builder.append(methodName).append('(');

            for (int i = 0; i < parameters.size(); i++) {
                Operand value = parameters.get(i);

                if (i + 1 != parameterTypes.length) {
                    builder.append(value).append(", ");
                } else {
                    // last parameter processing
                    if (value instanceof NewArray) {
                        NewArray array = (NewArray) value;

                        if (array.size == 0) {
                            // delete last separator ',' unless this method has only varargs
                            if (parameterTypes.length != 1) {
                                builder.delete(builder.length() - 2, builder.length());
                            }
                        } else {
                            // expand array elements
                            Iterator<Operand> iterator = array.elements.iterator();

                            if (iterator.hasNext()) {
                                builder.append(iterator.next());

                                while (iterator.hasNext()) {
                                    builder.append(", ").append(iterator.next());
                                }
                            }
                        }
                    } else {
                        builder.append(value);
                    }
                }
            }
            builder.append(')');

            return builder.toString();
        }
    }
}