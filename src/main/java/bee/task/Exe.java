/*
 * Copyright (C) 2022 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    @Command("Generate windows exe file which executes the main class.")
    public File build() {
        if (!Platform.isWindows()) {
            ui.warn("Skip the task [exe] because it is available in windows platform only.");
            return null;
        }

        // search main classes
        String main = require(FindMain::main);

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

                    for (String suffix : List.of("86", "64")) {
                        File exewrap = dir.file("exewrap1.6.4/x" + suffix + "/exewrap.exe");
                        File exe = temporary.file(project.getProduct() + suffix + ".exe");

                        // build command line
                        List<String> command = new ArrayList();
                        command.add(exewrap.toString());
                        command.add("-g");
                        command.add("-A");
                        command.add("x" + suffix);
                        command.add("-t");
                        command.add(Inputs.normalize(project.getJavaClassVersion()));
                        command.add("-j");
                        command.add(project.locateJar().toString());
                        command.add("-M");
                        command.add(main);
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
                    }
                });

        folder.add(project.locateJar(), o -> o.allocateIn("lib"));
        for (Library library : project.getDependency(Scope.Runtime)) {
            folder.add(library.getLocalJar(), o -> o.allocateIn("lib"));
        }

        ui.info("Packing application and libraries.");

        return folder.packTo(project.getOutput().file(project.getProduct() + "-" + project.getVersion() + ".zip"));
    }
}