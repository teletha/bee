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

import static kiss.Element.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import kiss.Element;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2010/09/05 20:52:29
 */
@Manageable(lifestyle = Singleton.class)
public class Repository {

    /** The local repository. */
    public static final Path Local;

    /** The list of repositories. */
    private static final List<URL> builtin = new ArrayList();

    /** The bee repository directory. */
    private static File repository;

    // built-in repositories
    static {
        // setup local repository
        Local = searchLocalRepository();

        // setup built-in external repositories
        try {
            builtin.add(new URL("http://repo1.maven.org/maven2/"));
        } catch (MalformedURLException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Path searchMavenHome() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(File.pathSeparator)) {
                    Path mvn = I.locate(path).resolve("mvn");

                    if (Files.exists(mvn)) {
                        return mvn.getParent().getParent();
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Search maven local respository.
     * </p>
     * 
     * @return
     */
    private static Path searchLocalRepository() {
        Path local = Paths.get(System.getProperty("user.home"), ".m2");
        Path home = searchMavenHome();

        if (home == null) {
            // maven is not found
            return local;
        } else {
            // maven is here
            Path conf = home.resolve("conf/settings.xml");

            if (Files.exists(conf)) {
                String path = $(conf).find("localRepository").text();

                if (path.length() != 0) {
                    return I.locate(path);
                }
            }

            // user custom local repository is not found
            return local;
        }
    }

    /**
     * Hide constructor.
     */
    Repository() {
    }

    public Set<Library> resolve(Set<Library> libraries, Scope scope) {
        DependencyTree tree = new DependencyTree();

        for (Library library : libraries) {
            tree.add(library, scope);
        }

        return new TreeSet(tree.libraries.values());
    }

    /**
     * @version 2012/03/22 20:16:50
     */
    private class DependencyTree {

        /** The all dependencies. */
        private final Map<String, Library> libraries = new ConcurrentHashMap();

        /**
         * <p>
         * Add dependency.
         * </p>
         * 
         * @param library
         */
        private void add(Library library, Scope scope) {
            String id = library.group + "-" + library.name;

            Library candidate = libraries.get(id);

            if (candidate != null) {
                // dupulication - compare version and use newly
                if (candidate.version.compareTo(library.version) < 0) {
                    add(id, library, scope);
                }
            } else {
                // new dependency
                add(id, library, scope);
            }
        }

        private void add(String id, Library library, Scope scope) {
            libraries.put(id, library);

            Path pom = library.getPOM();

            if (Files.exists(pom)) {
                for (Element e : $(pom).find("dependency")) {
                    String projectName = e.find("groupId").first().text();
                    String productName = e.find("artifactId").first().text();
                    String version = e.find("version").first().text();
                    String optional = e.find("optional").text();
                    System.out.println(e);
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
                        add(dependency, scope);
                    }
                }
            } else {
                // POM file is not found
                pom = downloadPOM(library);

                System.out.println($(pom));
            }
        }
    }

    private Path downloadPOM(Library library) {
        String path = library.localPath(".pom");

        // create destination
        Path dest = Local.resolve(path);

        try {
            Files.createDirectories(dest.getParent());
        } catch (IOException e) {
            throw I.quiet(e);
        }

        for (URL uil : builtin) {
            try {
                // create source
                URL source = new URL(uil, path);

                // download
                I.copy(source.openStream(), Files.newOutputStream(dest), true);
                break;
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        return dest;
    }
}
