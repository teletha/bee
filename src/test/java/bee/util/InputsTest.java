/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.util.List;
import java.util.Set;

import javax.lang.model.SourceVersion;

import org.junit.jupiter.api.Test;

import psychopath.File;
import psychopath.Locator;

class InputsTest {

    @Test
    void formatAsSize() {
        assert Inputs.formatAsSize(100).equals("100Bytes");
        assert Inputs.formatAsSize(5 * 1024).equals("5KB");
        assert Inputs.formatAsSize(1536).equals("1.50KB");
        assert Inputs.formatAsSize(1538).equals("1.50KB");
        assert Inputs.formatAsSize(1545).equals("1.51KB");
        assert Inputs.formatAsSize(1555).equals("1.52KB");
    }

    @Test
    void formatAsSizeWithoutUnit() {
        assert Inputs.formatAsSize(100, false).equals("100");
        assert Inputs.formatAsSize(5 * 1024, false).equals("5");
        assert Inputs.formatAsSize(1536, false).equals("1.50");
        assert Inputs.formatAsSize(1538, false).equals("1.50");
        assert Inputs.formatAsSize(1545, false).equals("1.51");
        assert Inputs.formatAsSize(1555, false).equals("1.52");
    }

    @Test
    void hyphenize() {
        assert Inputs.hyphenize("ok").equals("ok");
        assert Inputs.hyphenize("OK").equals("ok");
        assert Inputs.hyphenize("DoSomething").equals("do-something");
        assert Inputs.hyphenize("testXML").equals("test-xml");
        assert Inputs.hyphenize("CI").equals("ci");
    }

    @Test
    void recommend() {
        assert Inputs.recommend("clear", Set.of("clean", "unclear", "crest", "cool", "clover")).equals("clean");
        assert Inputs.recommend("en", Set.of("environment", "env", "enter", "cent", "tend")).equals("env");
    }

    @Test
    void capitalize() {
        assert Inputs.capitalize("bee").equals("Bee");
        assert Inputs.capitalize("Bee").equals("Bee");
        assert Inputs.capitalize("x").equals("X");
    }

    @Test
    void normalize() {
        assert Inputs.normalize(SourceVersion.RELEASE_5).equals("1.5");
        assert Inputs.normalize(SourceVersion.RELEASE_6).equals("1.6");
        assert Inputs.normalize(SourceVersion.RELEASE_7).equals("7");
        assert Inputs.normalize(SourceVersion.RELEASE_8).equals("8");
        assert Inputs.normalize(SourceVersion.RELEASE_17).equals("17");
    }

    @Test
    void template() {
        Sample sample = new Sample();

        assert Inputs.template("Hello {name}!", sample).equals("Hello Bee!");
        assert Inputs.template("{name} is {age} years old.", sample).equals("Bee is 3 years old.");
        assert Inputs.template("no placeholder", sample).equals("no placeholder");
    }

    @Test
    void templates() {
        assert Inputs.templates("A\nB\nC").equals(List.of("A", "B", "C"));
        assert Inputs.templates("{name}\n{age}", new Sample()).equals(List.of("Bee", "3"));
    }

    @Test
    void ref() {
        File file = Locator.temporaryFile().text("  hello  ");

        assert Inputs.ref(file).toString().equals("hello");
        assert Inputs.ref(file).length() == 5;
        assert Inputs.ref(file).charAt(0) == 'h';
    }

    /**
     * The context object for the template test.
     */
    static class Sample {
        public String name = "Bee";

        public int age = 3;
    }
}