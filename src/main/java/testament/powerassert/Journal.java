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

/**
 * @version 2012/01/24 20:50:02
 */
interface Journal {

    /**
     * <p>
     * Write array index acess.
     * </p>
     */
    void arrayIndex(Object value);

    /**
     * <p>
     * Write create array.
     * </p>
     */
    void arrayNew(String className, Object value);

    /**
     * <p>
     * Write array store acess.
     * </p>
     */
    void arrayStore();

    /**
     * <p>
     * Write conditional expression.
     * </p>
     * 
     * @param contionalExpression
     */
    void condition(String contionalExpression);

    /**
     * <p>
     * Write constant value.
     * </p>
     * 
     * @param constant
     */
    void constant(Object constant);

    /**
     * <p>
     * Write constructor call.
     * </p>
     * 
     * @param name A constructor name.
     * @param description A constructor parameter size.
     * @param value A constructed value.
     */
    void constructor(String name, String description, Object value);

    /**
     * <p>
     * Write local variable value.
     * </p>
     * 
     * @param methodId A method id.
     * @param index A local variable index.
     * @param variable A value.
     */
    void local(int methodId, int index, Object variable);

    /**
     * <p>
     * Write field access.
     * </p>
     * 
     * @param expression
     * @param description
     * @param variable
     * @param methodId A accessing method id.
     */
    void field(String expression, String description, Object variable, int methodId);

    /**
     * <p>
     * Write static field access.
     * </p>
     * 
     * @param className
     * @param fieldName
     * @param description
     * @param variable
     */
    void fieldStatic(String className, String fieldName, String description, Object variable);

    /**
     * <p>
     * Write increment operation.
     * </p>
     * 
     * @param methodId A method id.
     * @param index A local variable index.
     * @param increment A increment value.
     */
    void increment(int methodId, int index, int increment);

    /**
     * <p>
     * Write instanceof operation.
     * </p>
     * 
     * @param className
     */
    void instanceOf(String className);

    /**
     * <p>
     * Write method call.
     * </p>
     * 
     * @param name A method name.
     * @param description A method parameter size.
     * @param value A returned value.
     */
    void method(String name, String description, Object value);

    /**
     * <p>
     * Write static method call.
     * </p>
     * 
     * @param className A class name.
     * @param methodName A method name.
     * @param description A method parameter size.
     * @param value A returned value.
     */
    void methodStatic(String className, String methodName, String description, Object value);

    /**
     * <p>
     * Write negative value operation.
     * </p>
     */
    void negative();

    /**
     * <p>
     * Write operator.
     * </p>
     * 
     * @param expression
     */
    void operator(String operator);
}