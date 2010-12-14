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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import bee.ui.UserInterface;
import ezbean.ClassLoadListener;
import ezbean.I;
import ezbean.Manageable;
import ezbean.Singleton;
import ezbean.model.ClassUtil;

/**
 * @version 2010/04/02 3:44:35
 */
@Manageable(lifestyle = Singleton.class)
public class Bee implements ClassLoadListener<Project> {

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
        if (home == null || home.length() == 0) {
            home = ".";
        }
        return createProject(Paths.get(home), ui);
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
    public static final Project createProject(File home, UserInterface ui) {
        return createProject(home.toPath(), ui);
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
        if (ui == null) {
            ui = new CommandlineUserInterface();
        }

        // search Project from the specified file system
        Path sourceFile = home.resolve("src/project/Project.java");
        Path classFile = home.resolve("target/project-classes/Project.class");

        if (sourceFile.notExists()) {
            // Generate new project.
            String gropuId = ui.ask("Project Name");
            String artifactId = null;

            int index = gropuId.lastIndexOf('.');

            if (index == -1) {
                artifactId = gropuId;
            } else {
                artifactId = gropuId.substring(index + 1);
            }
            artifactId = ui.ask("Artifact Name", artifactId);

            String version = ui.ask("Initial version number", "0.1");

            ui.talk("Group : %s", gropuId);
            ui.talk("Artifact : %s", artifactId);
            ui.talk("Version : %s", version);
        }

        if (classFile.notExists()) {
            System.out.println("not");
        }

        // load project classes
        I.load(new File(classFile.getParent().toUri()));

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
}
