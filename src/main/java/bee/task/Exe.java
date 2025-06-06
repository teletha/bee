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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Library;
import bee.api.Loader;
import bee.api.Scope;
import bee.util.Inputs;
import bee.util.Process;
import kiss.I;
import kiss.Variable;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Location;
import psychopath.Locator;

public interface Exe extends Task<Exe.Config> {

    public static class Config {
        @Comment("The location for icon of exe file.")
        public Path icon;

        @Comment("The usage of custom JRE.")
        public boolean customJRE = true;

        @Comment("The additional packing data.")
        public final Set<Location> resources = new HashSet();
    }

    @Command("Generate windows exe file which executes the main class.")
    default File build() {
        if (!Platform.isWindows()) {
            ui().warn("Skip the task [exe] because it is available in windows platform only.");
            return null;
        }

        // search main classes
        Variable<String> main = require(FindMain::main);
        if (main.isAbsent()) {
            throw new Fail("Main class is not found.");
        }

        require(Test::test);

        // search main class in MANIFEST.MF
        File file = project().getSourceSet()
                .flatMap(dir -> dir.walkFile("META-INF/MANIFEST.MF"))
                .first()
                .to()
                .or(project().getSources().file("resources/META-INF/MANIFEST.MF"));

        try {
            Manifest manifest = new Manifest(file.newInputStream());
            try (OutputStream out = file.newOutputStream()) {
                manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
                manifest.getMainAttributes().putValue(Name.MAIN_CLASS.toString(), main.v);
                manifest.write(out);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }

        require(Jar::source);

        Config conf = config();

        // pack with dependency libraries
        Folder folder = Locator.folder();

        // download and unzip exewrap
        ui().info("Download and extract exewrap binary.");

        Directory dir = Loader.download("https://dforest.watch.impress.co.jp/library/e/exewrap/11824/exewrap1.6.5.zip").unpackToTemporary();
        Directory temporary = Locator.temporaryDirectory();

        File exewrap = dir.file("exewrap1.6.5/x64/exewrap.exe");
        File exe = temporary.file(project().getProduct() + ".exe");

        // build command line
        List<String> command = new ArrayList();
        command.add(exewrap.toString());
        command.add("-g");
        command.add("-A");
        command.add("x64");
        command.add("-t");
        command.add(Inputs.normalize(project().getJavaRequiredVersion()));
        command.add("-j");
        command.add(project().locateJar().toString());
        command.add("-e");
        command.add("IGNORE_UNCAUGHT_EXCEPTION");
        command.add("-o");
        command.add(exe.absolutize().toString());
        if (conf.icon != null && Files.isRegularFile(conf.icon) && conf.icon.toString().endsWith(".ico")) {
            command.add("-i");
            command.add(conf.icon.toString());
        }

        // execute exewrap
        Process.with().workingDirectory(exewrap.parent()).ignoreOutput().run(command);
        ui().info("Write " + exe.name() + ".");

        // pack
        folder.add(exe);

        folder.add(project().locateJar(), o -> o.allocateIn("lib"));
        for (Library library : project().getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar(), o -> o.allocateIn("lib"));
        }

        // Build custom JRE
        if (conf.customJRE) {
            Directory jre = Locator.temporaryDirectory("jre");

            List<String> commandForJRE = new ArrayList();
            commandForJRE.add("jlink");
            commandForJRE.add("--add-modules");
            commandForJRE.add(String.join(",", require(Dependency::module)));
            commandForJRE.add("--output");
            commandForJRE.add(jre.toString());
            commandForJRE.add("--strip-native-commands");
            Process.with().run(commandForJRE);

            // copy java.exe
            Platform.JavaHome.file("bin/java.exe").copyTo(jre.directory("bin"));

            // sync last-modified datetime
            jre.walkFileWithBase().to(copy -> {
                File relativize = copy.ⅰ.relativize(copy.ⅱ);
                File original = Platform.JavaHome.file(relativize);
                if (original.isPresent()) {
                    copy.ⅱ.lastModifiedTime(original.lastModifiedMilli());
                }
            });

            folder.add(jre);
        }

        // addtional data
        for (Location location : conf.resources) {
            folder.add(location);
        }

        ui().info("Packing application and libraries.");

        return folder.packTo(project().getOutput().file(project().getProduct() + "-" + project().getVersion() + ".zip"));
    }
}