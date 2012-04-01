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
package bee.definition;

import static bee.util.Inputs.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.lang.model.SourceVersion;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;

import org.apache.maven.wagon.PathUtils;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;

import bee.PathSet;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = Singleton.class)
public class Project {

    /** The project root directory. */
    public final Path root;

    /** The libraries. */
    final SortedSet<Library> libraries = new TreeSet();

    /** The excluded libraries. */
    final HashSet<Exclusion> exclusions = new HashSet();

    /** The repositories. */
    final ArrayList<RemoteRepository> repositories = new ArrayList();

    /** The project name. */
    private String projectName = "";

    /** The product name. */
    private String productName = "";

    /** The product version. */
    private String productVersion = "1.0";

    /** The product description. */
    private String description = "";

    /** The requirement of Java version. */
    private SourceVersion requirementJavaVersion;

    /** The input base directory. */
    private Path input;

    /** The output base directory. */
    private Path output;

    /**
     * 
     */
    protected Project() {
        Path archive = ClassUtil.getArchive(getClass());

        if (Files.isDirectory(archive)) {
            // directory
            this.root = archive.getParent().getParent();
        } else {
            // some archive
            this.root = archive;
        }

        setInput((Path) null);
        setOutput((Path) null);
    }

    /**
     * <p>
     * Return project name.
     * </p>
     * 
     * @return The project name.
     */
    public final String getProject() {
        return projectName;
    }

    /**
     * <p>
     * Return product name.
     * </p>
     * 
     * @return The product name.
     */
    public final String getProduct() {
        return productName;
    }

    /**
     * <p>
     * Return product version.
     * </p>
     * 
     * @return The product version.
     */
    public final String getVersion() {
        return productVersion;
    }

    /**
     * <p>
     * Declare project name, product name and product version.
     * </p>
     * 
     * @param projectName A project name.
     * @param productName A product name.
     * @param version A product version.
     */
    protected final void name(String projectName, String productName, String version) {
        this.projectName = normalize(projectName, "YourProject");
        this.productName = normalize(productName, "YourProduct");
        this.productVersion = normalize(version, "1.0");
    }

    /**
     * <p>
     * Return product description.
     * </p>
     * 
     * @return The product description.
     */
    public final String getDescription() {
        return description;
    }

    /**
     * <p>
     * Declare product description.
     * </p>
     * 
     * @param description A product description.
     */
    protected final void describe(String description) {
        if (description == null) {
            description = "";
        }
        this.description = description.trim();
    }

    /**
     * <p>
     * Resolve all dependencies for the specified scope.
     * </p>
     * 
     * @param scope
     * @return
     */
    public final Set<Library> getDependency(Scope scope) {
        return I.make(Repository.class).collectDependency(this, scope);
    }

    /**
     * <p>
     * Declare dependency.
     * </p>
     * 
     * @param projectName A project name.
     * @param productName A product name.
     * @param version A product version.
     * @return A dependency.
     */
    protected final Library require(String projectName, String productName, String version) {
        Library library = new Library(projectName, productName, version);
        libraries.add(library);

        // API definition
        return library;
    }

    /**
     * <p>
     * Returns Java version requirement.
     * </p>
     * 
     * @return A Java version requirement.
     */
    public final String getJavaVersion() {
        return normalize(requirementJavaVersion);
    }

    /**
     * <p>
     * Declare Java version requirement.
     * </p>
     * 
     * @param version A Java version to require.
     */
    protected final void require(SourceVersion version) {
        this.requirementJavaVersion = version;
    }

    /**
     * <p>
     * Exclude the specified library from transitive dependency resolution.
     * </p>
     * 
     * @param projectName A project name.
     * @param productName A product name.
     */
    protected final void unrequire(String projectName, String productName) {
        exclusions.add(new Exclusion(projectName, productName, "", "jar"));
    }

    /**
     * <p>
     * Add new repository by URI.
     * </p>
     * 
     * @param uri
     */
    protected final void repository(String uri) {
        if (uri != null && !uri.isEmpty()) {
            repositories.add(new RemoteRepository(PathUtils.host(uri), "default", uri));
        }
    }

    /**
     * Get the bese directory of input.
     * 
     * @return The base input directory.
     */
    public final Path getInput() {
        return input;
    }

    /**
     * <p>
     * Set base directory of input. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * </p>
     * 
     * @param input The base input directory to set.
     */
    protected final void setInput(Path input) {
        if (input == null) {
            input = root.resolve("src");
        }

        if (!input.isAbsolute()) {
            input = root.resolve(input);
        }
        this.input = input;
    }

    /**
     * <p>
     * Set base directory of input. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * </p>
     * 
     * @param input The base input directory to set.
     */
    protected final void setInput(String input) {
        if (input == null) {
            input = "src";
        }
        setInput(root.resolve(input));
    }

    /**
     * Get the bese directory of output.
     * 
     * @return The base output directory.
     */
    public final Path getOutput() {
        return output;
    }

    /**
     * <p>
     * Set base directory of output. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * </p>
     * 
     * @param output The base output directory to set.
     */
    protected final void setOutput(Path output) {
        if (output == null) {
            output = root.resolve("target");
        }

        if (!output.isAbsolute()) {
            output = root.resolve(output);
        }
        this.output = output;
    }

    /**
     * <p>
     * Set base directory of output. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * </p>
     * 
     * @param output The base output directory to set.
     */
    protected final void setOutput(String output) {
        if (output == null) {
            output = "target";
        }
        setOutput(root.resolve(output));
    }

    /**
     * <p>
     * Returns source directories.
     * </p>
     * 
     * @return
     */
    public final PathSet getSources() {
        PathSet set = new PathSet();

        for (Path path : I.walkDirectory(input.resolve("main"), "*")) {
            set.add(path);
        }
        return set;
    }

    /**
     * <p>
     * Returns class directory.
     * </p>
     * 
     * @return
     */
    public final Path getClasses() {
        return output.resolve("classes");
    }

    /**
     * <p>
     * Returns test source directories.
     * </p>
     * 
     * @return
     */
    public final PathSet getTestSources() {
        PathSet set = new PathSet();

        for (Path path : I.walkDirectory(input.resolve("test"), "*")) {
            set.add(path);
        }
        return set;
    }

    /**
     * <p>
     * Returns test class directory.
     * </p>
     * 
     * @return
     */
    public final Path getTestClasses() {
        return output.resolve("test-classes");
    }

    /**
     * <p>
     * Returns project source directories.
     * </p>
     * 
     * @return
     */
    public final PathSet getProjectSources() {
        PathSet set = new PathSet();

        for (Path path : I.walkDirectory(input.resolve("project"), "*")) {
            set.add(path);
        }
        return set;
    }

    /**
     * <p>
     * Returns project class directory.
     * </p>
     * 
     * @return
     */
    public final Path getProjectClasses() {
        return output.resolve("project-classes");
    }

    /**
     * <p>
     * Launch project build process.
     * </p>
     * 
     * @param definition
     */
    protected static final void launch(Class<? extends Project> definition) {
        Project project = I.make(definition);

    }
}
