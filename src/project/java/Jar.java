import java.io.IOException;
import java.nio.file.Files;

import kiss.I;
import bee.definition.Library;
import bee.definition.Scope;

/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */

/**
 * @version 2012/04/12 0:08:30
 */
public class Jar extends bee.task.Jar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge() {
        for (Library library : project.getDependency("xalan", "xalan", Scope.Runtime)) {
            ui.talk(library.getJar());
            try {
                Files.delete(library.getJar());
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }
}
