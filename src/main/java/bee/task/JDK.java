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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Loader;
import kiss.I;
import kiss.JSON;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;
import psychopath.Option;

/**
 * Manages the JDK used by Bee. The JDKs are downloaded from Adoptium (Temurin) and installed under
 * the Bee home directory, so the JDK can be switched without touching the environment path.
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

        List<Integer> releases = info.find(int.class, "available_releases", "*");
        List<Integer> lts = info.find(int.class, "available_lts_releases", "*");
        Map<Integer, String> dates = releaseDates(releases);
        String selected = Platform.config("java");
        int width = String.valueOf(releases.get(releases.size() - 1)).length();

        int version = ui().ask("Select the JDK version to use.", releases, v -> {
            Directory jdk = locate(v);
            String label = "Java " + padRight(String.valueOf(v), width) + " - " + padRight(date(dates.get(v)), 10) + " " + padRight(lts
                    .contains(v) ? "LTS" : "", 3);

            if (selected != null && jdk.absolutize().path().equalsIgnoreCase(selected)) {
                label += " [selected]";
            } else if (jdk.isPresent()) {
                label += " [installed]";
            }
            return label.stripTrailing();
        });

        Directory dest = locate(version);

        // download and install when the selected version is not installed yet
        if (dest.isAbsent()) {
            String url = "https://api.adoptium.net/v3/binary/latest/" + version + "/ga/" + os() + "/" + arch()
                    + "/jdk/hotspot/normal/eclipse";
            File archive = Locator.temporaryFile(url.substring(url.lastIndexOf('/') + 1));

            ui().info("Downloading the JDK [", version, "] from Adoptium.");
            Loader.download(url, archive);
            unpack(archive, dest, Option::strip);
            ui().info("Installed the JDK [", version, "] at ", dest);
        }

        if (!Platform.canRun(dest)) {
            ui().warn("The JDK [", dest, "] may be too old to run Bee.");
        }

        Platform.config("java", dest.absolutize().path());
        ui().info("Bee will use the JDK [", version, "] from the next invocation.");
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
     * Format the specified date as <code>yyyy/MM/dd</code>.
     * 
     * @param date A date in the ISO format, or <code>null</code>.
     * @return A formatted date.
     */
    private String date(String date) {
        return date == null ? "" : date.replace('-', '/');
    }

    /**
     * Resolve the initial GA release date of each version.
     * 
     * @param releases A list of versions.
     * @return A map from version to release date.
     */
    private Map<Integer, String> releaseDates(List<Integer> releases) {
        Map<Integer, String> dates = new HashMap();

        I.signal(releases)
                .flatMap(version -> I.http(releaseUrl(version), JSON.class).map(json -> I.pair(version, json)))
                .waitForTerminate()
                .skipError()
                .to(pair -> {
                    String timestamp = pair.ⅱ.get("0").text("timestamp");
                    if (timestamp != null && 10 <= timestamp.length()) {
                        dates.put(pair.ⅰ, timestamp.substring(0, 10));
                    }
                });

        return dates;
    }

    /**
     * Build the URL of the initial GA release of the specified version.
     * 
     * @param version A JDK version.
     * @return A release API URL.
     */
    private String releaseUrl(int version) {
        return "https://api.adoptium.net/v3/assets/feature_releases/" + version + "/ga?architecture=" + arch()
                + "&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=" + os()
                + "&page=0&page_size=1&project=jdk&vendor=eclipse&sort_order=ASC";
    }

    /**
     * Locate the installation directory of the specified version.
     * 
     * @param version A JDK version.
     * @return An installation directory.
     */
    private Directory locate(int version) {
        return Platform.BeeHome.directory("jdk").directory("temurin-" + version);
    }

    /**
     * Resolve the Adoptium OS name.
     * 
     * @return An OS name.
     */
    private String os() {
        return Platform.isWindows() ? "windows" : Platform.isMac() ? "mac" : "linux";
    }

    /**
     * Resolve the Adoptium architecture name.
     * 
     * @return An architecture name.
     */
    private String arch() {
        String arch = Platform.OSArch;
        return arch.contains("aarch64") || arch.contains("arm") ? "aarch64" : "x64";
    }
}
