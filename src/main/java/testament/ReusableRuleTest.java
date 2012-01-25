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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2010/11/19 17:41:56
 */
public class ReusableRuleTest {

    @Rule
    public static final TopRule rule = new TopRule();

    @Rule
    public static final SkipRule skip = new SkipRule();

    /** The counter of method invocation. */
    private static int counter = 0;

    @Rule
    public static final LifecycleMethodValidator checker = new LifecycleMethodValidator();

    /** The container for lifecycle methods. */
    private static List<String> invocations = new ArrayList();

    @BeforeClass
    public static void beforeClass() {
        assert 0 == invocations.size();
    }

    @AfterClass
    public static void afterClass() {
        assert 2 == invocations.size();
    }

    @Before
    public void before() {
        counter++;
    }

    @Test
    public void invokeSubRules1() throws Exception {
        assert rule.beforeClassInvoked;
        assert 1 == TopRule.sub1.beforeClassInvoked;
        assert 1 == rule.sub2.beforeClassInvoked;
        assert counter == TopRule.sub1.counter;
        assert counter == rule.sub2.counter;
    }

    @Test
    public void invokeSubRules2() throws Exception {
        assert rule.beforeClassInvoked;
        assert 1 == TopRule.sub1.beforeClassInvoked;
        assert 1 == rule.sub2.beforeClassInvoked;
        assert counter == TopRule.sub1.counter;
        assert counter == rule.sub2.counter;
    }

    @Test
    public void dontInvoke() throws Exception {
        throw new AssertionError("don't be invoked");
    }

    @Test
    public void doesntInvoke() throws Exception {
        throw new AssertionError("don't be invoked");
    }

    /**
     * @version 2010/11/19 17:41:52
     */
    private static class TopRule extends ReusableRule {

        // static sub rule
        @Rule
        public static final SubRule sub1 = new SubRule();

        // non-static sub rule
        @Rule
        public final SubRule sub2 = new SubRule();

        private boolean beforeClassInvoked = false;

        /**
         * @see testament.ReusableRule#beforeClass()
         */
        @Override
        protected void beforeClass() throws Exception {
            beforeClassInvoked = true;
        }
    }

    /**
     * @version 2010/10/07 15:55:57
     */
    private static class SubRule extends ReusableRule {

        private int beforeClassInvoked = 0;

        private int counter = 0;

        /**
         * @see testament.ReusableRule#beforeClass()
         */
        @Override
        protected void beforeClass() throws Exception {
            beforeClassInvoked++;
            assert ReusableRuleTest.class == testcase;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void before(Method method) throws Exception {
            counter++;
            assert method.getName().contains("nvoke");
        }
    }

    /**
     * @version 2010/10/11 0:59:19
     */
    private static class LifecycleMethodValidator extends ReusableRule {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void beforeClass() throws Exception {
            invocations.add("beforeClass from Sub");
            assert 1 == invocations.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void afterClass() {
            assert 1 == invocations.size();
            invocations.add("afterClass from Sub");
        }
    }

    /**
     * @version 2011/01/12 14:13:24
     */
    private static class SkipRule extends ReusableRule {

        @Rule
        @SuppressWarnings("unused")
        public static final SkipInSubRule skip = new SkipInSubRule();

        /**
         * @see testament.ReusableRule#skip(java.lang.reflect.Method)
         */
        @Override
        protected boolean skip(Method method) {
            return method.getName().contains("dont");
        }
    }

    /**
     * @version 2011/01/12 14:13:24
     */
    private static class SkipInSubRule extends ReusableRule {

        /**
         * @see testament.ReusableRule#skip(java.lang.reflect.Method)
         */
        @Override
        protected boolean skip(Method method) {
            return method.getName().contains("doesnt");
        }
    }
}
