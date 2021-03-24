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

import static bee.Platform.*;

import java.util.List;
import java.util.StringJoiner;

import bee.Task;
import bee.api.Command;
import bee.util.Inputs;
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
        String mavenCI = """
                name: Java CI with Maven

                on:
                  push:
                    branches: [master, main]
                  pull_request:
                    branches: [master, main]
                  workflow_dispatch:

                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                    - name: Check out repository
                      uses: actions/checkout@v2

                    - name: Set up JDK
                      uses: actions/setup-java@v1
                      with:
                        java-version: %s

                    - name: Build with Maven
                      run: mvn -B package --file pom.xml
                """;

        String releasePlease = """
                name: Release Please

                on:
                  push:
                    branches: [master, main]

                jobs:
                  release-please:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: GoogleCloudPlatform/release-please-action@v2
                        with:
                          release-type: simple
                          package-name: %s
                """;

        makeFile(".github/workflows/java-ci-with-maven.yml", mavenCI.formatted(Inputs.normalize(project.getJavaTestClassVersion())));
        makeFile(".github/workflows/release-please.yml", releasePlease.formatted(project.getProduct()));
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