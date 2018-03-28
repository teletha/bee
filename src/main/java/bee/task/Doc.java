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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.DocumentationTool.Location;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import bee.Platform;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
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
        try {
            // specify output directory
            if (output == null) {
                output = project.getOutput().resolve("api");
            }

            if (Files.exists(output) && !Files.isDirectory(output)) {
                throw new IllegalArgumentException("Javadoc output path is not directory. " + output.toAbsolutePath());
            }

            DocumentationTool doc = ToolProvider.getSystemDocumentationTool();
            StandardJavaFileManager manager = doc.getStandardFileManager(null, Locale.getDefault(), StandardCharsets.UTF_8);

            List<String> options = new CopyOnWriteArrayList();

            // classpath
            manager.setLocation(StandardLocation.CLASS_PATH, I.signal(project.getDependency(Scope.Compile))
                    .map(library -> library.getLocalJar().toFile())
                    .toList());

            // sourcepath
            manager.setLocation(StandardLocation.CLASS_PATH, I.signal(project.getSourceSet())
                    .map(d -> d.base.toAbsolutePath().toFile())
                    .toList());

            // output
            manager.setLocation(Location.DOCUMENTATION_OUTPUT, Collections.singleton(output.toAbsolutePath().toFile()));

            // encoding
            options.add("-encoding");
            options.add("UTF-8");

            // java sources
            // for (PathPattern sources : project.getSourceSet()) {
            // for (Path path : sources.list("**.java")) {
            // options.add(path.toString());
            // }
            // }
            List<File> list = I.signal(project.getSourceSet()).flatIterable(d -> d.list("**.java")).map(p -> p.toFile()).toList();

            // external links
            options.add("-link");
            options.add("http://docs.oracle.com/javase/8/docs/api");

            PrintWriter writer = new PrintWriter(I.make(UIWriter.class));
            DocumentationTask task = doc.getTask(writer, manager, null, null, options, manager.getJavaFileObjectsFromFiles(list));
            Boolean result = task.call();
            // int result = Main.execute("", new NoOperationWriter(), writer, writer,
            // docletClass.getName(), docletClass.getClassLoader(), options
            // .toArray(new String[options.size()]));

            if (result) {
                // success
            } else {
                // fail
                ui.error("Javadoc command is failed.");
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
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
