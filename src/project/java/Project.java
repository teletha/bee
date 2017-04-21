
/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
import bee.Bee;

/**
 * @version 2016/04/21 14:11:15
 */
public class Project extends bee.api.Project {

    String AetherGroup = "org.eclipse.aether";

    String AetherVersion = "1.1.0";

    {
        product(Bee.TOOL.getGroup(), Bee.TOOL.getProduct(), Bee.TOOL.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("com.github.teletha", "sinobu", "1.0");
        require("com.github.teletha", "filer", "0.5");
        require("com.github.teletha", "antibug", "0.3").atTest();
        require("cglib", "cglib", "3.2.5");
        require("junit", "junit", "4.12").atProvided();
        require(AetherGroup, "aether-api", AetherVersion);
        require(AetherGroup, "aether-util", AetherVersion);
        require(AetherGroup, "aether-impl", AetherVersion);
        require(AetherGroup, "aether-connector-basic", AetherVersion);
        require(AetherGroup, "aether-transport-http", AetherVersion);
        require("org.apache.maven", "maven-aether-provider", "3.3.9");
        require("org.slf4j", "slf4j-nop", "1.7.24");
        requireJavaTools().atSystem();

        unrequire("org.apache.ant", "ant"); // from cglib
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        repository("https://repo.eclipse.org/content/repositories/egit-releases/");

        versionControlSystem("https://github.com/teletha/bee");
    }
}
