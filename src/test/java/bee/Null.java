/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.InputStream;
import java.util.function.Predicate;

/**
 * <p>
 * Null object pattern for tests.
 * </p>
 * 
 * @version 2012/03/29 11:46:51
 */
public class Null {

    /** The null object for {@link UserInterface}. */
    public static final UserInterface UI = new NullUserInterface();

    /**
     * @version 2012/03/29 11:44:27
     */
    private static class NullUserInterface extends UserInterface {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void write(int type, String message) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void write(Throwable error) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected <T> T ask(String question, T defaultAnswer, Predicate<T> validator) {
            return defaultAnswer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Appendable getInterface() {
            return new StringBuilder();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected InputStream getSink() {
            return InputStream.nullInputStream();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startCommand(String name) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endCommand(String name) {
        }
    }
}