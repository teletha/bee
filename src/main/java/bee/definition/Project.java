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

import static kiss.Element.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import kiss.Element;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;

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
    public Version version;

    /** The libraries. */
    private final SortedSet<Library> libraries = new TreeSet();

    /**
     * 
     */
    protected Project() {
        root = ClassUtil.getArchive(getClass()).getParent().getParent();
    }

    /**
     * <p>
     * Resolve all dependencies for the specified scope.
     * </p>
     * 
     * @param scope
     * @return
     */
    public Set<Library> getDependency(Scope scope) {
        // Set<Library> set = new TreeSet();
        //
        // for (Library library : libraries) {
        // resolveDependency(library, scope, set);
        // }
        return I.make(Repository.class).resolve(libraries, scope);
    }

    /**
     * <p>
     * Resolve dependency.
     * </p>
     * 
     * @param library
     * @param scope
     * @param set
     */
    private void resolveDependency(Library library, Scope scope, Set<Library> set) {
        if (set.add(library)) {
            // analyze pom
            Path pom = library.getPOM();

            if (Files.exists(pom)) {
                for (Element e : $(pom).find("dependency")) {
                    String projectName = e.find("groupId").first().text();
                    String productName = e.find("artifactId").first().text();
                    String version = e.find("version").first().text();
                    String optional = e.find("optional").text();

                    Library dependency = new Library(projectName, productName, version);

                    switch (Scope.by(e.find("scope").text())) {
                    case TEST:
                        dependency.atTest();
                        break;

                    case COMPILE:
                        dependency.atCompile();
                        break;
                    }

                    if (dependency.scope == scope && !optional.equals("true")) {
                        resolveDependency(dependency, scope, set);
                    }
                }
            } else {
                System.out.println("no  " + pom);
            }
        }
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
     * Launch project build process.
     * </p>
     * 
     * @param definition
     */
    protected static final void launch(Class<? extends Project> definition) {
        Project project = I.make(definition);

    }
}
