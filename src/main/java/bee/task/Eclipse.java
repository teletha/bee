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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import kiss.I;
import kiss.XML;
import kiss.model.ClassUtil;
import bee.Bee;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.task.AnnotationProcessor.ProjectInfo;

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
        Map<String, Object> options = new HashMap();
        options.put(ProjectInfo.class.getName(), new ProjectInfo(project));

        createClasspath(project.getRoot().resolve(".classpath"));
        createProject(project.getRoot().resolve(".project"));
        createFactorypath(project.getRoot().resolve(".factorypath"));
        createAPT(project.getRoot().resolve(".settings/org.eclipse.jdt.apt.core.prefs"), options);
        createJDT(project.getRoot().resolve(".settings/org.eclipse.jdt.core.prefs"));

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
        XML doc = I.xml("projectDescription");
        doc.append(I.xml("name").text(project.getProduct()));
        doc.append(I.xml("comment").text(project.getDescription()));
        doc.append(I.xml("buildSpec").append(I.xml("buildCommand").append(I.xml("name")
                .text("org.eclipse.jdt.core.javabuilder"))));
        doc.append(I.xml("natures").append(I.xml("nature").text("org.eclipse.jdt.core.javanature")));

        makeFile(file, doc);
    }

    /**
     * <p>
     * Create classpath file.
     * </p>
     * 
     * @param file
     */
    private void createClasspath(Path file) {
        XML doc = I.xml("classpath");

        // tests
        for (Path path : project.getTestSources()) {
            doc.append(I.xml("classpathentry")
                    .attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getTestClasses())));
        }

        // sources
        for (Path path : project.getSources()) {
            doc.append(I.xml("classpathentry")
                    .attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getClasses())));
        }

        // projects
        for (Path path : project.getProjectSources()) {
            doc.append(I.xml("classpathentry")
                    .attr("kind", "src")
                    .attr("path", project.getRoot().relativize(path))
                    .attr("output", project.getRoot().relativize(project.getProjectClasses())));
        }

        // library
        for (Library library : project.getDependency(Scope.Test)) {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                XML e = I.xml("classpathentry").attr("kind", "lib").attr("path", jar);

                if (Files.exists(source)) {
                    e.attr("sourcepath", source);
                }
                doc.append(e);
            }
        }

        for (Library lib : project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion())) {
            doc.append(I.xml("classpathentry").attr("kind", "lib").attr("path", lib.getJar()));
        }
        doc.append(I.xml("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER"));

        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createFactorypath(Path file) {
        XML doc = I.xml("factorypath");
        doc.child("factorypathentry")
                .attr("kind", "EXTJAR")
                .attr("id", ClassUtil.getArchive(Bee.class))
                .attr("enabled", true)
                .attr("runInBatchMode", false);

        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createAPT(Path file, Map<String, Object> options) {
        List<String> doc = new ArrayList();
        doc.add("eclipse.preferences.version=1");
        doc.add("org.eclipse.jdt.apt.aptEnabled=true");
        doc.add("org.eclipse.jdt.apt.genSrcDir=src/main/auto");
        doc.add("org.eclipse.jdt.apt.reconcileEnabled=true");

        if (options != null) {
            for (Entry<String, Object> entry : options.entrySet()) {
                doc.add("org.eclipse.jdt.apt.processorOptions/" + entry.getKey() + "=" + entry.getValue());
            }
        }
        makeFile(file, doc);
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
            if (Files.notExists(file)) {
                makeFile(file, "");
            }

            Properties doc = new Properties();
            doc.load(Files.newInputStream(file));

            doc.put("org.eclipse.jdt.core.compiler.processAnnotations", "enabled");

            doc.store(Files.newOutputStream(file), "");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
