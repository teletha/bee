/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.lang.Character.UnicodeBlock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import kiss.I;

public class Ensure<T> implements Predicate<T> {

    public static final Ensure<Number> Num = new Ensure(Number.class);

    public static final Ensure<CharSequence> Text = new Ensure(CharSequence.class);

    public static final Ensure<CharSequence> Alphanumeric = Text.accept(Character::isAlphabetic, Character::isDigit);

    /** The target type. */
    private final Class type;

    /** The condition manager. */
    private final Predicate<T>[] conditions;

    private final String[] messages;

    /**
     * Create {@link Ensure}.
     * 
     * @param type
     */
    public Ensure(Class<T> type) {
        this(type, null, Objects::nonNull, null, "Inputed value is null.");
    }

    /**
     * Create {@link Ensure}.
     * 
     * @param type
     * @param conditions
     */
    private Ensure(Class<T> type, Predicate<T>[] conditions, Predicate<T> condition, String[] messages, String message) {
        this.type = type;
        this.conditions = I.array(conditions, condition);
        this.messages = I.array(messages, message);
    }

    public Ensure<T> define(Predicate<T> condition, String message) {
        return define(type, condition, message);
    }

    private <X> Ensure<T> define(Class<X> type, Predicate<X> condition, String mesage) {
        if (type.isAssignableFrom(this.type)) {
            return new Ensure(type, conditions, condition, messages, mesage);
        } else {
            return this;
        }
    }

    public Ensure<T> separator(String separators) {
        return define(CharSequence.class, v -> {
            return true;
        }, "");
    }

    public Ensure<T> length(int min, int max) {
        return define(CharSequence.class, v -> {
            int length = v.toString().codePointCount(0, v.length());
            return min <= length && length <= max;
        }, "The character length of [{.}] must be less than " + min + ".");
    }

    public Ensure<T> range(int min, int max) {
        return define(Number.class, v -> {
            return min <= v.intValue() && v.intValue() <= max;
        }, "");
    }

    public Ensure<T> accept(IntPredicate... types) {
        IntPredicate type = Arrays.stream(types).reduce(IntPredicate::or).get();

        return define(CharSequence.class, v -> v.codePoints().allMatch(type), "Invalid letter type.");
    }

    public Ensure<T> accept(UnicodeBlock... blocks) {
        Set<UnicodeBlock> set = Set.of(blocks);

        return define(CharSequence.class, v -> v.codePoints().mapToObj(UnicodeBlock::of).allMatch(set::contains), "Invalid code block.");
    }

    public Ensure<T> pattern(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return define(CharSequence.class, v -> pattern.matcher(v).matches(), "Pattern " + regex);
    }

    public T ensure(T value) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < conditions.length; i++) {
            if (!conditions[i].test(value)) {
                builder.append("- ").append(I.express(messages[i], value)).append("\n");
            }
        }

        if (builder.length() == 0) {
            return value;
        } else {
            throw new Error(builder.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(T value) {
        for (int i = 0; i < conditions.length; i++) {
            if (!conditions[i].test(value)) {
                return false;
            }
        }
        return true;
    }

}
