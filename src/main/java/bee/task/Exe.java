/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import javax.lang.model.SourceVersion;

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

    @Command("Generate windows exe file which executes the main class.")
    public File build() {
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
            Object userDefinedMainClass = manifest.getMainAttributes().get(Name.MAIN_CLASS.toString());
            if (userDefinedMainClass == null) {
                try (OutputStream out = file.newOutputStream()) {
                    manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "1.0");
                    manifest.getMainAttributes().putValue(Name.MAIN_CLASS.toString(), main);
                    manifest.write(out);
                }
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }

        require(Jar::source);

        try {
            // pack with dependency libraries
            Folder folder = Locator.folder();

            build(folder, "");
            build(folder, "64");

            folder.add(project.locateJar(), o -> o.allocateIn("lib"));
            for (Library library : project.getDependency(Scope.Runtime)) {
                folder.add(library.getLocalJar(), o -> o.allocateIn("lib"));
            }

            ui.talk("Packing application and libraries.");

            return folder.packTo(project.getOutput().file(project.getProduct() + "-" + project.getVersion() + ".zip"));
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Helper method to build windows native application launcher.
     * </p>
     * 
     * @param folder
     * @param suffix
     */
    private void build(Folder folder, String suffix) {
        Directory temporary = Locator.temporaryDirectory();
        File builder = temporary.file("exewrap" + suffix + ".exe");
        File exe = temporary.file(project.getProduct() + suffix + ".exe");

        try {

            // unzip exe builder
            I.copy(Exe.class.getResourceAsStream("exewrap" + suffix + ".exe"), builder.newOutputStream(), true);

            // build command line
            List<String> command = new ArrayList();
            command.add(builder.toString());
            command.add("-g");
            command.add("-t");
            command.add(Inputs.normalize(SourceVersion.latest()));
            command.add("-j");
            command.add(project.locateJar().toString());
            command.add("-o");
            command.add(exe.absolutize().toString());
            if (icon != null && Files.isRegularFile(icon) && icon.toString().endsWith(".ico")) {
                command.add("-i");
                command.add(icon.toString());
            }

            // execute exe builder
            Process.with().workingDirectory(builder.parent()).ignoreOutput().run(command);
            ui.talk("Write " + exe.name() + ".");
        } catch (Exception e) {
            throw I.quiet(e);
        }

        // pack
        folder.add(exe);
    }
}
