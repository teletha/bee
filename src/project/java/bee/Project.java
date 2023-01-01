/*
 * Copyright (C) 2023 The BEE Development Team
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

public class Project extends bee.api.Project {

    {
        product(Bee.Tool.getGroup(), Bee.Tool.getProduct(), ref("version.txt"));
        license(MIT);
        describe("""
                Bee is an open source build automation tool that focuses on conventions, type safety and performance.
                Project and build task definitions are written in Java, ensuring flexible extensibility for programmers.

                #### Minimize settings
                Use default values to minimize the number of items that need to be set as much as possible. Also, all settings are type-safe and complementary, so you don't have to search for minor settings in the documentation.

                #### Fast execution
                All tasks are executed in parallel, and all output is cached and reused.

                #### Repository oriented
                It recognizes source code and package repositories and automates the entire lifecycle from development to release.
                """);

        require(SourceVersion.RELEASE_16);

        // MAVEN REPOSITORY // 4.0.0-alpha-3 is buggy
        require("org.apache.maven", "maven-resolver-provider", "4.0.0-alpha-2");
        require("org.apache.maven.resolver", "maven-resolver-api");
        require("org.apache.maven.resolver", "maven-resolver-spi");
        require("org.apache.maven.resolver", "maven-resolver-util");
        require("org.apache.maven.resolver", "maven-resolver-impl");
        require("org.apache.maven.resolver", "maven-resolver-connector-basic");
        require("org.apache.maven.resolver", "maven-resolver-transport-http");

        // LOGGER
        require("org.slf4j", "slf4j-nop");
        require("org.slf4j", "jcl-over-slf4j");
        require("org.slf4j", "jul-to-slf4j");

        // REQUIRED
        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        require("org.ow2.asm", "asm");

        // DYNAMICALLY ON RUNTIME
        require("org.junit.platform", "junit-platform-engine").atProvided();
        require("org.junit.platform", "junit-platform-launcher").atProvided();
        require("net.bytebuddy", "byte-buddy-agent").atProvided();
        require("com.github.teletha", "javadng").atProvided();
        require("org.eclipse.jgit", "org.eclipse.jgit").atProvided();
        require("org.eclipse.jdt", "ecj").atProvided();

        // TEST
        require("com.github.teletha", "antibug").atTest();

        unrequire("commons-codec", "commons-codec");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.inject");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");
    }
}