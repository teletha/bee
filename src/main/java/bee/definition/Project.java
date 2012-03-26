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

import java.nio.file.Path;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;
import bee.PathSet;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = Singleton.class)
public class Project {

    /** The project root directory. */
    public final Path root;

    /** The project name. */
    public String group;

    /** The product name. */
    public String name;

    /** The product version. */
    public String version;

    /** The libraries. */
    private final SortedSet<Library> libraries = new TreeSet();

    /** The input base directory. */
    private Path input;

    /** The output base directory. */
    private Path output;

    /**
     * 
     */
    protected Project() {
        root = ClassUtil.getArchive(getClass()).getParent().getParent();
        setInput((Path) null);
        setOutput((Path) null);
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
        return I.make(Repository.class).collectDependency(libraries, scope);
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
     * Launch project build process.
     * </p>
     * 
     * @param definition
     */
    protected static final void launch(Class<? extends Project> definition) {
        Project project = I.make(definition);

    }
}
