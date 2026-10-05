/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.javadoc.Javadoc;

import bee.Isolation;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.License;
import bee.api.Scope;
import bee.api.VCS;
import bee.util.Inputs;
import kiss.I;
import psychopath.File;

public interface CI extends Task {

    @Command(defaults = true, value = "Setup CI/CD")
    default void setup() {
        VCS vcs = project().getVersionControlSystem();

        if (vcs == null) {
            ui().info("No version control system.");
        } else {
            ui().info("Detect version control system.");

            if (vcs.name().equals("github")) {
                require(CI::github);
            }
        }
    }

    @Command("Generate CI/CD configuration files for GitHub.")
    default void github() {
        require(CI::gitignore, CI::jitpack, CI::release);

        String build = """
                name: Build and Deploy

                on:
                  push:
                    branches: [master, main, test*]
                  pull_request:
                    branches: [master, main]
                  workflow_dispatch:

                # enable pipeline error in shell
                defaults:
                  run:
                    shell: bash

                concurrency:
                  group: ${{ github.workflow }}-${{ github.ref }}
                  cancel-in-progress: true

                jobs:
                  build:
                    runs-on: ubuntu-latest
                    timeout-minutes: 10
                    permissions:
                      contents: write
                    steps:
                    - name: Check out repository
                      uses: actions/checkout@v7

                    - name: Set up JDK
                      uses: actions/setup-java@v6
                      with:
                        distribution: zulu
                        java-version: %s

                    - name: Cache bee local repository
                      uses: actions/cache@v6
                      with:
                        path: ${{ env.JAVA_HOME }}/lib/bee/repository
                        key: ${{ runner.os }}-bee-${{ hashFiles('**/pom.xml') }}
                        restore-keys: ${{ runner.os }}-bee

                    - name: Build artifact and site
                      run: |
                        if [ -e "bee" ]; then
                          source bee install doc:site maven:pom ci:readme ci:license ci:release
                        else
                          version=$(curl -SsL https://git.io/stable-bee)
                          curl -SsL -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
                          java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -cp bee-${version}.jar bee.Bee install doc:site maven:pom ci:readme ci:license
                        fi

                    # The steps below write to the repository. A pull request from a fork runs with a
                    # read-only GITHUB_TOKEN, so a push there ends in a permission error, and a pull
                    # request from a branch of this repository would only rewrite its merge candidate,
                    # which is never merged. Therefore these steps run on pushes only.
                    - name: Deploy site
                      if: github.event_name != 'pull_request'
                      uses: peaceiris/actions-gh-pages@v4
                      with:
                        github_token: ${{ secrets.GITHUB_TOKEN }}
                        publish_dir: target/site

                    - name: Auto commit
                      if: github.event_name != 'pull_request'
                      uses: stefanzweifel/git-auto-commit-action@v7
                      with:
                        commit_message: update repository info
                """;

        String version = Inputs.normalize(project().getJavaSourceVersion());

        // The output result from the Release-Please action contains a newline,
        // so we will adjust it.
        makeFile("version.txt", List.of(project().getVersion(), "")).text(o -> o.replaceAll("\\R", "\n"));
        makeFile(".github/workflows/build.yml", String.format(build, version));
        license();
        readme();

        // delete old settings
        deleteFile(".github/workflows/java-ci-with-maven.yml");
        deleteFile(".github/workflows/release-please.yml");
    }

    /**
     * Create license file if needed
     */
    @Command("Generate license file.")
    default void license() {
        License license = project().license();

        if (license == null) {
            return; // not specified
        }

        if (checkFile("LICENSE.txt") || checkFile("LICENSE.md") || checkFile("LICENSE.rst")) {
            return; // already exists
        }

        makeFile("LICENSE.txt", license.text(false));
    }

