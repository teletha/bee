/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this Path except in compliance with the License.
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
package bee.project;

import java.nio.file.Path;
import java.util.List;

import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = Singleton.class)
public class Project {

    /** The root directory. */
    public Path root;

    /** The source directory. */
    public Path source;

    /** The target directory. */
    public Path target;

    /** The source directory. */
    public List<Path> sources;

    /** The source class directory. */
    public Path sourceClasses;

    /** The test directory. */
    public List<Path> tests;

    /** The test class directory. */
    public Path testClasses;

    /** The project directory. */
    public List<Path> projects;

    /** The project class directory. */
    public Path projectClasses;

    /** The project description. */
    private String description;

    /** The initialization flag for this project. */
    private boolean initialized = false;

}
