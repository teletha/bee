/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.file.Path;

import bee.api.Command;
import bee.api.Project;
import bee.api.Repository;
import bee.api.Task;
import kiss.I;

/**
 * @version 2017/01/16 14:34:48
 */
public class Install extends Task {

    @Command(defaults = true, value = "Install project into the local repository.")
    public void project() {
        require(Test.class).test();
        require(Jar.class).source();
        require(Jar.class).document();

        Repository repository = I.make(Repository.class);
        repository.install(project);
    }

    @Command("Install local jar file into the local repository.")
    public void jar() {
        Path selected = ui.ask("Select a jar file to install.", I.walk(project.getRoot(), "*.jar"));
        String group = ui.ask("Input group name.", selected.getFileName().toString());
        String product = ui.ask("Input product name.", group);
        String version = ui.ask("Input product version.", "1.0");

        Repository repository = I.make(Repository.class);
        repository.install(new TemporaryProject(group, product, version), selected);
    }

    /**
     * @version 2016/10/15 21:13:19
     */
    private static class TemporaryProject extends Project {

        /**
         * 
         */
        private TemporaryProject(String group, String product, String version) {
            product(group, product, version);
        }

    }
}
