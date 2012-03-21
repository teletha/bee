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
package bee;

import java.lang.reflect.Method;
import java.nio.file.Path;

import kiss.I;

import org.junit.Rule;

import bee.definition.Project;

import antibug.PrivateModule;
import antibug.ReusableRule;

/**
 * @version 2010/11/19 22:51:09
 */
public class DummyProject extends ReusableRule {

    /** The current testing project to expose. */
    public Project project;

    private final Class projectClass;

    /** The cache of current testing project. */
    private Project testProject;

    /** The project classes. */
    @Rule
    public final PrivateModule moduleForProject;

    /**
     * @param clazz
     */
    public DummyProject(Class<? extends Project> clazz) {
        this.projectClass = clazz;

        Path project = testcaseRoot.resolve(clazz.getPackage().getName().replace('.', '/'));
        Path root = testcaseDirectory;
        Path source = root.resolve("source");
        Path test = root.resolve("test");
        System.out.println(root.relativize(project).toString());
        moduleForProject = new PrivateModule(root.relativize(project).toString(), true, false);
    }

    /**
     * @see hub.ReusableRule#before(java.lang.reflect.Method)
     */
    @Override
    protected void before(Method method) throws Exception {
        project = I.make(moduleForProject.convert(projectClass));
    }

}
