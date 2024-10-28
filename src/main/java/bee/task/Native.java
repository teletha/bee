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
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import psychopath.Option;

public class Native extends Task {

    /** The location of graal builders. */
    protected Directory jdk;

    /** The available protocols. default is 'http,https' */
    protected String protocols = "http,https";

    /** The platform type. */
    private String kind = "windows-x64";

    /** The configuration directory. */
    private Directory config = project.getOutput().directory("native-config/" + kind + "-" + project.getVersion()).create();

    /** The output root. */
    private Directory output = project.getOutput().directory("native/" + qualifier()).create();

    /** The executional file. */
    private File executional = output.file(project.getProduct());

    /** The artifact's archive. */
    private File archive = project.getOutput().file(qualifier() + ".zip");

    /**
     * Compute the product qualifier.
     * 
     * @return
     */
    private String qualifier() {
        return project.getProduct() + "-" + kind + "-" + project.getVersion();
    }

    @Command(value = "Build native execution file.", defaults = true)
    public File build() {
        // run test with native-image-agent
        Test test = I.make(Test.class);
        test.java = findGraalVM(kind);
        test.params.add("-agentlib:native-image-agent=config-output-dir=" + config.path());
        test.test();

        require(Jar::source);
        String main = require(FindMain::main);
        Directory graal = findGraalVM(kind);

        buildRuntimeInfo();

        // build native-image command
        List<String> command = new ArrayList(Platform.isWindows() ? List.of("cmd", "/c") : List.of("bash", "-c"));
        command.add(graal.file("bin/native-image").path());

        // output location
        command.add("-o");
        command.add(executional.path());

        command.add("--no-fallback");
        command.add("--enable-url-protocols=" + protocols);

        // for debug
        command.add("-Ob");
        command.add("--color=always");
        command.add("-H:+ReportExceptionStackTraces");
        // command.add("-H:+PrintClassInitialization");

        command.add("-H:ConfigurationFileDirectories=" + config.path());

        // locate codes
        command.add("--class-path");
        command.add(project.getClasspath().stream().collect(Collectors.joining(java.io.File.pathSeparator)));

        // entry point
        command.add(main);

        if (bee.util.Process.with().run(command) == 0) {
            pack(output, archive);

            return archive;
        } else {
            Fail fail = new Fail("Fail to build native execution file.").solve("Executing: " + String.join(" ", command));

            if (Platform.isWindows()) {
                // ===============================================================
                // Windows - https://www.graalvm.org/latest/getting-started/windows/
                // ===============================================================
                // 1 - Installing from an Archive (Automatically)
                // 2 - Install Visual Studio Build Tools and Windows SDK (Manually, validate it)
                fail.solve("Install Visual Studio Build Tools and Windows SDK. see https://www.graalvm.org/latest/getting-started/windows/");
            }
            throw fail;
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
            unpack(temp, dest, Option::strip);
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

                    makeFile(output.file(".env"), Extensible.class.getName() + "=" + extensions);
                }
            }
        };
    }
}