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

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import bee.license.License;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = Singleton.class)
public class Project {

    @Question(message = "Your project name")
    private String project;

    @Question(message = "Your product name")
    private String product;

    @Question(message = "Your product version")
    private String version;

    @Question(message = "What is your product's license?")
    private License license;

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

    /**
     * <p>
     * You must override this constructer without parameters.
     * </p>
     */
    protected Project() {
    }

    /**
     * Get the project property of this {@link Bee.ProjectQuestion}.
     * 
     * @return The project property.
     */
    public String getProject() {
        return project;
    }

    /**
     * Set the project property of this {@link Bee.ProjectQuestion}.
     * 
     * @param project The project value to set.
     */
    protected void setProject(String project) {
        for (String part : project.split("\\.")) {
            if (SourceVersion.isKeyword(part)) {
                throw new IllegalArgumentException("Project Name contains Java keyword [ " + part + " ]");
            }
        }
        this.project = project;

        if (product == null) {
            this.product = project;
        }
    }

    /**
     * Get the product property of this {@link Bee.ProjectQuestion}.
     * 
     * @return The product property.
     */
    public String getProduct() {
        return product;
    }

    /**
     * Set the product property of this {@link Bee.ProjectQuestion}.
     * 
     * @param product The product value to set.
     */
    protected void setProduct(String product) {
        this.product = product;
    }

    /**
     * Get the version property of this {@link Bee.ProjectQuestion}.
     * 
     * @return The version property.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version property of this {@link Bee.ProjectQuestion}.
     * 
     * @param version The version value to set.
     */
    protected void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the license property of this {@link Project}.
     * 
     * @return The license property.
     */
    public License getLicense() {
        return license;
    }

    /**
     * Set the license property of this {@link Project}.
     * 
     * @param license The license value to set.
     */
    protected void setLicense(License license) {
        this.license = license;
    }

    /**
     * @return
     */
    Path findProjectSouce() {
        return null;
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
