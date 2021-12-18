/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;

import kiss.I;
import kiss.WiseTriConsumer;
import kiss.WiseTriFunction;

/**
 * Code writing helper.
 */
public class Code {

    /** The full code text. */
    public final List<String> lines = new ArrayList();

    /** The special code converter. */
    private final Function<String, String> converter;

    /**
     * Code writer for Java.
     * 
     * @return A new code writer.
     */
    public static Code java() {
        return new Code(text -> {
            return text.replaceAll("'", "\"");
        });
    }

    /**
     * Hide constructor.
     * 
     * @param converter
     */
    private Code(Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     * Write your code.
     * 
     * @param text
     * @return
     */
    public Code write(Object... text) {
        if (text == null || text.length == 0) {
            lines.add("");
        } else {
            StringBuilder builder = new StringBuilder();
            for (Object o : text) {
                builder.append(String.valueOf(o));
            }
            lines.add(converter.apply(builder.toString()));
        }
        return this;
    }

    /**
     * Return the full code.
     * 
     * @return
     */
    public String text() {
        return lines.stream().collect(Collectors.joining("\n"));
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P> Signature call(SignaturedConsumer<P> lambda, P param) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P1, P2> Signature call(SignaturedBiConsumer<P1, P2> lambda, P1 param1, P2 param2) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P1, P2, P3> Signature call(SignaturedTriConsumer<P1, P2, P3> lambda, P1 param1, P2 param2, P3 param3) {
        return methodNameFromLambda(lambda);
    }

    /**
     * @param lambda
     * @return
     */
    static Signature methodNameFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            SerializedLambda serialized = (SerializedLambda) m.invoke(lambda);
            String clazz = serialized.getImplClass().replace('/', '.');
            String method = serialized.getImplMethodName();
            List<Class> params = new ArrayList();
            for (Type param : Type.getArgumentTypes(serialized.getImplMethodSignature())) {
                params.add(I.type(param.getClassName()));
            }
            return new Signature(I.type(clazz), method, params);
        } catch (ReflectiveOperationException e) {
            throw I.quiet(e);
        }
    }

    /**
     * 
     */
    public static class Signature {

        /** The declared class. */
        public final Class clazz;

        /** The method name. */
        public final String method;

        /** The parameter types. */
        public final List<Class> params;

        /**
         * @param clazz
         * @param method
         * @param params
         */
        private Signature(Class clazz, String method, List<Class> params) {
            this.clazz = clazz;
            this.method = method;
            this.params = params;
        }
    }

    /**
     * 
     */
    public static interface SignaturedRunnable extends Runnable, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedConsumer<P> extends Consumer<P>, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedBiConsumer<P1, P2> extends BiConsumer<P1, P2>, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedTriConsumer<P1, P2, P3> extends WiseTriConsumer<P1, P2, P3>, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedFunction<P, R> extends Function<P, R>, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedBiFunction<P1, P2, R> extends BiFunction<P1, P2, R>, Serializable {
    }

    /**
     * 
     */
    public static interface SignaturedTriFunction<P1, P2, P3, R> extends WiseTriFunction<P1, P2, P3, R>, Serializable {
    }
}
