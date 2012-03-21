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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import javax.lang.model.SourceVersion;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;
import bee.compiler.JavaCompiler;
import bee.definition.Project;
import bee.license.License;
import bee.task.Tasks;

/**
 * @version 2010/04/02 3:44:35
 */
@Manageable(lifestyle = Singleton.class)
public class Bee {

    /** The executable file for Java. */
    public static final Path Java;

    /** The executable file for Bee. */
    public static final Path Bee;

    /** The root directory for Java. */
    public static final Path JavaHome;

    // initialization
    static {
        Path bin = null;
        Path java = null;
        Path bee = null;

        // Search Java SDK from path. Don't use java.home system property to avoid JRE.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(File.pathSeparator)) {
                    Path directory = I.locate(value);
                    Path linux = directory.resolve("javac");
                    Path windows = directory.resolve("javac.exe");

                    if (Files.exists(linux)) {
                        bin = directory;
                        java = linux;
                        bee = directory.resolve("bee");

                        break root;
                    } else if (Files.exists(windows)) {
                        bin = directory;
                        java = windows;
                        bee = directory.resolve("bee.bat");

                        break root;
                    }
                }
            }
        }

        if (bin == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        Bee = bee;
        Java = java;
        JavaHome = java.getParent().getParent();

        I.load(ClassUtil.getArchive(Bee.class));
    }

    /**
     * <p>
     * Create project.
     * </p>
     * 
     * @param home
     * @param ui
     * @return
     */
    public static final Project createProject(String home, UserInterface ui) {
        return createProject(home == null ? null : Paths.get(home), ui);
    }

    /**
     * <p>
     * Create project.
     * </p>
     * 
     * @param home
     * @param ui
     * @return
     */
    public static final Project createProject(Path home, UserInterface ui) {
        // Use current directory if user doesn't specify.
        if (home == null) {
            home = I.locate("");
        }

        // We need absolute path.
        home = home.toAbsolutePath();

        // We need present directory path.
        if (Files.notExists(home)) {
            try {
                Files.createDirectories(home);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        } else if (!Files.isDirectory(home)) {
            home = home.getParent();
        }

        // validate user interface and register it
        if (ui == null) {
            ui = new CommandLineUserInterface();
        }
        UserInterfaceLisfestyle.local.set(ui);

        // search Project from the specified file systems
        Path sources = home.resolve("src/project");
        Path classes = home.resolve("target/project");
        Path projectDefinitionSource = sources.resolve("Project.java");
        Path projectDefinitionClass = classes.resolve("Project.class");

        if (Files.notExists(projectDefinitionSource)) {
            // create Project source

            // compile Project source
            compileProject(sources, classes);
        } else if (Files.notExists(projectDefinitionClass)) {
            // compile Project source
            compileProject(sources, classes);
        }

        // load Project definition class
        I.load(classes);

        try {
            return (Project) I.make(Class.forName("Project"));
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Compile project definition.
     * </p>
     * 
     * @param input
     * @param output
     */
    private static final void compileProject(Path input, Path output) {
        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(input);
        compiler.addClassPath(ClassUtil.getArchive(Bee.class));
        compiler.addClassPath(ClassUtil.getArchive(I.class));
        compiler.setOutput(output);
        compiler.compile();
    }

    /**
     * <p>
     * Launch bee at the current location with commandline user interface.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        Tasks.execute(createProject("", null), "eclipse");
    }

    /**
     * @version 2011/09/30 16:12:02
     */
    protected static class ProjectQuestion {

        @Question(message = "Your project name")
        private String project;

        @Question(message = "Your product name")
        private String product;

        @Question(message = "Your product version")
        private String version;

        @Question(message = "What is your product's license?")
        private License license;

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
        public void setProject(String project) {
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
        public void setProduct(String product) {
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
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * Get the license property of this {@link Bee.ProjectQuestion}.
         * 
         * @return The license property.
         */
        public License getLicense() {
            return license;
        }

        /**
         * Set the license property of this {@link Bee.ProjectQuestion}.
         * 
         * @param license The license value to set.
         */
        public void setLicense(License license) {
            this.license = license;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "ProjectQuestion [project=" + project + ", artifact=" + product + ", version=" + version + "]";
        }
    }
}
