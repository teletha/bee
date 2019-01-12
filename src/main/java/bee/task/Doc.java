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

import static javax.tools.DocumentationTool.Location.DOCUMENTATION_OUTPUT;
import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.StandardLocation.SOURCE_PATH;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import bee.Platform;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;

/**
 * @version 2018/04/04 11:22:37
 */
public class Doc extends Task {

    /** The output root directory for javadoc. */
    protected Directory output;

    /**
     * <p>
     * Generate javadoc with the specified doclet.
     * </p>
     */
    @Command("Generate product javadoc.")
    public void javadoc() {
        // specify output directory
        if (output == null) {
            output = project.getOutput().directory("api");
        }
        output.create();

        if (!output.isDirectory()) {
            throw new IllegalArgumentException("Javadoc output path is not directory. " + output.absolutize());
        }

        List<String> options = new CopyOnWriteArrayList();

        // lint
        options.add("-Xdoclint:none");
        options.add("-Xmaxwarns");
        options.add("1");
        options.add("-Xmaxerrs");
        options.add("1");

        // format
        options.add("-html5");
        options.add("-javafx");

        // external links
        options.add("-link");
        options.add("http://docs.oracle.com/javase/8/docs/api");

        try {
            DocumentationTool doc = ToolProvider.getSystemDocumentationTool();
            StandardJavaFileManager manager = doc.getStandardFileManager(null, Locale.getDefault(), StandardCharsets.UTF_8);
            manager.setLocationFromPaths(DOCUMENTATION_OUTPUT, I.list(output.asJavaPath()));
            manager.setLocationFromPaths(SOURCE_PATH, project.getSourceSet()
                    .map(Location::asJavaPath)
                    .merge(I.signal(project.getDependency(Scope.Compile))
                            .map(lib -> lib.getLocalSourceJar())
                            .take(path -> path.toString().contains("sinobu")))
                    .toList());
            manager.setLocationFromPaths(CLASS_PATH, I.signal(project.getDependency(Scope.Test))
                    .map(library -> library.getLocalJar())
                    .toList());

            DocumentationTask task = doc
                    .getTask(new UIWriter(ui), manager, null, null, options, manager.getJavaFileObjectsFromPaths(project.getSourceSet()
                            .flatMap(dir -> dir.walkFiles("**.java"))
                            .map(File::asJavaPath)
                            .toList()));

            if (task.call()) {
                ui.talk("Build javadoc : " + output);
            } else {
                ui.talk("Fail building javadoc.");
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2018/04/04 11:25:57
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
}
