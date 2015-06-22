
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
 * @version 2012/03/20 15:45:08
 */
public class Project extends bee.api.Project {

    private String aetherGroup = "org.eclipse.aether";

    private String aetherVersion = "1.0.2.v20150114";

    {
        product(Bee.TOOL.getGroup(), Bee.TOOL.getProduct(), Bee.TOOL.getVersion());
        producer("Nameless Production Committee");
        describe("Task based project builder for Java");

        require("npc", "sinobu", "0.9.4");
        require("npc", "antibug", "0.3").atTest();
        require(aetherGroup, "aether-api", aetherVersion);
        require(aetherGroup, "aether-util", aetherVersion);
        require(aetherGroup, "aether-impl", aetherVersion);
        require(aetherGroup, "aether-transport-file", aetherVersion);
        require(aetherGroup, "aether-transport-wagon", aetherVersion);
        // require(aetherGroup, "aether-connector-wagon", "0.9.0-SNAPSHOT");
        require("org.apache.maven", "maven-aether-provider", "3.3.3");
        require("org.apache.maven.wagon", "wagon-http-lightweight", "2.9");
        // require("com.google.code.gson", "gson", "2.3");
        require("sun.jdk", "tools", "8.0").atSystem();

        unrequire("org.apache.maven.wagon", "wagon-http-shared");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");
        unrequire("org.sonatype.sisu", "*");
    }
}
