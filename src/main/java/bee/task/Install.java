/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import bee.Bee;
import bee.Task;
import bee.api.Command;
import bee.api.Project;
import bee.api.Repository;
import kiss.I;
import psychopath.File;
import psychopath.Locator;

public class Install extends Task {

    @Command(defaults = true, value = "Install project into the local repository.")
    public void project() {
        require(Test::test);
        require(Jar::document, Jar::source);

        Repository repository = I.make(Repository.class);
        repository.install(project);
    }

    @Command("Install jar file only into the local repository.")
    public void jar() {
        File selected = ui.ask("Select a jar file to install.", project.getRoot().walkFile("*.jar").toList());
        String group = ui.ask("Input group name.", selected.name());
        String product = ui.ask("Input product name.", group);
        String version = ui.ask("Input product version.", "1.0");

        Repository repository = I.make(Repository.class);
        repository.install(new TemporaryProject(group, product, version), selected);
    }

    @Command("Install bee-api.jar into the local repository.")
    public void beeAPI() {
        File source;

        if (project.equals(Bee.Tool)) {
            require(Install::project);
            source = project.locateJar();
        } else {
            source = Locator.locate(Bee.class).asFile();
        }

        File api = Locator.folder()
                .add(source.asArchive(), "bee/**", "!**.java")
                .add(source.asArchive(), "META-INF/services/**")
                .packToTemporary();

        I.make(Repository.class).install(bee.Bee.API, api);
    }

    /**
     * 
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