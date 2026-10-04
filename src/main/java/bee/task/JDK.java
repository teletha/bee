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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.JavaVersion;
import bee.api.Loader;
import kiss.I;
import kiss.JSON;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import psychopath.Option;

/**
 * Manages the JDK used by Bee. The JDKs are downloaded from Adoptium (Temurin) and installed under
 * the Bee home directory, so the JDK can be switched without touching the environment path. Early
 * Access builds are supported for versions which have not been released yet.
 */
public interface JDK extends Task {

    /** The endpoint which lists the available releases. */
    String AVAILABLE = "https://api.adoptium.net/v3/info/available_releases";

    /**
     * Select the JDK used by Bee. All the versions available on Adoptium are listed, and the selected
     * one is downloaded, installed and configured as the JDK used by Bee.
     */
    @Command(defaults = true, value = "Select, install and use a JDK.")
    default void select() {
        JSON info;
        try {
            info = I.json(AVAILABLE);
        } catch (Throwable e) {
            throw new Fail("Failed to access the Adoptium API.").reason(Fail.strip(e));
        }

        List<Integer> released = info.find(int.class, "available_releases", "*");
        List<Integer> lts = info.find(int.class, "available_lts_releases", "*");
        int tip = info.get(int.class, "tip_version");

        // List the released versions and the unreleased tip version as Early Access.
        List<Integer> releases = new ArrayList(released);
        if (!released.contains(tip)) {
            releases.add(tip);
        }
        releases.sort(Comparator.reverseOrder());

        String selected = Platform.config("java");
        int width = String.valueOf(releases.get(0)).length();

        int version = ui().ask("Select the JDK version to use.", releases, v -> {
            boolean earlyAccess = !released.contains(v);
            JavaVersion java = JavaVersion.of(v);
            Directory jdk = locate(v, earlyAccess);
            String label = "Java " + padRight(String.valueOf(v), width) + " - " + padRight(java != null ? java
                    .getReleaseDate() : date(v, earlyAccess), 10) + " " + padRight(java != null && java.lts ? "LTS" : "", 3) + padRight(earlyAccess
                            ? "EA"
                            : "", 2);

            if (jdk.absolutize().path().equalsIgnoreCase(selected)) {
                label += " [selected]";
            } else if (jdk.isPresent()) {
                label += " [installed]";
            }
            return label.stripTrailing();
        });

        boolean earlyAccess = !released.contains(version);
        Directory dest = install(version, earlyAccess);

        if (!Platform.canRun(dest)) {
            ui().warn("The JDK [", dest, "] may be too old to run Bee.");
        }

        Platform.config("java", dest.absolutize().path());
        ui().info("Bee will use the JDK [", version, "] from the next invocation.");
    }

    /**
     * Ensure the specified JDK version is installed under the Bee home. The JDK is downloaded from
     * Adoptium (Temurin) when it is missing.
     * 
     * @param version A JDK version.
     * @return The installation directory.
     */
    static Directory install(int version) {
        JavaVersion java = JavaVersion.of(version);
        return install(version, java != null && java.earlyAccess);
    }

    /**
     * Ensure the specified JDK version is installed under the Bee home. The JDK is downloaded from
     * Adoptium (Temurin) when it is missing.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether to use an Early Access build or not.
     * @return The installation directory.
     */
    static Directory install(int version, boolean earlyAccess) {
        Directory dest = locate(version, earlyAccess);

        if (dest.isAbsent()) {
            String type = earlyAccess ? "ea" : "ga";
            String url = "https://api.adoptium.net/v3/binary/latest/" + version + "/" + type + "/" + os() + "/" + arch()
                    + "/jdk/hotspot/normal/eclipse";
            File archive = Locator.temporaryFile(url.substring(url.lastIndexOf('/') + 1));

            ui().info("Downloading the ", earlyAccess ? "Early Access " : "", "JDK [", version, "] from Adoptium.");
            Loader.download(url, archive);
            unpack(archive, dest, Option::strip);
            ui().info("Installed the JDK [", version, "] at ", dest);
        }
        return dest;
    }

    /**
     * Pad the specified text with spaces on the right.
     * 
     * @param text A text.
     * @param width A minimum width.
     * @return A padded text.
     */
    private String padRight(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    /**
     * Resolve the release date of the specified version from the Adoptium API. This is used only when
     * the version is not declared by {@link JavaVersion}.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether to use an Early Access build or not.
     * @return A release date, or an empty text.
     */
    private String date(int version, boolean earlyAccess) {
        try {
            String timestamp = I.json(releaseUrl(version, earlyAccess)).get("0").text("timestamp");
            return timestamp != null && 10 <= timestamp.length() ? timestamp.substring(0, 10).replace('-', '/') : "";
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * Build the URL of the Adoptium release list of the specified version.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether to use an Early Access build or not.
     * @return A release API URL.
     */
    private String releaseUrl(int version, boolean earlyAccess) {
        return "https://api.adoptium.net/v3/assets/feature_releases/" + version + "/" + (earlyAccess ? "ea" : "ga")
                + "?architecture=" + arch() + "&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=" + os()
                + "&page=0&page_size=1&project=jdk&vendor=eclipse&sort_order=ASC";
    }

    /**
     * Locate the installation directory of the specified version.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether to use an Early Access build or not.
     * @return An installation directory.
     */
    private static Directory locate(int version, boolean earlyAccess) {
        return Platform.BeeHome.directory("jdk").directory("temurin-" + version + (earlyAccess ? "-ea" : ""));
    }

    /**
     * Resolve the Adoptium OS name.
     * 
     * @return An OS name.
     */
    private static String os() {
        return Platform.isWindows() ? "windows" : Platform.isMac() ? "mac" : "linux";
    }

    /**
     * Resolve the Adoptium architecture name.
     * 
     * @return An architecture name.
     */
    private static String arch() {
        String arch = Platform.OSArch;
        return arch.contains("aarch64") || arch.contains("arm") ? "aarch64" : "x64";
    }
}
