/*
 * The MIT License
 * <p>
 * Copyright (c) 2005, The Codehaus
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package bee.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.eclipse.jdt.internal.compiler.tool.EclipseFileManager;

import psychopath.Directory;
import psychopath.Folder;

/**
 *
 */
public class EclipseJavaCompiler {

    private Directory output;

    private Folder sources;

    public EclipseJavaCompiler(Directory output, Folder sources) {
        this.output = output;
        this.sources = sources;
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------
    boolean errorsAsWarnings = false;

    public boolean performCompile() {
        List<String> args = new ArrayList<>();
        args.add("-noExit"); // Make sure ecj does not System.exit on us 8-/
        args.add("-verbose");

        // Build settings from configuration
        args.add("-g:lines,source");

        String releaseVersion = "15";
        // EcjFailureException: Failed to run the ecj compiler: option -source is not supported when
        // --release is used
        args.add("--release");
        args.add(releaseVersion);

        args.add("-encoding");
        args.add("UTF-8");
        args.add("-parameters");
        args.add("-failOnWarning");

        // Output path
        args.add("-d");
        args.add(output.toString());

        // -- classpath
        // List<String> classpathEntries = new ArrayList<>(config.getClasspathEntries());
        // classpathEntries.add(config.getOutputLocation());
        // args.add("-classpath");
        // args.add(getPathString(classpathEntries));

        // Compile
        try {
            final List<String> messageList = new ArrayList();
            StringWriter sw = new StringWriter();
            PrintWriter devNull = new PrintWriter(sw);
            EclipseCompiler compiler = new EclipseCompiler();
            DiagnosticListener<? super JavaFileObject> messageCollector = new DiagnosticListener<JavaFileObject>() {

                @Override
                public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                    messageList.add(diagnostic.getMessage(null) + "  " + diagnostic.getCode() + "  " + diagnostic
                            .getLineNumber() + "  " + diagnostic.getSource() + "   " + diagnostic.getKind());
                }
            };

            try (EclipseFileManager manager = new EclipseFileManager(Locale.getDefault(), StandardCharsets.UTF_8)) {

                List<JavaFileObject> sources = new ArrayList();

                this.sources.walkFileWithBase("**.java").to(e -> {
                    psychopath.File classFile = output.directory(e.ⅰ.relativize(e.ⅱ.parent())).file(e.ⅱ.base() + ".class");

                    if (classFile.lastModifiedMilli() < e.ⅱ.lastModifiedMilli()) {
                        manager.getJavaFileObjects(e.ⅱ.asJavaFile()).forEach(v -> {
                            sources.add(v);
                        });
                    }
                });

                System.out.println(sources);

                System.out.println(args);
                Boolean result = compiler.getTask(devNull, manager, messageCollector, args, null, sources).call();
                System.out.println(result + "  " + messageList);
                System.out.println(sw);
            } catch (RuntimeException e) {
                throw new RuntimeException(e.getLocalizedMessage());
            }

            boolean hasError = false;
            return !hasError;
        } catch (Throwable x) {
            throw new RuntimeException(x); // sigh
        }
    }
}