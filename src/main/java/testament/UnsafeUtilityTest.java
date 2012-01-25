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

import org.junit.Test;

/**
 * @version 2010/02/12 19:59:11
 */
public class UnsafeUtilityTest {

    @Test
    public void instantiate() throws Exception {
        assert new Root().clazz == null;
        assert new Parent().clazz == UnsafeUtilityTest.class;
        assert new Child().clazz == UnsafeUtilityTest.class;
    }

    @Test
    public void instantiateFromStaticMethod() throws Exception {
        assert Root.class == Root.speculteParent();
        assert Root.class == Root.speculteChild();
        assert Parent.class == Parent.speculteParent();
        assert Parent.class == Parent.speculteChild();
        assert Child.class == Child.speculteParent();
        assert Child.class == Child.speculteChild();
    }

    @Test
    public void instantiateFromStaticInitializer() throws Exception {
        assert Root.class == Root.fromStatic;
        assert Parent.class == Parent.fromStatic;
        assert Child.class == Child.fromStatic;
    }

    /**
     * @version 2010/02/12 20:00:00
     */
    private static class Root {

        private static Class fromStatic;

        static {
            fromStatic = new Parent().clazz;
        }

        protected Class clazz;

        private static Class speculteParent() {
            return new Parent().clazz;
        }

        private static Class speculteChild() {
            return new Child().clazz;
        }
    }

    /**
     * @version 2010/02/12 20:00:46
     */
    private static class Parent extends Root {

        private static Class fromStatic;

        static {
            fromStatic = new Parent().clazz;
        }

        public Parent() {
            clazz = UnsafeUtility.speculateInstantiator();
        }

        private static Class speculteParent() {
            return new Parent().clazz;
        }

        private static Class speculteChild() {
            return new Child().clazz;
        }
    }

    /**
     * @version 2010/02/12 20:00:46
     */
    private static class Child extends Parent {

        private static Class fromStatic;

        static {
            fromStatic = new Parent().clazz;
        }

        private static Class speculteParent() {
            return new Parent().clazz;
        }

        private static Class speculteChild() {
            return new Child().clazz;
        }
    }
}
