/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.extension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;

/**
 * @version 2016/12/13 16:04:26
 */
public class JavaExtensionMethodDefinitionTest {

    @Test
    public void noType() {
        JavaExtensionMethodDefinition definition = method("noType");
        assert definition.signature() == null;
    }

    @Test
    public void noTypeParameterised() {
        JavaExtensionMethodDefinition definition = method("noTypeParameterised");
        assert definition.signature() == null;
    }

    @Test
    public void param1() {
        JavaExtensionMethodDefinition definition = method("param1");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)TP;");
    }

    @Test
    public void param2() {
        JavaExtensionMethodDefinition definition = method("param2");
        assert definition.signature().equals("<P1:Ljava/lang/Object;P2:Ljava/lang/Object;>(TP1;TP2;)TP1;");
    }

    @Test
    public void extendInterface() {
        JavaExtensionMethodDefinition definition = method("extendInterface");
        assert definition.signature().equals("<P::Ljava/lang/Appendable;>(TP;)TP;");
    }

    @Test
    public void extendConcreateAndInterface() {
        JavaExtensionMethodDefinition definition = method("extendConcreateAndInterface");
        assert definition.signature().equals("<P:Ljava/lang/Thread;:Ljava/lang/Readable;>(TP;)TP;");
    }

    @Test
    public void extendVariable() {
        JavaExtensionMethodDefinition definition = method("extendVariable");
        assert definition.signature().equals("<P:TV;V:Ljava/lang/Object;>(TP;)TP;");
    }

    @Test
    public void wildcard() {
        JavaExtensionMethodDefinition definition = method("wildcard");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(Ljava/util/List<*>;)TP;");
    }

    @Test
    public void wildcardExtend() {
        JavaExtensionMethodDefinition definition = method("wildcardExtend");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(Ljava/util/List<+TP;>;)TP;");
    }

    @Test
    public void wildcardExtendSuper() {
        JavaExtensionMethodDefinition definition = method("wildcardExtendSuper");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(Ljava/util/Map<+TP;-Ljava/lang/Appendable;>;)TP;");
    }

    @Test
    public void returnVoid() {
        JavaExtensionMethodDefinition definition = method("returnVoid");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)V");
    }

    @Test
    public void returnTyped() {
        JavaExtensionMethodDefinition definition = method("returnTyped");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)Ljava/util/List<TP;>;");
    }

    @Test
    public void returnArrayTyped() {
        JavaExtensionMethodDefinition definition = method("returnArrayTyped");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)[TP;");
    }

    @Test
    public void returnWildcard() {
        JavaExtensionMethodDefinition definition = method("returnWildcard");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)Ljava/util/List<*>;");
    }

    @Test
    public void returnWildcardExtend() {
        JavaExtensionMethodDefinition definition = method("returnWildcardExtend");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;)Ljava/util/List<+TP;>;");
    }

    @Test
    public void originalVariableUsage1() {
        JavaExtensionMethodDefinition definition = method("originalVariableUsage1");
        assert definition.signature().equals("(TT;)Ljava/lang/Runnable;");
    }

    @Test
    public void originalVariableUsage2() {
        JavaExtensionMethodDefinition definition = method("originalVariableUsage2");
        assert definition.signature().equals("(TT;)Ljava/util/function/Consumer<TR;>;");
    }

    @Test
    public void originalVariableUsageWithAdditionalVariable() {
        JavaExtensionMethodDefinition definition = method("originalVariableUsageWithAdditionalVariable");
        assert definition.signature().equals("<X:Ljava/lang/Object;>(TX;)Ljava/util/function/Function<TR;TX;>;");
    }

    @Test
    public void paramPrimitiveInt() {
        JavaExtensionMethodDefinition definition = method("paramPrimitiveInt");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;I)TP;");
    }

    @Test
    public void paramArray() {
        JavaExtensionMethodDefinition definition = method("paramArray");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;[Ljava/lang/String;)TP;");
    }

    @Test
    public void paramPrimitiveArray() {
        JavaExtensionMethodDefinition definition = method("paramPrimitiveArray");
        assert definition.signature().equals("<P:Ljava/lang/Object;>(TP;[F)TP;");
    }

    @Test
    public void paramVariableArray() {
        JavaExtensionMethodDefinition definition = method("paramVariableArray");
        assert definition.signature().equals("<P:Ljava/lang/Object;>([TP;)TP;");
    }

    @Test
    public void paramVariableVarArg() {
        JavaExtensionMethodDefinition definition = method("paramVariableVarArg");
        assert definition.signature().equals("<P:Ljava/lang/Object;>([TP;)TP;");
    }

    @Test
    public void throwType() {
        JavaExtensionMethodDefinition definition = method("throwType");
        assert definition.signature().equals("<T:Ljava/lang/Exception;>()TT;^TT;^Ljava/lang/IllegalAccessError;");
    }

    /**
     * <p>
     * Helper method to find method.
     * </p>
     * </p>
     * 
     * @param methodName
     * @return
     */
    private JavaExtensionMethodDefinition method(String methodName) {
        for (Method method : API.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class) == false) {
                if (method.getName().equals(methodName)) {
                    return new JavaExtensionMethodDefinition(method);
                }
            }
        }
        throw new AssertionError(methodName + " is not found.");
    }

    /**
     * @version 2016/12/13 12:38:17
     */
    private static interface API {

        String noType(String base, String param);

        String noTypeParameterised(String base, List<String> param);

        <P> P param1(String base, P param);

        <P1, P2> P1 param2(String base, P1 param, P2 param2);

        <P extends Appendable> P extendInterface(String base, P param);

        <P extends Thread & Readable> P extendConcreateAndInterface(String base, P param);

        <P extends V, V> P extendVariable(String base, P param);

        <P> P wildcard(String base, List<?> param);

        <P> P wildcardExtend(String base, List<? extends P> param);

        <P> P wildcardExtendSuper(String base, Map<? extends P, ? super Appendable> param);

        <P> void returnVoid(String base, P param);

        <P> P[] returnArrayTyped(String base, P param);

        <P> List<P> returnTyped(String base, P param);

        <P> List<?> returnWildcard(String base, P param);

        <P> List<? extends P> returnWildcardExtend(String base, P param);

        <P> Runnable originalVariableUsage1(Consumer<P> base, P param);

        <P, Q> Consumer<Q> originalVariableUsage2(Function<P, Q> base, P param);

        <P, Q, X> Function<Q, X> originalVariableUsageWithAdditionalVariable(Function<P, Q> base, X param);

        <P> P paramPrimitiveInt(String base, P param, int num);

        <P> P paramArray(String base, P param, String[] values);

        <P> P paramPrimitiveArray(String base, P param, float[] values);

        <P> P paramVariableArray(String base, P[] values);

        <P> P paramVariableVarArg(String base, P... values);

        <T extends Exception> T throwType(String base) throws T, IllegalAccessError;
    }
}
