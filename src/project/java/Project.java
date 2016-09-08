
/*
 * Copyright (C) 2015 Nameless Production Committee
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

        require("npc", "sinobu", "1.0");
        require("npc", "antibug", "0.3").atTest();
        require(aetherGroup, "aether-api", aetherVersion);
        require(aetherGroup, "aether-util", aetherVersion);
        require(aetherGroup, "aether-impl", aetherVersion);
        require(aetherGroup, "aether-connector-basic", aetherVersion);
        // require(aetherGroup, "aether-transport-file", aetherVersion);
        require(aetherGroup, "aether-transport-wagon", aetherVersion);
        require("org.apache.maven", "maven-aether-provider", "3.3.3");
        require("org.apache.maven.wagon", "wagon-http-lightweight", "2.9");
        require("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "4.0.0.201506090130-r");
        require("com.google.code.gson", "gson", "2.3.1");
        require("sun.jdk", "tools", "8.0").atSystem();
        requireLombok();

        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");
        unrequire("org.apache.maven.wagon", "wagon-http-shared");

        repository("https://repo.eclipse.org/content/repositories/egit-releases/");
    }
}
