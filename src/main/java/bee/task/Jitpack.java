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
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Comment;
import bee.api.VCS;
import kiss.I;
import kiss.JSON;

/**
 * Manages the JitPack builds of the project. The latest version is checked and, when its build
 * failed, the failed build state is deleted and a new build is triggered.
 * <p>
 * The deletion requires the JitPack API token, which is read from the {@code JITPACK_TOKEN}
 * environment variable or the {@code jitpack.token} entry of the Bee user configuration. Without the
 * token the build is only retriggered.
 */
public interface Jitpack extends Task<Jitpack.Config> {

    /** The interval (ms) between the build status polls. */
    long POLL_INTERVAL = 3000;

    /** The maximum time (ms) to wait for the build. */
    long TIMEOUT = 30 * 60 * 1000;

    /**
     * The configuration of the jitpack task.
     */
    class Config {

        /** The version to rebuild. The current project version is used when it is empty. */
        @Comment("The version to rebuild. The current project version is used when it is empty.")
        public String version;

        /** Whether the failed build state is kept instead of being deleted before the rebuild. */
        @Comment("Keep the failed build state instead of deleting it before the rebuild.")
        public boolean keep;
    }

    /**
     * Check the latest JitPack build and rebuild it when it failed.
     */
    @Command(defaults = true, value = "Check the latest JitPack build and rebuild it when it failed.")
    default void build() {
        VCS vcs = project().getVersionControlSystem();

        if (vcs == null || !vcs.name().equals("github")) {
            throw new Fail("The JitPack build requires a GitHub repository.");
        }

        // JitPack mirrors a project under its own Maven group (for example io.github.teletha) in
        // addition to the Git host group (com.github.teletha), so the project group is preferred
        // when it is a Git based group. Otherwise the Git host coordinate is used.
        String group = project().getGroup();
        String artifact = project().getProduct();

        if (group == null || !(group.startsWith("com.github.") || group.startsWith("io.github."))) {
            group = "com.github." + vcs.owner;
            artifact = vcs.repo;
        }

        String version = config().version == null || config().version.isBlank() ? project().getVersion() : config().version;

        ui().info("JitPack build : ", group, ":", artifact, ":", version);

        String status = status(group, artifact, version);
        if ("ok".equalsIgnoreCase(status)) {
            ui().info("The build of [", version, "] is already successful.");
            return;
        }

        if (status == null || "none".equalsIgnoreCase(status)) {
            ui().info("The build of [", version, "] does not exist. Triggering a new build.");
        } else {
            ui().warn("The build of [", version, "] is [", status, "]. Rebuilding.");
            if (!config().keep) {
                delete(group, artifact, version);

                // The deletion is asynchronous, so wait until the old state is cleared. Otherwise
                // the watch would read the stale error and report a failure immediately.
                awaitClear(group, artifact, version);
            }
        }

        trigger(group, artifact, version);
        watch(group, artifact, version);
    }

    /**
     * Wait until the asynchronous deletion clears the previous build state. The watch can only judge
     * the result of the new build after the old record is gone.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     */
    private void awaitClear(String group, String artifact, String version) {
        long start = System.currentTimeMillis();
        long timeout = 60 * 1000;

        while (System.currentTimeMillis() - start < timeout) {
            String status = status(group, artifact, version);
            if (status == null || "none".equalsIgnoreCase(status) || "building".equalsIgnoreCase(status)) {
                return;
            }
            sleep(POLL_INTERVAL);
        }
        ui().warn("The previous build state of [", version, "] is still present. Continuing anyway.");
    }

    /**
     * Resolve the build status of the specified version.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     * @return The status, or <code>null</code> when the build does not exist.
     */
    private String status(String group, String artifact, String version) {
        try {
            JSON build = I.http(endpoint(group, artifact, version), JSON.class).waitForTerminate().to().acquire();
            return build.text("status");
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Delete the build state of the specified version. The deletion is limited by JitPack to a
     * failed build, a snapshot or a tag which is newer than seven days.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     */
    private void delete(String group, String artifact, String version) {
        boolean prompted = false;
        String token = I.env("JITPACK_TOKEN");

        if (token == null || token.isBlank()) {
            token = Platform.config("jitpack.token");
        }
        if (token == null || token.isBlank()) {
            ui().info("Create a JitPack API token at ", "https://jitpack.io/w/user");
            token = ui().ask("JitPack API token (empty to skip the deletion)?");
            if (token == null || token.isBlank()) {
                ui().warn("The JitPack API token is not given, so the failed build is not deleted.");
                ui().warn("The build is retriggered without clearing the failed state.");
                return;
            }
            prompted = true;
        }
        token = token.trim();

        String authorization = "Basic " + Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint(group, artifact, version)))
                    .header("Authorization", authorization)
                    .DELETE();
            JSON response = I.http(request, JSON.class).waitForTerminate().to().acquire();
            ui().info("Deleted the previous build : ", response.text("message"));
        } catch (Throwable e) {
            // JitPack may answer with a transient error (for example "error code: 1101"), and the
            // build may already be deleted, so the failure does not stop the rebuild.
            ui().warn("Failed to delete the JitPack build of [", version, "]. Continuing with the rebuild.");
            ui().warn("  ", Fail.strip(e));
            return;
        }

        // The token is saved only after the deletion has been accepted, so a mistyped token is not
        // persisted.
        if (prompted) {
            Platform.config("jitpack.token", token);
            ui().info("Saved the JitPack token in the Bee user configuration [", Platform.Config, "].");
        }
    }

    /**
     * Trigger a build by requesting the artifact.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     */
    private void trigger(String group, String artifact, String version) {
        String jar = "https://jitpack.io/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";

        // Requesting the artifact starts the on-demand build, so only the transfer is started and
        // the result is discarded.
        I.http(jar, InputStream.class).to(in -> I.quiet(in), error -> {
        });
    }

    /**
     * Poll the build until it finishes.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     */
    private void watch(String group, String artifact, String version) {
        long start = System.currentTimeMillis();
        ui().spinner("Waiting for the JitPack build of " + version + "...");

        while (System.currentTimeMillis() - start < TIMEOUT) {
            String status = status(group, artifact, version);
            if ("ok".equalsIgnoreCase(status)) {
                ui().info("The build of [", version, "] succeeded.");
                return;
            }
            if (status == null || "none".equalsIgnoreCase(status) || "building".equalsIgnoreCase(status)) {
                // The build is not registered yet (none) or still running (building), so keep
                // waiting.
                sleep(POLL_INTERVAL);
                continue;
            }
            throw new Fail("The JitPack build of [" + version + "] failed.")
                    .solve("Build log : " + log(group, artifact, version));
        }

        throw new Fail("Timed out while waiting for the JitPack build of [" + version + "].")
                .solve("Build log : " + log(group, artifact, version));
    }

    /**
     * Build the status endpoint of the specified version.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     * @return An endpoint.
     */
    private String endpoint(String group, String artifact, String version) {
        return "https://jitpack.io/api/builds/" + group + "/" + artifact + "/" + version;
    }

    /**
     * Build the log URL of the specified version.
     * 
     * @param group A JitPack group.
     * @param artifact A JitPack artifact.
     * @param version A version.
     * @return A log URL.
     */
    private String log(String group, String artifact, String version) {
        return "https://jitpack.io/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/build.log";
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
            throw new Fail("Interrupted while waiting for the JitPack build.");
        }
    }
}
