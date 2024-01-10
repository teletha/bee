/*
 * Copyright (C) 2024 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.bun;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.OptionalLong;

import bee.Platform;
import bee.api.Project;
import bee.api.Transfers;
import kiss.I;
import kiss.WiseConsumer;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

public class Bun {

    /** The current project. */
    private final Project project;

    /** The bun version. */
    private final String version;

    /** The bun home. */
    private final Directory home;

    /** The bun repository. */
    private final Directory repository;

    /**
     * Create Bun runtime.
     * 
     * @param version A target version.
     */
    public Bun(String version) {
        this(version, null, null);
    }

    /**
     * Create Bun runtime.
     * 
     * @param version A target version.
     * @param root A home directory.
     * @param repository A repsitory directory.
     */
    public Bun(String version, Directory root, Directory repository) {
        this.project = I.make(Project.class);
        this.version = Objects.requireNonNullElse(version, "1.0.21");
        this.home = Objects.requireNonNullElse(root, project.getRoot().directory(".bun/" + this.version));
        this.repository = Objects.requireNonNullElse(repository, project.getRoot().directory(".bun/repository"));
    }

    /**
     * Install Bun runtime.
     */
    public void install() {
        File command = home.file(Platform.isWindows() ? "bun.exe" : "bun");

        if (command.isAbsent()) {
            String uri = Platform.isWindows() ? "https://github.com/oven-sh/bun/releases/download/canary/bun-windows-x64.zip"
                    : Platform.isLinux() ? "https://github.com/oven-sh/bun/releases/download/bun-v" + version + "/bun-linux-x64.zip"
                            : "https://github.com/oven-sh/bun/releases/download/bun-v" + version + "/bun-darwin-x64.zip";

            File zip = Locator.temporaryFile();

            I.http(uri, HttpResponse.class).waitForTerminate().to((WiseConsumer<HttpResponse>) res -> {
                // analyze header
                HttpHeaders headers = res.headers();
                OptionalLong length = headers.firstValueAsLong("Content-Length");

                // transfer data
                try (InputStream in = (InputStream) res.body(); OutputStream out = new FileOutputStream(zip.asJavaFile())) {
                    Transfers transfers = I.make(Transfers.class);
                    transfers.transferProgressed(0, length.orElse(0));

                    int read = -1;
                    byte[] buffer = new byte[1024 * 32];
                    while (0 < (read = in.read(buffer))) {
                        out.write(buffer, 0, read);
                        transfers.transferProgressed(read, length.orElse(0));
                    }
                }
            });
        }
    }

    /**
     * Check the runtime.
     * 
     * @return
     */
    private boolean isInstalled() {
        return home.file("bun.exe").isPresent();
    }
}
