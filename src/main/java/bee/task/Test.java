/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import bee.Bee;
import bee.Fail;
import bee.api.Command;
import bee.api.Scope;
import bee.api.Task;
import bee.util.Java;
import bee.util.Java.JVM;
import kiss.I;

/**
 * @version 2018/03/31 22:00:15
 */
public class Test extends Task {

    @Command("Test product codes.")
    public void test() {
        // require(Compile::source);
        require(Compile::source, Compile::test);

        Java.with()
                .classPath(project.getClasses())
                .classPath(project.getTestClasses())
                .classPath(project.getDependency(Scope.Test, Scope.Compile))
                .classPath(Bee.class)
                .enableAssertion()
                .encoding(project.getEncoding())
                .workingDirectory(project.getRoot())
                .run(Junit.class, project.getTestClasses(), project.getOutput().directory("test-reports").create());
    }

    /**
     * @version 2018/03/31 21:50:18
     */
    private static final class Junit extends JVM implements TestExecutionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void process() throws Exception {
            Set<Path> classes = I.set(Path.of(args[0]));

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClasspathRoots(classes))
                    .build();
            LauncherFactory.create().execute(request, new Summury());
        }

        /**
         * @version 2018/03/31 19:10:18
         */
        private class Summury implements TestExecutionListener {

            /** The number of test suite. */
            private int suites = 0;

            /** The number of runed tests. */
            private int runs = 0;

            /** The number of skipped tests. */
            private int skips = 0;

            /** The record of failed tests. */
            private List<Failure> fails = new ArrayList();

            /** The record of errored tests. */
            private List<Failure> errors = new ArrayList();

            /** The flag. */
            private boolean shows = false;

            /** The elapsed time. */
            private long times = 0;

            /** The current test container. */
            private TestSuite container;

            /**
             * {@inheritDoc}
             */
            @Override
            public void testPlanExecutionStarted(TestPlan testPlan) {
                ui.talk("Run\t\tFail\t\tError \tSkip\t\tTime(sec)");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void testPlanExecutionFinished(TestPlan testPlan) {
                if (shows) ui.talk("Run\t\tFail\t\tError \tSkip\t\tTime(sec)");

                ui.talk(buildResult(runs, fails.size(), errors.size(), skips, times, "TOTAL (" + suites + " classes)"));
                if (fails.size() != 0 || errors.size() != 0) {
                    Fail failure = new Fail("Test has failed.");
                    buildFailure(failure, errors);
                    buildFailure(failure, fails);
                    throw failure;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void executionSkipped(TestIdentifier identifier, String reason) {
                if (identifier.isContainer()) {
                    suites++;
                } else {
                    skips++;
                    container.skips++;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void executionStarted(TestIdentifier identifier) {
                if (identifier.isContainer()) {
                    suites++;
                    identifier.getSource().ifPresent(source -> {
                        container = new TestSuite(identifier);
                        container.startTime = System.nanoTime();
                    });
                } else {
                    runs++;
                    container.runs++;
                }

            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                if (identifier.isContainer()) {
                    identifier.getSource().ifPresent(source -> {
                        long elapsed = System.nanoTime() - container.startTime;
                        times += elapsed;

                        boolean show = 0 < container.failures || 0 < container.errors || 0 < container.skips;

                        if (!shows) {
                            shows = show;
                        }

                        ui.talk(buildResult(container.runs, container.failures, container.errors, container.skips, elapsed, container.identifier
                                .getLegacyReportingName()) + (show ? "" : "\r"));
                    });
                } else {
                    switch (result.getStatus()) {
                    case SUCCESSFUL:
                        break;

                    case FAILED:
                        Failure failure = new Failure(container.identifier, identifier, result.getThrowable().get());

                        if (failure.error instanceof AssertionError) {
                            fails.add(failure);
                            container.failures++;
                        } else {
                            errors.add(failure);
                            container.errors++;
                        }
                        break;

                    case ABORTED:
                        container.aborts++;
                        break;
                    }
                }
            }

            /**
             * <p>
             * Build result message.
             * </p>
             */
            private String buildResult(int tests, int fails, int errors, int ignores, long time, String name) {
                StringBuilder builder = new StringBuilder();
                builder.append(String
                        .format("%-8d\t%-8d\t%-8d\t%-8d\t%.3f\t\t\t%s", tests, fails, errors, ignores, (float) time / 1000000000, name));

                if (errors != 0) {
                    builder.append("  <<<  ERROR!");
                } else if (fails != 0) {
                    builder.append("  <<<  FAILURE!");
                }
                return builder.toString();
            }

            /**
             * <p>
             * Build {@link Fail}.
             * </p>
             * 
             * @param failure A current resolver.
             * @param list A list of test results.
             */
            private void buildFailure(Fail failure, List<Failure> list) {
                for (Failure fail : list) {
                    String className = fail.container.getLegacyReportingName();
                    StackTraceElement element = fail.error.getStackTrace()[0];
                    int line = element.getClassName().equals(className) ? element.getLineNumber() : 0;

                    StringBuilder builder = new StringBuilder();
                    builder.append("Fix ").append(fail.container.getDisplayName());
                    if (line != 0) builder.append(":").append(line);
                    builder.append("#")
                            .append(fail.identifier.getDisplayName())
                            .append("\r\n  >>>  ")
                            .append(fail.message().trim().split("[\\r\\n]")[0]);

                    failure.solve(builder);
                }
            }

            /**
             * @version 2018/03/31 19:53:27
             */
            private class TestSuite {

                /** The container identifier. */
                private final TestIdentifier identifier;

                /** The number of runed tests. */
                private int runs;

                /** The number of aborted tests. */
                private int aborts;

                /** The number of skiped tests. */
                private int skips;

                /** The number of failed tests. */
                private int failures;

                /** The number of errored tests. */
                private int errors;

                /** The start time. */
                private long startTime;

                /**
                 * @param identifier
                 */
                private TestSuite(TestIdentifier identifier) {
                    this.identifier = identifier;
                }
            }

            /**
             * @version 2018/03/31 20:07:31
             */
            private class Failure {

                /** The container identifier. */
                private final TestIdentifier container;

                /** The test identifier. */
                private final TestIdentifier identifier;

                /** The error reuslt. */
                private final Throwable error;

                /**
                 * @param container
                 * @param identifier
                 * @param error
                 */
                private Failure(TestIdentifier container, TestIdentifier identifier, Throwable error) {
                    this.container = container;
                    this.identifier = identifier;
                    this.error = error;
                }

                /**
                 * Retrieve error message.
                 * 
                 * @return
                 */
                private String message() {
                    String message = error.getLocalizedMessage();

                    return message == null ? "" : message;
                }
            }
        }
    }
}
