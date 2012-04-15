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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.model.ClassUtil;

/**
 * @version 2011/03/23 18:55:51
 */
class BeeInstaller {

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void main(String... args) {
        try {
            Path jar = JavaHome.resolve("lib/bee.jar");
            Path current = ClassUtil.getArchive(BeeInstaller.class);

            if (Files.isDirectory(current)) {
                // The current directory is class files store.
                // We should pack them as jar file.
                // This process is mainly used by Bee developers.
                Bee bee = new Bee();
                bee.execute("bee:install");
            } else if (Files.exists(jar) && Files.getLastModifiedTime(jar).toMillis() != Files.getLastModifiedTime(current)
                    .toMillis()) {
                // The current bee.jar is newer.
                // We should copy it to JDK directory.
                // This process is mainly used by Bee users while install phase.
                I.copy(current, jar);
            }

            // create bat file
            List<String> bat = new ArrayList();

            if (Bee.getFileName().toString().endsWith(".bat")) {
                // windows
                bat.add("@echo off");
                bat.add("java -cp \"" + jar.toString() + "\" " + Bee.class.getName() + " %*");
            } else {
                // linux
                // TODO
            }
            Files.write(Bee, bat, I.$encoding);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}
