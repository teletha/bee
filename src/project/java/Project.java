
/*
 * Copyright (C) 2018 Nameless Production Committee
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

/**
 * @version 2016/04/21 14:11:15
 */
public class Project extends bee.api.Project {

    String AetherGroup = "org.eclipse.aether";

    String AetherVersion = "[1.1.0,)";

    String ByteBuddyVersion = "[1.8.0,)";

    {
        product(Bee.TOOL.getGroup(), Bee.TOOL.getProduct(), Bee.TOOL.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require(AetherGroup, "aether-api", AetherVersion);
        require(AetherGroup, "aether-util", AetherVersion);
        require(AetherGroup, "aether-impl", AetherVersion);
        require(AetherGroup, "aether-connector-basic", AetherVersion);
        require(AetherGroup, "aether-transport-http", AetherVersion);
        require("org.apache.maven", "maven-aether-provider", "3.3.9");
        require("net.bytebuddy", "byte-buddy", ByteBuddyVersion);
        require("net.bytebuddy", "byte-buddy-agent", ByteBuddyVersion);
        require("org.junit.platform", "junit-platform-launcher", "1.1.0");
        require("org.slf4j", "slf4j-nop", "1.8.0-beta2");
        require("org.slf4j", "jcl-over-slf4j", "1.8.0-beta2");
        require("com.github.teletha", "sinobu", "1.0");
        require("com.github.teletha", "filer", "0.5");
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
