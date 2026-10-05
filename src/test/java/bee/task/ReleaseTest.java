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

import java.util.List;

import org.junit.jupiter.api.Test;

import bee.AbstractTaskTest;
import bee.Task;
import bee.util.ConventionalCommit;
import bee.util.SemanticVersion;
import bee.util.SemanticVersion.Bump;

class ReleaseTest extends AbstractTaskTest {

    @Test
    void bump() {
        Release release = Task.by(Release.class);
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("feat: x"))) == Bump.MINOR;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("fix: x"))) == Bump.PATCH;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("deps: x"))) == Bump.PATCH;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("chore: x"))) == Bump.NONE;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("feat!: x"))) == Bump.MAJOR;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("wip"))) == Bump.NONE;
    }

    @Test
    void bumpOfMultipleCommits() {
        Release release = Task.by(Release.class);
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("fix: x"), commit("feat: y"))) == Bump.MINOR;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("fix: x"), commit("docs: y"))) == Bump.PATCH;
    }

    @Test
    void breakingOnZeroMajorBumpsMinor() {
        Release release = Task.by(Release.class);
        assert release.determineBump(new SemanticVersion("0.2.3"), List.of(commit("feat!: x"))) == Bump.MINOR;
        assert release.determineBump(new SemanticVersion("1.2.3"), List.of(commit("feat!: x"))) == Bump.MAJOR;
    }

    /**
     * Create a commit.
     * 
     * @param subject A subject.
     * @return A commit.
     */
    private ConventionalCommit commit(String subject) {
        return new ConventionalCommit("0000000", subject, "");
    }
}
