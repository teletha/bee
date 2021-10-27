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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

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
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Grab;
import bee.api.Scope;
import bee.util.Java;
import bee.util.Java.JVM;
import kiss.I;

@Grab(group = "org.junit.platform", module = "junit-platform-engine")
@Grab(group = "org.junit.platform", module = "junit-platform-launcher")
public class Test extends Task {

    /** The threshold time (ms) to show the prolonged test. */
    public static int showProlongedTest = 1000;

    @Command("Test product codes.")
    public void test() {
        require(Compile::source, Compile::test);

        if (project.getTestClasses().walkFile("**Test.class").first().to().isAbsent()) {
            ui.info("Test class not found.");
        } else {
            Java.with()
                    .classPath(project.getClasses())
                    .classPath(project.getTestClasses())
                    .classPath(project.getDependency(Scope.Test, Scope.Compile))
                    .classPath(Bee.class)
                    .enableAssertion()
                    .encoding(project.getEncoding())
                    .workingDirectory(project.getRoot())
                    .run(Junit.class, project.getTestClasses(), project.getOutput().directory("test-reports").create(), showProlongedTest);
        }
    }

    /**
     * 
     */
    private static final class Junit extends JVM implements TestExecutionListener {

        /** The threshold time (ns) to show the prolonged test. */
        private long showProlongedTime = Test.showProlongedTest * 1000000;

        /**
         * {@inheritDoc}
         */
        @Override
        public void process() throws Exception {
            // disable logging for Junit
            Logger global = Logger.getLogger("");
            for (Handler handler : global.getHandlers()) {
                global.removeHandler(handler);
            }

            Set<Path> classes = I.set(Path.of(args[0]));
            showProlongedTime = Long.parseLong(args[2]) * 1000000;

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClasspathRoots(classes))
                    .build();

            Summury summury = new Summury();
            LauncherFactory.create().execute(request, summury);
        }

        /**
         * 
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

            /** The test container manager. */
            private final Map<String, TestSuite> containers = new ConcurrentHashMap();

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void testPlanExecutionStarted(TestPlan testPlan) {
                showHeader();
            }

            private void showHeader() {
                ui.info(String.format("%-4s\t%-4s\t%-4s\t%-4s\t%-4s", "Run", "Fail", "Error", "Skip", "Time(sec)"));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void testPlanExecutionFinished(TestPlan testPlan) {
                if (shows) showHeader();

                ui.info(buildResult(runs, fails.size(), errors.size(), skips, times, "TOTAL (" + suites + " classes)"));
                if (fails.size() != 0 || errors.size() != 0) {
                    Fail fail = new Fail("Test has failed.");
                    // The stack trace created here is useless and should be deleted. (Since it is
                    // almost a fixed content executed in a remote JVM)
                    fail.setStackTrace(new StackTraceElement[0]);

                    buildFailure(fail, errors);
                    buildFailure(fail, fails);
                    ui.error(fail);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void executionSkipped(TestIdentifier identifier, String reason) {
                if (identifier.isContainer()) {
                    suites++;
                } else {
                    skips++;
                    containers.get(identifier.getParentId().get()).skips++;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void executionStarted(TestIdentifier identifier) {
                if (identifier.isContainer()) {
                    suites++;
                    identifier.getSource().ifPresent(source -> {
                        TestSuite container = new TestSuite(identifier);
                        container.startTime = System.nanoTime();
                        containers.put(identifier.getUniqueId(), container);
                    });
                } else {
                    runs++;
                    containers.get(identifier.getParentId().get()).runs++;
                }

            }

            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                if (identifier.isContainer()) {
                    TestSuite container = containers.get(identifier.getUniqueId());

                    identifier.getSource().ifPresent(source -> {
                        long elapsed = System.nanoTime() - container.startTime;
                        times += elapsed;

                        boolean show = 0 < container.failures || 0 < container.errors || 0 < container.skips || showProlongedTime <= elapsed;

                        if (!shows) {
                            shows = show;
                        }

                        String message = buildResult(container.runs, container.failures, container.errors, container.skips, elapsed, container.identifier
                                .getLegacyReportingName());

                        if (show) {
                            ui.info(message);
                        } else {
                            ui.trace(message);
                        }
                    });
                    containers.remove(identifier.getUniqueId());
                } else {
                    TestSuite container = containers.get(identifier.getParentId().get());

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
                        break;
                    }
                }
            }

            /**
             * Build result message.
             */
            private String buildResult(int tests, int fails, int errors, int ignores, long time, String name) {
                StringBuilder builder = new StringBuilder();
                builder.append(String
                        .format("%-4d\t%-4d\t%-4d\t%-4d\t%.3f   \t%s", tests, fails, errors, ignores, (float) time / 1000000000, name));

                if (errors != 0) {
                    builder.append("  <<<  ERROR!");
                } else if (fails != 0) {
                    builder.append("  <<<  FAILURE!");
                }
                return builder.toString();
            }

            /**
             * Build {@link Fail}.
             * 
             * @param fail A current resolver.
             * @param list A list of test results.
             */
            private void buildFailure(Fail fail, List<Failure> list) {
                for (Failure e : list) {
                    String name = e.clazz.getLegacyReportingName();
                    StackTraceElement element = e.error.getStackTrace()[0];
                    int line = element.getClassName().equals(name) ? element.getLineNumber() : 0;

                    StringBuilder message = new StringBuilder("FIX: ").append(e.name())
                            .append(" @" + line + " \t")
                            .append(e.message().trim().split("[" + Platform.EOL + "]")[0]);

                    fail.solve(message);
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
             * 
             */
            private class Failure {

                /** The container identifier. */
                private final TestIdentifier clazz;

                /** The test identifier. */
                private final TestIdentifier test;

                /** The error reuslt. */
                private final Throwable error;

                /**
                 * Data holder.
                 * 
                 * @param clazz
                 * @param test
                 * @param error
                 */
                private Failure(TestIdentifier clazz, TestIdentifier test, Throwable error) {
                    this.clazz = clazz;
                    this.test = test;
                    this.error = error;
                }

                /**
                 * Retrieve the human-readable test case's name.
                 * 
                 * @return
                 */
                private String name() {
                    String methodName = test.getDisplayName();
                    if (methodName.endsWith("()")) {
                        methodName = methodName.substring(0, methodName.length() - 2);
                    }
                    return clazz.getDisplayName() + " #" + methodName;
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