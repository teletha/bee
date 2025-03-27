/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.FileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import bee.Depend;
import bee.Fail;
import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.util.Inputs;
import javadng.page.Javadoc;
import javadng.repository.CodeRepository;
import jdk.javadoc.doclet.Doclet;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;

@SuppressWarnings("serial")
public interface Doc extends Task {

    /**
     * Generate javadoc with the specified doclet.
     */
    @Command(defaults = true, value = "Generate product javadoc.")
    default Directory javadoc() {
        Directory output = project().getOutput().directory("api").create();

        Class<? extends Doclet> doclet = null;
        List<String> options = new ArrayList();
        options.add("--release");
        options.add(Inputs.normalize(project().getJavaSourceVersion()));
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
        options.add("https://docs.oracle.com/en/java/javase/" + Inputs.normalize(project().getJavaSourceVersion()) + "/docs/api/");

        DocumentationTool doc = ToolProvider.getSystemDocumentationTool();
        try (Listener listener = new Listener();
                StandardJavaFileManager manager = doc.getStandardFileManager(null, Locale.getDefault(), project().getEncoding())) {
            manager.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT, I.list(output.asJavaPath()));
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, project().getSourceSet().map(Location::asJavaPath).toList());
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, project().getDependency(Scope.values())
                    .stream()
                    .map(lib -> lib.getLocalJar().asJavaPath())
                    .collect(Collectors.toList()));

            List<Path> sourceFiles = project().getSourceSet().flatMap(dir -> dir.walkFile("**.java")).map(File::asJavaPath).toList();

            if (sourceFiles.isEmpty()) {
                ui().info("No documentation will be generated because the source files don't exist in the following directories.");
                ui().info(project().getSourceSet().toList());
                return output;
            }

            DocumentationTask task = doc
                    .getTask(listener, manager, listener, doclet, options, manager.getJavaFileObjectsFromPaths(sourceFiles));

            if (task.call() && listener.errors.isEmpty()) {
                ui().info("Build javadoc to " + output);
            } else {
                throw new Fail("Fail building Javadoc.", listener.errors);
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
        return output;
    }

    @Command("Generate product site.")
    default void site() {
        List<Path> sourceFiles = project().getSourceSet().flatMap(dir -> dir.walkFile("**.java")).map(File::asJavaPath).toList();

        if (sourceFiles.isEmpty()) {
            ui().info("No documentation site will be generated because any document or source files don't exist in the following directories.");
            ui().info(project().getSourceSet().toList());
            return;
        }

        Listener listener = new Listener();
        Directory output = project().getOutput().directory("site");

        new Depend("com.github.teletha : javadng") {

            @Override
            public void run() {
                Javadoc.with.sources(project().getSourceSet().toList())
                        .output(output)
                        .product(project().getProduct())
                        .project(project().getGroup())
                        .version(project().getVersion())
                        .encoding(project().getEncoding())
                        .sample(project().getTestSourceSet().toList())
                        .classpath(I.signal(project().getDependency(Scope.values())).map(Library::getLocalJar).toList())
                        .repository(CodeRepository.of(project().getVersionControlSystem().toString()))
                        .listener(listener)
                        .useExternalJDKDoc()
                        .build();
            };
        };

        if (listener.errors.isEmpty()) {
            ui().info("Build site resources to " + output);
        } else {
            throw new Fail("Fail building document site.", listener.errors);
        }
    }

    class Listener extends Writer implements DiagnosticListener<FileObject>, Serializable {

        private List<String> errors = new ArrayList();

        /**
         * {@inheritDoc}
         */
        @Override
        public void report(Diagnostic<? extends FileObject> e) {
            String message = e.getMessage(null);
            if (message.startsWith("javadoc: ")) {
                message = message.substring(9);
            }

            switch (e.getKind()) {
            case ERROR:
                errors.add(message);
                ui().error(message);
                break;

            case NOTE:
                ui().trace(message);
                break;

            case WARNING:
            case MANDATORY_WARNING:
                ui().warn(message);
                break;

            case OTHER:
                ui().debug(message);
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            String message = new String(cbuf, off, len).trim();
            if (message.length() != 0) {
                ui().trace(message);
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