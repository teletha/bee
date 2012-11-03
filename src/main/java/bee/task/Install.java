/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import kiss.I;
import bee.api.ArtifactLocator;
import bee.api.Command;
import bee.api.Repository;
import bee.api.Task;

/**
 * @version 2012/04/04 2:43:44
 */
public class Install extends Task {

    @Command("Install project into the local repository.")
    public void project() {
        require(Test.class).test();
        require(Jar.class).source();

        Repository repository = I.make(Repository.class);
        repository.install(project, ArtifactLocator.Jar.in(project));
    }
}
