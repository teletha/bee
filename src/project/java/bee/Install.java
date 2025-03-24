/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.TaskOperations.*;

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
        TaskOperations.config(Jar.class, conf -> {
            conf.merging = o -> o
                    .glob("!licenses/**", "!META-INF/*", "!META-INF/licenses/**", "!META-INF/maven/**", "!META-INF/sisu/**", "!META-INF/versions/**");
        });

        require(Test::test);
        require(Jar::document, Jar::merge);

        I.make(Repository.class).install(TaskOperations.project());

        BeeInstaller.install(true, true, false);
    }
}