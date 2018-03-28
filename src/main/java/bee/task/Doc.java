/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import bee.Platform;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.Process;
import kiss.I;

/**
 * @version 2017/01/16 14:28:51
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
        command.add("-verbose");

        // output
        command.add("-d");
        command.add(output.toAbsolutePath().toString());

        // encoding
        command.add("-encoding");
        command.add("UTF-8");

        // lint
        command.add("-Xdoclint:none");

        // external links
        command.add("-link");
        command.add("http://docs.oracle.com/javase/8/docs/api");

        // sourcepath
        // command.add("-sourcepath");
        // command.add("src/main/java");
        //
        // classpath
        command.add("-classpath");
        command.add(project.getDependency(Scope.Compile)
                .stream()
                // .filter(v -> !v.toString().contains("sinobu"))
                .map(library -> library.getLocalJar().toString())
                .collect(Collectors.joining(File.pathSeparator)));

        // java sources
        // root: for (PathPattern sources : project.getSourceSet()) {
        // for (Path path : sources.list("**.java")) {
        // command.add(path.toString());
        // break root;
        // }
        // }
        I.signal(project.getSourceSet()).flatIterable(s -> s.list("**.java")).map(Path::toString).to(command::add);
        System.out.println(command);

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
