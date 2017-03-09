
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

    private String aetherGroup = "org.eclipse.aether";

    private String aetherVersion = "1.1.0";

    {
        product(Bee.TOOL.getGroup(), Bee.TOOL.getProduct(), Bee.TOOL.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("com.github.teletha", "sinobu", "1.0");
        require("com.github.teletha", "antibug", "0.3").atTest();
        require("junit", "junit", "4.12").atProvided();
        require(aetherGroup, "aether-api", aetherVersion);
        require(aetherGroup, "aether-util", aetherVersion);
        require(aetherGroup, "aether-impl", aetherVersion);
        require(aetherGroup, "aether-connector-basic", aetherVersion);
        require(aetherGroup, "aether-transport-http", aetherVersion);
        require("org.apache.maven", "maven-aether-provider", "3.3.9");
        require("org.slf4j", "slf4j-nop", "1.7.22");
        require("sun.jdk", "tools", "8.0").atSystem();
        require("org.bytedeco", "javacv-platform", "1.3.1");

        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");

        repository("https://repo.eclipse.org/content/repositories/egit-releases/");

        versionControlSystem("https://github.com/teletha/bee");
    }
}
