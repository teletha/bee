/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import bee.BlinkProject;
import psychopath.Directory;
import psychopath.Locator;

public class RandomProject extends BlinkProject {

    /**
     * 
     */
    public RandomProject(String name, Directory directory) {
        product("temporary.test.project", name, "1.0");

        setOutput(Locator.temporaryDirectory());
        locateJar().create();

        Repository repo = new Repository(this);
        repo.setLocalRepository(directory);
        repo.install(this);
    }
}
