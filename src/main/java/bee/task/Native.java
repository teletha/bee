/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import bee.Fail;
import bee.Isolation;
import bee.Platform;
import bee.Task;
import bee.TaskOperations;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Loader;
import bee.api.Scope;
import bee.coder.FileType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import kiss.Extensible;
import kiss.I;
import kiss.Variable;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import psychopath.Option;

@SuppressWarnings("serial")
public interface Native extends Task<Native.Config> {

    public static class Config implements Serializable {
        @Comment("The available protocols.")
        public List<String> protocols = I.list("http", "https");

        @Comment("The available resources.")
        public List<String> resources = FileType.list().stream().map(FileType::extension).toList();

        @Comment("The additional parameters.")
        public List<String> params = new ArrayList();

        /** The graal kind. */
        private String kind = project().getDependency(Scope.Runtime).stream().anyMatch(lib -> lib.group.equals("org.openjfx")) ? "gluon"
                : "graal";

        /** The graal version. */
        private int version = project().getJavaRequiredVersion().runtimeVersion().feature();

        /** The platform type. */
        private String target = detectOS() + "-" + detectArch();

        /** The configuration directory. */
        private Directory config = project().getOutput().directory("native-config/" + kind + "-" + qualifier()).create();

        /** The output root. */
        private Directory output = project().getOutput().directory("native/" + qualifier()).create();

        /** The executional file. */
        private File executional = output.file(project().getProduct());

        /** The artifact's archive. */
        private File archive = project().getOutput().file(qualifier() + ".zip");

        private Serialization serialization = new Serialization();

        /**
         * Compute the product qualifier.
         * 
         * @return
         */
        private String qualifier() {
            return project().getProduct() + "-" + target + "-" + project().getVersion();
        }
    }

    @Command(value = "Build native execution file.", defaults = true)
    default File build() {
        Variable<String> main = require(FindMain::main);
        if (main.isAbsent()) {
            throw new Fail("Main class is not found.");
        }

        Config conf = config();
        Directory graal = findGraalVM(conf);

        // run test with native-image-agent
        TaskOperations.config(Test.class, test -> {
            test.java = graal;
            test.params.add("-agentlib:native-image-agent=config-output-dir=" + conf.config.path());
        });
        require(Test::test);
        require(Jar::source);

        buildRuntimeInfo(conf);

        // build native-image command
        List<String> command = new ArrayList(Platform.isWindows() ? List.of("cmd", "/c") : List.of("bash", "-c"));
        command.add(graal.file("bin/native-image").path());

        command.addAll(conf.params);

        // output location
        command.add("-o");
        command.add(conf.executional.path());

        command.add("--no-fallback");
        command.add("--enable-url-protocols=" + conf.protocols.stream().collect(Collectors.joining(",")));

        // for debug
        command.add("-Ob");
        command.add("--color=always");
        command.add("-H:+ReportExceptionStackTraces");
        // command.add("-H:+PrintClassInitialization");

        // metadata
        command.add("-H:+UnlockExperimentalVMOptions");
        command.add("-H:ConfigurationFileDirectories=" + conf.config);
        command.add("-H:ResourceConfigurationFiles=" + conf.config.file("auto-resources.json").text(I.express("""
                {=< >=}
                {
                  "resources": [
                    <#.>
                    { "pattern": ".*.<.>$" },
                    </.>
                    { "pattern": ".*.zip$" }
                  ]
                }
                """, conf.resources)));
        command.add("-H:SerializationConfigurationFiles=" + conf.config.file("auto-serialize.json").text(I.write(conf.serialization)));

        // locate codes
        command.add("--class-path");
        command.add(I.signal(project().getClasspath())
                .sort(Comparator.naturalOrder())
                .scan(Collectors.joining(java.io.File.pathSeparator))
                .to().v);

        // entry point
        command.add(main.v);

        if (bee.util.Process.with().run(command) == 0) {
            pack(conf.output, conf.archive, o -> o.glob("*"));

            return conf.archive;
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
    default void run() {
        Config conf = config();
        bee.util.Process.with().verbose().workingDirectory(conf.output).encoding(project().getEncoding()).run(conf.executional.path());
    }

    /**
     * Detect GraalVM.
     * 
     * @return
     */
    private Directory findGraalVM(Config conf) {
        Directory dest = Platform.BeeHome.directory("jdk").directory(conf.kind + "-" + conf.version);

        if (dest.isAbsent()) {
            String url = conf.kind.equals("gluon")
                    ? "https://github.com/gluonhq/graal/releases/download/gluon-23+25.1-dev-2409082136/graalvm-java23-windows-amd64-gluon-23+25.1-dev.zip"
                    : "https://download.oracle.com/graalvm/" + conf.version + "/archive/graalvm-jdk-" + conf.version + "_" + conf.target + "_bin.zip";

            // download archive
            File temp = Locator.temporaryFile(url.substring(url.lastIndexOf('/') + 1));
            Loader.download(url, temp);

            // unpack to local graal holder
            unpack(temp, dest, Option::strip);
        }
        return dest;
    }

    private void buildRuntimeInfo(Config conf) {
        new Isolation("io.github.classgraph:classgraph") {

            @Override
            public void run() {
                try (ScanResult scan = new ClassGraph().enableAllInfo().overrideClasspath(project().getClasspath()).scan()) {
                    String extensions = scan.getClassesImplementing(Extensible.class)
                            .stream()
                            .map(ClassInfo::getName)
                            .collect(Collectors.joining(","));

                    conf.serialization.lambdaCapturingTypes.addAll(scan.getAllClasses()
                            .stream()
                            .filter(x -> x.hasDeclaredMethod("$deserializeLambda$"))
                            .map(info -> new Item(info.getName()))
                            .toList());

                    StringBuilder builder = new StringBuilder();
                    builder.append(Extensible.class.getName()).append("=").append(extensions).append("\n");

                    makeFile(conf.output.file(".env"), builder.toString());
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

    static class Serialization {

        public List<Item> types = new ArrayList();

        public List<Item> lambdaCapturingTypes = new ArrayList();
    }

    record Item(String name) {
    }
}