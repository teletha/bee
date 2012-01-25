/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.model.ClassUtil;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>
 * {@link ReusableRule} is replacement of the old-school super testcase class for JUnit. User can
 * override methods to intercept test method invocation.
 * </p>
 * <ul>
 * <li>{@link ReusableRule#beforeClass()}</li>
 * <li>{@link ReusableRule#before(Method)}</li>
 * <li>{@link ReusableRule#after(Method)}</li>
 * <li>{@link ReusableRule#afterClass()}</li>
 * </ul>
 * <p>
 * Testcase must define {@link ReusableRule} as static field because JUnit instantiates testcase
 * class each test method invocation. The example is following:
 * </p>
 * 
 * <pre>
 * public class SomeTest {
 * 
 *     &#064;Rule
 *     public static YourReusableRule rule = new YourReusable();
 * }
 * </pre>
 * <p>
 * The implementation of {@link ReusableRule} can contains sub rules like the following:
 * </p>
 * 
 * <pre>
 * public class YourRule extends ReusableRule {
 * 
 *     &#064;Rule
 *     public static SubRule sub = new SubRule();
 * }
 * </pre>
 * 
 * @version 2010/10/07 21:37:38
 */
public abstract class ReusableRule implements TestRule {

    /** The testcase class. */
    protected final Class testcase = getCaller();

    /** The root directory of testcases. */
    protected final Path testcaseRoot = ClassUtil.getArchive(testcase);

    /** The parent directory of testcase class. */
    protected final Path testcaseDirectory = testcaseRoot.resolve(testcase.getPackage().getName().replace('.', '/'));

    /** The sub rules. */
    private List<Field> rules = new ArrayList();

    /** The nunber of test methods. */
    private int tests = 0;

    /** The nunber of executed test methods. */
    private int executed = 0;

    /** The current trapped error. */
    private Throwable error;

    /** The shutdown hook to invoke afterClass method when only selected test method is executed. */
    private final AfterClassInvoker invoker = new AfterClassInvoker();

    /**
     * Subclass only can instantiate.
     */
    protected ReusableRule() {
        for (Field field : getClass().getFields()) {
            if (field.isAnnotationPresent(Rule.class) && Modifier.isPublic(field.getModifiers())) {
                if (TestRule.class.isAssignableFrom(field.getType())) {
                    rules.add(field);
                }
            }
        }

        for (Method method : testcase.getMethods()) {
            if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Ignore.class)) {
                tests++;
            }
        }
    }

    /**
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement,
     *      org.junit.runner.Description)
     */
    @Override
    public final Statement apply(final Statement base, final Description description) {
        // reset previous error
        error = null;

        return new Statement() {

            /**
             * @see org.junit.runners.model.Statement#evaluate()
             */
            public void evaluate() throws Throwable {
                try {
                    Method method = description.getTestClass().getMethod(description.getMethodName());

                    // call before class
                    if (executed++ == 0) {
                        // register
                        Runtime.getRuntime().addShutdownHook(invoker);

                        // invoke beforeClass
                        beforeClass();
                    }

                    if (!skip(method)) {
                        try {
                            // invoke before
                            before(method);

                            // make chain of method rules
                            Statement statement = base;

                            for (Field rule : rules) {
                                statement = ((TestRule) rule.get(ReusableRule.this)).apply(statement, description);
                            }

                            // invoke test method
                            statement.evaluate();
                        } catch (Throwable e) {
                            catchError(e);
                        } finally {
                            // invoke after
                            after(method);
                        }
                    }
                } catch (Throwable e) {
                    catchError(e);
                } finally {
                    // call after class
                    if (executed == tests) {
                        // unregister
                        Runtime.getRuntime().removeShutdownHook(invoker);

                        // invoke afterClass
                        try {
                            afterClass();
                        } catch (Throwable e) {
                            catchError(e);
                        }
                    }

                    if (error != null) {
                        error = validateError(error);

                        if (error != null) {
                            throw error;
                        }
                    }
                }
            }

        };
    }

    /**
     * <p>
     * This method is invoked once before all test methods.
     * </p>
     * 
     * @throws Exception Test will be stop.
     */
    protected void beforeClass() throws Exception {
    }

    /**
     * <p>
     * This method is invoked every before all test methods.
     * </p>
     * 
     * @param method A current processing test method.
     * @throws Exception Test will be stop.
     */
    protected void before(Method method) throws Exception {
    }

    /**
     * <p>
     * Decide whether the specified method is invoked or not.
     * </p>
     * 
     * @param method A target test method.
     * @return A result.
     */
    protected boolean skip(Method method) {
        return false;
    }

    /**
     * <p>
     * This method is invoked every after all test methods.
     * </p>
     * 
     * @param method A current processing test method.
     */
    protected void after(Method method) {
    }

    /**
     * <p>
     * This method is invoked once after all test methods.
     * </p>
     */
    protected void afterClass() {
    }

    /**
     * <p>
     * Validate test error. You can ignore error by returning <code>null</code>.
     * </p>
     * 
     * @param throwable A curret test error.
     * @return A validated error.
     */
    protected Throwable validateError(Throwable throwable) {
        return throwable;
    }

    /**
     * <p>
     * Burke the current trapped error unconditionaly.
     * </p>
     */
    protected final void burkeError() {
        burkeError(null);
    }

    /**
     * <p>
     * Burke the current trapped error if its type is the specified {@link Throwable}.
     * </p>
     * 
     * @param condition The conditional type.
     */
    protected final void burkeError(Class<? extends Throwable> condition) {
        if (condition == null) {
            condition = Throwable.class;
        }

        if (condition.isInstance(error)) {
            error = null;
        }
    }

    /**
     * <p>
     * Helper method to throw error user-friendly.
     * </p>
     * 
     * @param throwable A current caught error.
     */
    protected final void catchError(Throwable throwable) {
        if (error == null) {
            error = throwable;
        } else if (error != throwable) {
            error.addSuppressed(throwable);
        }
    }

    /**
     * <p>
     * Search caller testcase class.
     * </p>
     * 
     * @return A testcase class.
     */
    private static final Class getCaller() {
        // caller
        Exception e = new Exception();
        StackTraceElement[] elements = e.getStackTrace();

        for (StackTraceElement element : elements) {
            String name = element.getClassName();

            if (name.endsWith("Test")) {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException classNotFoundException) {
                    // If this exception will be thrown, it is bug of this program. So we must
                    // rethrow the wrapped error in here.
                    throw new Error(classNotFoundException);
                }
            }
        }

        // suitable test class is not found
        return ReusableRule.class;
    }

    /**
     * @version 2010/01/08 2:11:08
     */
    private final class AfterClassInvoker extends Thread {

        /**
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            afterClass();
        }
    }

}
