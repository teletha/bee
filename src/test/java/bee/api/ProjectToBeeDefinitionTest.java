/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;

import bee.Bee;
import bee.BlinkProject;
import bee.Platform;
import bee.util.JavaCompiler;
import kiss.I;
import psychopath.Locator;

class ProjectToBeeDefinitionTest {

    @Test
    void checkFullSource() {
        License license = License.BSD;

        BlinkProject project = new BlinkProject();
        project.product("GROUP", "PRODUCT", "1.0");
        project.license(license);

        List<String> lines = project.toBeeDefinition();

        assert lines.get(0).equals("/*");
        assert lines.get(1).equals(" * Copyright (C) " + Year.now() + " The PRODUCT Development Team");
        assert lines.get(2).equals(" *");
        assert lines.get(3).equals(" * Licensed under the " + license.name + " License (the \"License\");");
        assert lines.get(4).equals(" * you may not use this file except in compliance with the License.");
        assert lines.get(5).equals(" * You may obtain a copy of the License at");
        assert lines.get(6).equals(" *");
        assert lines.get(7).equals(" *          " + license.uri);
        assert lines.get(8).equals(" */");
        assert lines.get(9).equals("import static " + License.class.getName() + ".*;");
        assert lines.get(10).equals("");
        assert lines.get(11).equals("public class Project extends bee.api.Project {");
        assert lines.get(12).equals("    {");
        assert lines.get(13).equals("        product(\"GROUP\", \"PRODUCT\", \"1.0\");");
        assert lines.get(14).equals("        license(" + license.name + ");");
        assert lines.get(15).equals("    }");
        assert lines.get(16).equals("}");
        assert lines.size() == 17;
    }

    @Test
    void names() {
        BlinkProject project = new BlinkProject();
        project.product("GROUP", "PRODUCT", "1.5");

        Project compiled = compileProject(project);
        assert compiled.getGroup().equals("GROUP");
        assert compiled.getProduct().equals("PRODUCT");
        assert compiled.getVersion().equals("1.5");
    }

    @Test
    void license() {
        BlinkProject project = new BlinkProject();
        project.license(License.BSD);

        Project compiled = compileProject(project);
        assert compiled.license() == License.BSD;
    }

    @Test
    void licenseDefault() {
        BlinkProject project = new BlinkProject();

        Project compiled = compileProject(project);
        assert compiled.license() == License.MIT;
    }

    @Test
    void versionControlSystem() {
        BlinkProject project = new BlinkProject();
        project.versionControlSystem("http://github.com/owner/repo");

        Project compiled = compileProject(project);
        assert compiled.getVersionControlSystem().uri().equals("http://github.com/owner/repo");
    }

    @Test
    void versionControlSystemEmpty() {
        BlinkProject project = new BlinkProject();
        project.versionControlSystem("");

        Project compiled = compileProject(project);
        assert compiled.getVersionControlSystem() == null;
    }

    /**
     * Compile project definition in memory.
     * 
     * @param project
     * @return
     */
    private Project compileProject(BlinkProject project) {
        List<String> lines = project.toBeeDefinition();

        ErrorListener listener = new ErrorListener();

        try {
            ClassLoader loader = JavaCompiler.with()
                    .addClassPath(Locator.locate(Bee.class))
                    .addSource("Project", lines.stream().collect(Collectors.joining(Platform.EOL)))
                    .setListener(listener)
                    .compile();

            return (Project) loader.loadClass("Project").getConstructor().newInstance();
        } catch (Error e) {
            // for compile error
            for (Diagnostic<? extends JavaFileObject> error : listener) {
                int num = (int) error.getLineNumber() - 1;
                lines.set(num, lines.get(num) + " \t <<< " + error.getMessage(null).replaceAll("\\R", " "));
            }

            // messsage
            lines.addAll(0, List.of("Output invalid definition.", ""));

            throw new AssertionError(lines.stream().collect(Collectors.joining(Platform.EOL)), e);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Compiler message listener.
     */
    @SuppressWarnings("serial")
    private static class ErrorListener extends ArrayList<Diagnostic<? extends JavaFileObject>>
            implements DiagnosticListener<JavaFileObject> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            add(diagnostic);
        }
    }

}