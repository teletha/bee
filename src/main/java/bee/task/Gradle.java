/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import bee.Task;
import bee.api.Command;
import kiss.I;

public class Gradle extends Task {

    @Command("Generate gradle build file.")
    public void kotlin() {
        StringBuilder builder = new StringBuilder();

        String build = """
                plugins {
                    `java-library`
                    `maven-publish`
                }

                repositories {
                    mavenLocal()
                    maven {
                        url = uri("https://repo1.maven.org/maven2/")
                    }

                    maven {
                        url = uri("https://jitpack.io/")
                    }

                    maven {
                        url = uri("https://repo.maven.apache.org/maven2/")
                    }
                }

                dependencies {
                    testImplementation(libs.com.pgs.soft.httpclientmock)
                    testImplementation(libs.com.github.teletha.antibug)
                    testImplementation(libs.com.github.spullara.mustache.java.compiler)
                    testImplementation(libs.com.lmax.disruptor)
                    testImplementation(libs.com.alibaba.fastjson2.fastjson2)
                    testImplementation(libs.com.google.code.gson.gson)
                    testImplementation(libs.net.sourceforge.htmlcleaner.htmlcleaner)
                    testImplementation(libs.com.fasterxml.jackson.core.jackson.databind)
                    testImplementation(libs.com.fasterxml.jackson.module.jackson.module.afterburner)
                    testImplementation(libs.javax.javaee.api)
                    testImplementation(libs.com.samskivert.jmustache)
                    testImplementation(libs.org.jsoup.jsoup)
                    testImplementation(libs.org.apache.logging.log4j.log4j.core)
                    testImplementation(libs.ch.qos.logback.logback.classic)
                    testImplementation(libs.io.reactivex.rxjava3.rxjava)
                    testImplementation(libs.org.tinylog.tinylog.impl)
                }

                group = "{group}"
                version = "{version}"
                description = "{product}"
                java.sourceCompatibility = JavaVersion.VERSION_1_8

                publishing {
                    publications.create<MavenPublication>("maven") {
                        from(components["java"])
                    }
                }
                """;

        String express = I.express(build, project);
        System.out.println(express);

        makeFile("build.gradl.kts", builder.toString());
    }
}