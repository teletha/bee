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

import static bee.Platform.EOL;
import static bee.Platform.isLinux;
import static bee.Platform.isWindows;

import java.util.List;
import java.util.StringJoiner;

import bee.Task;
import bee.api.Command;
import kiss.I;
import psychopath.File;

public class Git extends Task {

    @Command(value = "Generate .gitignore file.", defaults = true)
    public void gitignore() {
        File ignore = project.getRoot().file(".gitignore");

        makeFile(ignore, update(ignore.lines().toList()));

        if (project.getVersionControlSystem().isPresent()) {
            github();
        }
    }

    @Command("Generate utilities for Github integration.")
    public void github() {
        makeFile(".github/workflows/release-please.yml", I.express(readResource("github-release-please-action.yml"), project));
        makeFile("version.txt", project.getVersion());
        makeFile(project.getProjectDefinition(), line -> {
            if (line.trim().startsWith("product(")) {
                return line.replaceAll(",[^,]+\\);", ", ref(\"version.txt\"));");
            } else {
                return line;
            }
        });
    }

    /**
     * Update gitignore configuration.
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

        return I.http(uri.toString(), String.class)
                .waitForTerminate()
                .flatArray(rule -> rule.split(EOL))
                .startWith(".*", "!/.gitignore", "!/.github")
                .startWith(lines)
                .distinct()
                .toList();
    }
}