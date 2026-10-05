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

import org.junit.jupiter.api.Test;

import bee.util.SemanticVersion.Bump;

class ConventionalCommitTest {

    @Test
    void feat() {
        ConventionalCommit commit = new ConventionalCommit("abcdefg", "feat: add a feature", "");
        assert commit.type.equals("feat");
        assert commit.scope == null;
        assert commit.subject.equals("add a feature");
        assert commit.bump() == Bump.MINOR;
    }

    @Test
    void scope() {
        ConventionalCommit commit = new ConventionalCommit("abcdefg", "fix(api): fix a bug", "");
        assert commit.type.equals("fix");
        assert commit.scope.equals("api");
        assert commit.bump() == Bump.PATCH;
    }

    @Test
    void breakingMark() {
        ConventionalCommit commit = new ConventionalCommit("abcdefg", "feat(api)!: change the api", "");
        assert commit.breaking;
        assert commit.bump() == Bump.MAJOR;
    }

    @Test
    void breakingFooter() {
        ConventionalCommit commit = new ConventionalCommit("abcdefg", "fix: fix a bug", "BREAKING CHANGE: the api changed");
        assert commit.breaking;
        assert commit.bump() == Bump.MAJOR;
    }

    @Test
    void patchTypes() {
        for (String type : new String[] {"fix", "deps", "perf", "revert"}) {
            ConventionalCommit commit = new ConventionalCommit("abcdefg", type + ": a change", "");
            assert commit.bump() == Bump.PATCH;
        }
    }

    @Test
    void nonReleasableTypes() {
        for (String type : new String[] {"chore", "docs", "style", "refactor", "test", "build", "ci"}) {
            ConventionalCommit commit = new ConventionalCommit("abcdefg", type + ": a change", "");
            assert commit.bump() == Bump.NONE;
        }
    }

    @Test
    void nonConventional() {
        ConventionalCommit commit = new ConventionalCommit("abcdefg", "wip", "");
        assert commit.type == null;
        assert commit.subject.equals("wip");
        assert commit.bump() == Bump.NONE;
    }
}
