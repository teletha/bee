/*
 * Copyright (C) 2021 Nameless Production Committee
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
        describe("Task based project builder for Java");
        license(MIT);

        require(SourceVersion.RELEASE_16, SourceVersion.RELEASE_11, SourceVersion.RELEASE_16);

        // MAVEN REPOSITORY
        require("org.apache.maven", "maven-resolver-provider");
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

        // TEST
        require("com.github.teletha", "antibug", "1.2.0").atTest();

        unrequire("commons-codec", "commons-codec");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.inject");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        versionControlSystem("https://github.com/teletha/bee");

    }

    public class API extends bee.api.Project {
        {
            product(Bee.API.getGroup(), Bee.API.getProduct(), ref("version.txt"));
            describe("Task based project builder for Java");
        }
    }
}