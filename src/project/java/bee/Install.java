/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import bee.api.Repository;
import kiss.I;

/**
 * @version 2017/01/08 20:58:44
 */
public class Install extends bee.task.Install {

    /**
     * <p>
     * Install the current Bee into your environment.
     * </p>
     */
    @Override
    public void project() {
        require(Jar.class).merge();

        I.make(Repository.class).install(project);

        BeeInstaller.install(project.locateJar());
    }
}
