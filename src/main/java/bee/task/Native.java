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

import java.awt.Desktop;
import java.io.IOException;
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

    /** The available protocols. default is 'http,https' */
    protected String protocols = "http,https";

    /** The additional parameters. */
    protected List<String> params = new ArrayList();

    /** The graal kind. */
    private String kind = project.getDependency(Scope.Runtime).stream().anyMatch(lib -> lib.group.equals("org.openjfx")) ? "gluon"
            : "graal";

    /** The graal version. */
    private int version = project.getJavaClassVersion().runtimeVersion().feature();

    /** The platform type. */
    private String target = detectOS() + "-" + detectArch();

    /** The configuration directory. */
    private Directory config = project.getOutput().directory("native-config/" + kind + "-" + qualifier()).create();

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
        return project.getProduct() + "-" + target + "-" + project.getVersion();
    }

    @Command(value = "Build native execution file.", defaults = true)
    public File build() {
        Directory graal = findGraalVM();

        // run test with native-image-agent
        Test test = I.make(Test.class);
        test.java = graal;
        test.params.add("-agentlib:native-image-agent=config-output-dir=" + config.path());
        test.test();

        require(Jar::source);
        String main = require(FindMain::main);

        buildRuntimeInfo();

        // build native-image command
        List<String> command = new ArrayList(Platform.isWindows() ? List.of("cmd", "/c") : List.of("bash", "-c"));
        command.add(graal.file("bin/native-image").path());

        command.addAll(params);

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
            pack(output, archive, o -> o.glob("*"));

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

    @Command("Run the native executable.")
    public void run() {
        try {
            Desktop.getDesktop().open(executional.extension(Platform.isWindows() ? "exe" : "").asJavaFile());
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Detect GraalVM.
     * 
     * @return
     */
    private Directory findGraalVM() {
        Directory dest = Platform.BeeHome.directory("jdk").directory(kind + "-" + version);

        if (dest.isAbsent()) {
            String url = kind.equals("gluon")
                    ? "https://github.com/gluonhq/graal/releases/download/gluon-23+25.1-dev-2409082136/graalvm-java23-windows-amd64-gluon-23+25.1-dev.zip"
                    : "https://download.oracle.com/graalvm/" + version + "/archive/graalvm-jdk-" + version + "_" + target + "_bin.zip";

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

    private static String detectOS() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("win")) {
            return "windows";
        } else if (name.contains("nux") || name.contains("nix")) {
            return "linux";
        } else if (name.contains("mac")) {
            return "macos";
        } else {
            throw new Error("Unknown OS [" + name + "]");
        }
    }

    private static String detectArch() {
        String name = System.getProperty("os.arch").toLowerCase();
        if (name.contains("x86_64") || name.contains("amd64")) {
            return "x64";
        } else if (name.equals("x86") || name.equals("i386")) {
            return "x86";
        } else if (name.equals("aarch64") || name.equals("arm64")) {
            return "aarch64";
        } else if (name.equals("arm") || name.equals("arm32")) {
            return "arm";
        } else {
            throw new Error("Unknown Architecture [" + name + "]");
        }
    }
}