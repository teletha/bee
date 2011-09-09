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
package bee;

import java.nio.file.Path;
import java.util.List;

import javax.lang.model.SourceVersion;

import ezbean.I;
import ezbean.Manageable;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = ProjectBuildProcess.class)
public abstract class Project {

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

    /** The group id. */
    private String groupId;

    /** The artifact id. */
    private String artifactId;

    /** The version number. */
    private Version version;

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
        System.out.println(I.make(UserInterface.class));

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
        // Path archive = ClassUtil.getArchive(getClass());
        //
        // isHost = archive.isPath();
        // base = archive.isPath() ? new Path("") : new Path(archive, "../../");
        // source = new Path(base, "src");
        // target = new Path(base, "target");
        //
        // // source Paths
        // sources = Collections.unmodifiableList(Arrays.asList(new Path(source,
        // "main").listPaths(I.make(DirectoryFilter.class))));
        // sourceClasses = new Path(target, "classes");
        //
        // // test Paths
        // tests = Collections.unmodifiableList(Arrays.asList(new Path(source,
        // "test").listPaths(I.make(DirectoryFilter.class))));
        // testClasses = new Path(target, "test-classes");
        //
        // // project Paths
        // projects = Collections.unmodifiableList(Arrays.asList(new Path(source,
        // "project").listPaths(I.make(DirectoryFilter.class))));
        // projectClasses = new Path(target, "project-classes");
    }

    /**
     * @return
     */
    Path findProjectSouce() {
        return null;
    }

    /**
     * Get the groupId property of this {@link Project}.
     * 
     * @return The groupId property.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Set the groupId property of this {@link Project}.
     * 
     * @param groupId The groupId value to set.
     */
    void setGroupId(String groupId) {
        for (String part : groupId.split("\\.")) {
            if (SourceVersion.isKeyword(part)) {
                throw new IllegalArgumentException("Group Id contains Java keyword [" + part + "]");
            }
        }
        this.groupId = groupId;
    }

    /**
     * Get the artifactId property of this {@link Project}.
     * 
     * @return The artifactId property.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Set the artifactId property of this {@link Project}.
     * 
     * @param artifactId The artifactId value to set.
     */
    void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Get the version property of this {@link Project}.
     * 
     * @return The version property.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Set the version property of this {@link Project}.
     * 
     * @param version The version value to set.
     */
    void setVersion(Version version) {
        this.version = version;
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
        Path[] Paths = new Path[directoryPaths.length];

        for (int i = 0; i < Paths.length; i++) {
            Paths[i] = I.locate(directoryPaths[i]);
        }

        addSource(Paths);
    }

    protected void addSource(Path... directories) {
        checkInitialization();
    }

    protected void addTest(String... directoryPaths) {
        checkInitialization();
    }

    protected void addTest(Path... directories) {
        checkInitialization();
    }

    protected void setTestOutput(String directoryPath) {
        checkInitialization();
    }

    protected void setTestOutput(Path directory) {
        checkInitialization();
    }

    private void checkInitialization() {
        if (initialized) {
            throw new IllegalStateException("Project is initialized already. Don't use project setting methods.");
        }
    }
}
