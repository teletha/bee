
/*
 * Copyright (C) 2019 Nameless Production Committee
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

    String ResolverVersion = "1.3.1";

    String ByteBuddyVersion = "[1.8.0,)";

    {
        product(Bee.TOOL.getGroup(), Bee.TOOL.getProduct(), Bee.TOOL.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("org.apache.maven", "maven-resolver-provider", "3.6.0");
        require("org.apache.maven.resolver", "maven-resolver-api", ResolverVersion);
        require("org.apache.maven.resolver", "maven-resolver-util", ResolverVersion);
        require("org.apache.maven.resolver", "maven-resolver-impl", ResolverVersion);
        require("org.apache.maven.resolver", "maven-resolver-connector-basic", ResolverVersion);
        require("org.apache.maven.resolver", "maven-resolver-transport-http", ResolverVersion);

        require("net.bytebuddy", "byte-buddy", ByteBuddyVersion);
        require("net.bytebuddy", "byte-buddy-agent", ByteBuddyVersion);
        require("org.junit.platform", "junit-platform-launcher", "1.4.0");
        require("org.slf4j", "slf4j-nop", "1.8.0-beta2");
        require("org.slf4j", "jcl-over-slf4j", "1.8.0-beta2");
        require("com.github.teletha", "sinobu", "[1.2,)");
        require("com.github.teletha", "psychopath", "0.9");
        require("com.github.teletha", "antibug", "0.6").atTest();

        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        repository("https://repo.eclipse.org/content/repositories/egit-releases/");

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
