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
            if (scope.contains(library.scope)) {
                tree.add(library);
            }
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
        private void add(Library library) {
            String id = library.group + "-" + library.name;

            Library candidate = libraries.get(id);

            if (candidate != null) {
                // dupulication - compare version and use newly
                if (candidate.version.compareTo(library.version) < 0) {
                    add(id, library);
                }
            } else {
                // new dependency
                add(id, library);
            }
        }

        private void add(String id, Library library) {
            libraries.put(id, library);

            Path pom = library.getPOM();

            if (Files.exists(pom)) {
                Element doc = $(pom);

                for (Element e : doc.find("dependency")) {
                    String projectName = e.find("groupId").first().text();
                    String productName = e.find("artifactId").first().text();
                    String version = e.find("version").text();
                    String optional = e.find("optional").text();

                    if (projectName.startsWith("$")) {
                        projectName = searchProperty(doc, projectName.substring(2, projectName.length() - 1));
                    }

                    if (version.startsWith("$")) {
                        version = searchProperty(doc, version.substring(2, version.length() - 1));
                    }
                    System.out.println(e);
                    Library dependency = new Library(projectName, productName, version);

                    switch (Scope.by(e.find("scope").text())) {
                    case Test:
                        dependency.atTest();
                        break;

                    case Compile:
                        dependency.atCompile();
                        break;

                    case Provided:
                        dependency.atProvided();
                        break;

                    case System:
                        dependency.atSystem();
                        break;
                    }

                    if (dependency.scope.contains(Scope.Runtime) && !optional.equals("true")) {
                        add(dependency);
                    }
                }
            } else {
                // POM file is not found
                pom = downloadPOM(library);

                System.out.println($(pom));
            }
        }
    }

    private String searchProperty(Element doc, String name) {
        Element property = doc.find("properties " + name.replaceAll("(\\.|-)", "\\\\$1"));

        if (property.size() == 0) {
            // search parent pom
            Element parent = doc.find("parent");
            String projectName = parent.find("groupId").text();
            String productName = parent.find("artifactId").text();
            String version = parent.find("version").text();

            if (name.equals("project.parent.version")) {
                return version;
            }

            if (name.equals("project.parent.groupId")) {
                return projectName;
            }

            Path pom = downloadPOM(new Library(projectName, productName, version));

            return searchProperty($(pom), name);
        }
        return property.text();
    }

    private Path downloadPOM(Library library) {
        String path = library.localPath(".pom");
        System.out.println(path + "  @@@" + " " + library.group + "  " + library.name);
        // create destination
        Path dest = Local.resolve(path);

        if (Files.notExists(dest)) {
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
        }

        return dest;
    }
}
