/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.rule;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import kiss.WiseRunnable;

/**
 * Unlimited rule book.
 */
public class Rule<T> {

    private final Supplier<T> target;

    /**
     * @param target
     */
    private Rule(Supplier<T> target) {
        this.target = Objects.requireNonNull(target);
    }

    public Rule<T> constraint(Predicate<T> constraint, WiseRunnable recoveryProcess) {
        if (constraint != null && recoveryProcess != null) {
            T value = target.get();

            while (constraint.test(value) == false) {
                recoveryProcess.run();
            }
        }
        return this;
    }

    public static <R> Rule<R> against(Supplier<R> target) {
        return new Rule(target);
    }
}
