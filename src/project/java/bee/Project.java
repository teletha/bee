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

public class Project extends bee.api.Project {

    String resolver = "1.6.2";

    {
        product(Bee.Tool.getGroup(), Bee.Tool.getProduct(), ref("version.txt"));
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("org.apache.maven", "maven-resolver-provider");
        require("org.apache.maven.resolver", "maven-resolver-api", resolver);
        require("org.apache.maven.resolver", "maven-resolver-spi", resolver);
        require("org.apache.maven.resolver", "maven-resolver-util", resolver);
        require("org.apache.maven.resolver", "maven-resolver-impl", resolver);
        require("org.apache.maven.resolver", "maven-resolver-connector-basic", resolver);
        require("org.apache.maven.resolver", "maven-resolver-transport-http", resolver);

        require("net.bytebuddy", "byte-buddy-agent");
        require("org.ow2.asm", "asm");
        require("org.junit.platform", "junit-platform-engine", "1.7.1").atProvided();
        require("org.junit.platform", "junit-platform-launcher", "1.7.1").atProvided();
        require("org.slf4j", "slf4j-nop");
        require("org.slf4j", "jcl-over-slf4j");
        require("org.slf4j", "jul-to-slf4j");
        require("com.github.teletha", "antibug").atTest();
        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        unrequire("commons-codec", "commons-codec");
        unrequire("org.eclipse.sisu", "org.eclipse.sisu.inject");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");
        unrequire("javax.inject", "javax.inject");

        versionControlSystem("https://github.com/teletha/bee");
    }
}