/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.jupiter.api.Test;

import kiss.I;
import psychopath.Locator;

public class EclipseCompilerTest {

    private DiagnosticListener<JavaFileObject> listener = new DiagnosticListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void report(Diagnostic diagnostic) {
            System.out.println(diagnostic.getLineNumber() + "   " + diagnostic.getColumnNumber() + "  " + diagnostic
                    .getPosition() + "   " + diagnostic
                            .getStartPosition() + "    " + diagnostic.getEndPosition() + "   " + diagnostic.getSource());
        }
    };

    private String file = "src/test/java/bee/util/Sample.java";

    @Test
    void testName() {
        List<String> options = new ArrayList();
        options.add("-d");
        options.add("ecjapi");
        options.add("-proc:none");
        options.add("-g");
        options.addAll(I.list("-encoding", "UTF-8", "-source", "16", "-target", "16", "-sourcepath", "F:\\Development\\bee"+ file));
        // options.add("src/test/java/bee/util/Sample.java");
        // [-g, -d,
        // C:\Users\Teletha\AppData\Local\Temp\psychopath\temporary2113211058\temporary3309363669\target\classes,
        // -proc:none, -encoding, UTF-8, -source, 16, -target, 16, -sourcepath,
        // C:\Users\Teletha\AppData\Local\Temp\psychopath\temporary2113211058\temporary3309363669\src\main\java\bee\sample\Interface.java]

        EclipseCompiler compiler = new EclipseCompiler();
        StandardJavaFileManager manager = compiler.getStandardFileManager(listener, Locale.getDefault(), StandardCharsets.UTF_8);

        Iterable<? extends JavaFileObject> files = manager.getJavaFileObjects(Locator.file(file).asJavaPath());
        for (JavaFileObject file : files) {
            System.out.println(file + "  " + Locator.file(file.toString()).isPresent());
        }

        System.out.println(options);
        CompilationTask task = compiler.getTask(new PrintWriter(System.out), manager, listener, options, null, files);

        Boolean call = task.call();
        System.out.println("API " + call);
    }
}
