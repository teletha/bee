
/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
import java.lang.reflect.Field;
import java.util.TreeSet;

import bee.Bee;
import kiss.I;

public class Project extends bee.api.Project {

    String resolver = "1.4.2";

    {
        product(Bee.Tool.getGroup(), Bee.Tool.getProduct(), Bee.Tool.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("org.apache.maven", "maven-resolver-provider");
        require("org.apache.maven.resolver", "maven-resolver-api", resolver);
        require("org.apache.maven.resolver", "maven-resolver-spi", resolver);
        require("org.apache.maven.resolver", "maven-resolver-util", resolver);
        require("org.apache.maven.resolver", "maven-resolver-impl", resolver);
        require("org.apache.maven.resolver", "maven-resolver-connector-basic", resolver);
        require("org.apache.maven.resolver", "maven-resolver-transport-http", resolver);

        require("net.bytebuddy", "byte-buddy");
        require("net.bytebuddy", "byte-buddy-agent");
        require("org.junit.platform", "junit-platform-engine");
        require("org.junit.platform", "junit-platform-launcher");
        require("org.slf4j", "slf4j-nop");
        require("org.slf4j", "jcl-over-slf4j");
        require("org.slf4j", "jul-to-slf4j");
        require("com.github.teletha", "antibug").atTest();
        require("com.github.teletha", "sinobu", "[2.0,)");
        require("com.github.teletha", "psychopath");
        require("com.github.teletha", "stoneforge");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        versionControlSystem("https://github.com/teletha/bee");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            Field libraries = bee.api.Project.class.getDeclaredField("libraries");
            libraries.setAccessible(true);
            Object stored = libraries.get(this);
            libraries.set(this, new TreeSet());
            String pom = super.toString();
            libraries.set(this, stored);
            return pom;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}