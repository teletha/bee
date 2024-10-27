/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Loader;
import bee.api.Require;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import kiss.Extensible;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import psychopath.Option;

public class Native extends Task {

    /** The location of graal builders. */
    protected Directory jdk;

    /** The output location of the generated native execution file. */
    protected File output = project.getOutput().file(project.getProduct() + "-" + project.getVersion());

    /** The available protocols. default is 'http,https' */
    protected String protocols = "http,https";

    @Command(value = "Build native execution file.", defaults = true)
    public void build() {
        require(Jar::source);

        Directory jdk = detectEnvironment(false, "23", "windows-x64");
        buildRuntimeInfo();

        // build native-image command
        List<String> command = new ArrayList(Platform.isWindows() ? List.of("cmd", "/c") : List.of("bash", "-c"));
        command.add(jdk.file("bin/native-image").path());

        // output location
        // command.add("-o");
        // command.add(output.path());

        command.add("--no-fallback");
        command.add("--enable-url-protocols=" + protocols);

        // for debug
        command.add("-H:+ReportExceptionStackTraces");
        command.add("-H:+PrintClassInitialization");

        //
        command.add("-H:ConfigurationFileDirectories=agent");

        // locate codes
        command.add("--class-path");
        command.add(project.getClasspath().stream().collect(Collectors.joining(java.io.File.pathSeparator)));

        // entry point
        command.add(require(FindMain::main));

        // bee.util.Process.with().run(command);
    }

    private Directory detectEnvironment(boolean needJavaFX, String version, String platform) {
        if (jdk != null) {
            return jdk;
        }

        Directory dest = Platform.BeeHome.directory("jdk").directory((needJavaFX ? "gluon-" : "graal-") + version);

        if (dest.isAbsent()) {
            String url = needJavaFX
                    ? "https://github.com/gluonhq/graal/releases/download/gluon-23+25.1-dev-2409082136/graalvm-java23-windows-amd64-gluon-23+25.1-dev.zip"
                    : "https://download.oracle.com/graalvm/" + version + "/archive/graalvm-jdk-" + version + "_" + platform + "_bin.zip";

            // download archive
            File temp = Locator.temporaryFile();
            Loader.donwload(url, temp);

            // unpack to local jdk holder
            unpackFile(temp, dest, Option::strip);
        }
        return dest;
    }

    private void buildRuntimeInfo() {
        new Require("io.github.classgraph:classgraph") {
            {
                try (ScanResult scan = new ClassGraph().enableAllInfo().overrideClasspath(project.getClasspath()).scan()) {
                    String extensions = scan.getClassesImplementing(Extensible.class)
                            .stream()
                            .map(info -> info.getName())
                            .collect(Collectors.joining(","));

                    makeFile(".env", Extensible.class.getName() + "=" + extensions);
                }
            }
        };
    }

    @Command("Run anlyzer agent.")
    public void agent() {
    }
}
