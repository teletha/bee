/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
class BeeOptionTest {

    @BeforeEach
    void clean() {
        for (BeeOption o : BeeOption.options) {
            o.value = o.defaultValue;
        }
    }

    @Test
    void none() {
        List<String> washed = BeeOption.parse("pass");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Help.value() == false;
    }

    @Test
    void singleShort() {
        List<String> washed = BeeOption.parse("pass", "-h");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == false;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void extraShort() {
        List<String> washed = BeeOption.parse("pass", "-?");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == false;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void multipleShorts() {
        List<String> washed = BeeOption.parse("pass", "-h", "-d");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == true;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void sequencialShorts() {
        List<String> washed = BeeOption.parse("pass", "-dh");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == true;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void sequencialShortsInversed() {
        List<String> washed = BeeOption.parse("pass", "-hd");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == true;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void singleLong() {
        List<String> washed = BeeOption.parse("pass", "--help");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == false;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void multipleLongs() {
        List<String> washed = BeeOption.parse("pass", "--help", "--debug");
        assert washed.size() == 1;
        assert washed.get(0) == "pass";
        assert BeeOption.Debug.value() == true;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void parameterizedShort() {
        List<String> washed = BeeOption.parse("task", "-x", "param");
        assert washed.size() == 1;
        assert washed.get(0) == "task";

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 1;
        assert list.get(0).equals("param");
    }

    @Test
    void parameterizedSequencialShorts() {
        List<String> washed = BeeOption.parse("task", "-hx", "param");
        assert washed.size() == 1;
        assert washed.get(0) == "task";
        assert BeeOption.Help.value() == true;

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 1;
        assert list.get(0).equals("param");
    }

    @Test
    @Disabled
    void parameterizedShortWithoutSpace() {
        List<String> washed = BeeOption.parse("task", "-xparam");
        assert washed.size() == 1;
        assert washed.get(0) == "task";
        assert BeeOption.Help.value() == true;

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 1;
        assert list.get(0).equals("param");
    }

    @Test
    void parameterizedLong() {
        List<String> washed = BeeOption.parse("task", "--skip", "param");
        assert washed.size() == 1;
        assert washed.get(0) == "task";

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 1;
        assert list.get(0).equals("param");
    }

    @Test
    void multipleParameterizedLong() {
        List<String> washed = BeeOption.parse("task", "--skip", "param1", "param2", "param3");
        assert washed.size() == 1;
        assert washed.get(0) == "task";

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 3;
        assert list.get(0).equals("param1");
        assert list.get(1).equals("param2");
        assert list.get(2).equals("param3");
    }

    @Test
    void multipleParameterizedLongBeforeOtherOption() {
        List<String> washed = BeeOption.parse("task", "--skip", "param1", "param2", "param3", "-h");
        assert washed.size() == 1;
        assert washed.get(0) == "task";
        assert BeeOption.Help.value() == true;

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 3;
        assert list.get(0).equals("param1");
        assert list.get(1).equals("param2");
        assert list.get(2).equals("param3");
    }

    @Test
    void parameterizedLongEqual() {
        List<String> washed = BeeOption.parse("task", "--skip=param");
        assert washed.size() == 1;
        assert washed.get(0) == "task";

        List<String> list = BeeOption.Skip.value;
        assert list.size() == 1;
        assert list.get(0).equals("param");
    }

    @Test
    void betweenTasks() {
        List<String> washed = BeeOption.parse("task1", "--help", "task2");
        assert washed.size() == 2;
        assert washed.get(0) == "task1";
        assert washed.get(1) == "task2";
        assert BeeOption.Debug.value() == false;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void betweenOptions() {
        List<String> washed = BeeOption.parse("--debug", "task", "--help");
        assert washed.size() == 1;
        assert washed.get(0) == "task";
        assert BeeOption.Debug.value() == true;
        assert BeeOption.Help.value() == true;
    }

    @Test
    void systemProperty() {
        List<String> washed = BeeOption.parse("task", "-Dsetting.to.system.property=anyValue");
        assert washed.size() == 1;
        assert washed.get(0) == "task";
        assert System.getProperty("setting.to.system.property").equals("anyValue");
    }
}
