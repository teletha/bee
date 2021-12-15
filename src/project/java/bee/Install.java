/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import bee.api.Repository;
import bee.task.Jar;
import bee.task.Test;
import kiss.I;

public class Install extends bee.task.Install {

    /**
     * Install the current Bee into your environment.
     */
    @Override
    public void project() {
        require(Test::test);
        require(Jar::document, Jar::merge);

        I.make(Repository.class).install(project);

        BeeInstaller.install(true, true, true);
    }
}