/*
 * Copyright (C) 2021 Nameless Production Committee
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
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import bee.api.Command;
import kiss.Decoder;
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

    /** The debug mode. */
    private final boolean debug = BeeOption.Debug.value();

    /**
     * Talk to user with decoration like title.
     * 
     * @param title
     */
    public final void title(CharSequence title) {
        write(TITLE, String.valueOf(title));
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
        talk(DEBUG, messages);
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
                write(type, build(length - 1, messages));

                while (e.getCause() != null) {
                    e = e.getCause();
                }

                write(e);
            } else {
                write(type, build(length, messages));
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
    public String ask(String question) {
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
    public String ask(String question, Predicate<String> validator) {
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
    public <T> T ask(String question, T defaultAnswer) {
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
    private <T> T ask(String question, T defaultAnswer, Predicate<T> validator) {
        StringBuilder builder = new StringBuilder();
        builder.append(question);
        if (defaultAnswer != null) builder.append(build(" [", defaultAnswer, "]"));
        builder.append(" : ");

        // Question
        write(INFO, builder.toString());

        try {
            // Answer
            String answer = new BufferedReader(new InputStreamReader(System.in, Encoding)).readLine();

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
        }
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
    public <T> T ask(String question, List<T> items) {
        if (items == null) {
            throw new Error(build("Question needs some items. [" + question, "]"));
        }

        switch (items.size()) {
        case 0:
            return null;

        case 1:
            return items.get(0); // unconditionally

        default:
            info(question);
            info(items);

            return items.get(select(1, items.size()) - 1);
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
    public <E extends Enum> E ask(String question, Class<E> enumeration) {
        if (enumeration == null) {
            throw new Error(build("Question needs some items. [", question, "]"));
        }
        return ask(question, Arrays.asList(enumeration.getEnumConstants()));
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
    public Path file(String question) {
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
    public Path directory(String question) {
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
    protected static String build(Object... messages) {
        return build(messages.length, messages);
    }

    /**
     * Helper method to build message.
     * 
     * @param messages Your messages.
     * @return A combined message.
     */
    private static String build(int length, Object... messages) {
        StringBuilder builder = new StringBuilder();
        build(builder, length, messages);
        return builder.toString();
    }

    /**
     * Helper method to build message.
     * 
     * @param builder A message builder.
     * @param messages Your messages.
     */
    private static void build(StringBuilder builder, int length, Object... messages) {
        for (int i = 0; i < length; i++) {
            Object message = messages[i];
            if (message == null) {
                builder.append("null");
            } else {
                Class type = message.getClass();

                if (type.isArray()) {
                    buildArray(builder, type.getComponentType(), message);
                } else if (CharSequence.class.isAssignableFrom(type)) {
                    builder.append((CharSequence) message);
                } else if (List.class.isAssignableFrom(type)) {
                    buildList(builder, (List) message);
                } else {
                    builder.append(I.transform(message, String.class));
                }
            }
        }
    }

    /**
     * Helper method to build message from various array type.
     * 
     * @param builder A message builder.
     * @param type A array type.
     * @param array A message array.
     */
    private static void buildArray(StringBuilder builder, Class type, Object array) {
        if (type == int.class) {
            builder.append(Arrays.toString((int[]) array));
        } else if (type == long.class) {
            builder.append(Arrays.toString((long[]) array));
        } else if (type == float.class) {
            builder.append(Arrays.toString((float[]) array));
        } else if (type == double.class) {
            builder.append(Arrays.toString((double[]) array));
        } else if (type == boolean.class) {
            builder.append(Arrays.toString((boolean[]) array));
        } else if (type == char.class) {
            builder.append(Arrays.toString((char[]) array));
        } else if (type == byte.class) {
            builder.append(Arrays.toString((byte[]) array));
        } else if (type == short.class) {
            builder.append(Arrays.toString((short[]) array));
        } else {
            Object[] o = (Object[]) array;
            build(builder, o.length, o);
        }
    }

    /**
     * Build listup message.
     * 
     * @param builder A message builder.
     * @param list Items.
     */
    private static void buildList(StringBuilder builder, List list) {
        if (builder.length() != 0) {
            builder.append(EOL);
        }
        builder.append(EOL);

        for (int i = 0; i < list.size(); i++) {
            builder.append("  [").append(i + 1).append("] ").append(list.get(i)).append(EOL);
        }
    }

    /**
     * Get underlaying message listener.
     * 
     * @return
     */
    public abstract Appendable getInterface();

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
     * @param command A coommand info.
     */
    protected abstract void startCommand(String name, Command command);

    /**
     * Display message about command ends.
     * 
     * @param name A command name.
     * @param command A coommand info.
     */
    protected abstract void endCommand(String name, Command command);

    /**
     * Default implementation.
     */
    private static class CommandLineUserInterface extends UserInterface {

        /** Ansi escape code must start with this PREFIX. */
        public static final String PREFIX = "[";

        private static final boolean disableANSI = Platform.isJitPack();

        private static final boolean disableTrace = Platform.isJitPack() || Platform.isGithub() || Platform.isEclipse();

        /** The original standard output. */
        private final PrintStream standardOutput;

        /** The original standard error. */
        @SuppressWarnings("unused")
        private final PrintStream standardError;

        /** The task state. */
        private boolean first = false;

        /** The task state. */
        private boolean blank = true;

        /** The command queue. */
        private Deque<String> commands = new ArrayDeque();

        /** The view state. */
        private boolean eraseNextLine = false;

        /**
         * 
         */
        private CommandLineUserInterface() {
            standardOutput = System.out;
            standardError = System.err;

            System.setOut(new Delegator());
            System.setErr(new Delegator());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void startCommand(String name, Command command) {
            first = true;
            commands.add(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void endCommand(String name, Command command) {
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
            switch (type) {
            case TITLE:
                blank = false;
                write("------------------------------------------------------------", true);
                write(stain(message, "Build SUCCESS", "76", "Build FAILURE", "1"), true);
                write("------------------------------------------------------------", true);
                return;

            case TRACE:
            case DEBUG:
                if (!disableTrace) {
                    write(message.concat("\r"), false);
                }
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
            error.printStackTrace(standardOutput);
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

            if (eraseNextLine && !disableANSI) {
                message = PREFIX + "2K" + message;
            }
            eraseNextLine = message.endsWith("\r");

            if (eraseNextLine || !enforceLine) {
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
         * Show command name.
         */
        private void showCommandName() {
            String command = commands.pollLast();

            if (command != null) {
                if (blank) {
                    standardOutput.print(Platform.EOL);
                }
                standardOutput.println(stain("◆ " + command.replace(":", " : ") + " ◆", "75"));
            }
        }

        /**
         * Colorize the text.
         * 
         * @param text
         * @param colorCode
         * @return
         */
        private static String stain(String text, String colorCode) {
            return disableANSI ? text : PREFIX + "38;5;" + colorCode + "m" + text + PREFIX + "0m";
        }

        /**
         * Colorize specified parts of text.
         * 
         * @param text
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
}