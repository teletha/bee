/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.task;

import static kiss.Element.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import kiss.Element;
import kiss.I;
import bee.Bee;
import bee.api.Library;
import bee.api.Scope;

/**
 * @version 2010/04/02 3:58:58
 */
public class Eclipse extends Task {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Command
    public void eclipse() {
        createClasspath(project.getRoot().resolve(".classpath"));
        ui.talk("Generate classpath file.");

        // createFactorypath(project.getRoot().resolve(".factorypath"));
        // ui.talk("Generate factorypath file.");

        createProject(project.getRoot().resolve(".project"));
        ui.talk("Generate project file.");

        // createAPT(project.getRoot().resolve(".settings/org.eclipse.jdt.apt.core.prefs"));
        // ui.talk("Generate APT preference file.");
        //
        // createJDT(project.getRoot().resolve(".settings/org.eclipse.jdt.core.prefs"));
        // ui.talk("Generate JDT preference file.");

        ui.talk("Create Eclipse configuration files.");
    }

    /**
     * <p>
     * Create project file.
     * </p>
     * 
     * @param file
     */
    private void createProject(Path file) {
        Element doc = $("projectDescription");
        doc.append($("name").text(project.getProduct()));
        doc.append($("comment").text(project.getDescription()));
        doc.append($("buildSpec").append($("buildCommand").append($("name").text("org.eclipse.jdt.core.javabuilder"))));
        doc.append($("natures").append($("nature").text("org.eclipse.jdt.core.javanature")));

        try {
            Files.write(file, doc.toString().getBytes());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Create classpath file.
     * </p>
     * 
     * @param file
     */
    private void createClasspath(Path file) {
        Element doc = $("classpath");

        // tests
        for (Path path : project.getTestSources()) {
            doc.append($("classpathentry").attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getTestClasses())));
        }

        // sources
        for (Path path : project.getSources()) {
            doc.append($("classpathentry").attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getClasses())));
        }

        // projects
        for (Path path : project.getProjectSources()) {
            doc.append($("classpathentry").attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getProjectClasses())));
        }

        // library
        for (Library library : project.getDependency(Scope.Test)) {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                Element e = $("classpathentry").attr("kind", "lib").attr("path", jar);

                if (Files.exists(source)) {
                    e.attr("sourcepath", source);
                }
                doc.append(e);
            }
        }

        for (Library lib : project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion())) {
            doc.append($("classpathentry").attr("kind", "lib").attr("path", lib.getJar()));
        }
        doc.append($("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER"));

        try {
            Files.write(file, doc.toString().getBytes());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createFactorypath(Path file) {
        Element doc = $("factorypath");

        for (Library library : project.getLibrary("npc", "bee", "0.1")) {
            doc.append($("factorypathentry").attr("kind", "EXTJAR")
                    .attr("id", library.getJar())
                    .attr("enabled", true)
                    .attr("runInBatchMode", false));
        }

        try {
            Files.write(file, doc.toString().getBytes());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createAPT(Path file) {
        List<String> doc = new ArrayList();
        doc.add("eclipse.preferences.version=1");
        doc.add("org.eclipse.jdt.apt.aptEnabled=true");
        doc.add("org.eclipse.jdt.apt.genSrcDir=.apt_generated");
        doc.add("org.eclipse.jdt.apt.reconcileEnabled=true");

        try {
            Files.write(file, doc, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createJDT(Path file) {
        try {
            Properties doc = new Properties();
            doc.load(Files.newInputStream(file));

            doc.put("org.eclipse.jdt.core.compiler.processAnnotations", "enabled");

            doc.store(Files.newOutputStream(file), "");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
