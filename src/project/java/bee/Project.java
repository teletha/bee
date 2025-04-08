/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.api.License.*;

import javax.lang.model.SourceVersion;

import bee.task.FindMain;

public class Project extends bee.api.Project {

    {
        product(Bee.Tool.getGroup(), Bee.Tool.getProduct(), ref("version.txt"));
        license(MIT);
        describe("""
                Bee is an open source build automation tool that focuses on conventions, type safety and performance.
                Project and build task definitions are written in Java, ensuring flexible extensibility for programmers.

                #### Minimize settings
                Use default values to minimize the number of items that need to be set as much as possible. Also, all settings are type-safe and completion, so you don't have to search for minor settings in the documentation.

                #### Fast execution
                All tasks are executed in parallel, and all output is cached and reused.

                #### Repository oriented
                It recognizes source code and package repositories and automates the entire lifecycle from development to release.


                ## Install by script
                Open your terminal and execute the following command to install bee.

                #### In Linux / MacOS
                ```bash
                curl -Ls https://git.io/install-bee | bash
                ```

                #### In Windows
                ```cmd
                curl -Ls https://git.io/install-bee -o install.bat && install
                ```

                After installation is complete, verify that the tool was installed successfully by running:
                ```
                bee -v
                ```

                ## Basic Usage
                Once installed, you can start using Bee by running the following commands:

                #### Setup with your IDE
                ```
                bee ide
                ```
                #### Install library into local repository
                ```
                bee install
                ```
                """);

        require(SourceVersion.latest(), SourceVersion.RELEASE_24);

        // MAVEN REPOSITORY
        // Since 4.0.0-beta, Maven has become a super heavyweight library, with dependencies
        // on woodstox for XML parsing, bouncycastle for checksum(?) and apache-http-client
        // for HTTP communication.
        // Maven seems to thoroughly adhere to backward compatibility, so further version upgrades
        // are currently unnecessary.
        //
        // The use of alpha-8 causes a dependency on woodstox, so it is stopped at alpha-7.
        String version = "2.0.0-alpha-7";
        require("org.apache.maven", "maven-resolver-provider", "4.0.0-alpha-7");
        require("org.apache.maven.resolver", "maven-resolver-api", version);
        require("org.apache.maven.resolver", "maven-resolver-spi", version);
        require("org.apache.maven.resolver", "maven-resolver-util", version);
        require("org.apache.maven.resolver", "maven-resolver-impl", version);
        require("org.apache.maven.resolver", "maven-resolver-connector-basic", version);
        require("org.apache.maven.resolver", "maven-resolver-named-locks", version);

        // LOGGER
        require("org.slf4j", "slf4j-api", "[2.0,)");
        // require("org.slf4j", "slf4j-nop");
        // require("org.slf4j", "jul-to-slf4j");
        require("com.github.teletha", "conjure");

        // REQUIRED
        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        require("com.github.teletha", "auto483");

        // DYNAMICALLY ON RUNTIME
        require("org.ow2.asm", "asm").atProvided();
        require("org.junit.platform", "junit-platform-engine").atProvided();
        require("org.junit.platform", "junit-platform-launcher").atProvided();
        require("com.github.teletha", "javadng").atProvided();
        require("org.eclipse.jgit", "org.eclipse.jgit").atProvided();
        require("org.eclipse.jdt", "ecj").atProvided();
        require("io.github.classgraph", "classgraph").atProvided();
        // require("org.graalvm.polyglot", "polyglot").atProvided();
        // require("org.graalvm.polyglot", "java-community").atProvided().byPom();
        // require("org.graalvm.polyglot", "js-community").atProvided().byPom();
        // require("org.graalvm.polyglot", "python-community").atProvided().byPom();
        // require("org.graalvm.espresso", "java").atProvided().byPom();
        // require("org.graalvm.espresso", "espresso-runtime-resources-jdk21").atProvided();

        // TEST
        require("com.github.teletha", "antibug").atTest();

        unrequire("commons-codec", "commons-codec");
        unrequire("org.apache.maven", "plexus-utils");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.inject");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.plexus");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        config(FindMain.class, task -> {
            task.main = BeeInstaller.class.getName();
        });
    }
}