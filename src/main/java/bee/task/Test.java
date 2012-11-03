/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import bee.Platform;
import bee.api.Scope;
import bee.api.Task;
import bee.tool.Java;
import bee.tool.Java.JVM;

/**
 * @version 2012/03/28 9:58:39
 */
public class Test extends Task {

    @Command("Test product code.")
    public void test() {
        Compile compile = require(Compile.class);
        compile.source();
        compile.test();

        try {
            Path report = project.getOutput().resolve("test-reports");
            Files.createDirectories(report);

            Java java = new Java();
            java.addClassPath(project.getClasses());
            java.addClassPath(project.getTestClasses());
            java.addClassPath(project.getDependency(Scope.Test));
            java.addClassPath(loadBee());
            java.addClassPath(load("junit", "junit", "4.10"));
            java.enableAssertion();
            java.setWorkingDirectory(project.getRoot());
            java.run(Junit.class, project.getTestClasses(), report);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2012/04/04 17:51:29
     */
    private static final class Junit extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean process() {
            Path classes = I.locate(args[0]);
            List<Path> tests = I.walk(classes, "**Test.class");

            if (tests.isEmpty()) {
                ui.talk("Nothing to test");

                return true;
            }

            // execute test classes
            ui.title(" T E S T S");

            int runs = 0;
            int skips = 0;
            List<Failure> fails = new ArrayList();
            List<Failure> errors = new ArrayList();

            JUnitCore core = new JUnitCore();

            for (Path path : tests) {
                String fqcn = classes.relativize(path).toString();
                fqcn = fqcn.substring(0, fqcn.length() - 6).replace(File.separatorChar, '.');

                try {
                    ui.talk("Running ", fqcn);

                    Result result = core.run(Class.forName(fqcn));
                    List<Failure> failures = result.getFailures();
                    List<Failure> fail = new ArrayList();
                    List<Failure> error = new ArrayList();

                    for (Failure failure : failures) {
                        if (failure.getException() instanceof AssertionError) {
                            fail.add(failure);
                        } else {
                            error.add(failure);
                        }
                    }

                    StringBuilder builder = new StringBuilder();
                    builder.append("Tests run: ").append(result.getRunCount());
                    builder.append(", Failures: ").append(fail.size());
                    builder.append(", Errors: ").append(error.size());
                    builder.append(", Skipped: ").append(result.getIgnoreCount());
                    builder.append(", Time elapsed: ").append((float) result.getRunTime() / 1000).append("sec");

                    if (error.size() != 0) {
                        builder.append(" <<< ERROR!");
                    } else if (fail.size() != 0) {
                        builder.append(" <<< FAILURE!");
                    }

                    ui.talk(builder.toString());

                    runs += result.getRunCount();
                    skips += result.getIgnoreCount();
                    fails.addAll(fail);
                    errors.addAll(error);
                } catch (ClassNotFoundException e) {
                    throw I.quiet(e);
                }
            }

            ui.talk(Platform.EOL, "Results :", Platform.EOL);

            showFailure("Tests in error", errors);
            showFailure("Tests in failure", fails);

            ui.talk("Tests run: ", runs, ", Failures: ", fails.size(), ", Errors: ", errors.size(), ", Skipped: ", skips);

            return fails.size() == 0 && errors.size() == 0;
        }

        /**
         * <p>
         * Show failures in detail.
         * </p>
         * 
         * @param message
         * @param failures
         */
        private void showFailure(String message, List<Failure> failures) {
            if (!failures.isEmpty()) {
                ui.talk(message + ":");
                for (Failure failure : failures) {
                    Description desc = failure.getDescription();

                    ui.talk(" ", desc.getMethodName(), "(", desc.getClassName(), ")");

                    ui.error(failure.getException());
                }
                ui.talk("");
            }
        }
    }
}
