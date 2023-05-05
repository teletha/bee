/*
 * Copyright (C) 2023 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.util.Inputs;
import bee.util.Process;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;

public class Exe extends Task {

    /** The location for icon of exe file. */
    protected Path icon;

    /** The usage of custom JRE. */
    protected boolean useCustomJRE = true;

    @Command("Generate windows exe file which executes the main class.")
    public File build() {
        if (!Platform.isWindows()) {
            ui.warn("Skip the task [exe] because it is available in windows platform only.");
            return null;
        }

        require(Test::test);

        // search main classes
        String main = require(FindMain::main);

        // search main class in MANIFEST.MF
        File file = project.getSourceSet()
                .flatMap(dir -> dir.walkFile("META-INF/MANIFEST.MF"))
                .first()
                .to()
                .or(project.getSources().file("resources/META-INF/MANIFEST.MF"));

        try {
            Manifest manifest = new Manifest(file.newInputStream());
            try (OutputStream out = file.newOutputStream()) {
                manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
                manifest.getMainAttributes().putValue(Name.MAIN_CLASS.toString(), main);
                manifest.write(out);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }

        require(Jar::source);

        // pack with dependency libraries
        Folder folder = Locator.folder();

        // download and unzip exewrap
        ui.info("Download and extract exewrap binary.");

        I.http("http://osdn.net/frs/redir.php?m=nchc&f=exewrap%2F73589%2Fexewrap1.6.4.zip", InputStream.class)
                .map(Locator.temporaryFile("exewrap.zip")::writeFrom)
                .map(File::unpackToTemporary)
                .waitForTerminate()
                .to(dir -> {
                    Directory temporary = Locator.temporaryDirectory();

                    File exewrap = dir.file("exewrap1.6.4/x64/exewrap.exe");
                    File exe = temporary.file(project.getProduct() + ".exe");

                    // build command line
                    List<String> command = new ArrayList();
                    command.add(exewrap.toString());
                    command.add("-g");
                    command.add("-A");
                    command.add("x64");
                    command.add("-t");
                    command.add(Inputs.normalize(project.getJavaClassVersion()));
                    command.add("-j");
                    command.add(project.locateJar().toString());
                    command.add("-e");
                    command.add("IGNORE_UNCAUGHT_EXCEPTION");
                    command.add("-o");
                    command.add(exe.absolutize().toString());
                    if (icon != null && Files.isRegularFile(icon) && icon.toString().endsWith(".ico")) {
                        command.add("-i");
                        command.add(icon.toString());
                    }

                    // execute exewrap
                    Process.with().workingDirectory(exewrap.parent()).ignoreOutput().run(command);
                    ui.info("Write " + exe.name() + ".");

                    // pack
                    folder.add(exe);
                });

        folder.add(project.locateJar(), o -> o.allocateIn("lib"));
        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar(), o -> o.allocateIn("lib"));
        }

        // Build custom JRE
        if (useCustomJRE) {
            Directory jre = Locator.temporaryDirectory("jre");
            List<String> command = new ArrayList();
            command.add("jlink");
            command.add("--add-modules");
            command.add(String.join(",", modules(project.getDependency(Scope.Runtime))));
            command.add("--output");
            command.add(jre.toString());
            command.add("--compress");
            command.add("2");
            command.add("--strip-native-commands");
            Process.with().run(command);
            folder.add(jre);
        }

        ui.info("Packing application and libraries.");

        return folder.packTo(project.getOutput().file(project.getProduct() + "-" + project.getVersion() + ".zip"));
    }

    /**
     * Collect all JDK modules.
     * 
     * @param libraries
     * @return
     */
    private Set<String> modules(Set<Library> libraries) {
        Set<String> names = new HashSet();
        names.add("jdk.localedata");
        names.add("jdk.crypto.ec");
        names.add("jdk.crypto.cryptoki");
        names.add("jdk.crypto.mscapi");
        names.add("jdk.net");
        names.add("jdk.zipfs");
        names.add("java.naming");
        names.add("jdk.naming.dns");

        for (Library library : libraries) {
            try {
                ModuleFinder finder = ModuleFinder.of(library.getLocalJar().asJavaPath());
                for (ModuleReference ref : finder.findAll()) {
                    for (Requires requires : ref.descriptor().requires()) {
                        String name = requires.name();
                        if (name.startsWith("java.") || name.startsWith("jdk.")) {
                            if (ModuleLayer.boot().findModule(name).isPresent()) {
                                names.add(name);
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        ui.info("Modules: " + names.stream().sorted().toList());
        return names;
    }
}