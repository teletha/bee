/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.Platform.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import kiss.Decoder;
import kiss.Disposable;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;

/**
 * Interactive user interface.
 */
@Managed(value = Singleton.class)
public abstract class UserInterface {

    /** The for command line user interface. */
    public static final UserInterface CUI = new CommandLineUserInterface(); // use constructor

    /** Message type magic number. */
    protected static final int TRACE = 0;

    /** Message type magic number. */
    protected static final int DEBUG = 1;

    /** Message type magic number. */
    protected static final int INFO = 2;

    /** Message type magic number. */
    protected static final int WARNING = 3;

    /** Message type magic number. */
    protected static final int ERROR = 4;

    /** Message type magic number. */
    protected static final int TITLE = 5;

    /** Message type magic number. */
    protected static final int PROGRESS = 6;

    /** The predefined answers. */
    private final Deque<String> answers = new ArrayDeque(BeeOption.Input.value());

    /** The debug flag. */
    private final boolean debuggable = BeeOption.Debug.value;

    // /** The log file. */
    // private final BufferedWriter log = BeeOption.Log.value == null ? null
    // : BeeOption.Log.value.newBufferedWriter(StandardOpenOption.CREATE,
    // StandardOpenOption.APPEND);
    //
    // private void log(CharSequence message) {
    // if (log != null) {
    // try {
    // log.append(message);
    // } catch (IOException e) {
    // throw I.quiet(e);
    // }
    // }
    // }

    /**
     * Talk to user with decoration like title.
     * 
     * @param title
     */
    public final void title(CharSequence title) {
        write(TITLE, String.valueOf(title));
    }

    /**
     * Talk to user as progress message.
     * 
     * @param message Your message.
     */
    public final void progress(CharSequence message) {
        if (!BeeOption.Quiet.value) {
            write(PROGRESS, String.valueOf(message));
        }
    }

    /**
     * Talk to user.
     * 
     * @param messages Your message.
     */
    public final void trace(Object... messages) {
        talk(TRACE, messages);
    }

    /**
     * Talk to user.
     * 
     * @param messages Your message.
     */
    public final void debug(Object... messages) {
        if (debuggable) {
            talk(DEBUG, messages);
        }
    }

    /**
     * Talk to user.
     * 
     * @param messages Your message.
     */
    public final void info(Object... messages) {
        talk(INFO, messages);
    }

    /**
     * Warn to user.
     * 
     * @param messages Your warning message.
     */
    public final void warn(Object... messages) {
        talk(WARNING, messages);
    }

    /**
     * Declare a state of emergency.
     * 
     * @param messages Your emergency message.
     */
    public final void error(Object... messages) {
        talk(ERROR, messages);
    }

    /**
     * General talk to user.
     * 
     * @param type
     * @param messages
     */
    private void talk(int type, Object[] messages) {
        if (BeeOption.Quiet.value() && type != ERROR) {
            return;
        }

        int length = messages.length;
        if (0 < length) {
            // extract the last throwable parameter
            if (messages[length - 1] instanceof Throwable e) {
                write(type, buildMessage(length - 1, messages));

                // while (e.getCause() != null) {
                // e = e.getCause();
                // }

                write(e);
            } else {
                write(type, buildMessage(length, messages));
            }
        }
    }

    /**
     * Ask user about your question and return his/her answer.
     * 
     * @param question Your question message.
     * @return An answer.
     */
    public boolean confirm(String question) {
        String answer = ask(Platform.EOL + question + " (y/n)").toLowerCase();

        if (answer.equals("y") || answer.equals("ye") || answer.equals("yes")) {
            return true;
        } else if (answer.equals("n") || answer.equals("no")) {
            return false;
        } else {
            info("Type 'y' or 'n'.");

            return confirm(question);
        }
    }

    /**
     * Ask user about your question and return his/her answer.
     * 
     * @param question Your question message.
     * @return An answer.
     */
    public final String ask(String question) {
        return ask(question, (String) null);
    }

