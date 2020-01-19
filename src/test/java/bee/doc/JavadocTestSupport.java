/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import kiss.I;
import kiss.Variable;
import kiss.XML;

class JavadocTestSupport {

    private static final TestableJavadoc doc = new TestableJavadoc();

    static {
        doc.sources("src/test/java").build();
    }

    protected final MethodInfo currentMethod() {
        StackFrame frame = caller();

        return doc.findByClassName(frame.getClassName())
                .exact()
                .findByMethodSignature(frame.getMethodName(), frame.getMethodType().parameterArray())
                .exact();
    }

    protected final MethodInfo method(String name) {
        StackFrame frame = caller();

        return doc.findByClassName(frame.getClassName()).exact().findByMethodSignature(name).exact();
    }

    /**
     * Retrieve the caller info.
     * 
     * @return
     */
    private final StackFrame caller() {
        return StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 2).walk(s -> s.skip(2).limit(1).findFirst().get());
    }

    /**
     * Test xml equality.
     * 
     * @param actual
     * @param expected
     * @return
     */
    protected final boolean sameXML(Variable<XML> actual, String expected) {
        return sameXML(actual.exact(), expected);
    }

    /**
     * Test xml equality.
     * 
     * @param actual
     * @param expected
     * @return
     */
    protected final boolean sameXML(XML actual, String expected) {
        return sameXML(actual, I.xml(expected.replace('\'', '"')));
    }

    /**
     * Test xml equality.
     * 
     * @param actual
     * @param expected
     * @return
     */
    protected final boolean sameXML(XML actual, XML expected) {
        XML root = I.xml("<r/>").append(actual).children();

        return sameXML(root, root.to(), expected, expected.to());
    }

    /**
     * Test xml equality.
     * 
     * @param actual
     * @param expected
     * @return
     */
    private boolean sameXML(XML actualXML, Node actual, XML expectedXML, Node expected) {
        // base
        assert actual != null;
        assert expected != null;
        assert Objects.equals(actual.getLocalName(), expected.getLocalName()) : error(actualXML, expectedXML);
        assert actual.getNodeType() == expected.getNodeType() : error(actualXML, expectedXML);
        assert actual.getTextContent().trim().equals(expected.getTextContent().trim()) : error(actualXML, expectedXML);

        // attributes
        NamedNodeMap actualAttrs = actual.getAttributes();
        NamedNodeMap expectedAttrs = expected.getAttributes();
        if (actualAttrs != null && expectedAttrs != null) {
            assert actualAttrs.getLength() == expectedAttrs.getLength() : error(actualXML, expectedXML);

            for (int i = 0; i < actualAttrs.getLength(); i++) {
                Attr attr = (Attr) actualAttrs.item(i);
                Attr pair = (Attr) expectedAttrs.getNamedItem(attr.getName());
                assert pair != null : "Expected element has no attribute [" + attr.getName() + "] in " + expectedXML;
                assert attr.getName().equals(pair.getName()) : error(actualXML, expectedXML);
                assert attr.getValue().equals(pair.getValue()) : error(actualXML, expectedXML);
            }
            for (int i = 0; i < expectedAttrs.getLength(); i++) {
                Attr attr = (Attr) expectedAttrs.item(i);
                Attr pair = (Attr) actualAttrs.getNamedItem(attr.getName());
                assert pair != null : "Actual element has no attribute [" + attr.getName() + "] in " + actualXML;
                assert attr.getName().equals(pair.getName()) : error(actualXML, expectedXML);
                assert attr.getValue().equals(pair.getValue()) : error(actualXML, expectedXML);
            }
        }

        // children
        NodeList actualChildren = actual.getChildNodes();
        NodeList expectedChildren = expected.getChildNodes();
        assert actualChildren.getLength() == expectedChildren.getLength() : error(actualXML, expectedXML);
        for (int i = 0; i < actualChildren.getLength(); i++) {
            assert sameXML(actualXML, actualChildren.item(i), expectedXML, expectedChildren.item(i));
        }

        // next
        Node actualNext = actual.getNextSibling();
        Node expectedNext = expected.getNextSibling();

        if (actualNext == null) {
            assert actualNext == expectedNext : error(actualXML, expectedXML);
        } else if (expectedNext != null) {
            assert sameXML(actualXML, actualNext, expectedXML, expectedNext);
        }
        return true;
    }

    /**
     * @param actualXML
     * @param expectedXML
     * @return
     */
    private String error(XML actualXML, XML expectedXML) {
        return "\r\n=============== ACTUAL ===============\r\n" + actualXML + "\r\n\r\n=============== EXPECTED ===============\r\n" + expectedXML + "\r\n";
    }

    /**
     * Provide only null.
     */
    protected static class NullProvider implements ArgumentsProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext arg0) throws Exception {
            return Stream.of(Arguments.of(new Object[] {null, null, null, null, null, null}));
        }
    }

    /**
     * 
     */
    private static class TestableJavadoc extends DocTool<TestableJavadoc> {

        private final List<ClassInfo> infos = new ArrayList();

        private Set<String> internals;

        /**
         * {@inheritDoc}
         */
        @Override
        protected void initialize() {
            internals = findSourcePackages();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(TypeElement root) {
            infos.add(new ClassInfo(root, new TypeResolver(null, internals, root)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(PackageElement root) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(ModuleElement root) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void complete() {
        }

        /**
         * @param className
         */
        private Variable<ClassInfo> findByClassName(String className) {
            for (ClassInfo info : infos) {
                if ((info.packageName + "." + info.name).equals(className)) {
                    return Variable.of(info);
                }
            }
            return Variable.empty();
        }
    }
}
