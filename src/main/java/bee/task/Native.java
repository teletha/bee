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

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Loader;
import bee.api.Require;
import bee.api.Scope;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import kiss.Extensible;
import kiss.Variable;
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
        String main = require(FindMain::main);

        validateBuildEnvironment();

        buildRuntimeInfo();

        Directory graal = findGraalVM("windows-x64");

        // build native-image command
        List<String> command = new ArrayList(Platform.isWindows() ? List.of("cmd", "/c") : List.of("bash", "-c"));
        command.add(graal.file("bin/native-image").path());

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
        command.add(main);

        if (bee.util.Process.with().run(command) != 0) {
            throw new Fail("Fail to build native execution file.");
        }
    }

    /**
     * Validate the requirement of building environment.
     */
    private void validateBuildEnvironment() {
        if (Platform.isWindows()) {
            // ===============================================================
            // Windows - https://www.graalvm.org/latest/getting-started/windows/
            // ===============================================================
            // 1 - Installing from an Archive (Automatically)
            // 2 - Install Visual Studio Build Tools and Windows SDK (Manually, validate it)
            Variable<File> file = Locator.directory("C:\\Program Files\\Microsoft Visual Studio").walkFile("**/cl.exe").first().to();
            if (file.isAbsent()) {
                throw new Fail("On Windows, Native Image requires Visual Studio and Microsoft Visual C++(MSVC). Use Visual Studio 2022 version 17.6.0 or later.")
                        .solve("Install Visual Studio Build Tools and Windows SDK. see https://www.graalvm.org/latest/getting-started/windows/");
            }
        }
    }

    /**
     * Detect GraalVM.
     * 
     * @param platform
     * @return
     */
    private Directory findGraalVM(String platform) {
        if (jdk != null) {
            return jdk;
        }

        boolean needJavaFX = project.getDependency(Scope.Runtime).stream().anyMatch(lib -> lib.group.equals("org.openjfx"));
        int version = project.getJavaClassVersion().runtimeVersion().feature();

        Directory dest = Platform.BeeHome.directory("jdk").directory((needJavaFX ? "gluon-" : "graal-") + version);

        if (dest.isAbsent()) {
            String url = needJavaFX
                    ? "https://github.com/gluonhq/graal/releases/download/gluon-23+25.1-dev-2409082136/graalvm-java23-windows-amd64-gluon-23+25.1-dev.zip"
                    : "https://download.oracle.com/graalvm/" + version + "/archive/graalvm-jdk-" + version + "_" + platform + "_bin.zip";

            // download archive
            File temp = Locator.temporaryFile(url.substring(url.lastIndexOf('/') + 1));
            Loader.donwload(url, temp);

            // unpack to local graal holder
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
