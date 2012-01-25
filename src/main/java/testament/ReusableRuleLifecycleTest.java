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

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2011/03/23 8:53:39
 */
public class ReusableRuleLifecycleTest {

    @Rule
    public static final Lifecycle rule = new Lifecycle();

    @Test
    public void notInvoked() throws Exception {
        throw new AssertionError("We must not invoke this test method.");
    }

    @Test
    public void notExecuted() throws Exception {
        throw new AssertionError("We must not invoke this test method.");
    }

    /**
     * @version 2011/03/20 10:09:44
     */
    private static final class Lifecycle extends ReusableRule {

        /**
         * @see testament.ReusableRule#before(java.lang.reflect.Method)
         */
        @Override
        protected void before(Method method) throws Exception {
            // error in before method
            throw new BeforeError();
        }

        /**
         * @see testament.ReusableRule#after(java.lang.reflect.Method)
         */
        @Override
        protected void after(Method method) {
            burkeError(BeforeError.class);
        }
    }

    /**
     * @version 2011/03/20 10:11:10
     */
    private static final class BeforeError extends Error {

        private static final long serialVersionUID = -6146166690359683917L;
    }
}
