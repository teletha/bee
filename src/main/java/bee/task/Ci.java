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
import bee.api.License;
import bee.api.VCS;
import bee.util.Inputs;
import kiss.I;
import psychopath.File;

public class Ci extends Task {

    @Command(defaults = true, value = "Setup CI/CD")
    public void setup() {
        VCS vcs = project.getVersionControlSystem();

        if (vcs == null) {
            ui.info("No version control system.");
        } else {
            ui.info("Detect version control system.");

            if (vcs.name().equals("github")) {
                require(Ci::github);
            }
        }
    }

    @Command("Generate CI/CD configuration files for GitHub.")
    public void github() {
        require(Ci::gitignore, Ci::jitpack);

        String build = """
                name: Build and Deploy

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

                    - name: Build artifact and site
                      run: |
                        if [ -e "bee" ]; then
                          source bee install doc:site
                        else
                          version=$(curl -SsL https://git.io/stable-bee)
                          curl -SsL -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
                          java -javaagent:bee-${version}.jar -cp bee-${version}.jar bee.Bee install doc:site
                        fi

                    - name: Deploy site
                      uses: peaceiris/actions-gh-pages@v3
                      with:
                        github_token: ${{ secrets.GITHUB_TOKEN }}
                        publish_dir: target/site

                    - name: Request Releasing
                      uses: GoogleCloudPlatform/release-please-action@v2
                      with:
                        release-type: simple
                        package-name: %s
                """;

        String testVersion = Inputs.normalize(project.getJavaTestClassVersion());

        makeFile(".github/workflows/build.yml", String.format(build, testVersion, project.getProduct()));
        makeFile("version.txt", project.getVersion());
        makeLicenseFile();
        makeReadMeFile();

        // delete old settings
        deleteFile(".github/workflows/java-ci-with-maven.yml");
        deleteFile(".github/workflows/release-please.yml");
    }

    /**
     * Create license file if needed
     */
    private void makeLicenseFile() {
        License license = project.getLicense();

        if (license == null) {
            return; // not specified
        }

        if (checkFile("LICENSE.txt") || checkFile("LICENSE.md") || checkFile("LICENSE.rst")) {
            return; // already exists
        }

        makeFile("LICENSE.txt", license.text());
    }

    /**
     * Create README file if needed
     */
    private void makeReadMeFile() {
        if (checkFile("README.md")) {
            return; // already exists
        }
    }

    @Command("Generate CI/CD configuration files for JitPack.")
    public void jitpack() {
        String sourceVersion = Inputs.normalize(project.getJavaSourceVersion());

        makeFile("jitpack.yml", String.format("""
                jdk:
                  - openjdk%s

                before_install: |
                  source ~/.sdkman/bin/sdkman-init.sh
                  sdk install java %s-open
                  source ~/.sdkman/bin/sdkman-init.sh

                install: |
                  if [ -e "bee" ]; then
                    source bee install pom
                  else
                    version=$(curl -SsL https://git.io/stable-bee)
                    curl -SsL -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
                    java -javaagent:bee-${version}.jar -cp bee-${version}.jar bee.Bee install pom
                  fi
                """, sourceVersion, sourceVersion));
    }

    @Command("Generate .gitignore file.")
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
        StringJoiner uri = new StringJoiner(",", "https://www.gitignore.io/api/", "").add("Java").add("Maven").add("Windows").add("Linux");

        // IDE
        for (IDESupport ide : I.find(IDESupport.class)) {
            uri.add(ide.toString());
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
