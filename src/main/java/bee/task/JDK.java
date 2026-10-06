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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bee.BeeInstaller;
import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.api.Loader;
import bee.util.Process;
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

    /** The directory name which links to the selected JDK. */
    String SELECTED_LINK = "selected";

    /**
     * Select the JDK used by Bee. All the versions available on Adoptium are listed, together with
     * the early access build. The selected JDK is exposed through a stable link, so the configured
     * path does not change when another version is selected. The selected one is downloaded,
     * installed and configured as the JDK used by Bee.
     */
    @Command(defaults = true, value = "Select, install and use a JDK.")
    default void select() {
        JSON info;
        try {
            info = I.json(AVAILABLE);
        } catch (Throwable e) {
            throw new Fail("Failed to access the Adoptium API.").reason(Fail.strip(e));
        }

        // The tip version is the early access build which the GA releases do not contain yet.
        int tip = info.has("tip_version") ? Integer.parseInt(info.text("tip_version")) : -1;

        List<Integer> releases = new ArrayList<>(info.find(int.class, "available_releases", "*"));
        if (0 < tip && !releases.contains(tip)) {
            releases.add(tip);
        }
        releases.sort(Comparator.reverseOrder());

        List<Integer> lts = info.find(int.class, "available_lts_releases", "*");
        Map<Integer, String> dates = releaseDates(releases, tip);
        String selected = Platform.config("java");
        int width = String.valueOf(releases.get(0)).length();

        int version = ui().ask("Select the JDK version to use.", releases, v -> {
            boolean earlyAccess = v == tip;
            Directory jdk = locate(v, earlyAccess);
            String status = earlyAccess ? "EarlyAccess" : lts.contains(v) ? "LTS" : "";
            String label = "Java " + padRight(String.valueOf(v), width) + " - " + padRight(date(dates.get(v)), 10) + " " + status;

            if (isSelected(jdk, selected)) {
                label += " [selected]";
            }
            return label.stripTrailing();
        });

        // Bee can not run on a JDK older than the one it requires, so such a selection is replaced
        // by the minimum supported version.
        if (version < Platform.RequiredJava) {
            ui().warn("Java [", version, "] is not supported by Bee. Using Java [", Platform.RequiredJava, "] instead.");
            version = Platform.RequiredJava;
        }

        use(version, version == tip);
    }

    /**
     * Select, install and use the latest general availability JDK. The tip version points to the
     * early access build, so it is excluded because it is not a stable release. When the latest
     * version is already selected nothing changes, and when another version is selected the user
     * is asked before the update.
     */
    @Command("Select, install and use the latest JDK.")
    default void update() {
        int latest = Math.max(latestVersion(), Platform.RequiredJava);
        int current = selectedVersion();

        if (current == latest) {
            ui().info("The latest JDK [", latest, "] is already selected.");
            return;
        }
        if (current > latest) {
            ui().info("Java [", current, "] is newer than the latest general availability [", latest, "].");
            return;
        }

        String question = current < 0 ? "Java [" + latest + "] is available. Install"
                : "Java [" + latest + "] is available. Update from [" + current + "]";
        if (!ui().confirm(question)) {
            ui().info("Keeping the current JDK.");
            return;
        }

        use(latest, false);
    }

    /**
     * Resolve the latest general availability JDK version. The tip version points to the early
     * access build, so it is not included.
     * 
     * @return A JDK version.
     */
    private int latestVersion() {
        try {
            JSON info = I.json(AVAILABLE);

            // The available releases list the general availability versions, so the maximum is the
            // latest one which is not an early access build.
            List<Integer> releases = new ArrayList<>(info.find(int.class, "available_releases", "*"));
            return releases.stream().mapToInt(Integer::intValue).max().orElse(Platform.RequiredJava);
        } catch (Throwable e) {
            throw new Fail("Failed to access the Adoptium API.").reason(Fail.strip(e));
        }
    }

    /**
     * Resolve the major version of the selected JDK.
     * 
     * @return A major version, or a negative value when no JDK is selected.
     */
    private int selectedVersion() {
        File release = selectedDirectory().file("release");

        if (release.isAbsent()) {
            return -1;
        }

        for (String line : release.text().split("\\R")) {
            if (line.startsWith("JAVA_VERSION=")) {
                String value = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                int dot = value.indexOf('.');
                try {
                    return Integer.parseInt(dot < 0 ? value : value.substring(0, dot));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Install the specified JDK, link it as the selected one and configure Bee to use it from the
     * next invocation.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether the version is an early access build.
     */
    private void use(int version, boolean earlyAccess) {
        Directory installed = install(version, earlyAccess);
        Directory dest = linkSelected(installed);

        // The configuration uses the stable link, but the launcher must not depend on it, so the
        // launcher is rewritten with the real JDK path.
        Platform.config("java", dest.absolutize().path());
        BeeInstaller.writeLauncher(BeeInstaller.executorJar(), installed);

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
        return install(version, false);
    }

    /**
     * Ensure the specified JDK version is installed under the Bee home. The JDK is downloaded from
     * Adoptium (Temurin) when it is missing. An early access version is downloaded from the early
     * access channel instead of the general availability one.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether the version is an early access build.
     * @return The installation directory.
     */
    static Directory install(int version, boolean earlyAccess) {
        Directory dest = locate(version, earlyAccess);

        if (!isInstalled(dest)) {
            // A previous download or extraction may have been interrupted, leaving a directory
            // which is not a usable JDK. Such a directory is removed and reinstalled, otherwise the
            // incomplete install would be linked and never repaired.
            if (dest.isPresent()) {
                dest.delete();
            }
            dest.create();

            String url = "https://api.adoptium.net/v3/binary/latest/" + version + "/" + (earlyAccess ? "ea" : "ga") + "/" + os() + "/" + arch()
                    + "/jdk/hotspot/normal/eclipse";
            File archive = Locator.temporaryFile(url.substring(url.lastIndexOf('/') + 1));

            ui().info("Downloading the JDK [", version, earlyAccess ? " EarlyAccess" : "", "] from Adoptium.");
            Loader.download(url, archive);
            unpack(archive, dest, Option::strip);

            if (!isInstalled(dest)) {
                throw new Fail("Failed to install the JDK [" + version + "] at " + dest + ".");
            }
            ui().info("Installed the JDK [", version, "] at ", dest);
        }
        return dest;
    }

    /**
     * Test whether the specified directory contains a complete JDK installation. A missing
     * executable or release file indicates an interrupted download or extraction.
     * 
     * @param jdk An installation directory.
     * @return A result.
     */
    private static boolean isInstalled(Directory jdk) {
        return jdk.file("release").isPresent() && jdk.file(Platform.isWindows() ? "bin/java.exe" : "bin/java").isPresent();
    }

    /**
     * Point the selected link to the specified JDK. The link is a junction on Windows, because it
     * can be created without the administrator privilege, and a symbolic link on the other
     * platforms.
     * 
     * @param target The installed JDK directory.
     * @return The link directory.
     */
    static Directory linkSelected(Directory target) {
        Directory link = selectedDirectory().absolutize();
        target = target.absolutize();

        if (!isInstalled(target)) {
            throw new Fail("The JDK [" + target + "] is not installed correctly.");
        }

        unlink(link);
        link.parent().create();

        try {
            if (Platform.isWindows()) {
                if (Process.with().ignoreOutput().run("cmd", "/c", "mklink", "/J", nativePath(link), nativePath(target)) != 0) {
                    throw new Error("mklink /J failed");
                }
            } else {
                Files.createSymbolicLink(link.asJavaPath(), target.asJavaPath());
            }
            ui().info("Linked the selected JDK [", link, "] to ", target);
        } catch (Throwable e) {
            throw new Fail("Failed to link the selected JDK [" + link + "] to [" + target + "].").reason(Fail.strip(e));
        }
        return link;
    }

    /**
     * Remove the existing link. A junction is removed by the system command, because a recursive
     * delete would follow it and remove the target contents.
     * 
     * @param link A link directory.
     */
    private static void unlink(Directory link) {
        try {
            if (Platform.isWindows()) {
                // The system command removes a junction without touching the target contents, and it
                // does nothing when the link is absent.
                Process.with().ignoreOutput().run("cmd", "/c", "rmdir", nativePath(link));
            } else {
                Files.deleteIfExists(link.asJavaPath());
            }
        } catch (Throwable e) {
            throw new Fail("Failed to remove the existing link [" + link + "].").reason(Fail.strip(e));
        }
    }

    /**
     * Resolve the specified directory in the native form. A Windows system command treats a forward
     * slash as a switch, so the path must use the platform separator.
     * 
     * @param directory A directory.
     * @return A native path.
     */
    private static String nativePath(Directory directory) {
        String path = directory.absolutize().path();
        return Platform.isWindows() ? path.replace('/', '\\') : path;
    }

    /**
     * Locate the directory which links to the selected JDK.
     * 
     * @return A link directory.
     */
    private static Directory selectedDirectory() {
        return Platform.BeeHome.directory("jdk").directory(SELECTED_LINK);
    }

    /**
     * Test whether the specified JDK is the configured one. The configured value is usually the
     * selected link, so the real paths are compared when the textual comparison does not match.
     * 
     * @param jdk A JDK directory.
     * @param configured A configured value, or <code>null</code>.
     * @return A result.
     */
    private static boolean isSelected(Directory jdk, String configured) {
        if (configured == null) {
            return false;
        }
        if (configured.equalsIgnoreCase(jdk.absolutize().path())) {
            return true;
        }
        try {
            return Locator.directory(configured).asJavaPath().toRealPath().equals(jdk.asJavaPath().toRealPath());
        } catch (Exception e) {
            return false;
        }
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
     * Resolve the initial release date of each version. The early access version is resolved from
     * the early access channel.
     * 
     * @param releases A list of versions.
     * @param tip The early access version, or a negative value when it is unknown.
     * @return A map from version to release date.
     */
    private Map<Integer, String> releaseDates(List<Integer> releases, int tip) {
        Map<Integer, String> dates = new HashMap();

        I.signal(releases)
                .flatMap(version -> I.http(releaseUrl(version, version == tip), JSON.class).map(json -> I.pair(version, json)))
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
     * @param earlyAccess Whether the version is an early access build.
     * @return A release API URL.
     */
    private String releaseUrl(int version, boolean earlyAccess) {
        return "https://api.adoptium.net/v3/assets/feature_releases/" + version + "/" + (earlyAccess ? "ea" : "ga")
                + "?architecture=" + arch()
                + "&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=" + os()
                + "&page=0&page_size=1&project=jdk&vendor=eclipse&sort_order=ASC";
    }

    /**
     * Locate the installation directory of the specified version.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether the version is an early access build.
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
