/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
import bee.api.ProjectDefinition;

/**
 * @version 2012/03/20 15:45:08
 */
@ProjectDefinition(group = "sass", name = "bee", version = "0.1")
public class Project extends bee.api.Project {

    private String aetherVersion = "1.13.1";

    {
        name("npc", "bee", "0.1");
        describe("Task based project builder for Java");

        require("npc", "sinobu", "0.9.1");
        require("npc", "antibug", "0.2").atTest();
        require("org.sonatype.aether", "aether-api", aetherVersion);
        require("org.sonatype.aether", "aether-util", aetherVersion);
        require("org.sonatype.aether", "aether-impl", aetherVersion);
        require("org.sonatype.aether", "aether-connector-file", aetherVersion);
        require("org.sonatype.aether", "aether-connector-wagon", aetherVersion);
        require("org.apache.maven", "maven-aether-provider", "3.0.4");
        require("org.apache.maven.wagon", "wagon-http-lightweight", "1.0");

        unrequire("org.apache.maven.wagon", "wagon-http-shared");
        unrequire("org.codehaus.plexus", "plexus-classworlds");
        unrequire("org.codehaus.plexus", "plexus-component-annotations");
        unrequire("org.sonatype.sisu", "*");
    }
}
