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
package bee.definition;

import java.nio.file.Path;

import kiss.I;
import kiss.model.ClassUtil;
import antibug.PrivateModule;
import antibug.ReusableRule;

/**
 * @version 2010/11/19 22:51:09
 */
public class TemporaryProjectBuilder extends ReusableRule {

    /** The current testing project to expose. */
    public Project project;

    /** The root directory of the temporary project. */
    private final Path root = I.locateTemporary();

    /** The actula project class. */
    private final Class projectClass;

    /** The project classes. */
    public PrivateModule moduleForProject;

    /**
     * @param clazz
     */
    public TemporaryProjectBuilder(Class<? extends Project> clazz) {
        this.projectClass = clazz;

        // create inital project structure
        Path projectSourceDirectory = root.resolve("src/project/java");

        Path projectClassDirectory = testcaseRoot.resolve(clazz.getPackage().getName().replace('.', '/'));
        Path sourceDirectories = ClassUtil.getArchive(clazz).relativize(projectClassDirectory.getParent().getParent());
        Path actualSource = I.locate("src/test/java").resolve(sourceDirectories);

        // copy sources
        I.copy(actualSource, root.resolve("src"), "**");

        // create project

        // System.out.println();
        // Path root = testcaseDirectory;
        //
        // System.out.println(projectClassDirectory);
        //
        // moduleForProject = new PrivateModule(root.relativize(projectClassDirectory).toString(),
        // true, false);
    }

    // /**
    // * {@inheritDoc}
    // */
    // @Override
    // protected void beforeClass() throws Exception {
    // moduleForProject.load();
    // System.out.println(moduleForProject.path);
    // project = (Project) I.make(moduleForProject.convert(projectClass));
    // }

    /**
     * @version 2012/04/02 15:40:59
     */
    private static class TemporaryProject extends Project {

        /**
         * 
         */
        private TemporaryProject(Path root) {
            super(root);
        }
    }
}
