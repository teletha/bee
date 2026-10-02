/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.io.InputStream;

import bee.Fail;
import bee.Task;
import bee.api.Command;
import bee.api.VCS;
import kiss.I;
import kiss.JSON;

public interface Deploy extends Task {

    /** The interval (ms) between the JitPack API polls. */
    long POLL_INTERVAL = 3000;

    /** The maximum time (ms) to wait for the JitPack build. */
    long TIMEOUT = 30 * 60 * 1000;

    @Command(defaults = true, value = "Check whether the current version is deployed to JitPack successfully.")
    default void check() {
        // JitPack serves a GitHub repository under the com.github.<owner> coordinates, which may
        // differ from the project's own Maven coordinates.
        String group = project().getGroup();
        String product = project().getProduct();

        VCS vcs = project().getVersionControlSystem();
        if (vcs != null && vcs.name().equals("github")) {
            group = "com.github." + vcs.owner;
            product = vcs.repo;
        }

        check(group, product, project().getVersion());
    }

    /**
     * Wait until the specified artifact is built on JitPack successfully. The build status is polled
     * every few seconds and a spinner is shown while waiting.
     *
     * @param group A JitPack group.
     * @param product A JitPack artifact.
     * @param version A target version.
     */
    private void check(String group, String product, String version) {
        String endpoint = "https://jitpack.io/api/builds/" + group + "/" + product;
        String coordinates = group + ":" + product + ":" + version;

        // Requesting the artifact starts the on-demand build on JitPack.
        trigger(group, product, version);

        // The spinner stops automatically when the next message (info or error) is written.
        ui().spinner("Waiting for the JitPack build of " + coordinates + "...");

        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < TIMEOUT) {
            // Use the build list endpoint because the per-version endpoint can take tens of seconds
            // on a cold cache. The build list is fast and returns every version with its outcome.
            String status;
            try {
                JSON builds = I.json(endpoint);
                JSON versions = builds.get(group);
                status = versions == null ? null : versions.get(product).text(version);
            } catch (Throwable e) {
                throw new Fail("Failed to access the JitPack API : " + endpoint).reason(Fail.strip(e));
            }

            if (status != null) {
                switch (status.toLowerCase()) {
                case "ok":
                    ui().info("Deployed to JitPack : ", coordinates);
                    return;

                case "building":
                    break;

                default:
                    throw new Fail("The JitPack build has failed : " + coordinates)
                            .solve("Build log : " + log(group, product, version));
                }
            }

            sleep(POLL_INTERVAL);
        }

        throw new Fail("Timed out while waiting for the JitPack build : " + coordinates)
                .solve("Build log : " + log(group, product, version));
    }

    /**
     * Request the artifact to trigger the on-demand build on JitPack. The response is discarded
     * because only the build status is needed.
     *
     * @param group A JitPack group.
     * @param product A JitPack artifact.
     * @param version A target version.
     */
    private void trigger(String group, String product, String version) {
        I.http(artifact(group, product, version), InputStream.class).to(in -> I.quiet(in), error -> {
        });
    }

    /**
     * Sleep the current thread for the specified milliseconds.
     *
     * @param millis A sleep time.
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Fail("Interrupted while waiting for the JitPack build.").reason(e);
        }
    }

    /**
     * Build the URL to the artifact on JitPack.
     *
     * @param group A JitPack group.
     * @param product A JitPack artifact.
     * @param version A target version.
     * @return An artifact URL.
     */
    private String artifact(String group, String product, String version) {
        return "https://jitpack.io/" + group.replace('.', '/') + "/" + product + "/" + version + "/" + product + "-" + version + ".jar";
    }

    /**
     * Build the URL to the build log on JitPack.
     *
     * @param group A JitPack group.
     * @param product A JitPack artifact.
     * @param version A target version.
     * @return A build log URL.
     */
    private String log(String group, String product, String version) {
        return "https://jitpack.io/" + group.replace('.', '/') + "/" + product + "/" + version + "/build.log";
    }
}
