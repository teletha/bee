/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

import kiss.I;

/**
 * <p>
 * Jar file is unwraped for fat style jar.
 * </p>
 * 
 * @version 2012/01/26 16:26:58
 */
public class JarArchiver extends ZipArchiver {

    /**
     * 
     */
    public JarArchiver() {
        encoding = StandardCharsets.UTF_8;
        manifest = new Manifest();
    }

    /**
     * <p>
     * Set manifest attribute.
     * </p>
     * 
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        manifest.getMainAttributes().putValue(key, value.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Path base, String... patterns) {
        if (Files.isRegularFile(base) && base.getFileName().toString().endsWith(".jar")) {
            try {
                // use ZipFS
                base = FileSystems.newFileSystem(base, ClassLoader.getSystemClassLoader()).getPath("/");
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
        super.add(base, patterns);
    }
}
