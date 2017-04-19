/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.util.Process;
import bee.util.ZipArchiver;
import filer.Filer;
import kiss.I;

/**
 * @version 2016/07/01 15:10:46
 */
public class Exe extends Task {

    /** The temporary directory. */
    private final Path temporary;

    /** The output for the generated zip file. */
    private final Path zipOutput;

    /** The location for icon of exe file. */
    protected Path icon;

    /**
     * 
     */
    public Exe() {
        try {
            temporary = Filer.locateTemporary();
            zipOutput = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + ".zip");

            Files.createDirectories(temporary);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    @Command("Generate windows exe file which executes the main class.")
    public Path build() {
        require(Jar.class).source();

        try {
            // pack with dependency libraries
            ZipArchiver zip = new ZipArchiver();

            build(zip, "");
            build(zip, "64");

            zip.add("lib", project.locateJar());
            for (Library library : project.getDependency(Scope.Runtime)) {
                zip.add("lib", library.getLocalJar());
            }

            ui.talk("Packing application and libraries.");
            zip.pack(zipOutput);

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
     * @param archiver
     * @param suffix
     */
    private void build(ZipArchiver archiver, String suffix) {
        Path builder = temporary.resolve("exewrap" + suffix + ".exe");
        Path exe = temporary.resolve(project.getProduct() + suffix + ".exe");

        try {
            // search main classes
            String main = require(FindMain.class).main();

            // unzip exe builder
            I.copy(Exe.class.getResourceAsStream("exewrap" + suffix + ".exe"), Files.newOutputStream(builder), true);

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
            command.add(exe.toAbsolutePath().toString());
            if (icon != null && Files.isRegularFile(icon) && icon.toString().endsWith(".ico")) {
                command.add("-i");
                command.add(icon.toString());
            }

            // execute exe builder
            Process.with().workingDirectory(builder.getParent()).ignoreOutput().run(command);
            ui.talk("Write " + exe.getFileName() + ".");
        } catch (Exception e) {
            throw I.quiet(e);
        }

        // pack
        archiver.add(exe);
    }
}
