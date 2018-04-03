/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import bee.Platform;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import kiss.I;

/**
 * @version 2018/03/29 20:46:30
 */
public class Doc extends Task {

    /** The output root directory for javadoc. */
    protected Path output;

    /**
     * <p>
     * Generate javadoc with the specified doclet.
     * </p>
     */
    @Command("Generate product javadoc.")
    public void javadoc() {
        // specify output directory
        if (output == null) {
            output = project.getOutput().resolve("api");
        }

        if (Files.exists(output) && !Files.isDirectory(output)) {
            throw new IllegalArgumentException("Javadoc output path is not directory. " + output.toAbsolutePath());
        }

        List<String> command = new CopyOnWriteArrayList();
        command.add("javadoc");

        // lint
        command.add("-Xdoclint:none");
        command.add("-Xmaxwarns");
        command.add("1");
        command.add("-Xmaxerrs");
        command.add("1");

        // output
        command.add("-d");
        command.add(output.toAbsolutePath().toString());

        // encoding
        command.add("-encoding");
        command.add(project.getEncoding().displayName());

        // format
        command.add("-html5");
        command.add("-javafx");

        // external links
        command.add("-link");
        command.add("http://docs.oracle.com/javase/8/docs/api");

        // sourcepath
        command.add("-sourcepath");
        I.signal(project.getSourceSet())
                .startWith(project.getTestSourceSet())
                .map(path -> path.base.toString())
                .scan(Collectors.joining(File.pathSeparator))
                .last()
                .to(command::add);

        // classpath
        Set<Library> dependencies = project.getDependency(Scope.Test);
        if (!dependencies.isEmpty()) {
            command.add("-classpath");
            I.signal(dependencies)
                    .map(library -> library.getLocalJar().toString())
                    .skip(path -> path.contains("sinobu"))
                    .scan(Collectors.joining(File.pathSeparator))
                    .last()
                    .to(command::add);
        }

        // java sources
        I.signal(project.getSourceSet()).flatIterable(s -> s.list("**.java")).map(Path::toString).to(command::add);

        // ToolProvider.findFirst("javadoc").ifPresent(tool -> {
        // tool.run(System.out, System.err, command.toArray(new String[command.size()]));
        // });

        // execute
        Process.with().run(command);
    }

    /**
     * @version 2012/11/09 14:37:40
     */
    private static class UIWriter extends Writer {

        private UserInterface ui;

        /**
         * @param ui
         */
        private UIWriter(UserInterface ui) {
            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            String message = String.valueOf(cbuf, off, len).trim();

            if (message.endsWith(Platform.EOL)) {
                message = message.substring(0, message.length() - Platform.EOL.length());
            }

            if (message.length() != 0) {
                ui.talk(message + "\r");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
        }
    }

    /**
     * @version 2014/07/26 21:29:15
     */
    private static class NoOperationWriter extends PrintWriter {

        /**
         * @param out
         */
        public NoOperationWriter() {
            super(new NoOperationOutputStream());
        }
    }

    /**
     * @version 2014/07/26 21:30:07
     */
    private static class NoOperationOutputStream extends OutputStream {

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int b) throws IOException {
            // ignore
        }
    }
}
