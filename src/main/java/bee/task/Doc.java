/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.lang.model.SourceVersion;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Library;
import bee.api.Require;
import bee.api.Scope;
import bee.util.Inputs;
import javadng.parser.Javadoc;
import javadng.repository.Github;
import jdk.javadoc.doclet.Doclet;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;

public class Doc extends Task {

    @Command("Generate product site.")
    public void site() {
        new Require("com.github.teletha : javadng") {
            {
                javadng.repository.Repository externalRepository = project.getVersionControlSystem().map(vcs -> {
                    if (vcs.name().equals("GitHub")) {
                        return new Github(vcs.owner, vcs.repo, "master");
                    } else {
                        return null;
                    }
                }).get();

                Javadoc.with.sources(project.getSourceSet().toList())
                        .output(project.getOutput().directory("site"))
                        .product(project.getProduct())
                        .project(project.getGroup())
                        .version(project.getVersion())
                        .encoding(project.getEncoding())
                        .classpath(I.signal(project.getDependency(Scope.Compile, Scope.Provided, Scope.Test))
                                .map(Library::getLocalJar)
                                .toList())
                        .repository(externalRepository)
                        .prefix("/" + project.getProduct() + "/")
                        .listener(e -> {
                            switch (e.getKind()) {
                            case ERROR:
                                throw new Fail(e.getMessage(null));

                            case NOTE:
                                ui.trace(e.getMessage(null));
                                break;

                            case WARNING:
                            case MANDATORY_WARNING:
                                ui.warn(e.getMessage(null));
                                break;

                            case OTHER:
                                ui.debug(e.getMessage(null));
                                break;
                            }
                        })
                        .build();
            }
        };
    }

    /**
     * Generate javadoc with the specified doclet.
     */
    @Command(value = "Generate product javadoc.", defaults = true)
    public Directory javadoc() {
        // specify output directory
        Directory output = project.getOutput().directory("api").create();

        Class<? extends Doclet> doclet = null;
        List<String> options = new CopyOnWriteArrayList();
        options.add("--release");
        options.add(Inputs.normalize(SourceVersion.latest()));
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
        options.add("https://docs.oracle.com/en/java/javase/17/docs/api/");

        try {
            DocumentationTool doc = ToolProvider.getSystemDocumentationTool();
            StandardJavaFileManager manager = doc.getStandardFileManager(null, Locale.getDefault(), StandardCharsets.UTF_8);
            manager.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT, I.list(output.asJavaPath()));
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, project.getSourceSet()
                    .map(Location::asJavaPath)
                    .merge(I.signal(project.getDependency(Scope.Compile))
                            .map(lib -> lib.getLocalSourceJar().asJavaPath())
                            .take(path -> path.toString().contains("sinobu")))
                    .toList());

            DocumentationTask task = doc
                    .getTask(new UIWriter(ui), manager, null, doclet, options, manager.getJavaFileObjectsFromPaths(project.getSourceSet()
                            .flatMap(dir -> dir.walkFile("**.java"))
                            .map(File::asJavaPath)
                            .toList()));

            if (task.call()) {
                ui.info("Build javadoc : " + output);
            } else {
                ui.info("Fail building javadoc.");
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return output;
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
                ui.trace(message);
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