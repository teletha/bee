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

import java.nio.file.Files;
import java.nio.file.Path;

import kiss.Element;
import bee.definition.Library;
import bee.definition.Scope;

/**
 * @version 2010/04/02 3:58:58
 */
public class Eclipse extends Task {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Command(defaults = true)
    public void eclipse() {
        createClasspath(project.root.resolve("classpath.xml"));
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
                    .attr("path", path)
                    .attr("output", project.getTestClasses()));
        }

        // sources
        for (Path path : project.getSources()) {
            doc.append($("classpathentry").attr("kind", "src").attr("path", path).attr("output", project.getClasses()));
        }

        // projects
        for (Path path : project.getProjectSources()) {
            doc.append($("classpathentry").attr("kind", "src")
                    .attr("path", path)
                    .attr("output", project.getProjectClasses()));
        }

        // library
        for (Library library : project.getDependency(Scope.Test)) {
            Path jar = library.getJar();
            Path source = library.getSourceJar();

            if (Files.exists(jar)) {
                Element e = $("classpathentry").attr("kind", "var").attr("path", jar);

                if (Files.exists(source)) {
                    e.attr("sourcepath", source);
                }
                doc.append(e);
            }
        }
        doc.append($("classpathentry").attr("kind", "con").attr("path", "org.eclipse.jdt.launching.JRE_CONTAINER"));

        System.out.println(doc);

    }
}
