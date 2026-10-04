/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * The Java versions known by Bee. Unlike {@link javax.lang.model.SourceVersion}, which can not describe
 * a version newer than the JDK which compiles the project, this enum declares every Java version so a
 * project may target a newer Java version than the one used by the IDE or the build. Each entry carries
 * the Long Term Support flag, the release date and whether the version is only available as an Early
 * Access build.
 * <p>
 * This file is generated from the Adoptium (Temurin) REST API by the {@code JavaVersionTask}. Do not edit
 * it by hand, run {@code bee JavaVersionTask:generate} instead.
 */
public enum JavaVersion {

    /** Java 11 */
    JAVA_11(11, true, "2021-08-01", false),

    /** Java 16 */
    JAVA_16(16, false, "2021-07-30", false),

    /** Java 17 */
    JAVA_17(17, true, "2021-09-22", false),

    /** Java 18 */
    JAVA_18(18, false, "2022-03-24", false),

    /** Java 19 */
    JAVA_19(19, false, "2022-09-26", false),

    /** Java 20 */
    JAVA_20(20, false, "2023-03-23", false),

    /** Java 21 */
    JAVA_21(21, true, "2023-10-10", false),

    /** Java 22 */
    JAVA_22(22, false, "2024-03-20", false),

    /** Java 23 */
    JAVA_23(23, false, "2024-09-18", false),

    /** Java 24 */
    JAVA_24(24, false, "2025-03-20", false),

    /** Java 25 */
    JAVA_25(25, true, "2025-09-17", false),

    /** Java 26 */
    JAVA_26(26, false, "2026-03-23", false),

    /** Java 27 */
    JAVA_27(27, false, "2026-09-21", false),

    /** Java 28 (Early Access) */
    JAVA_28(28, false, "2026-06-16", true);

    /** The feature version. */
    public final int feature;

    /** Whether this is a Long Term Support release or not. */
    public final boolean lts;

    /** The initial GA release date, or the first Early Access build date. */
    public final LocalDate release;

    /** Whether this version is only available as an Early Access build or not. */
    public final boolean earlyAccess;

    /**
     * Java version definition.
     * 
     * @param feature A feature version.
     * @param lts Whether this is a Long Term Support release.
     * @param release A release date in the ISO format.
     * @param earlyAccess Whether this version is only available as an Early Access build.
     */
    private JavaVersion(int feature, boolean lts, String release, boolean earlyAccess) {
        this.feature = feature;
        this.lts = lts;
        this.release = LocalDate.parse(release);
        this.earlyAccess = earlyAccess;
    }

    /**
     * Format the release date as <code>yyyy/MM/dd</code>.
     * 
     * @return A formatted release date.
     */
    public String getReleaseDate() {
        return release.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * The display name such as <code>Java 21</code>.
     * 
     * @return A display name.
     */
    public String getDisplayName() {
        return "Java " + feature;
    }

    /**
     * The Adoptium release type of this version, either <code>ga</code> or <code>ea</code>.
     * 
     * @return A release type.
     */
    public String getReleaseType() {
        return earlyAccess ? "ea" : "ga";
    }

    /**
     * Resolve the version from a feature version.
     * 
     * @param feature A feature version.
     * @return A Java version, or <code>null</code> when it is unknown.
     */
    public static JavaVersion of(int feature) {
        for (JavaVersion version : values()) {
            if (version.feature == feature) {
                return version;
            }
        }
        return null;
    }

    /**
     * Resolve the version from a text such as <code>21</code> or <code>Java 25</code>.
     * 
     * @param text A version text.
     * @return A Java version, or <code>null</code> when it can not be resolved.
     */
    public static JavaVersion parse(String text) {
        if (text == null) {
            return null;
        }

        String value = text.trim().replaceAll("(?i)java\\s*", "").replaceAll("(?i)-?(ea|ga)$", "");
        try {
            return of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * The latest Java version known by Bee.
     * 
     * @return A latest Java version.
     */
    public static JavaVersion latest() {
        return values()[values().length - 1];
    }

    /**
     * The Java version of the JDK which is running this process.
     * 
     * @return A current Java version.
     */
    public static JavaVersion current() {
        return of(Runtime.version().feature());
    }

    /**
     * All the Long Term Support versions.
     * 
     * @return A list of LTS versions.
     */
    public static JavaVersion[] lts() {
        return Arrays.stream(values()).filter(version -> version.lts).toArray(JavaVersion[]::new);
    }

    /**
     * All the Early Access versions.
     * 
     * @return A list of Early Access versions.
     */
    public static JavaVersion[] earlyAccess() {
        return Arrays.stream(values()).filter(version -> version.earlyAccess).toArray(JavaVersion[]::new);
    }
}
