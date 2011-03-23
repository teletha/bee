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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import ezbean.I;

/**
 * @version 2010/04/27 22:25:04
 */
public class Platform {

    /** The executable file for Java. */
    public static final Path Java;

    /** The executable file for Bee. */
    public static final Path Bee;

    // initialization
    static {
        Path bin = null;
        Path java = null;
        Path bee = null;

        // Search Java SDK from path.
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

        Java = java;
        Bee = bee;
    }

    /**
     * Avoid construction.
     */
    private Platform() {
    }

}
