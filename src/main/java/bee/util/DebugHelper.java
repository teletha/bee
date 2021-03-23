/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.objectweb.asm.Type;

import kiss.I;
import kiss.WiseTriConsumer;
import kiss.WiseTriFunction;

/**
 * 
 */
public class DebugHelper {

    public static <P> String $(DebugConsumer<P> lambda) {
        return methodNameFromLambda(lambda);
    }

    public static String $(DebugRunnable lambda) {
        return methodNameFromLambda(lambda);
    }

    public static <P, R> String $(DebugFunction<P, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    public static <R> String $(Supplier<R> lambda) {
        return "CC";
    }

    public static <P1, P2> String $(DebugBiConsumer<P1, P2> lambda) {
        return methodNameFromLambda(lambda);
    }

    public static <P1, P2, R> String $(DebugBiFunction<P1, P2, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    public static <P1, P2, P3> String $(DebugTriConsumer<P1, P2, P3> lambda) {
        return methodNameFromLambda(lambda);
    }

    public static <P1, P2, P3, R> String $(DebugTriFunction<P1, P2, P3, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    static String methodNameFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            SerializedLambda serialized = (SerializedLambda) m.invoke(lambda);
            String clazz = serialized.getImplClass().replace('/', '.');
            String method = serialized.getImplMethodName();

            StringJoiner params = new StringJoiner(", ", "(", ")");
            for (Type param : Type.getArgumentTypes(serialized.getImplMethodSignature())) {
                params.add(I.type(param.getClassName()).getSimpleName());
            }
            return clazz + "#" + method + params;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 
     */
    public static interface DebugRunnable extends Runnable, Serializable {
    }

    /**
     * 
     */
    public static interface DebugConsumer<P> extends Consumer<P>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugBiConsumer<P1, P2> extends BiConsumer<P1, P2>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugTriConsumer<P1, P2, P3> extends WiseTriConsumer<P1, P2, P3>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugFunction<P, R> extends Function<P, R>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugBiFunction<P1, P2, R> extends BiFunction<P1, P2, R>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugTriFunction<P1, P2, P3, R> extends WiseTriFunction<P1, P2, P3, R>, Serializable {
    }
}