    /**
     * <p>
     * Ask user about your question and return his/her answer.
     * </p>
     * 
     * @param question Your question message.
     * @param validator Input validator.
     * @return An answer.
     */
    public final String ask(String question, Predicate<String> validator) {
        return ask(question, (String) null, validator);
    }

    /**
     * Ask user about your question and return his/her answer.
     * <p>
     * UserInterface can display a default answer and user can use it with simple action. If the
     * returned answer is incompatible with the default anwser type, default answer will be
     * returned.
     * 
     * @param <T> Anwser type.
     * @param question Your question message.
     * @param defaultAnswer A default anwser.
     * @return An answer.
     */
    public final <T> T ask(String question, T defaultAnswer) {
        return ask(question, defaultAnswer, null);
    }

    /**
     * Ask user about your question and return his/her answer.
     * <p>
     * UserInterface can display a default answer and user can use it with simple action. If the
     * returned answer is incompatible with the default anwser type, default answer will be
     * returned.
     * 
     * @param <T> Anwser type.
     * @param question Your question message.
     * @param defaultAnswer A default anwser.
     * @param validator Input validator.
     * @return An answer.
     */
    @SuppressWarnings("resource")
    protected <T> T ask(String question, T defaultAnswer, Predicate<T> validator) {
        StringBuilder builder = new StringBuilder();
        builder.append(question);
        if (defaultAnswer != null) builder.append(" [").append(defaultAnswer).append("]");
        builder.append(" : ");

        // Question
        write(INFO, builder.toString());

        try {
            // Answer
            String answer = answers.pollFirst();
            if (answer == null) {
                answer = new BufferedReader(new InputStreamReader(getSink(), Encoding)).readLine();
            } else {
                info("Use the prepared answers. [", answer, "]");
            }

            // Remove whitespaces.
            answer = answer == null ? "" : answer.trim();

            // Validate user input.
            if (defaultAnswer == null) {
                if (answer.length() == 0) {
                    info("Your input is empty, plese retry.");

                    // Retry!
                    return ask(question, (T) null, validator);
                } else if (validator != null && !validator.test((T) answer)) {
                    info("Your input is invalid, plese retry.");

                    // Retry!
                    return ask(question, (T) null, validator);
                }

                // API definition
                return (T) answer;
            } else {
                Decoder<T> decoder = I.find(Decoder.class, defaultAnswer.getClass());

                return answer.length() == 0 ? defaultAnswer : decoder.decode(answer);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        } catch (Exception e) {
            return defaultAnswer;
        }
    }

    /**
     * Ask user about your question and return his/her selected item.
     * <p>
     * UserInterface can display a list of items and user can select it with simple action.
     * 
     * @param question Your question message.
     * @param enumeration A list of selectable items.
     * @return A selected item.
     */
    public final <E extends Enum> E ask(String question, Class<E> enumeration) {
        if (enumeration == null) {
            throw new Fail("Question needs some items. [" + question + "]");
        }
        return ask(question, Arrays.asList(enumeration.getEnumConstants()));
    }

    /**
     * Ask user about your question and return his/her selected item.
     * <p>
     * UserInterface can display a list of items and user can select it with simple action.
     * 
     * @param question Your question message.
     * @param items A list of selectable items.
     * @return A selected item.
     */
    public final <T> T ask(String question, List<T> items) {
        return ask(question, items, (Function<T, String>) null);
    }

    /**
     * Ask user about your question and return his/her selected item.
     * <p>
     * UserInterface can display a list of items and user can select it with simple action.
     * 
     * @param question Your question message.
     * @param items A list of selectable items.
     * @return A selected item.
     */
    public <T> T ask(String question, List<T> items, Function<T, String> naming) {
        if (items == null) {
            throw new Fail("Question needs some items. [" + question + "]");
        }

        switch (items.size()) {
        case 0:
            return null;

        case 1:
            return items.get(0); // unconditionally

        default:
            info(question);
            info(naming == null ? items : items.stream().map(naming).toList());

            return items.get(select(1, items.size()) - 1);
        }
    }

    /**
     * <p>
     * Ask user about your question and return his/her specified location.
     * </p>
     * <p>
     * UserInterface can display the file chooser and user can select it with simple action.
     * </p>
     * 
     * @param question Your question message.
     * @return A specified location.
     */
    public final Path file(String question) {
        return file(question, null);
    }

    /**
     * Ask user about your question and return his/her specified location.
     * <p>
     * UserInterface can display the file chooser and user can select it with simple action.
     * 
     * @param question Your question message.
     * @return A specified location.
     */
    public Path file(String question, Path defaultFile) {
        try {
            Path answer = ask(question, defaultFile);

            if (!answer.isAbsolute()) {
                answer = answer.toAbsolutePath();
            }

            if (Files.notExists(answer)) {
                if (confirm("File [" + answer + "] doesn't exist. Create it?")) {
                    Files.createDirectories(answer.getParent());
                    Files.createFile(answer);
                } else {
                    throw bee.Bee.Abort;
                }
            } else if (!Files.isRegularFile(answer)) {
                error("Path [", answer, "] is not file.");

                return directory(question);
            }
            return answer;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Ask user about your question and return his/her specified location.
     * <p>
     * UserInterface can display the directory chooser and user can select it with simple action.
     * 
     * @param question Your question message.
     * @return A specified location.
     */
    public final Path directory(String question) {
        return directory(question, null);
    }

    /**
     * <p>
     * Ask user about your question and return his/her specified location.
     * </p>
     * <p>
     * UserInterface can display the directory chooser and user can select it with simple action.
     * </p>
     * 
     * @param question Your question message.
     * @return A specified location.
     */
    public Path directory(String question, Path defaultDirectory) {
        try {
            Path answer = ask(question, defaultDirectory);

            if (!answer.isAbsolute()) {
                answer = answer.toAbsolutePath();
            }

            if (Files.notExists(answer)) {
                if (confirm("Directory [" + answer + "] doesn't exist. Create it?")) {
                    Files.createDirectories(answer);
                } else {
                    throw bee.Bee.Abort;
                }
            } else if (!Files.isDirectory(answer)) {
                error("Path [", answer, "] is not directory.");

                return directory(question);
            }
            return answer;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Select number.
     * 
     * @param min A minimum number.
     * @param max A maximum number.
     * @return A user input.
     */
    private int select(int min, int max) {
        try {
            int index = Integer.parseInt(ask("Input number to select one"));

            if (max < index) {
                warn("Max number is " + max + ", please retry");

                return select(min, max);
            }

            if (index < min) {
                warn("Min number is " + min + ", please retry.");

                return select(min, max);
            }
            return index;
        } catch (NumberFormatException e) {
            warn("Invalid number format, please retry.");

            return select(min, max);
        }
    }

    /**
     * Helper method to build message.
     * 
     * @param messages Your messages.
     * @return A combined message.
     */
    private String buildMessage(int length, Object... messages) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            Object message = messages[i];
            if (message == null) {
                builder.append("null");
            } else {
                buildMessage(builder, message);
            }
        }
        return builder.toString();
    }

    /**
     * Helper method to build message.
     * 
     * @param builder A message builder.
     * @param value A message object.
     */
    protected void buildMessage(StringBuilder builder, Object value) {
        if (value instanceof Map<?, ?> map) {
            value = map.entrySet().stream().map(entry -> String.format("%-12s \t%s", entry.getKey(), entry.getValue())).toList();
        }

        if (value instanceof CharSequence seq) {
            builder.append(seq);
        } else if (value instanceof Iterable iterable) {
            if (builder.length() != 0) {
                builder.append(EOL);
            }
            builder.append(EOL);

            int i = 0;
            for (Object object : iterable) {
                builder.append("  [").append(++i).append("] ").append(object).append(EOL);
            }
        } else if (value instanceof int[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof long[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof float[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof double[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof boolean[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof char[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof byte[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof short[] array) {
            builder.append(Arrays.toString(array));
        } else if (value instanceof Object[] array) {
            builder.append(Arrays.toString(array));
        } else {
            builder.append(I.transform(value, String.class));
        }
    }

    /**
     * Get underlaying message listener.
     * 
     * @return
     */
    public abstract Appendable getInterface();

    /**
     * Get underlaying message sink.
     * 
     * @return
     */
    protected abstract InputStream getSink();

    /**
     * Write message to user.
     * 
     * @param message
     */
    protected abstract void write(int type, String message);

    /**
     * Write error message to user.
     * 
     * @param error
     */
    protected abstract void write(Throwable error);

    /**
     * Display message about command starts.
     * 
     * @param name A command name.
     */
    protected abstract void startCommand(String name);

    /**
     * Display message about command ends.
     * 
     * @param name A command name.
     */
    protected abstract void endCommand(String name);

    /**
     * Default implementation.
     */
    static class CommandLineUserInterface extends UserInterface {

        /** Ansi escape code must start with this PREFIX. */
        public static final String PREFIX = "[";

        private static final boolean disableANSI = Platform.isJitPack();

        private static final boolean disableTrace = Platform.isJitPack() || Platform.isGithub();

        /** The original standard output. */
        private final PrintStream standardOutput;

        /** The original standard error. */
        @SuppressWarnings("unused")
        private final PrintStream standardError;

        /** The original standard input. */
        private final InputStream standardInput;

        /** The task state. */
        private boolean first = false;

        /** The task state. */
        private boolean blank = true;

        /** The command queue. */
        private Deque<String> commands = new ArrayDeque();

        /** The view state. */
        private int erasableLine;

        /** The progress message. */
        private Disposable progress;

        /**
         * Build with standard output and error.
         */
        CommandLineUserInterface() {
            this(System.out, System.err, System.in);
        }

        /**
         * Build with your output and error.
         * 
         * @param output
         * @param error
         */
        CommandLineUserInterface(PrintStream output, PrintStream error, InputStream input) {
            standardOutput = output;
            standardError = error;
            standardInput = input;

            System.setOut(new Delegator());
            System.setErr(new Delegator());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void startCommand(String name) {
            first = true;
            commands.add(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void endCommand(String name) {
            if (first) {
                showCommandName();
            }
            first = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized void write(int type, String message) {
            if (type != TRACE && progress != null) {
                progress.dispose();
                progress = null;
            }

            switch (type) {
            case TITLE:
                blank = false;
                write("------------------------------------------------------------", true);
                write(stain(message, "Build SUCCESS", "76", "Build FAILURE", "1"), true);
                write("------------------------------------------------------------", true);
                return;

            case PROGRESS:
                if (!disableTrace) {
                    progress = I.schedule(0, 1000, TimeUnit.MILLISECONDS, true).to(count -> {
                        long minutes = (count - 1) / 60;
                        long sec = (count - 1) % 60;
                        write(TRACE, message + "  (" + String.format("%02d:%02d", minutes, sec) + ")");
                    });
                }
                break;

            case TRACE:
                if (!disableTrace) {
                    write(message, true);
                    erasableLine = message.split(Platform.EOL).length;
                }
                break;

            case DEBUG:
                write(message, true);
                break;

            case WARNING:
                write(stain("[WARN] ", "227").concat(message), true);
                break;

            case ERROR:
                write(stain("[ERROR] ", "1").concat(message), true);
                break;

            default:
                write(message, true);
                break;
            }

            blank = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized void write(Throwable error) {
            if (first) {
                showCommandName();
                first = false;
            }

            int count = 1;
            while (error != null) {
                writeStackTrace(count++, error);
                error = error.getCause();
            }
            standardOutput.flush();
        }

        private void writeStackTrace(int counter, Throwable error) {
            standardOutput.append(toCircledNumber(counter))
                    .append("  Caused by ")
                    .append(stain(error.getClass().getCanonicalName(), "208"))
                    .append(" : ")
                    .append(Objects.requireNonNullElse(error.getMessage(), ""))
                    .append(Platform.EOL);

            if (BeeOption.Debug.value || error.getCause() == null) {
                StackTraceElement[] elements = error.getStackTrace();
                for (int i = 0; i < elements.length; i++) {
                    StackTraceElement e = elements[i];
                    String fqcn = e.getClassName();
                    String file = e.getFileName();
                    standardOutput.append("\t%3d.  ".formatted(elements.length - i)).append(fqcn).append(".").append(e.getMethodName());
                    if (file != null) {
                        standardOutput.append(" (")
                                .append(e.getFileName())
                                .append(":")
                                .append(String.valueOf(e.getLineNumber()))
                                .append(")");
                    }
                    standardOutput.append(Platform.EOL);
                }
            }
        }

        private static String toCircledNumber(int number) {
            if (number >= 1 && number <= 20) {
                return String.valueOf((char) ('\u2460' + number - 1));
            } else {
                return "(" + number + ")";
            }
        }

        /**
         * Write message actually.
         * 
         * @param message A message.
         * @param enforceLine A line feed status.
         */
        private synchronized void write(String message, boolean enforceLine) {
            if (first) {
                showCommandName();
                first = false;
            }

            if (0 < erasableLine && !disableANSI) {
                message = PREFIX + erasableLine + "F" + PREFIX + "J" + message;
                erasableLine = 0;
            }

            if (!enforceLine) {
                standardOutput.print(message);
            } else {
                standardOutput.println(message);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Appendable getInterface() {
            if (first) {
                showCommandName();
                first = false;
            }
            return System.out; // use delegator
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected InputStream getSink() {
            return standardInput;
        }

        /**
         * Show command name.
         */
        private void showCommandName() {
            String command = commands.pollLast();

            if (command != null) {
                if (blank) {
                    standardOutput.print(Platform.EOL);
                }
                standardOutput.println(stain("◆◇◆◇◆   " + command.replace(":", " : ") + "   ◆◇◆◇◆", "75"));
            }
        }

        /**
         * Colorize the text.
         * 
         * @param code
         * @param colorCode
         * @return
         */
        private static String stain(String text, String colorCode) {
            return disableANSI ? text : PREFIX + "38;5;" + colorCode + "m" + text + PREFIX + "0m";
        }

        /**
         * Colorize specified parts of text.
         * 
         * @param code
         * @param partAndColorCode
         * @return
         */
        private static String stain(String text, String... partAndColorCode) {
            for (int i = 0; i < partAndColorCode.length; i++) {
                text = text.replace(partAndColorCode[i], stain(partAndColorCode[i], partAndColorCode[++i]));
            }
            return text;
        }

        /**
         * Delgator for UI.
         */
        private class Delegator extends PrintStream {

            /**
             * 
             */
            public Delegator() {
                super(new ByteArrayOutputStream(), false, Platform.Encoding);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(byte[] b) throws IOException {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(b), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void flush() {
                standardOutput.flush();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void close() {
                standardOutput.close();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean checkError() {
                return standardOutput.checkError();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(int b) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(b), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(byte[] buf, int off, int len) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(new String(buf, off, len), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(boolean b) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(b), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(char c) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(c), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(int i) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(i), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(long l) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(l), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(float f) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(f), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(double d) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(d), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(char[] s) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(s), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(String s) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(s, false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void print(Object obj) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(obj), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println() {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write("", true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(boolean x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(char x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(int x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(long x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(float x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(double x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(char[] x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(String x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(x, true);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void println(Object x) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(String.valueOf(x), true);
            }
        }
    }

    private static class Tee extends PrintStream {
        private final PrintStream second;

        public Tee(OutputStream main, PrintStream second) {
            super(main, true);
            this.second = second;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int b) {
            super.write(b);
            second.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            second.write(buf, off, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() {
            super.flush();
            second.flush();
        }
    }
}