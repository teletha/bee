/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Project;
import bee.api.Repository;
import kiss.I;
import psychopath.File;

public interface Install extends Task {

    @Command(defaults = true, value = "Install project into the local repository.")
    default void project() {
        require(Test::test);
        require(Jar::document, Jar::source);

        I.make(Repository.class).install(TaskOperations.project());
    }

    @Command("Install jar file only into the local repository.")
    default void jar() {
        File selected = ui().ask("Select a jar file to install.", TaskOperations.project().getRoot().walkFile("*.jar").toList());
        String group = ui().ask("Input group name.", selected.name());
        String product = ui().ask("Input product name.", group);
        String version = ui().ask("Input product version.", "1.0");

        Repository repository = I.make(Repository.class);
        repository.install(new TemporaryProject(group, product, version), selected);
    }

    class TemporaryProject extends Project {

        /**
         * 
         */
        private TemporaryProject(String group, String product, String version) {
            product(group, product, version);
        }
    }
}