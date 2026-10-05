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

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import bee.util.SemanticVersion.Bump;
import psychopath.Directory;
import psychopath.Locator;

class GitTest {

    @Test
    void basic() {
        Assumptions.assumeTrue(Git.isAvailable(), "git is not available");

        Directory directory = Locator.temporaryDirectory();
        Git git = Git.at(directory);

        git.init();
        git.run("config", "user.email", "test@example.com");
        git.run("config", "user.name", "Test");
        directory.file("a.txt").text("hello");
        git.add("a.txt").commit("feat: add a file");
        git.tag("1.2.0");

        assert git.isClean();
        assert !git.branch().isBlank();
        assert git.tags().contains("1.2.0");
        assert git.latestVersionTag().equals("1.2.0");
        assert git.hasTag("1.2.0");
        assert !git.hasTag("9.9.9");

        List<ConventionalCommit> commits = git.conventionalCommits(null);
        assert commits.size() == 1;
        assert commits.get(0).type.equals("feat");
        assert commits.get(0).bump() == Bump.MINOR;

        // A commit after the tag is collected from the tag exclusively.
        directory.file("b.txt").text("world");
        git.add("b.txt").commit("fix: fix a bug");

        List<ConventionalCommit> since = git.conventionalCommits("1.2.0");
        assert since.size() == 1;
        assert since.get(0).type.equals("fix");
        assert since.get(0).bump() == Bump.PATCH;
    }

    @Test
    void latestVersionTagIgnoresGitSortOrder() {
        Assumptions.assumeTrue(Git.isAvailable(), "git is not available");

        Directory directory = Locator.temporaryDirectory();
        Git git = Git.at(directory);

        git.init();
        git.run("config", "user.email", "test@example.com");
        git.run("config", "user.name", "Test");
        directory.file("a.txt").text("hello");
        git.add("a.txt").commit("feat: a");

        // The repository has mixed tag conventions, and the git refname sort returns the v
        // prefixed tag first, so the highest version must be selected here.
        git.tag("v0.64.0");
        git.tag("v0.59.0");
        git.tag("0.65.0");
        git.tag("0.79.1");

        assert git.latestVersionTag().equals("0.79.1");
    }
}
