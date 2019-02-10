/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.util.List;

import bee.api.Command;

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
        protected void write(String message) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String ask(String question) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T ask(String question, T defaultAnswer) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T ask(String question, List<T> items) {
            return null;
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
        public void startCommand(String name, Command command) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endCommand(String name, Command command) {
        }
    }
}
