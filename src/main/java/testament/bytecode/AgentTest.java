/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.bytecode;


import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.junit.Rule;
import org.junit.Test;

/**
 * @version 2012/01/10 19:26:11
 */
public class AgentTest {

    private static boolean called = false;

    @Rule
    public static final Agent agent = new Agent(new Transformer());

    @Test
    public void agentable() throws Exception {
        // load new class
        new Runnable() {

            @Override
            public void run() {
            }
        }.run();

        assert called;
    }

    /**
     * @version 2012/01/02 11:17:34
     */
    private static final class Transformer implements ClassFileTransformer {

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            called = true;
            return classfileBuffer;
        }
    }
}
