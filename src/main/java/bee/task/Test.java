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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import bee.compiler.JavaCompiler;
import bee.definition.Scope;

/**
 * @version 2012/03/28 9:58:39
 */
public class Test extends Task {

    @Command(defaults = true)
    public void test() {
        // compile test codes
        JavaCompiler compiler = new JavaCompiler();
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Test));
        compiler.addSourceDirectory(project.getTestSources());
        compiler.setOutput(project.getTestClasses().resolveSibling("aaa"));
        compiler.setNoWarn();
        compiler.compile();

        // execute test classes
        ui.talk("-------------------------------------------------------");
        ui.talk(" T E S T S");
        ui.talk("-------------------------------------------------------");

        int runs = 0;
        int skips = 0;
        List<Failure> fails = new ArrayList();
        List<Failure> errors = new ArrayList();

        JUnitCore core = new JUnitCore();

        for (Path path : I.walk(project.getTestClasses(), "**Test.class")) {
            String fqcn = project.getTestClasses().relativize(path).toString();
            fqcn = fqcn.substring(0, fqcn.length() - 6).replace(File.separatorChar, '.');

            try {
                ui.talk("Running " + fqcn);

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

        ui.talk("\r\nResults :\r\n");

        showFailure("Tests in error", errors);
        showFailure("Tests in failure", fails);

        ui.talk("Tests run: " + runs + ", Failures: " + fails.size() + ", Errors: " + errors.size() + ", Skipped: " + skips);
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
                ui.talk(" %1$s (%2$s)", failure.getDescription().getMethodName(), failure.getDescription()
                        .getClassName());
            }
            ui.talk(" ");
        }
    }
}
