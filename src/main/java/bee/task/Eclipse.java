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
package bee.task;

import java.nio.file.Files;
import java.nio.file.Path;

import bee.definition.Library;
import bee.definition.Scope;

/**
 * @version 2010/04/02 3:58:58
 */
public class Eclipse extends Task {

    /**
     * <p>
     * Create eclipse's project file.
     * </p>
     */
    @Command(defaults = true)
    public void eclipse() {
        Path path = project.root.resolve("classpath.xml");

        for (Library library : project.getDependency(Scope.Development)) {
            Path jar = library.getJar();

            if (Files.exists(jar)) {
                System.out.println("==== " + jar + " ====");
            }
        }
    }
}
