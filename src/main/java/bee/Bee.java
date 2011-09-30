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

import ezbean.ClassListener;
import ezbean.I;
import ezbean.Manageable;
import ezbean.Singleton;
import ezbean.model.ClassUtil;

/**
 * @version 2010/04/02 3:44:35
 */
@Manageable(lifestyle = Singleton.class)
public class Bee implements ClassListener<Project> {

    /** The executable file for Java. */
    public static final File Java;

    /** The executable file for Bee. */
    public static final File Bee;

    static {
        // Scan Platform
        File bin = null;
        File java = null;
        File bee = null;

        // Search Java SDK from path.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                // Search classpath for Bee.
                for (String value : entry.getValue().split(File.pathSeparator)) {
                    File directory = new File(value);
                    File linux = new File(directory, "javac");
                    File windows = new File(directory, "javac.exe");

                    if (linux.exists()) {
                        bin = directory;
                        java = linux;
                        bee = new File(directory, "bee");

                        break root;
                    } else if (windows.exists()) {
                        bin = directory;
                        java = windows;
                        bee = new File(directory, "bee.bat");

                        break root;
                    }
                }
            }
        }

        if (bin == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        Java = java;
        Bee = bee;

        I.load(ClassUtil.getArchive(Bee.class));
    }

    /**
     * @see ezbean.ClassLoadListener#load(java.lang.Class)
     */
    @Override
    public void load(Class<Project> clazz) {

    }

    /**
     * @see ezbean.ClassLoadListener#unload(java.lang.Class)
     */
    @Override
    public void unload(Class<Project> clazz) {
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
        Path project = home.resolve("src/project/Project.java");

        if (Files.exists(project)) {

        } else {
            System.out.println(ui.ask(ProjectQuestion.class));
        }

        return null;
    }

    /**
     * <p>
     * Launch bee at the current location with commandline user interface.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        createProject("", null);
    }

    protected static class ProjectQuestion {

        @Question(message = "Your project name")
        private String project;

        @Question(message = "Your artifact name")
        private String artifact;

        @Question(message = "Your product version")
        private String version;

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
            this.project = project;
        }

        /**
         * Get the artifact property of this {@link Bee.ProjectQuestion}.
         * 
         * @return The artifact property.
         */
        public String getArtifact() {
            return artifact;
        }

        /**
         * Set the artifact property of this {@link Bee.ProjectQuestion}.
         * 
         * @param artifact The artifact value to set.
         */
        public void setArtifact(String artifact) {
            this.artifact = artifact;
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
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "ProjectQuestion [project=" + project + ", artifact=" + artifact + ", version=" + version + "]";
        }

    }
}
