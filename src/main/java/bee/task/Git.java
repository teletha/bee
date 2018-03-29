/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.Platform.*;

import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

import bee.api.Command;
import bee.api.Task;
import bee.util.Paths;
import bee.util.RESTClient;
import kiss.I;

/**
 * @version 2017/01/19 10:29:59
 */
public class Git extends Task {

    @Command("Generate .gitignore file.")
    public void gitignore() {
        Path ignore = project.getRoot().resolve(".gitignore");

        makeFile(ignore, update(Paths.readLines(ignore)));
    }

    /**
     * <p>
     * Update gitignore configuration.
     * </p>
     * 
     * @param lines Lines to update.
     * @return An updated lines.
     */
    List<String> update(List<String> lines) {
        StringJoiner uri = new StringJoiner(",", "https://www.gitignore.io/api/", "").add("Java").add("Maven");

        // OS
        if (isWindows()) uri.add("Windows");
        if (isLinux()) uri.add("Linux");

        // IDE
        for (IDESupport ide : I.find(IDESupport.class)) {
            if (ide.exist(project)) {
                uri.add(ide.toString());
            }
        }

        return new RESTClient().get(uri.toString())
                .flatArray(rule -> rule.split(EOL))
                .startWith(".*", "!/.gitignore")
                .startWith(lines)
                .distinct()
                .toList();
    }
}
