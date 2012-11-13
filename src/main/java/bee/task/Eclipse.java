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
        createClasspath(project.getRoot().resolve(".classpath"));
        createProject(project.getRoot().resolve(".project"));
        createFactorypath(project.getRoot().resolve(".factorypath"));
        createAPT(project.getRoot().resolve(".settings/org.eclipse.jdt.apt.core.prefs"), new ProjectInfo(project));
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
        doc.child("name").text(project.getProduct());
        doc.child("comment").text(project.getDescription());
        doc.child("buildSpec").child("buildCommand").child("name").text("org.eclipse.jdt.core.javabuilder");
        doc.child("natures").child("nature").text("org.eclipse.jdt.core.javanature");

        // write file
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
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path))
                    .attr("output", relative(project.getTestClasses()));
        }

        // sources
        for (Path path : project.getSources()) {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path))
                    .attr("output", relative(project.getClasses()));
        }

        // projects
        for (Path path : project.getProjectSources()) {
            doc.child("classpathentry")
                    .attr("kind", "src")
                    .attr("path", relative(path))
                    .attr("output", relative(project.getProjectClasses()));
        }

        // library
        for (Library library : project.getDependency(Scope.Test)) {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                XML child = doc.child("classpathentry").attr("kind", "lib").attr("path", jar);

                if (Files.exists(source)) {
                    child.attr("sourcepath", source);
                }
            }
        }

        // Bee API
        if (!project.equals(Bee.TOOL)) {
            for (Library lib : project.getLibrary(Bee.API.getGroup(), Bee.API.getProduct(), Bee.API.getVersion())) {
                doc.child("classpathentry").attr("kind", "lib").attr("path", lib.getJar());
            }
        }

        // Eclipse configurations
        doc.child("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER");
        doc.child("classpathentry").attr("kind", "output").attr("path", relative(project.getClasses()));

        // write file
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

        // write file
        makeFile(file, doc);
    }

    /**
     * <p>
     * Create factorypath file.
     * </p>
     * 
     * @param file
     */
    private void createAPT(Path file, Entry<String, String> option) {
        Properties properties = new Properties();
        properties.put("eclipse.preferences.version", "1");
        properties.put("org.eclipse.jdt.apt.aptEnabled", "true");
        properties.put("org.eclipse.jdt.apt.genSrcDir", "src/main/auto");
        properties.put("org.eclipse.jdt.apt.reconcileEnabled", "true");

        if (option != null) {
            properties.put("org.eclipse.jdt.apt.processorOptions/" + option.getKey(), option.getValue());
        }

        // write file
        makeFile(file, properties);
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

    /**
     * <p>
     * Locate relative path.
     * </p>
     * 
     * @param path
     * @return
     */
    private Path relative(Path path) {
        return project.getRoot().relativize(path);
    }
}