    /**
     * Create README file if needed
     */
    @SuppressWarnings("serial")
    @Command("Generate readme file.")
    default void readme() {
        new Isolation("com.github.javaparser : javaparser-core") {

            @Override
            public void isolate() {
                List<Snippet> snippets = project().getRoot()
                        .walkFile("**/ReadMe*Test.java")
                        .first()
                        .map(File::text)
                        .flatIterable(text -> parse(text, "Test"))
                        .toList();

                makeFile("README.md", I
                        .express("""
                                <p align="center">
                                    <a href="https://docs.oracle.com/en/java/javase/{java}/"><img src="https://img.shields.io/badge/Java-Release%20{java}-green"/></a>
                                    <span>&nbsp;</span>
                                    <a href="https://jitpack.io/#{owner}/{repo}"><img src="https://img.shields.io/jitpack/v/{name}/{owner}/{repo}?label=Repository&color=green"></a>
                                    <span>&nbsp;</span>
                                    <a href="https://{owner}.github.io/{repo}"><img src="https://img.shields.io/website.svg?down_color=red&down_message=CLOSE&label=Official%20Site&up_color=green&up_message=OPEN&url=https%3A%2F%2F{owner}.github.io%2F{repo}"></a>
                                </p>

                                {#description}
                                ## Summary
                                {.}
                                <p align="right"><a href="#top">back to top</a></p>
                                {/description}


                                {#snippets}
                                ## Usage
                                {.}
                                <p align="right"><a href="#top">back to top</a></p>
                                {/snippets}


                                {#benchmark}
                                ## Benchmark
                                {.}
                                <p align="right"><a href="#top">back to top</a></p>
                                {/benchmark}


                                ## Prerequisites
                                {ProductName} runs on all major operating systems and requires only [Java version {java}](https://docs.oracle.com/en/java/javase/{java}/) or later to run.
                                To check, please run `java -version` on your terminal.
                                <p align="right"><a href="#top">back to top</a></p>

                                ## Install
                                For any code snippet below, please substitute the version given with the version of {ProductName} you wish to use.
                                #### [Maven](https://maven.apache.org/)
                                Add JitPack repository at the end of repositories element in your build.xml:
                                ```xml
                                <repository>
                                    <id>jitpack.io</id>
                                    <url>https://jitpack.io</url>
                                </repository>
                                ```
                                Add it into in the dependencies element like so:
                                ```xml
                                <dependency>
                                    <groupId>{group}</groupId>
                                    <artifactId>{product}</artifactId>
                                    <version>{version}</version>
                                </dependency>
                                ```
                                #### [Gradle](https://gradle.org/)
                                Add JitPack repository at the end of repositories in your build.gradle:
                                ```gradle
                                {=% %=}
                                repositories {
                                    maven { url "https://jitpack.io" }
                                }
                                ```
                                Add it into the dependencies section like so:
                                ```gradle
                                dependencies {
                                %={ }=%
                                    implementation '{group}:{product}:{version}'
                                }
                                ```
                                #### [SBT](https://www.scala-sbt.org/)
                                Add JitPack repository at the end of resolvers in your build.sbt:
                                ```scala
                                resolvers += "jitpack" at "https://jitpack.io"
                                ```
                                Add it into the libraryDependencies section like so:
                                ```scala
                                libraryDependencies += "{group}" % "{product}" % "{version}"
                                ```
                                #### [Leiningen](https://leiningen.org/)
                                Add JitPack repository at the end of repositories in your project().clj:
                                ```clj
                                :repositories [["jitpack" "https://jitpack.io"]]
                                ```
                                Add it into the dependencies section like so:
                                ```clj
                                :dependencies [[{group}/{product} "{version}"]]
                                ```
                                #### [Bee](https://teletha.github.io/bee)
                                Add it into your project definition class like so:
                                ```java
                                require("{group}", "{product}", "{version}");
                                ```
                                <p align="right"><a href="#top">back to top</a></p>


                                ## Contributing
                                Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.
                                If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
                                Don't forget to give the project a star! Thanks again!

                                1. Fork the Project
                                2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
                                3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
                                4. Push to the Branch (`git push origin feature/AmazingFeature`)
                                5. Open a Pull Request

                                The overwhelming majority of changes to this project don't add new features at all. Optimizations, tests, documentation, refactorings -- these are all part of making this product meet the highest standards of code quality and usability.
                                Contributing improvements in these areas is much easier, and much less of a hassle, than contributing code for new features.

                                ### Bug Reports
                                If you come across a bug, please file a bug report. Warning us of a bug is possibly the most valuable contribution you can make to {ProductName}.
                                If you encounter a bug that hasn't already been filed, [please file a report](https://github.com/{owner}/{repo}/issues/new) with an [SSCCE](http://sscce.org/) demonstrating the bug.
                                If you think something might be a bug, but you're not sure, ask on StackOverflow or on [{product}-discuss](https://github.com/{owner}/{repo}/discussions).
                                <p align="right"><a href="#top">back to top</a></p>


                                ## Dependency
                                {ProductName} depends on the following products on runtime.
                                {#dependencies}
                                * [{.}](https://mvnrepository.com/artifact/{group}/{name}/{version})
                                {/dependencies}
                                {^dependencies}
                                * No Dependency
                                {/dependencies}
                                <p align="right"><a href="#top">back to top</a></p>


                                ## License
                                {license}
                                <p align="right"><a href="#top">back to top</a></p>
                                """, new Object[] {
                                project()}, (m, o, e) -> {
                                    switch (e) {
                                    case "ProductName":
                                        return Inputs.capitalize(project().getProduct());

                                    case "java":
                                        return Inputs.normalize(project().getJavaRequiredVersion());

                                    case "owner":
                                        return project().getVersionControlSystem().owner;

                                    case "repo":
                                        return project().getVersionControlSystem().repo;

                                    case "name":
                                        return project().getVersionControlSystem().name();

                                    case "dependencies":
                                        return new ArrayList(project().getDependency(Scope.Runtime));

                                    case "testDependencies":
                                        return new ArrayList(project().getDependency(Scope.Test));

                                    case "license":
                                        return project().license().text(false).stream().collect(Collectors.joining(Platform.EOL));

                                    case "snippets":
                                        return snippets.isEmpty() ? null
                                                : snippets.stream()
                                                        .map(sn -> sn.comment + "\n```java\n" + sn.code + "\n```\n")
                                                        .collect(Collectors.joining(Platform.EOL));

                                    case "benchmark":
                                        File benchmark = project().getRoot().file("benchmark/README.md");
                                        return benchmark.isAbsent() ? null : benchmark.text();

                                    default:
                                        return null;
                                    }
                                })
                        .replace("}}", "{"));
            }

            /**
             * Parse source code.
             * 
             * @param source
             * @param annotationFQCN
             * @return
             */
            private List<Snippet> parse(String source, String annotationFQCN) {
                List<Snippet> snippets = new ArrayList();

                String[] lines = source.split("\\r?\\n");

                StaticJavaParser.getParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_17).setLexicalPreservationEnabled(true);

                CompilationUnit root = StaticJavaParser.parse(source);
                List<MethodDeclaration> methods = root.findAll(MethodDeclaration.class);

                for (MethodDeclaration method : methods) {
                    if (method.isAnnotationPresent(annotationFQCN)) {
                        Snippet snippet = new Snippet(lines, method);

                        snippets.add(snippet);
                    }
                }

                return snippets;
            }

            class Snippet {

                final String code;

                final String comment;

                Snippet(String[] lines, MethodDeclaration method) {
                    this.code = formatCode(lines, method.getBody().get());
                    this.comment = method.getJavadoc().map(Javadoc::toText).orElse("").strip();
                }

                /**
                 * @param block
                 * @return
                 */
                String formatCode(String[] lines, BlockStmt block) {
                    Position begin = block.getBegin().get();
                    Position end = block.getEnd().get();
                    StringJoiner join = new StringJoiner("\n");
                    for (int i = begin.line; i < end.line - 1; i++) {
                        join.add(lines[i]);
                    }
                    return join.toString().stripIndent();
                }
            }
        };
    }

    @Command("Generate CI/CD configuration files for JitPack.")
    default void jitpack() {
        String javaVersion = Inputs.normalize(project().getJavaSourceVersion());

        makeFile("jitpack.yml", String
                .format("""
                        jdk:
                          - openjdk%s

                        before_install:
                          - sdk install java %s-open
                          - sdk use java %s-open
                          - export PATH="$HOME/.sdkman/candidates/java/%s-open/bin:$PATH"

                        install: |
                          if [ -e "bee" ]; then
                            source bee install maven --skip test
                          else
                            BeeVersion=$(curl -SsL https://git.io/stable-bee)
                            curl -SsL -o bee-${BeeVersion}.jar https://jitpack.io/com/github/teletha/bee/${BeeVersion}/bee-${BeeVersion}.jar
                            java -cp bee-${BeeVersion}.jar bee.Bee install maven --skip test
                          fi

                          # To support SNAPSHOT and Commit ID version, read the VERSION in version.txt,
                          # not the VERSION in the environment variable.
                          ProductVersion=$(cat version.txt | xargs)

                          # Until the end of 2024, Jitpack would recognize it as an Artifact if I put the appropriate
                          # Jar files, etc. in the right place. However, since 2025, Jitpack no longer recognizes them.
                          # But, I found that I could build without any problem if I sent the following Maven log-like
                          # string to standard output. NO WAY!
                          echo "[INFO] Installing /home/jitpack/build/pom.xml to /home/jitpack/.m2/repository/${GROUP//./\\/}/${ARTIFACT}/${ProductVersion}/${ARTIFACT}-${ProductVersion}.pom"
                        """, javaVersion, javaVersion, javaVersion, javaVersion, javaVersion, javaVersion));
    }

    @Command("Generate CI/CD configuration files for Maven Central.")
    default void release() {
        VCS vcs = project().getVersionControlSystem();

        if (vcs == null) {
            ui().info("No version control system.");
            return;
        }

        String javaVersion = Inputs.normalize(project().getJavaSourceVersion());
        String product = project().getProduct();
        String group = project().getGroup();
        // A Maven repository layout separates the group with slashes, where a Central Portal
        // namespace separates it with dots.
        String layout = group.replace('.', '/') + "/" + product;

        // The version which the release task writes. It follows the tag format of the publishing
        // workflows, hence no v prefix and the same shape as version.txt.
        String version = project().getVersion();

        String release = """
                name: Release

                # The release task bumps the version and pushes the release commit, then dispatches
                # this workflow with the version. This workflow builds the release revision, creates
                # the version tag once the build has succeeded, lets JReleaser create the GitHub
                # Release with the changelog generated from the conventional commits, and publishes
                # the artifacts to Maven Central. The tag is created here, after the build, so a
                # successful release always has its tag and a failed one does not leave the tag.
                on:
                  repository_dispatch:
                    types: [release]
                  workflow_dispatch:
                    inputs:
                      version:
                        description: The released version, such as %s.
                        type: string
                        required: true

                # The run name carries the dispatch nonce so that the release task can find the run
                # which it started.
                run-name: Release ${{ github.event.client_payload.version || inputs.version }} [${{ github.event.client_payload.nonce || 'manual' }}]

                # The shell declares bash so that pipefail is enabled, which makes the pipeline in
                # the version reading step below fail when a component of it fails.
                defaults:
                  run:
                    shell: bash

                # A publish must never be cancelled halfway, and a re-dispatch must wait for the
                # running one rather than deploying the same version at the same time. The version
                # is part of the group, because a manual run of another version is not the same
                # deploy.
                concurrency:
                  group: release-${{ github.event.client_payload.version || inputs.version }}
                  cancel-in-progress: false

                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    timeout-minutes: 20
                    permissions:
                      # JReleaser creates the GitHub Release and reads the repository to build the
                      # changelog.
                      contents: write
                    steps:
                    - name: Check publishing secrets
                      id: secrets
                      env:
                        MAVEN_CENTRAL_USERNAME: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
                        MAVEN_CENTRAL_TOKEN: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
                        MAVEN_CENTRAL_GPG_PRIVATE_KEY: ${{ secrets.MAVEN_CENTRAL_GPG_PRIVATE_KEY }}
                        MAVEN_CENTRAL_GPG_PUBLIC_KEY: ${{ secrets.MAVEN_CENTRAL_GPG_PUBLIC_KEY }}
                        MAVEN_CENTRAL_GPG_PASSPHRASE: ${{ secrets.MAVEN_CENTRAL_GPG_PASSPHRASE }}
                      run: |
                        # A missing secret must not fail the whole release. The GitHub Release is
                        # still created, and only the Maven Central deploy is skipped.
                        missing=()
                        for name in MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_TOKEN MAVEN_CENTRAL_GPG_PRIVATE_KEY MAVEN_CENTRAL_GPG_PUBLIC_KEY MAVEN_CENTRAL_GPG_PASSPHRASE; do
                          if [ -z "${!name}" ]; then
                            missing+=("${name}")
                          fi
                        done

                        if [ ${#missing[@]} -gt 0 ]; then
                          echo "::warning::Not registered as a GitHub Actions secret: ${missing[*]}. The Maven Central deploy is skipped."
                          echo "publish=false" >> "$GITHUB_OUTPUT"
                        else
                          echo "publish=true" >> "$GITHUB_OUTPUT"
                        fi

                    - name: Resolve the release version
                      id: release
                      env:
                        DISPATCH_VERSION: ${{ github.event.client_payload.version }}
                        MANUAL_VERSION: ${{ inputs.version }}
                      run: |
                        value="${DISPATCH_VERSION:-${MANUAL_VERSION}}"
                        if [ -z "${value}" ]; then
                          value=$(cat version.txt | xargs)
                          echo "::warning::No version was given, using ${value} from version.txt."
                        fi
                        echo "version=${value}" >> "$GITHUB_OUTPUT"
                        echo "Releasing %s ${value}"

                    - name: Check out repository
                      uses: actions/checkout@v7
                      with:
                        # The release commit on the default branch, so the tag is created on it.
                        fetch-depth: 0

                    - name: Set up JDK
                      uses: actions/setup-java@v6
                      with:
                        distribution: zulu
                        java-version: %s

                    - name: Cache bee local repository
                      uses: actions/cache@v6
                      with:
                        path: ${{ env.JAVA_HOME }}/lib/bee/repository
                        key: ${{ runner.os }}-bee-${{ hashFiles('**/pom.xml') }}
                        restore-keys: ${{ runner.os }}-bee

                    - name: Build artifacts
                      run: |
                        if [ -e "bee" ]; then
                          source bee install
                        else
                          version=$(curl -SsL https://git.io/stable-bee)
                          curl -SsL -o bee-${version}.jar https://jitpack.io/com/github/teletha/bee/${version}/bee-${version}.jar
                          java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -cp bee-${version}.jar bee.Bee install
                        fi

                    - name: Stage artifacts in Maven repository layout
                      env:
                        PRODUCT_VERSION: ${{ steps.release.outputs.version }}
                      run: |
                        # Maven Central requires a jar, a sources jar, a javadoc jar and a POM file
                        # arranged under the group path. JReleaser signs, checksums and bundles
                        # whatever it finds in this directory.
                        directory=target/staging-deploy/%s/${PRODUCT_VERSION}
                        mkdir -p ${directory}
                        cp target/%s-${PRODUCT_VERSION}.jar ${directory}/
                        cp target/%s-${PRODUCT_VERSION}-sources.jar ${directory}/
                        cp target/%s-${PRODUCT_VERSION}-javadoc.jar ${directory}/
                        cp pom.xml ${directory}/%s-${PRODUCT_VERSION}.pom
                        ls -l ${directory}

                    - name: Skip the Maven Central deploy
                      if: steps.secrets.outputs.publish != 'true'
                      run: |
                        # Disable the signing and the Central deploy so that JReleaser only creates
                        # the GitHub Release. Both settings use the same RELEASE value.
                        sed -i 's/active: RELEASE/active: NEVER/g' jreleaser.yml

                    - name: Create the version tag
                      env:
                        PRODUCT_VERSION: ${{ steps.release.outputs.version }}
                      run: |
                        # The tag is created only after the build has succeeded, so a failed release
                        # does not leave a tag. JReleaser then creates the GitHub Release for it.
                        if git rev-parse -q --verify "refs/tags/${PRODUCT_VERSION}" >/dev/null; then
                          echo "The tag ${PRODUCT_VERSION} already exists."
                        else
                          git tag "${PRODUCT_VERSION}"
                          git push origin "${PRODUCT_VERSION}"
                        fi

                    - name: Release with JReleaser
                      uses: jreleaser/release-action@v2
                      env:
                        # Keep JReleaser in sync with the version which the release task wrote into version.txt.
                        JRELEASER_PROJECT_VERSION: ${{ steps.release.outputs.version }}
                        JRELEASER_GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
                        JRELEASER_GPG_SECRET_KEY: ${{ secrets.MAVEN_CENTRAL_GPG_PRIVATE_KEY }}
                        JRELEASER_GPG_PUBLIC_KEY: ${{ secrets.MAVEN_CENTRAL_GPG_PUBLIC_KEY }}
                        JRELEASER_GPG_PASSPHRASE: ${{ secrets.MAVEN_CENTRAL_GPG_PASSPHRASE }}
                        JRELEASER_MAVENCENTRAL_CENTRAL_USERNAME: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
                        JRELEASER_MAVENCENTRAL_CENTRAL_PASSWORD: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
                      with:
                        version: 1.26.0
                        arguments: release --debug
                """;

        // In a format string, a double brace denotes a single brace, so {{projectVersion}} is
        // rendered as {projectVersion}, which is a JReleaser expression and not a placeholder.
        String jreleaser = """
                project:
                  name: %s
                  # Overridden by JRELEASER_PROJECT_VERSION in the release workflow, so that this
                  # file does not have to be regenerated every time the release task bumps version.txt.
                  version: '%s'
                  links:
                    homepage: %s
                release:
                  github:
                    owner: %s
                    name: %s
                    tagName: '{{projectVersion}}'
                    # The tag is created by the release task, so JReleaser creates only the GitHub
                    # Release. The changelog is generated from the conventional commits.
                    skipTag: true
                    skipRelease: false
                    changelog:
                      formatted: ALWAYS
                      preset: conventional-commits
                files:
                  globs:
                    - pattern: target/%s-{{projectVersion}}.jar
                    - pattern: target/%s-{{projectVersion}}-sources.jar
                    - pattern: target/%s-{{projectVersion}}-javadoc.jar
                signing:
                  pgp:
                    active: RELEASE
                    armored: true
                    # The key material is injected through JRELEASER_GPG_SECRET_KEY,
                    # JRELEASER_GPG_PUBLIC_KEY and JRELEASER_GPG_PASSPHRASE.
                deploy:
                  maven:
                    mavenCentral:
                      central:
                        active: RELEASE
                        url: https://central.sonatype.com/api/v1/publisher
                        # The directory prepared by the release workflow, in Maven repository layout.
                        stagingRepositories:
                          - target/staging-deploy
                        # The registered namespace of the Sonatype Central Portal account.
                        namespace: %s
                        applyMavenCentralRules: true
                """;

        // The arguments follow the order in which the placeholders appear in the template above:
        // the description names the version, the version step names the product, the JDK step takes
        // the java version, the staging path takes the repository layout and the rest the product.
        makeFile(".github/workflows/release.yml",
                String.format(release, version, product, javaVersion, layout, product, product, product, product));

        makeFile("jreleaser.yml", String
                .format(jreleaser, product, project().getVersion(), vcs.uri(), vcs.owner, vcs.repo, product, product, product, group));
    }

    @Command("Generate .gitignore file.")
    default void gitignore() {
        File ignore = project().getRoot().file(".gitignore");

        makeFile(ignore, update(ignore.lines().toList()));
    }

    /**
     * Update gitignore configuration.
     * 
     * @param lines Lines to update.
     * @return An updated lines.
     */
    default List<String> update(List<String> lines) {
        StringJoiner uri = new StringJoiner(",", "https://www.gitignore.io/api/", "").add("Java").add("Maven").add("Windows").add("Linux");

        // IDE
        for (IDESupport ide : I.find(IDESupport.class)) {
            uri.add(ide.toString());
        }

        LinkedList<String> updated = I.http(uri.toString(), String.class)
                .waitForTerminate()
                .flatArray(rule -> rule.split("\\R"))
                .startWith(".*", "!/.gitignore", "!/.github")
                .startWith(lines)
                .take(HashSet::new, (set, v) -> v.isBlank() || set.add(v))
                .toCollection(new LinkedList());

        while (updated.peekLast().isBlank()) {
            updated.pollLast();
        }
        // The file writer only separates the lines it is given, so an empty last line is what makes
        // the file end with a line separator.
        updated.add("");
        return updated;
    }
}