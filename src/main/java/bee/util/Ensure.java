/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.lang.Character.UnicodeBlock;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bee.Fail;
import kiss.WiseConsumer;

public class Ensure<T> {

    /** Built-in format for text. */
    public static final Ensure<CharSequence> Text = new Ensure(CharSequence.class, null).proxy("");

    /** Built-in format for text. */
    public static final Ensure<CharSequence> Alphabetic = Text.strip()
            .min(1)
            .available(((IntPredicate) Character::isUpperCase).or(Character::isLowerCase).or(Character::isTitleCase), "Alphabet");

    /** Built-in format for text. */
    public static final Ensure<CharSequence> Numeric = Text.strip().normalize().available(Character::isDigit, "Digit");

    /** Built-in format for text. */
    public static final Ensure<CharSequence> Alphanumeric = Alphabetic.available(Character::isDigit, "Alphanumeric");

    /** The target data type. */
    private final Class<T> type;

    /** The nomalizer manager. */
    private final List<Function<T, T>> canonicalizers = new ArrayList();

    /** The limitter manager. */
    private final List<BiConsumer<T, List<String>>> limitters = new ArrayList();

    /** The letter-set manager. */
    private IntPredicate availables;

    /** The letter-set names. */
    private final Set<String> availableNames = new LinkedHashSet();

    /**
     * Create new {@link Ensure}.
     * 
     * @param type
     * @param base
     */
    private Ensure(Class<T> type, Ensure<T> base) {
        if (base == null) {
            this.type = type;
        } else {
            this.type = base.type;
            canonicalizers.addAll(base.canonicalizers);
            limitters.addAll(base.limitters);
            availables = base.availables;
            availableNames.addAll(base.availableNames);
        }
    }

    /**
     * Create new {@link Ensure} with the specified canonicalizer.
     * 
     * @param canonicalizer
     * @return
     */
    public <X> Ensure<T> canonicalize(Class<X> type, Function<X, X> canonicalizer) {
        if (canonicalizer == null || !type.isAssignableFrom(this.type)) {
            return this;
        }

        Ensure<X> update = new Ensure(null, this);
        update.canonicalizers.add(canonicalizer);
        return (Ensure<T>) update;
    }

    /**
     * Create new {@link Ensure} with the specified limitter.
     * 
     * @param limiter
     * @return
     */
    public <X> Ensure<T> limit(Class<X> type, BiConsumer<X, List<String>> limiter) {
        if (limiter == null || !type.isAssignableFrom(this.type)) {
            return this;
        }

        Ensure<X> update = new Ensure(null, this);
        update.limitters.add(limiter);
        return (Ensure<T>) update;
    }

    /**
     * Validate the given value. If it is invalid, throw {@link Fail}.
     * 
     * @param value
     * @return
     */
    public final <X extends T> X validate(X value) {
        return validate(value, null);
    }

    /**
     * Validate the given value.
     * 
     * @param value
     * @return
     */
    public final <X extends T> X validate(X value, WiseConsumer<List<String>> invalid) {
        for (Function normalizer : canonicalizers) {
            value = (X) normalizer.apply(value);
        }

        List<String> messages = new ArrayList();
        for (BiConsumer<T, List<String>> limitter : limitters) {
            limitter.accept(value, messages);
        }

        if (value instanceof CharSequence chars) {
            if (availables != null && !chars.codePoints().allMatch(availables)) {
                messages.add("Available characters are " + availableNames.stream().collect(Collectors.joining(" ")));
            }
        }

        if (messages.size() != 0) {
            if (invalid == null) {
                X v = value;
                invalid = x -> {
                    throw new Fail("Input value [" + v + "] is invalid.", x);
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
    public Ensure<T> proxy(T value) {
        return canonicalize(Object.class, v -> Objects.requireNonNullElse(v, value));
    }

    /**
     * Normalize {@link CharSequence}.
     * 
     * @return
     */
    public Ensure<T> normalize() {
        return normalize(Form.NFKC);
    }

    /**
     * Normalize {@link CharSequence}.
     * 
     * @return
     */
    public Ensure<T> normalize(Form form) {
        return canonicalize(CharSequence.class, v -> Normalizer.normalize(v, form));
    }

    /**
     * Strip space-like charas from {@link CharSequence}.
     * 
     * @return
     */
    public Ensure<T> strip() {
        return canonicalize(CharSequence.class, v -> v.toString().strip());
    }

    /**
     * Limit the minimum size of the given value.
     * 
     * @param min
     * @return
     */
    public Ensure<T> min(int min) {
        return limit(CharSequence.class, (v, messages) -> {
            int length = v.toString().codePointCount(0, v.length());
            if (length < min) {
                messages.add("The inputed value [%s] is %s letters, but it must be at least %s letters.".formatted(v, v.length(), min));
            }
        });
    }

    /**
     * Limit the maximum size of the given value.
     * 
     * @param max
     * @return
     */
    public Ensure<T> max(int max) {
        return limit(CharSequence.class, (v, messages) -> {
            int length = v.toString().codePointCount(0, v.length());
            if (max < length) {
                messages.add("The inputed value [%s] is %s letters, but it must be no more than %s letters.".formatted(v, v.length(), max));
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
    public Ensure<T> size(int min, int max) {
        return min(min).max(max);
    }

    /**
     * Limit the text pattern by regular expression.
     * 
     * @param regex
     * @return
     */
    public Ensure<T> pattern(String regex, String... invalid) {
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

    /**
     * Limit the available letter set.
     * 
     * @param set The letter set.
     * @param name Human-readable name of the specified letter set.
     * @return
     */
    public Ensure<T> available(IntPredicate set, String name) {
        if (set == null || !CharSequence.class.isAssignableFrom(type)) {
            return this;
        }

        Ensure<T> update = new Ensure(null, this);
        update.availables = availables == null ? set : availables.or(set);
        update.availableNames.add(name);
        return update;
    }

    /**
     * Limit the available code block.
     * 
     * @param block The code block.
     * @param name Human-readable name of the specified code block.
     * @return
     */
    public Ensure<T> available(UnicodeBlock block, String name) {
        return available(block == null ? null : code -> UnicodeBlock.of(code) == block, name);
    }

    /**
     * Limit the available separator characters.
     * 
     * @param separators
     * @return
     */
    public Ensure<T> separator(String separators) {
        if (separators == null) {
            return this;
        }

        int[] codes = separators.codePoints().toArray();

        return available(code -> {
            for (int i : codes) {
                if (code == i) {
                    return true;
                }
            }
            return false;
        }, separators).limit(CharSequence.class, (v, messages) -> {
            if (v.length() != 0) {
                if (separators.indexOf(v.charAt(0)) != -1) {
                    messages.add("The letter [" + separators + "] are unavailable for the first letter.");
                }

                if (separators.indexOf(v.charAt(v.length() - 1)) != -1) {
                    messages.add("The letter [" + separators + "] are unavailable for the last letter.");
                }
            }

            if (Pattern.compile("[" + Pattern.quote(separators) + "]{2}").matcher(v).find()) {
                messages.add("The letter [" + separators + "] cannot be used consecutively.");
            }
        });
    }
}