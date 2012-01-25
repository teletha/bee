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

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2011/03/20 9:26:12
 */
public class ReusableRuleClassLifecycleTest {

    @Rule
    public static final ClassLifeCycle rule = new ClassLifeCycle();

    @Test
    public void notInvoked() throws Exception {
        throw new AssertionError("We must not invoke this test method.");
    }

    /**
     * @version 2011/03/20 9:26:32
     */
    private static final class ClassLifeCycle extends ReusableRule {

        /**
         * @see testament.ReusableRule#beforeClass()
         */
        @Override
        protected void beforeClass() throws Exception {
            // error in before class
            throw new BeforeClassError();
        }

        /**
         * @see testament.ReusableRule#afterClass()
         */
        @Override
        protected void afterClass() {
            burkeError(BeforeClassError.class);
        }
    }

    /**
     * @version 2011/03/20 10:06:58
     */
    private static class BeforeClassError extends Error {

        private static final long serialVersionUID = -8184462531495172018L;
    }
}
