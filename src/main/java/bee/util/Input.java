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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bee.Fail;
import kiss.I;
import kiss.WiseConsumer;

public class Input<T> {

    /** The target data type. */
    private final Class<T> type;

    /** The nomalizer manager. */
    private final List<Function<T, T>> normalizers = new ArrayList();

    /** The limitter manager. */
    private final List<BiConsumer<T, List<String>>> limitters = new ArrayList();

    private final Predicate<T> letters = I::accept;

    private final Set<Integer> extras = new HashSet();

    /**
     * Create new {@link Input}.
     * 
     * @param type
     * @param base
     */
    private Input(Class<T> type, Input<T> base) {
        this.type = type;

        if (base != null) {
            normalizers.addAll(base.normalizers);
            limitters.addAll(base.limitters);
            extras.addAll(base.extras);
        }
    }

    /**
     * Create new {@link Input} with the specified normalizer.
     * 
     * @param normalizer
     * @return
     */
    public <X> Input<T> normalize(Class<X> type, Function<X, X> normalizer) {
        if (normalizer == null || !type.isAssignableFrom(this.type)) {
            return this;
        }

        Input<X> update = new Input(type, this);
        update.normalizers.add(normalizer);
        return (Input<T>) update;
    }

    /**
     * Create new {@link Input} with the specified limitter.
     * 
     * @param limiter
     * @return
     */
    public <X> Input<T> limit(Class<X> type, BiConsumer<X, List<String>> limiter) {
        if (limiter == null || !type.isAssignableFrom(this.type)) {
            return this;
        }

        Input<X> update = new Input(type, this);
        update.limitters.add(limiter);
        return (Input<T>) update;
    }

    /**
     * Validate the given value. If it is invalid, throw {@link Fail}.
     * 
     * @param value
     * @return
     */
    public final T validate(T value) {
        return validate(value, null);
    }

    /**
     * Validate the given value.
     * 
     * @param value
     * @return
     */
    public final T validate(T value, WiseConsumer<List<String>> invalid) {
        for (Function<T, T> normalizer : normalizers) {
            value = normalizer.apply(value);
        }

        List<String> messages = new ArrayList();
        for (BiConsumer<T, List<String>> limitter : limitters) {
            limitter.accept(value, messages);
        }

        if (messages.size() != 0) {
            if (invalid == null) {
                invalid = x -> {
                    throw new Fail(x.stream().collect(Collectors.joining("\n", "- ", "")));
                };
            }
            invalid.accept(messages);
        }

        return value;
    }

    /**
     * Test the given value.
     * 
     * @param value
     * @return
     */
    public final boolean test(T value) {
        boolean[] result = {true};
        validate(value, x -> result[0] = false);
        return result[0];
    }

    /**
     * Set the default value when the given value is null.
     * 
     * @param value
     * @return
     */
    public Input<T> proxy(T value) {
        return normalize(Object.class, v -> Objects.requireNonNullElse(v, value));
    }

    /**
     * Normalize {@link CharSequence}.
     * 
     * @return
     */
    public Input<T> normalize() {
        return normalize(CharSequence.class, v -> Normalizer.normalize(v, Normalizer.Form.NFKC));
    }

    /**
     * Strip space-like charas from {@link CharSequence}.
     * 
     * @return
     */
    public Input<T> strip() {
        return normalize(CharSequence.class, v -> v.toString().strip());
    }

    /**
     * Limit the minimum size of the given value.
     * 
     * @param min
     * @return
     */
    public Input<T> min(int min) {
        return limit(CharSequence.class, (v, messages) -> {
            int length = v.toString().codePointCount(0, v.length());
            if (length < min) {
                messages.add("[%s] is %s letters, but it must be at least %s letters.".formatted(v, v.length(), min));
            }
        });
    }

    /**
     * Limit the maximum size of the given value.
     * 
     * @param max
     * @return
     */
    public Input<T> max(int max) {
        return limit(CharSequence.class, (v, messages) -> {
            int length = v.toString().codePointCount(0, v.length());
            if (max < length) {
                messages.add("[%s] is %s letters, but it must be no more than %s letters.".formatted(v, v.length(), max));
            }
        });
    }

    /**
     * Limit the minimum and maximum size of the given value.
     * 
     * @param min
     * @param max
     * @return
     */
    public Input<T> size(int min, int max) {
        return min(min).max(max);
    }

    /**
     * Limit the text pattern by regular expression.
     * 
     * @param regex
     * @return
     */
    public Input<T> pattern(String regex, String... invalid) {
        Pattern pattern = Pattern.compile(regex);
        return limit(CharSequence.class, (v, messages) -> {
            if (!pattern.matcher(v).matches()) {
                if (invalid == null || invalid.length == 0) {
                    messages.add("[%s] is not appropriate.".formatted(v));
                } else {
                    messages.add(invalid[0].formatted(v));
                }
            }
        });
    }

    public Input<T> available(String name, IntPredicate... types) {
        return limit(CharSequence.class, (v, messages) -> {
            if (v.codePoints().allMatch())
        });
    }

}
