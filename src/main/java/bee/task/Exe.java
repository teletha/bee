/*
 * Copyright (C) 2015 Nameless Production Committee
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

import kiss.I;
import bee.Platform;
import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;
import bee.util.ProcessMaker;
import bee.util.ZipArchiver;

/**
 * @version 2012/04/01 9:34:01
 */
public class Exe extends Task {

    /** The icon exchanger file name. */
    private static final String iconChangerName = "ResHacker.exe";

    /** The temporary directory. */
    private final Path temporary;

    /** The location of icon changer. */
    private final Path iconChanger;

    /** The output for the generated zip file. */
    private final Path zipOutput;

    /** The location for icon of exe file. */
    protected Path icon;

    /**
     * 
     */
    public Exe() {
        try {
            temporary = I.locateTemporary();
            iconChanger = temporary.resolve(iconChangerName);
            zipOutput = project.getOutput().resolve(project.getProduct() + "-" + project.getVersion() + ".zip");

            Files.createDirectories(temporary);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    @Command
    public Path build() {
        require(Jar.class).source();

        try {
            // pack with dependency libraries
            ZipArchiver zip = new ZipArchiver();

            build(zip, "");
            build(zip, "64");

            zip.add("lib", project.locateJar());
            for (Library library : project.getDependency(Scope.Runtime)) {
                zip.add("lib", library.getJar());
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
        Path settingFile = temporary.resolve(project.getProduct() + suffix + ".lap");
        Path exeFile = temporary.resolve(project.getProduct() + suffix + ".exe");

        try {
            // search main classes
            String main = require(FindMain.class).main();

            // write setting file
            List<String> setting = new ArrayList();
            setting.add("janel.main.class=" + main);
            setting.add("janel.classpath.jars.dir=lib");
            Files.write(settingFile, setting, Platform.Encoding);
            ui.talk("Write " + settingFile.getFileName() + ".");

            // copy exe launcher
            I.copy(Exe.class.getResourceAsStream("JanelWindows" + suffix + ".exe"), Files.newOutputStream(exeFile), true);
            ui.talk("Write " + exeFile.getFileName() + ".");

            if (icon != null && Files.isRegularFile(icon) && icon.toString().endsWith(".ico")) {
                // unzip icon changer
                I.copy(Exe.class.getResourceAsStream(iconChangerName), Files.newOutputStream(iconChanger), true);

                // build command line
                List<String> command = new ArrayList();
                command.add(iconChanger.toString());
                command.add("-addoverwrite");
                command.add(exeFile.toString() + ",");
                command.add(exeFile.toString() + ",");
                command.add(icon.toAbsolutePath() + ",");
                command.add("ICONGROUP,");
                command.add("101,");

                // execute icon changer
                ProcessMaker maker = new ProcessMaker();
                maker.setWorkingDirectory(iconChanger.getParent());
                maker.run(command);
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }

        // pack
        archiver.add(settingFile);
        archiver.add(exeFile);
    }
}
