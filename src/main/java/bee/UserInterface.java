/*
 * Copyright (C) 2026 The BEE Development Team
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import bee.util.Terminal;
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

    /** Message type magic number. */
    protected static final int SPINNER = 7;

    /** The frames of the spinner. */
    protected static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    /** The interval (ms) between the spinner frames. */
    protected static final long SPINNER_INTERVAL = 100;

    /**
     * The for command line user interface.
     * <p>
     * This field must be declared after all constants which the command line user interface reads
     * in its static initializer.
     * </p>
     */
    public static final UserInterface CUI = new CommandLineUserInterface(); // use constructor

    /** The predefined answers. */
    private final Deque<String> answers = new ArrayDeque(BeeOption.Input.value());

    /** The user input reader. */
    private BufferedReader reader;

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
     * Talk to user as an animated spinner. The animation continues until the next non-trace message
     * is written.
     * 
     * @param message Your message.
     */
    public final void spinner(CharSequence message) {
        if (!BeeOption.Quiet.value) {
            write(SPINNER, String.valueOf(message));
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
                answer = read();
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
     * UserInterface can display a list of items and user can select it with simple action. If the
     * user interface supports a TUI, the user can select an item with arrow keys and the enter
     * key, otherwise the user must input the number of the item.
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
            List<String> names = naming == null ? items.stream().map(item -> String.valueOf(item)).toList()
                    : items.stream().map(naming).toList();

            return items.get(select(question, names) - 1);
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
     * <p>
     * Show a list of selectable items and ask user to select one of them.
     * </p>
     * <p>
     * This default implementation displays all items with their numbers and requires the user to
     * input a number. A user interface which can control the terminal can override this method to
     * select an item interactively.
     * </p>
     * 
     * @param question Your question message.
     * @param names A list of displayable item names.
     * @return A 1-based index of the selected item.
     */
    protected int select(String question, List<String> names) {
        info(question);
        info(names);

        return select(1, names.size());
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
     * Read a line from the user input sink.
     * <p>
     * The reader is cached because a new reader discards the bytes which the previous reader has
     * already buffered.
     * </p>
     * 
     * @return A user input, or <code>null</code> when the input is closed.
     */
    protected final String read() {
        try {
            if (reader == null) {
                reader = new BufferedReader(new InputStreamReader(getSink(), Encoding));
            }
            return reader.readLine();
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Check whether the user has prepared answers or not.
     * 
     * @return <code>true</code> if the user has prepared answers by the input option.
     */
    protected final boolean hasPreparedAnswers() {
        return answers.isEmpty() == false;
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

        private static final boolean disableANSI = resolveColor();

        private static final boolean disableTrace = Platform.isJitPack() || Platform.isGithub();

        /** The terminal width used for the layout. */
        private static final int WIDTH = resolveWidth();

        /** The original standard output. */
        private final PrintStream standardOutput;

        /** The original standard error. */
        @SuppressWarnings("unused")
        private final PrintStream standardError;

        /** The original standard input. */
        private final InputStream standardInput;

        /** The console charset. */
        private static final Charset CONSOLE = System.out.charset();

        /**
         * The spinner frames. Falls back to ASCII when the console can not encode the unicode
         * frames.
         */
        private static final String[] FRAMES = canEncode(SPINNER_FRAMES[0]) ? SPINNER_FRAMES
                : new String[] {"|", "/", "-", "\\"};

        /** The cursor marker of the interactive selector. Falls back to ASCII. */
        private static final String MARKER = glyph("\u25b6 ", "> ");

        /** The blank which keeps the unselected items aligned with the marker. */
        private static final String MARKER_BLANK = MARKER.replaceAll(".", " ");

        /** The arrow key mark of the interactive selector. Falls back to ASCII. */
        private static final String ARROW = glyph("\u2193/\u2191", "DOWN/UP");

        /** The decoration of the command name. Falls back to ASCII. */
        private static final String DECORATION = glyph("\u25c6\u25c7\u25c6\u25c7\u25c6", "-----");

        /** The horizontal rule. Falls back to ASCII. */
        private static final String RULE = glyph("\u2500", "-");

        /** Whether the terminal supports the OSC8 hyper links or not. */
        private static final boolean hyperlink = isHyperlinkSupported();

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

        /** The spinner. */
        private Disposable spinner;

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
            if (type != TRACE) {
                stopDynamicMessages();
            }

            switch (type) {
            case TITLE:
                blank = false;
                String rule = rule(WIDTH);
                write(rule, true);
                write(stain(message, "Build SUCCESS", "76", "Build FAILURE", "1"), true);
                write(rule, true);
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

            case SPINNER:
                if (!disableTrace) {
                    spinner = I.schedule(0, SPINNER_INTERVAL, TimeUnit.MILLISECONDS, true).to(count -> {
                        write(TRACE, FRAMES[(int) ((count - 1) % FRAMES.length)] + " " + message);
                    });
                }
                break;

            case TRACE:
                if (!disableTrace) {
                    write(message, true);
                    erasableLine = lines(message);
                }
                break;

            case DEBUG:
                write(message, true);
                break;

            case WARNING:
                write(stain(mark("! ", "[WARN] "), "227").concat(message), true);
                break;

            case ERROR:
                write(stain(mark("\u2716 ", "[ERROR] "), "1").concat(message), true);
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
            standardOutput.append(stain(toCircledNumber(counter), "240"))
                    .append(stain("  Caused by ", "240"))
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
                    standardOutput.append(stain("\t%3d.  ".formatted(elements.length - i), "240")).append(fqcn).append(".").append(e.getMethodName());
                    if (file != null) {
                        // Omit the O in "fqcn" so the link text stays aligned with the normal style.
                        String location = file + ":" + e.getLineNumber();
                        standardOutput.append(" (").append(link(e, location)).append(")");
                    }
                    standardOutput.append(Platform.EOL);
                }
            }
        }

        /**
         * Build the clickable link to the source location.
         * 
         * @param element A stack trace element.
         * @param location A location text.
         * @return A link text.
         */
        private static String link(StackTraceElement element, String location) {
            if (hyperlink) {
                return "\u001b]8;;" + element.getClassName().replace('.', '/') + ".java#" + element.getLineNumber() + "\u001b\\" + location + "\u001b]8;;\u001b\\";
            }
            return location;
        }

        private static String toCircledNumber(int number) {
            if (number >= 1 && number <= 20 && canEncode((char) ('\u2460' + number - 1))) {
                return String.valueOf((char) ('\u2460' + number - 1));
            } else {
                return "(" + number + ")";
            }
        }

        /**
         * Stop the progress message and the spinner.
         */
        private synchronized void stopDynamicMessages() {
            if (progress != null) {
                progress.dispose();
                progress = null;
            }
            if (spinner != null) {
                spinner.dispose();
                spinner = null;
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
         * {@inheritDoc}
         * <p>
         * If the terminal can display ANSI escape sequences and the user is interacting with a
         * terminal, the user can select an item with the arrow keys and the enter key. Otherwise
         * the user must input the number of the item.
         * </p>
         */
        @Override
        protected int select(String question, List<String> names) {
            if (selectorAvailable()) {
                try (Terminal terminal = terminal()) {
                    if (terminal.isEnabled()) {
                        return browse(terminal, question, names);
                    }
                }
            }
            return super.select(question, names);
        }

        /**
         * Check whether the interactive selector is available or not.
         * 
         * @return <code>true</code> if the user can select an item with the arrow keys.
         */
        protected boolean selectorAvailable() {
            if (disableANSI || disableTrace) {
                return false;
            }
            if (hasPreparedAnswers()) { // the user has prepared answers.
                return false;
            }
            if (System.console() == null) { // the input or the output is redirected.
                return false;
            }
            String term = System.getenv("TERM");

            return Platform.isWindows() || (term != null && term.equals("dumb") == false);
        }

        /**
         * Open the terminal. A user interface which can simulate the terminal can override this
         * method.
         * 
         * @return A terminal.
         */
        protected Terminal terminal() {
            return Terminal.raw();
        }

        /**
         * <p>
         * Show an interactive item selector and return the index of the selected item.
         * </p>
         * <p>
         * The user moves the cursor by the arrow keys and decides the item by the enter key. The
         * terminal is switched to the raw mode, so the arrow key is delivered immediately. To keep
         * the compatibility with the other user interfaces, the user can also input the number of
         * the item.
         * </p>
         * 
         * @param terminal A terminal on the raw mode.
         * @param question Your question message.
         * @param names A list of displayable item names.
         * @return A 1-based index of the selected item.
         */
        private int browse(Terminal terminal, String question, List<String> names) {
            int cursor = 0;
            StringBuilder input = new StringBuilder();
            String notice = null;

            showList(question, names, cursor);

            while (true) {
                int key = readKey(terminal);

                if (key == -1) { // the input is closed, choose the current item.
                    standardOutput.println();
                    return cursor + 1;
                }

                if (key == 3 || key == 4) { // Ctrl+C (ETX) or Ctrl+D (EOT)
                    standardOutput.println();
                    throw bee.Bee.Abort;
                }

                if (key == KEY_UP) {
                    if (cursor > 0) {
                        showItem(names, cursor--, false);
                        showItem(names, cursor, true);
                    }
                    notice = null;
                } else if (key == KEY_DOWN) {
                    if (cursor < names.size() - 1) {
                        showItem(names, cursor++, false);
                        showItem(names, cursor, true);
                    }
                    notice = null;
                } else if (key == '\r' || key == '\n') {
                    String text = input.toString().trim();

                    if (text.isEmpty()) { // the user pushed the enter key.
                        standardOutput.println();
                        return cursor + 1;
                    }

                    int number = number(text, names.size());

                    if (number != -1) { // the user inputted a number.
                        standardOutput.println();
                        return number;
                    }

                    // Show the error on the hint line so that no extra line breaks the layout.
                    notice = "Invalid input, please retry.";
                    input.setLength(0);
                } else if (key == '\b' || key == 127) { // backspace
                    if (input.length() > 0) {
                        input.setLength(input.length() - 1);
                    }
                    notice = null;
                } else if ('0' <= key && key <= '9') {
                    input.append((char) key);
                    notice = null;
                } else {
                    continue;
                }

                showHint(input, notice);
            }
        }

        /** The pseudo key code of the up arrow. */
        private static final int KEY_UP = -2;

        /** The pseudo key code of the down arrow. */
        private static final int KEY_DOWN = -3;

        /**
         * Read one key from the terminal. The arrow key is delivered as a sequence of the escape
         * character, the bracket and the letter.
         * 
         * @param terminal A terminal on the raw mode.
         * @return A read key, <code>-1</code> on EOF, {@link #KEY_UP} or {@link #KEY_DOWN} for the
         *         arrow keys.
         */
        private static int readKey(Terminal terminal) {
            int first = terminal.read();

            if (first != '\u001b') {
                return first;
            }

            // The escape sequence may be delivered in a few bytes.
            int second = terminal.read();

            if (second == '[' || second == 'O') {
                int third = terminal.read();

                if (third == 'A') {
                    return KEY_UP;
                }
                if (third == 'B') {
                    return KEY_DOWN;
                }
            }

            // Not a cursor movement, ignore the sequence.
            return 0;
        }

        /**
         * Show the numbered item list.
         * 
         * @param question Your question message.
         * @param names A list of displayable item names.
         * @param cursor A current cursor position.
         */
        private synchronized void showList(String question, List<String> names, int cursor) {
            stopDynamicMessages(); // the animated message breaks the selector.

            write(stain(question, "76"), true);

            for (int i = 0; i < names.size(); i++) {
                standardOutput.println(item(names, i, i == cursor));
            }

            // The hint line is always placed below the list. The cursor stays below the hint.
            standardOutput.println("  " + ARROW + " to move, Enter to select, or input a number.");
            standardOutput.flush();
        }

        /**
         * Rewrite the single item line. The cursor is expected to be below the list.
         * 
         * @param names A list of displayable item names.
         * @param index An index of the item to rewrite.
         * @param marked Whether the item is selected by the cursor or not.
         */
        private synchronized void showItem(List<String> names, int index, boolean marked) {
            int above = names.size() - index + 1; // the items below and the hint line

            standardOutput.print(PREFIX + above + "A"); // move to the item line
            standardOutput.print("\r" + PREFIX + "2K"); // go to the head and erase the line
            standardOutput.print(item(names, index, marked));
            standardOutput.print(PREFIX + above + "B"); // move back below the list
            standardOutput.print("\r");
            standardOutput.flush();
        }

        /**
         * Rewrite the hint line with the current input.
         * 
         * @param input A current input.
         */
        private synchronized void showHint(StringBuilder input, String notice) {
            standardOutput.print(PREFIX + "1A"); // move to the hint line
            standardOutput.print("\r" + PREFIX + "2K"); // go to the head and erase the line

            if (notice != null) {
                standardOutput.print(stain(notice, "227"));
            } else {
                standardOutput.print("  " + ARROW + " to move, Enter to select, or input a number.");
                if (input.length() > 0) {
                    standardOutput.print("  [" + stain(input.toString(), "76") + "]");
                }
            }

            standardOutput.print(PREFIX + "1B"); // move back below the list
            standardOutput.print("\r");
            standardOutput.flush();
        }

        /**
         * Build the single item line.
         * 
         * @param names A list of displayable item names.
         * @param index An index of the item.
         * @param marked Whether the item is selected by the cursor or not.
         * @return A line.
         */
        private static String item(List<String> names, int index, boolean marked) {
            int width = String.valueOf(names.size()).length();
            String number = String.format("%" + width + "d", index + 1);

            // Keep the marker column on every line so the items stay aligned.
            if (marked) {
                return "  " + stain(MARKER + "[" + number + "] " + names.get(index), "76");
            }
            return "  " + MARKER_BLANK + "[" + number + "] " + names.get(index);
        }

        /**
         * Parse the user input as the number of an item.
         * 
         * @param input A user input.
         * @param size A number of the selectable items.
         * @return A 1-based number of the item, or <code>-1</code> if the input is not a valid
         *         number.
         */
        private static int number(String input, int size) {
            try {
                int number = Integer.parseInt(input);

                return 1 <= number && number <= size ? number : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
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
                standardOutput.println(stain(DECORATION + "   " + command.replace(":", " : ") + "   " + DECORATION, "75"));
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
         * Select a unicode glyph or its ASCII fallback depending on the console charset.
         * 
         * @param unicode A preferred text.
         * @param ascii An ASCII fallback.
         * @return A displayable text.
         */
        private static String glyph(String unicode, String ascii) {
            return canEncode(unicode) ? unicode : ascii;
        }

        /**
         * Select a unicode mark or its ASCII fallback depending on the console charset.
         * 
         * @param mark A preferred mark.
         * @param original An original marker used by the old format.
         * @return A displayable mark.
         */
        private static String mark(String mark, String original) {
            return canEncode(mark) ? mark : original;
        }

        /**
         * Check that the console can encode the specified text or not.
         * 
         * @param text A text to check.
         * @return A result.
         */
        private static boolean canEncode(String text) {
            return CONSOLE.newEncoder().canEncode(text);
        }

        /**
         * Check that the console can encode the specified character or not.
         * 
         * @param character A character to check.
         * @return A result.
         */
        private static boolean canEncode(char character) {
            return CONSOLE.newEncoder().canEncode(character);
        }

        /**
         * Build a horizontal rule.
         * 
         * @param width A line width.
         * @return A rule.
         */
        private static String rule(int width) {
            return RULE.repeat(Math.max(1, width));
        }

        /**
         * Count the number of the terminal lines which the specified message occupies. Long lines
         * wrap on the terminal and must be counted as multiple lines.
         * 
         * @param message A message.
         * @return A line count.
         */
        private static int lines(String message) {
            int count = 0;
            for (String line : message.split("\r\n|\r|\n", -1)) {
                count += Math.max(1, (displayWidth(line) + WIDTH - 1) / WIDTH);
            }
            return Math.max(1, count);
        }

        /**
         * Calculate the display width of the text. Full-width characters, including CJK and emoji,
         * occupy two columns while combining marks occupy none.
         * 
         * @param text A text.
         * @return A display width.
         */
        private static int displayWidth(CharSequence text) {
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                width += charWidth(text.charAt(i));
            }
            return width;
        }

        /**
         * Calculate the display width of a character.
         * 
         * @param c A character.
         * @return A display width.
         */
        private static int charWidth(char c) {
            if (c == 0 || c == '\r') {
                return 0;
            }
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                return 0;
            }
            if (c < 0x1100) {
                return 1;
            }
            // The typical full-width and emoji ranges.
            if (c >= 0x1100 && (c <= 0x115f || (c >= 0x2e80 && c <= 0xa4cf) || (c >= 0xac00 && c <= 0xd7a3) || (c >= 0xf900 && c <= 0xfaff) || (c >= 0xfe30 && c <= 0xfe4f) || (c >= 0xff00 && c <= 0xff60) || (c >= 0xffe0 && c <= 0xffe6) || (c >= 0x20000 && c <= 0x3fffd))) {
                return 2;
            }
            return 1;
        }

        /**
         * Resolve the terminal width from the system property, the environment variable or the
         * default value.
         * 
         * @return A terminal width.
         */
        private static int resolveWidth() {
            for (String name : new String[] {"bee.width", "COLUMNS"}) {
                String value = name.startsWith("bee.") ? System.getProperty(name) : System.getenv(name);
                if (value != null) {
                    try {
                        int width = Integer.parseInt(value.trim());
                        if (0 < width) {
                            return width;
                        }
                    } catch (NumberFormatException e) {
                        // ignore and try the next source
                    }
                }
            }
            return 80;
        }

        /**
         * Resolve whether the ANSI escape sequence is available or not. The user can force the
         * decision by the [bee.color] system property.
         * 
         * @return A result.
         */
        private static boolean resolveColor() {
            String preference = System.getProperty("bee.color");

            if (preference != null) {
                switch (preference.trim().toLowerCase()) {
                case "always":
                case "true":
                case "on":
                    return false; // enable color

                case "never":
                case "false":
                case "off":
                    return true; // disable color

                default:
                    break;
                }
            }

            if (Platform.isJitPack()) {
                return true;
            }

            // https://no-color.org
            String noColor = System.getenv("NO_COLOR");
            if (noColor != null && noColor.isEmpty() == false) {
                return true;
            }

            // An explicit request for color.
            for (String name : new String[] {"FORCE_COLOR", "CLICOLOR_FORCE"}) {
                String force = System.getenv(name);
                if (force != null && force.isEmpty() == false && force.equals("0") == false) {
                    return false;
                }
            }

            // A dumb terminal can not render the escape sequences.
            if ("dumb".equals(System.getenv("TERM"))) {
                return true;
            }

            // When the output is redirected, the escape sequences only pollute the log.
            if (System.console() == null) {
                return true;
            }

            // The legacy Windows console doesn't render the virtual terminal sequences.
            if (Platform.isWindows() && isVirtualTerminalSupported() == false) {
                return true;
            }
            return false;
        }

        /**
         * Check whether the Windows console supports the virtual terminal sequences or not.
         * 
         * @return A result.
         */
        private static boolean isVirtualTerminalSupported() {
            if (System.getenv("WT_SESSION") != null || System.getenv("WT_PROFILE_ID") != null) {
                return true; // Windows Terminal
            }
            if (System.getenv("ANSICON") != null || "ON".equals(System.getenv("ConEmuANSI"))) {
                return true;
            }
            String term = System.getenv("TERM");
            if (term != null && term.startsWith("xterm")) {
                return true;
            }
            String program = System.getenv("TERM_PROGRAM");
            if (program != null && (program.contains("vscode") || program.contains("iTerm"))) {
                return true;
            }
            return System.getenv("WEZTERM_EXECUTABLE") != null || System.getenv("ALACRITTY_LOG") != null;
        }

        /**
         * Check whether the terminal supports the OSC8 hyper links or not.
         * 
         * @return A result.
         */
        private static boolean isHyperlinkSupported() {
            if (disableANSI) {
                return false;
            }
            if ("1".equals(System.getenv("BEE_HYPERLINK"))) {
                return true; // force
            }
            if ("0".equals(System.getenv("BEE_HYPERLINK"))) {
                return false;
            }

            for (String name : new String[] {"WT_SESSION", "WEZTERM_EXECUTABLE", "VTE_VERSION", "KITTY_WINDOW_ID", "ALACRITTY_LOG"}) {
                if (System.getenv(name) != null) {
                    return true;
                }
            }
            String program = System.getenv("TERM_PROGRAM");
            if (program != null && (program.contains("vscode") || program.contains("iTerm") || program.contains("WezTerm"))) {
                return true;
            }
            return "iTerm.app".equals(program) || "Apple_Terminal".equals(program);
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
                CommandLineUserInterface.this.write(new String(b, Platform.Encoding), false);
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
                CommandLineUserInterface.this.write(new String(new byte[] {(byte) b}, Platform.Encoding), false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(byte[] buf, int off, int len) {
                // Javac requires a fully qualified method call, so I had no choice.
                CommandLineUserInterface.this.write(new String(buf, off, len, Platform.Encoding), false);
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