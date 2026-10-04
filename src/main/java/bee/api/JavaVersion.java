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
 * The released Java versions. Unlike {@link javax.lang.model.SourceVersion}, which can not describe a
 * version newer than the JDK which compiles the project, this enum declares every Java version so a
 * project may target a newer Java version than the one used by the IDE or the build. Each entry also
 * carries the Long Term Support flag and the initial GA release date.
 * <p>
 * This file is generated from the Adoptium (Temurin) REST API by the {@code JavaVersionTask}. Do not edit
 * it by hand, run {@code bee JavaVersionTask:generate} instead.
 */
public enum JavaVersion {

    /** Java 1.0 */
    JAVA_1(1, false, "1996-01-23"),

    /** Java 1.1 */
    JAVA_2(2, false, "1997-02-19"),

    /** Java 1.2 */
    JAVA_3(3, false, "1998-12-08"),

    /** Java 1.3 */
    JAVA_4(4, false, "2000-05-08"),

    /** Java 1.4 */
    JAVA_5(5, false, "2004-09-30"),

    /** Java 1.5 */
    JAVA_6(6, false, "2006-12-11"),

    /** Java 1.6 */
    JAVA_7(7, false, "2011-07-28"),

    /** Java 8 */
    JAVA_8(8, true, "2021-07-29"),

    /** Java 11 */
    JAVA_11(11, true, "2021-08-01"),

    /** Java 16 */
    JAVA_16(16, false, "2021-07-30"),

    /** Java 17 */
    JAVA_17(17, true, "2021-09-22"),

    /** Java 18 */
    JAVA_18(18, false, "2022-03-24"),

    /** Java 19 */
    JAVA_19(19, false, "2022-09-26"),

    /** Java 20 */
    JAVA_20(20, false, "2023-03-23"),

    /** Java 21 */
    JAVA_21(21, true, "2023-10-10"),

    /** Java 22 */
    JAVA_22(22, false, "2024-03-20"),

    /** Java 23 */
    JAVA_23(23, false, "2024-09-18"),

    /** Java 24 */
    JAVA_24(24, false, "2025-03-20"),

    /** Java 25 */
    JAVA_25(25, true, "2025-09-17"),

    /** Java 26 */
    JAVA_26(26, false, "2026-03-23"),

    /** Java 27 */
    JAVA_27(27, false, "2026-09-21");

    /** The feature version. */
    public final int feature;

    /** Whether this is a Long Term Support release or not. */
    public final boolean lts;

    /** The initial GA release date. */
    public final LocalDate release;

    /**
     * Java version definition.
     * 
     * @param feature A feature version.
     * @param lts Whether this is a Long Term Support release.
     * @param release An initial GA release date in the ISO format.
     */
    private JavaVersion(int feature, boolean lts, String release) {
        this.feature = feature;
        this.lts = lts;
        this.release = LocalDate.parse(release);
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
     * Resolve the version from a text such as <code>21</code>, <code>1.8</code> or <code>Java 21</code>.
     * 
     * @param text A version text.
     * @return A Java version, or <code>null</code> when it can not be resolved.
     */
    public static JavaVersion parse(String text) {
        if (text == null) {
            return null;
        }

        String value = text.trim().replaceAll("(?i)java\\s*", "").replace("1.", "");
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
}
