/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.lang.model.SourceVersion;

import org.objectweb.asm.Type;

import bee.UserInterface;
import kiss.I;
import kiss.Observer;
import kiss.WiseTriConsumer;
import kiss.WiseTriFunction;
import psychopath.File;
import psychopath.Locator;
import psychopath.Progress;

public class Inputs {

    public static Observer<Progress> observerFor(UserInterface ui, File output, String progressMessage, String completeMessage) {
        return new Observer<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void accept(Progress info) {
                ui.trace(progressMessage, ": ", info.completedFiles(), "/", info.totalFiles, " (", info.rateByFiles(), "%)");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void error(Throwable e) {
                ui.error(e);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void complete() {
                ui.info(completeMessage, ": ", output, " (", formatAsSize(output.size()), ")");
            }
        };
    }

    public static String format(Object p1, String text) {
        return String.format(text, p1);
    }

    /**
     * Format as human-readable size.
     * 
     * @param bytes
     * @return
     */
    public static String formatAsSize(long bytes) {
        return formatAsSize(bytes, true);
    }

    /**
     * Format as human-readable size.
     * 
     * @param bytes
     * @return
     */
    public static String formatAsSize(long bytes, boolean unit) {
        double kb = bytes / 1024.0;
        if (kb < 0.9) {
            return Long.toString(bytes).concat(unit ? "Bytes" : "");
        }

        double mb = kb / 1024.0;
        if (mb < 0.9) {
            return formatAsSize(kb, unit ? "KB" : "");
        }

        double gb = mb / 1024.0;
        if (gb < 0.9) {
            return formatAsSize(mb, unit ? "MB" : "");
        }

        double tb = gb / 1024.0;
        if (tb < 0.9) {
            return formatAsSize(gb, unit ? "GB" : "");
        }
        return formatAsSize(tb, unit ? "TB" : "");
    }

    /**
     * Remove tailing zero.
     * 
     * @param size
     * @param unit
     * @return
     */
    private static String formatAsSize(double size, String unit) {
        long rouded = (long) size;
        if (rouded == size) {
            return Long.toString(rouded).concat(unit);
        } else {
            return Double.toString(Math.round(size * 100.0) / 100.0).concat(unit);
        }
    }

    /**
     * Normalize user input.
     * 
     * @param input A user input.
     * @param defaultValue A default value.
     * @return A normalized input.
     */
    public static String normalize(CharSequence input, String defaultValue) {
        if (input == null) {
            input = defaultValue;
        }

        // trim whitespcae
        input = input.toString().trim();

        if (input.length() == 0) {
            input = defaultValue;
        }

        // API definition
        return input.toString();
    }

    /**
     * Normalize {@link SourceVersion} to human-readable version number.
     * 
     * @param version A target version.
     * @return A version number.
     */
    public static String normalize(SourceVersion version) {
        if (version == null) {
            version = SourceVersion.latest();
        }

        switch (version) {
        case RELEASE_0:
            return "1.0";

        case RELEASE_1:
            return "1.1";

        case RELEASE_2:
            return "1.2";

        case RELEASE_3:
            return "1.3";

        case RELEASE_4:
            return "1.4";

        case RELEASE_5:
            return "1.5";

        case RELEASE_6:
            return "1.6";

        default:
            return version.name().substring(8);
        }
    }

    /**
     * Hyphenize user input.
     * 
     * @param input A user input.
     * @return A hyphenized input.
     */
    public static String hyphenize(String input) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (i != 0 && Character.isUpperCase(c)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A path to the target text file.
     * @return A file contents.
     */
    public static CharSequence ref(String file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    public static CharSequence ref(Path file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    public static CharSequence ref(File file) {
        return new CharSequence() {

            private long modified = file.lastModifiedMilli();

            private String text = file.text().strip();

            /**
             * Update contents.
             */
            private void update() {
                long time = file.lastModifiedMilli();
                if (modified != time) {
                    modified = time;
                    text = file.text().strip();
                }
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                update();
                return text.subSequence(start, end);
            }

            @Override
            public int length() {
                update();
                return text.length();
            }

            @Override
            public char charAt(int index) {
                update();
                return text.charAt(index);
            }

            @Override
            public int hashCode() {
                update();
                return text.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                update();
                return text.equals(obj);
            }

            @Override
            public String toString() {
                update();
                return text.toString();
            }
        };
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P> String signature(DebugConsumer<P> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static String signature(DebugRunnable lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P, R> String signature(DebugFunction<P, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <R> String signature(Supplier<R> lambda) {
        return "CC";
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P1, P2> String signature(DebugBiConsumer<P1, P2> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P1, P2, R> String signature(DebugBiFunction<P1, P2, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */
    public static <P1, P2, P3> String signature(DebugTriConsumer<P1, P2, P3> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * Reference the method signature.
     * 
     * @param lambda Method reference.
     * @return A method signature.
     */

    public static <P1, P2, P3, R> String signature(DebugTriFunction<P1, P2, P3, R> lambda) {
        return methodNameFromLambda(lambda);
    }

    /**
     * @param lambda
     * @return
     */
    static String methodNameFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            SerializedLambda serialized = (SerializedLambda) m.invoke(lambda);
            String clazz = serialized.getImplClass().replace('/', '.');
            String method = serialized.getImplMethodName();

            StringJoiner params = new StringJoiner(", ", "(", ")");
            for (Type param : Type.getArgumentTypes(serialized.getImplMethodSignature())) {
                params.add(I.type(param.getClassName()).getSimpleName());
            }
            return clazz + "#" + method + params;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     */
    public static interface DebugRunnable extends Runnable, Serializable {
    }

    /**
     * 
     */
    public static interface DebugConsumer<P> extends Consumer<P>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugBiConsumer<P1, P2> extends BiConsumer<P1, P2>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugTriConsumer<P1, P2, P3> extends WiseTriConsumer<P1, P2, P3>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugFunction<P, R> extends Function<P, R>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugBiFunction<P1, P2, R> extends BiFunction<P1, P2, R>, Serializable {
    }

    /**
     * 
     */
    public static interface DebugTriFunction<P1, P2, P3, R> extends WiseTriFunction<P1, P2, P3, R>, Serializable {
    }
}