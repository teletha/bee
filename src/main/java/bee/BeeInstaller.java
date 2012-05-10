/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import static bee.Platform.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.model.ClassUtil;
import bee.tool.Java;
import bee.tool.Java.JVM;
import bee.util.Paths;

/**
 * @version 2011/03/23 18:55:51
 */
public class BeeInstaller {

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void main(String... args) {
        install(ClassUtil.getArchive(BeeInstaller.class));
    }

    /**
     * <p>
     * Install Bee into your system.
     * </p>
     * 
     * @param source
     */
    public static final void install(Path source) {
        Path dest = JavaHome.resolve("lib/bee.jar");

        try {
            if (Files.exists(dest)) {

                Path renamed = JavaHome.resolve("lib/bee2.jar");
                Files.move(dest, renamed, StandardCopyOption.REPLACE_EXISTING);
            }

            if (Paths.getLastModified(source) != Paths.getLastModified(dest)) {
                // The current bee.jar is newer.
                // We should copy it to JDK directory.
                // This process is mainly used by Bee users while install phase.
                I.copy(source, dest);
            }

            // create bat file
            List<String> bat = new ArrayList();

            if (Bee.getFileName().toString().endsWith(".bat")) {
                // windows
                // use JDK full path to avoid using JRE
                bat.add("@echo off");
                bat.add(JavaHome.resolve("bin/java") + " -cp \"" + dest.toString() + "\" " + Bee.class.getName() + " %*");
            } else {
                // linux
                // TODO
            }
            Files.write(Bee, bat, I.$encoding);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    private static void install() {
        try {
            Path temp = I.locateTemporary();
            Files.createDirectories(temp);
            Path bee = temp.resolve("bee.jar");

            I.copy(ClassUtil.getArchive(BeeInstaller.class), bee);

            Java sub = new Java();
            sub.addClassPath(bee);
            sub.run(Sub.class);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2012/04/27 16:31:34
     */
    private static class Sub extends JVM {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean process() {
            ui.error(bee.Bee.AbortedByUser);
            I.copy(ClassUtil.getArchive(BeeInstaller.class), Platform.Bee);
            return false;
        }
    }
}
