/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

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
            System.out.println(message);
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
        public <T> T ask(Class<T> question) {
            return null;
        }
    }
}
