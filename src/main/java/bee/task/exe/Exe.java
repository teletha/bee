/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task.exe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.model.ClassUtil;
import bee.task.Command;
import bee.task.Task;
import bee.util.JarArchiver;

/**
 * @version 2012/04/01 9:34:01
 */
public class Exe extends Task {

    /** The builder file name. */
    private static final String exeBuilderName = "exewrap.exe";

    /** The temporary directory. */
    private final Path temporary;

    /** The location of exewrap.exe. */
    private final Path exeBuilder;

    /** The output for the generated exe file. */
    private final Path exeOutput;

    /** The location for icon of exe file. */
    private Path icon;

    /**
     * 
     */
    public Exe() {
        try {
            temporary = I.locateTemporary();
            exeBuilder = temporary.resolve(exeBuilderName);
            exeOutput = project.getOutput().resolve(project.getProduct() + ".exe");

            Files.createDirectories(temporary);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    @Command(defaults = true)
    public void build() {
        try {
            // create temporary executable jar
            Path jar = temporary.resolve("starter.jar");

            JarArchiver archiver = new JarArchiver();
            archiver.set("Manifest-Version", "1.0");
            archiver.set("Main-Class", Activator.class.getName());
            archiver.add(ClassUtil.getArchive(Activator.class), "**/" + Activator.class.getSimpleName() + ".class");
            archiver.pack(jar);

            // unzip exewrap.exe
            I.copy(Exe.class.getResourceAsStream(exeBuilderName), Files.newOutputStream(exeBuilder), true);

            // build command line
            List<String> command = new ArrayList();
            command.add(exeBuilder.toString());
            command.add("-g");
            command.add("-o");
            command.add(exeOutput.toString());
            command.add("-j");
            command.add(jar.toString());
            command.add("-v");
            command.add(project.getVersion().replace('-', '.'));
            command.add("-t");
            command.add(project.getJavaVersion());

            String description = project.getDescription();

            if (description.length() != 0) {
                command.add("-d");
                command.add(description);
            }

            if (icon != null && Files.isRegularFile(icon) && icon.toString().endsWith(".ico")) {
                command.add("-i");
                command.add(icon.toAbsolutePath().toString());
            }

            // execute exewrap.exe
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(exeBuilder.getParent().toFile());
            builder.command(command);
            builder.start().waitFor();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}
