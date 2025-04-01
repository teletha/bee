/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import org.junit.jupiter.api.Test;

import antibug.powerassert.PowerAssertOff;
import bee.Task;
import bee.TaskInfo;
import bee.AbstractTaskTest;
import bee.api.Command;
import kiss.I;

class TaskCacheTest extends AbstractTaskTest {

    static {
        I.load(TaskCacheTest.class);
    }

    private static int countNoValue;

    @Test
    void noValue() {
        NoValue task = TaskInfo.find(NoValue.class);
        assert countNoValue == 0;
        task.run();
        assert countNoValue == 1;
        task.run();
        assert countNoValue == 1;
    }

    /**
     * Task returns no value.
     */
    protected interface NoValue extends Task {

        @Command("Test")
        default void run() {
            countNoValue++;
        }
    }

    private static int countValue;

    @Test
    void valued() {
        Value task = TaskInfo.find(Value.class);
        assert countValue == 0;
        assert task.run().equals("1");
        assert countValue == 1;
        assert task.run().equals("1");
        assert countValue == 1;
    }

    /**
     * Task returns some value.
     */
    protected interface Value extends Task {

        @Command("Test")
        default String run() {
            countValue++;
            return String.valueOf(countValue);
        }
    }

    private static int countPrimitiveInt;

    @Test
    void primitiveInt() {
        Int task = TaskInfo.find(Int.class);
        assert countPrimitiveInt == 0;
        assert task.run() == 1;
        assert countPrimitiveInt == 1;
        assert task.run() == 1;
        assert countPrimitiveInt == 1;
    }

    /**
     * Task returns primitive value.
     */
    protected interface Int extends Task {

        @Command("Test")
        default int run() {
            return ++countPrimitiveInt;
        }
    }

    private static int countPrimitiveLong;

    @Test
    void primitiveLong() {
        Long task = TaskInfo.find(Long.class);
        assert countPrimitiveLong == 0;
        assert task.run() == 1;
        assert countPrimitiveLong == 1;
        assert task.run() == 1;
        assert countPrimitiveLong == 1;
    }

    /**
     * Task returns primitive value.
     */
    protected interface Long extends Task {

        @Command("Test")
        default long run() {
            return ++countPrimitiveLong;
        }
    }

    private static int countPrimitiveFloat;

    @Test
    void primitiveFloat() {
        Float task = TaskInfo.find(Float.class);
        assert countPrimitiveFloat == 0;
        assert task.run() == 1;
        assert countPrimitiveFloat == 1;
        assert task.run() == 1;
        assert countPrimitiveFloat == 1;
    }

    /**
     * Task returns primitive value.
     */
    protected interface Float extends Task {

        @Command("Test")
        default float run() {
            return ++countPrimitiveFloat;
        }
    }

    private static int countPRimitiveDouble;

    @Test
    void primitiveDouble() {
        Double task = TaskInfo.find(Double.class);
        assert countPRimitiveDouble == 0;
        assert task.run() == 1;
        assert countPRimitiveDouble == 1;
        assert task.run() == 1;
        assert countPRimitiveDouble == 1;
    }

    /**
     * Task returns primitive value.
     */
    protected interface Double extends Task {

        @Command("Test")
        default double run() {
            return ++countPRimitiveDouble;
        }
    }

    private static int countPrimitiveBoolean;

    @Test
    void primitiveBoolean() {
        Boolean task = TaskInfo.find(Boolean.class);
        assert countPrimitiveBoolean == 0;
        assert task.run();
        assert countPrimitiveBoolean == 1;
        assert task.run();
        assert countPrimitiveBoolean == 1;
    }

    /**
     * Task returns primitive value.
     */
    protected interface Boolean extends Task {

        @Command("Test")
        default boolean run() {
            countPrimitiveBoolean++;
            return true;
        }
    }

    private static int countRequire;

    @Test
    @PowerAssertOff
    void require() {
        Req task = TaskInfo.find(Req.class);
        assert countRequire == 0;
        task.run();
        assert countRequire == 1;
        task.run();
        assert countRequire == 1;
    }

    /**
     * Task returns no value.
     */
    protected interface Req extends Task {

        @Command("Test")
        default void run() {
            require(ReqCaller::run);
            require(ReqCaller::run);
            require(ReqCaller::run);
            require(ReqCaller::run);
        }
    }

    /**
     * Task returns no value.
     */
    public interface ReqCaller extends Task {

        @Command("Test")
        default void run() {
            countRequire++;
        }
    }
}