/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.api.License;
import bee.api.Project;
import bee.api.VCS;

/**
 * Empty project.
 */
class ZeroProject extends Project {

    ZeroProject() {
    }

    ZeroProject(String group, String name, String version, License license, VCS vcs) {
        product(group, name, version);
        license(license);
        if (vcs != null) versionControlSystem(vcs.uri());
    }
}