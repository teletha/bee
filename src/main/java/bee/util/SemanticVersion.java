/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A semantic version which consists of a major, a minor and a patch number.
 */
public class SemanticVersion implements Comparable<SemanticVersion> {

    /** The pattern of a version. A leading v prefix and a pre-release suffix are tolerated. */
    private static final Pattern VERSION = Pattern.compile("v?(\\d+)(\\.(\\d+))?(\\.(\\d+))?(?:[-+].*)?");

    /** The kind of a version bump. The declaration order defines the priority. */
    public enum Bump {
        NONE, PATCH, MINOR, MAJOR;

        /**
         * Select the higher bump.
         * 
         * @param other Another bump.
         * @return The higher bump.
         */
        public Bump max(Bump other) {
            return ordinal() >= other.ordinal() ? this : other;
        }
    }

    /** The major version. */
    public final int major;

    /** The minor version. */
    public final int minor;

    /** The patch version. */
    public final int patch;

    /**
     * Parse a version.
     * 
     * @param version A version such as 1.2.3.
     */
    public SemanticVersion(String version) {
        this(parse(version));
    }

    /**
     * Create a version.
     * 
     * @param major A major version.
     * @param minor A minor version.
     * @param patch A patch version.
     */
    public SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Create a version from an array of at least one number.
     * 
     * @param numbers A major, minor and patch number.
     */
    private SemanticVersion(int... numbers) {
        this(numbers[0], numbers.length < 2 ? 0 : numbers[1], numbers.length < 3 ? 0 : numbers[2]);
    }

    /**
     * Test whether the specified text is a version.
     * 
     * @param text A text.
     * @return A result.
     */
    public static boolean isVersion(String text) {
        return text != null && VERSION.matcher(text.trim()).matches();
    }

    /**
     * Parse a version text into numbers.
     * 
     * @param version A version text.
     * @return A number array.
     */
    private static int[] parse(String version) {
        String text = version == null ? "" : version.trim();
        if (!VERSION.matcher(text).matches()) {
            throw new IllegalArgumentException("Invalid version : " + version);
        }

        // Cut the pre-release and build metadata away and keep the numeric components.
        int hyphen = text.indexOf('-');
        if (hyphen != -1) {
            text = text.substring(0, hyphen);
        }
        int plus = text.indexOf('+');
        if (plus != -1) {
            text = text.substring(0, plus);
        }
        if (text.startsWith("v")) {
            text = text.substring(1);
        }

        String[] parts = text.split("\\.");
        return new int[] {Integer.parseInt(parts[0]), parts.length < 2 ? 0 : Integer.parseInt(parts[1]), parts.length < 3 ? 0 : Integer.parseInt(parts[2])};
    }

    /**
     * Bump this version.
     * 
     * @param bump A bump kind.
     * @return A bumped version.
     */
    public SemanticVersion bump(Bump bump) {
        switch (bump) {
        case MAJOR:
            return new SemanticVersion(major + 1, 0, 0);

        case MINOR:
            return new SemanticVersion(major, minor + 1, 0);

        case PATCH:
            return new SemanticVersion(major, minor, patch + 1);

        default:
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) {
            result = Integer.compare(minor, other.minor);
        }
        if (result == 0) {
            result = Integer.compare(patch, other.patch);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof SemanticVersion version && compareTo(version) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
