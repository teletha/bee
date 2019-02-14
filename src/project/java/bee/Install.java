/*
 * Copyright (C) 2019 Nameless Production Committee
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
import kiss.I;

public class Install extends bee.task.Install {

    /**
     * <p>
     * Install the current Bee into your environment.
     * </p>
     */
    @Override
    public void project() {
        Jar jar = require(Jar.class);
        jar.source();
        jar.document();
        jar.merge();

        I.make(Repository.class).install(project);

        BeeInstaller.install(project.locateJar());
    }
}
