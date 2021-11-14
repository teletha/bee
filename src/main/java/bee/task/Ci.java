/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
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

public class Ci extends Task {

    @Command(value = "Generate CI/CD configuration files for GitHub.", defaults = true)
    public void github() {
        require(Ci::gitignore, Ci::jitpack);

        String mavenCI = """
                name: Continuous Integration

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
                      uses: actions/setup-java@v2
                      with:
                        distribution: zulu
                        java-version: %s
                        cache: maven

                    - name: Build by Maven
                      run: mvn -B package --file pom.xml

                    - name: Build site
                      run: |
                        curl -sL -o bee.jar https://github.com/Teletha/bee/blob/master/bee-0.10.0.jar?raw=true
                        java -javaagent:bee.jar -cp bee.jar bee.Bee doc:site

                    - name: Deploy site
                      uses: peaceiris/actions-gh-pages@v3
                      with:
                        github_token: ${{ secrets.GITHUB_TOKEN }}
                        publish_dir: target/site
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

        String testVersion = Inputs.normalize(project.getJavaTestClassVersion());

        makeFile(".github/workflows/java-ci-with-maven.yml", String.format(mavenCI, testVersion));
        makeFile(".github/workflows/release-please.yml", String.format(releasePlease, project.getProduct()));
        makeFile("version.txt", project.getVersion());
        makeFile(project.getProjectDefinition(), line -> {
            if (line.trim().startsWith("product(")) {
                return line.replaceAll(",[^,]+\\);", ", ref(\"version.txt\"));");
            } else {
                return line;
            }
        });
    }

    @Command("Generate CI/CD configuration files for JitPack.")
    public void jitpack() {
        String sourceVersion = Inputs.normalize(project.getJavaSourceVersion());

        makeFile("jitpack.yml", String.format("""
                jdk:
                  - openjdk16

                before_install:
                  - source ~/.sdkman/bin/sdkman-init.sh
                  - sdk install java 16-open
                  - source ~/.sdkman/bin/sdkman-init.sh

                install:
                  - curl -sL -o bee.jar https://github.com/Teletha/bee/blob/master/bee-0.10.0.jar?raw=true
                  - java -javaagent:bee.jar -cp bee.jar bee.Bee install -disableANSI
                """, sourceVersion, sourceVersion));
    }

    @Command(value = "Generate .gitignore file.")
    public void gitignore() {
        File ignore = project.getRoot().file(".gitignore");

        makeFile(ignore, update(ignore.lines().toList()));
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
