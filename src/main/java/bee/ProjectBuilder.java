/*
 * Copyright (C) 2011 Nameless Production Committee.
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

import java.nio.file.Path;
import java.util.List;

import javax.lang.model.SourceVersion;

import ezbean.I;

/**
 * @version 2011/06/10 10:04:07
 */
class ProjectBuilder {

    private String group;

    /**
     * 
     */
    ProjectBuilder(Path root, UserInterface ui) {
        // search project file
        Path projectSource;
        List<Path> files = I.walk(root.resolve("src/project"), "Project.java");

        if (files.isEmpty()) {
            // create new project file
            ui.talk("Project is not found, so create new one.");

            String group = ui.ask("Group Name", new GropuValidator());
            String artifact = ui.ask("Artifact Name");
            String version = ui.ask("Version Number", "0.1");

        } else {
            // use user project file
        }
    }

    /**
     * @version 2011/06/10 10:53:28
     */
    private static class GropuValidator implements Validator<String> {

        /**
         * @see bee.Validator#validate(java.lang.Object)
         */
        @Override
        public void validate(String value) {
            for (String part : value.split("\\.")) {
                if (SourceVersion.isKeyword(part)) {
                    throw new IllegalArgumentException("Group Name contains Java keyword [" + part + "]");
                }
            }
        }
    }
}
