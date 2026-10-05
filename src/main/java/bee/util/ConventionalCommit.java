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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bee.util.SemanticVersion.Bump;

/**
 * A <a href="https://www.conventionalcommits.org/">Conventional Commit</a> parsed from a git log.
 */
public class ConventionalCommit {

    /** The pattern of a commit header such as {@code feat(scope)!: subject}. */
    private static final Pattern HEADER = Pattern.compile("^(?<type>[A-Za-z]+)(?:\\((?<scope>[^)]*)\\))?(?<breaking>!)?:\\s*(?<subject>.*)$");

    /** The full commit hash. */
    public final String hash;

    /** The commit type such as {@code feat}, or <code>null</code> when the commit is not conventional. */
    public final String type;

    /** The optional scope. */
    public final String scope;

    /** The subject. */
    public final String subject;

    /** Whether the commit contains a breaking change. */
    public final boolean breaking;

    /** The raw header. */
    private final String header;

    /**
     * Parse a conventional commit.
     * 
     * @param hash A full commit hash.
     * @param subject A commit subject.
     * @param body A commit body.
     */
    public ConventionalCommit(String hash, String subject, String body) {
        this.hash = hash;
        this.header = subject == null ? "" : subject.trim();

        Matcher matcher = HEADER.matcher(header);
        if (matcher.matches()) {
            this.type = matcher.group("type").toLowerCase();
            String scope = matcher.group("scope");
            this.scope = scope == null || scope.isBlank() ? null : scope.trim();
            this.subject = matcher.group("subject").trim();
            this.breaking = matcher.group("breaking") != null || breaking(body);
        } else {
            this.type = null;
            this.scope = null;
            this.subject = header;
            this.breaking = false;
        }
    }

    /**
     * Test whether the body declares a breaking change.
     * 
     * @param body A commit body.
     * @return A result.
     */
    private static boolean breaking(String body) {
        if (body == null) {
            return false;
        }
        for (String line : body.split("\\R")) {
            String text = line.trim().toUpperCase();
            if (text.startsWith("BREAKING CHANGE:") || text.startsWith("BREAKING-CHANGE:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the version bump of this commit.
     * 
     * @return A bump.
     */
    public Bump bump() {
        if (type == null) {
            return Bump.NONE;
        }
        if (breaking) {
            return Bump.MAJOR;
        }

        switch (type) {
        case "feat":
            return Bump.MINOR;

        case "fix":
        case "deps":
        case "perf":
        case "revert":
            return Bump.PATCH;

        default:
            return Bump.NONE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String shortHash = hash == null || hash.length() < 7 ? hash : hash.substring(0, 7);
        return shortHash + " " + header;
    }
}
