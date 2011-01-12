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

import java.io.File;
import java.util.List;

import ezbean.I;
import ezbean.Manageable;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = ProjectBuildProcess.class)
public abstract class Project {

    /** The base directory. */
    public File base;

    /** The target directory. */
    public File target;

    /** The source directory. */
    public File source;

    /** The source directory. */
    public List<File> sources;

    /** The source class directory. */
    public File sourceClasses;

    /** The test directory. */
    public List<File> tests;

    /** The test class directory. */
    public File testClasses;

    /** The project directory. */
    public List<File> projects;

    /** The project class directory. */
    public File projectClasses;

    /** The group id. */
    public String groupId;

    /** The artifact id. */
    public String artifactId;

    /** The version number. */
    public Version version;

    /** The project name. */
    private String name;

    /** The project description. */
    private String description;

    /** The initialization flag for this project. */
    private boolean initialized = false;

    private boolean isHost;

    /**
     * <p>
     * You must override this constructer without parameters.
     * </p>
     */
    protected Project() {
        // String artifactId = getClass().getSimpleName().toLowerCase().replace('_', '-');
        //
        // if (artifactId.endsWith("project")) {
        // artifactId = artifactId.substring(0, artifactId.length() - 7);
        // }
        //
        // this.artifactId = artifactId;
        // this.groupId = getClass().getPackage().getName().replace('_', '-');
        // this.version = new Version(getClass().getAnnotation(ProjectVersion.class));
        //
        // File archive = ClassUtil.getArchive(getClass());
        //
        // isHost = archive.isFile();
        // base = archive.isFile() ? new File("") : new File(archive, "../../");
        // source = new File(base, "src");
        // target = new File(base, "target");
        //
        // // source files
        // sources = Collections.unmodifiableList(Arrays.asList(new File(source,
        // "main").listFiles(I.make(DirectoryFilter.class))));
        // sourceClasses = new File(target, "classes");
        //
        // // test files
        // tests = Collections.unmodifiableList(Arrays.asList(new File(source,
        // "test").listFiles(I.make(DirectoryFilter.class))));
        // testClasses = new File(target, "test-classes");
        //
        // // project files
        // projects = Collections.unmodifiableList(Arrays.asList(new File(source,
        // "project").listFiles(I.make(DirectoryFilter.class))));
        // projectClasses = new File(target, "project-classes");
    }

    public final <T extends Task> T createTask(Class<T> task) {
        return null;
    }

    /**
     * <p>
     * Add source directory.
     * </p>
     * 
     * @param directoryPaths
     */
    protected final void addSource(String... directoryPaths) {
        File[] files = new File[directoryPaths.length];

        for (int i = 0; i < files.length; i++) {
            files[i] = I.locate(directoryPaths[i]);
        }

        addSource(files);
    }

    protected void addSource(File... directories) {
        checkInitialization();
    }

    protected void addTest(String... directoryPaths) {
        checkInitialization();
    }

    protected void addTest(File... directories) {
        checkInitialization();
    }

    protected void setTestOutput(String directoryPath) {
        checkInitialization();
    }

    protected void setTestOutput(File directory) {
        checkInitialization();
    }

    private void checkInitialization() {
        if (initialized) {
            throw new IllegalStateException("Project is initialized already. Don't use project setting methods.");
        }
    }
}
