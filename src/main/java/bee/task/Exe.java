/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.util.Process;
import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Folder;
import psychopath.Locator;

public class Exe extends Task {

    /** The temporary directory. */
    private final Directory temporary = Locator.temporaryDirectory();

    /** The output for the generated zip file. */
    private final File zipOutput = project.getOutput().file(project.getProduct() + "-" + project.getVersion() + ".zip");

    /** The location for icon of exe file. */
    protected Path icon;

    @Command("Generate windows exe file which executes the main class.")
    public File build() {
        require(Jar.class).source();

        try {
            // pack with dependency libraries
            Folder folder = Locator.folder();

            build(folder, "");
            build(folder, "64");

            folder.add(project.locateJar());
            zip.add("lib", project.locateJar().asJavaPath());
            for (Library library : project.getDependency(Scope.Runtime)) {
                zip.add("lib", library.getLocalJar().asJavaPath());
            }

            ui.talk("Packing application and libraries.");
            zip.pack(zipOutput.asJavaPath());

            return zipOutput;
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
        File builder = temporary.file("exewrap" + suffix + ".exe");
        File exe = temporary.file(project.getProduct() + suffix + ".exe");

        try {
            // search main classes
            String main = require(FindMain.class).main();

            // unzip exe builder
            I.copy(Exe.class.getResourceAsStream("exewrap" + suffix + ".exe"), builder.newOutputStream(), true);

            // build command line
            List<String> command = new ArrayList();
            command.add(builder.toString());
            command.add("-e");
            command.add("SINGLE");
            command.add("-M");
            command.add(main);
            command.add("-g");
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
